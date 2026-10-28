//! Runs a scenario against one server (`record`, `replay`) or two (`diff`).

use crate::auth;
use crate::cassette::{Cassette, Meta};
use crate::compare::{compare_requests, compare_responses, Mismatch};
use crate::http::HttpClient;
use crate::step::{capture, resolve, State, Step};
use anyhow::{Context, Result};
use std::fmt;
use std::path::Path;

/// The scenario: its steps and the password of the admin it creates (forged tokens are signed
/// with that password's stored hash).
#[derive(Debug, Clone)]
pub struct Scenario {
    pub steps: Vec<Step>,
    pub admin_password: String,
}

impl Scenario {
    pub fn state(&self) -> State {
        State::new(&auth::stored_hash(&self.admin_password))
    }
}

#[derive(Debug, Clone, Default)]
pub struct Options {
    /// No per-step progress on stderr.
    pub quiet: bool,
    /// The fixed clock the server(s) run with, written to the cassette set's `_meta.json`.
    pub fixed_now: Option<String>,
}

#[derive(Debug, Clone)]
pub struct StepOutcome {
    pub index: usize,
    pub name: String,
    /// Status from the server (the right one in diff mode).
    pub status: Option<u16>,
    pub mismatches: Vec<Mismatch>,
}

impl StepOutcome {
    pub fn passed(&self) -> bool {
        self.mismatches.is_empty()
    }
}

#[derive(Debug, Clone)]
pub struct Report {
    pub mode: &'static str,
    pub outcomes: Vec<StepOutcome>,
    /// Set when the run stopped early (a capture a later step needs could not be made).
    pub aborted: Option<String>,
}

impl Report {
    fn new(mode: &'static str) -> Report {
        Report {
            mode,
            outcomes: Vec::new(),
            aborted: None,
        }
    }

    pub fn failed(&self) -> impl Iterator<Item = &StepOutcome> {
        self.outcomes.iter().filter(|o| !o.passed())
    }

    pub fn is_clean(&self) -> bool {
        self.aborted.is_none() && self.outcomes.iter().all(StepOutcome::passed)
    }
}

impl fmt::Display for Report {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let failed: Vec<&StepOutcome> = self.failed().collect();
        for outcome in &failed {
            writeln!(
                f,
                "### {:03}-{} ({}): {} mismatch{}",
                outcome.index,
                outcome.name,
                outcome
                    .status
                    .map(|s| s.to_string())
                    .unwrap_or_else(|| "no response".into()),
                outcome.mismatches.len(),
                if outcome.mismatches.len() == 1 {
                    ""
                } else {
                    "es"
                }
            )?;
            for m in &outcome.mismatches {
                for line in m.to_string().lines() {
                    writeln!(f, "    {line}")?;
                }
            }
        }
        write!(
            f,
            "{}: {} steps, {} with mismatches",
            self.mode,
            self.outcomes.len(),
            failed.len()
        )?;
        if let Some(reason) = &self.aborted {
            write!(f, "; aborted: {reason}")?;
        }
        Ok(())
    }
}

fn progress(options: &Options, index: usize, name: &str, detail: &str) {
    if !options.quiet {
        eprintln!("{index:03} {name:<40} {detail}");
    }
}

/// Runs the scenario against `base_url` and writes one cassette per step into `out_dir`, plus `_meta.json`.
///
/// # Errors
///
/// Returns `Err` when the client cannot be built, a step cannot be resolved or sent, or a file cannot be written.
pub async fn record(
    base_url: &str,
    out_dir: &Path,
    scenario: &Scenario,
    options: &Options,
) -> Result<Report> {
    let client = HttpClient::new(base_url)?;
    let mut state = scenario.state();
    let mut report = Report::new("record");
    for (i, step) in scenario.steps.iter().enumerate() {
        let index = i + 1;
        let request =
            resolve(step, &state).with_context(|| format!("step {index:03}-{}", step.name))?;
        let response = client
            .send(&request)
            .await
            .with_context(|| format!("step {index:03}-{}", step.name))?;
        progress(options, index, &step.name, &response.status.to_string());
        let cassette = Cassette {
            index,
            name: step.name.clone(),
            auth: step.auth.clone(),
            compare: step.compare,
            volatile: step.volatile.clone(),
            request: request.recorded.clone(),
            response: response.clone(),
        };
        cassette.write(out_dir)?;
        let outcome = StepOutcome {
            index,
            name: step.name.clone(),
            status: Some(response.status),
            mismatches: Vec::new(),
        };
        report.outcomes.push(outcome);
        if let Err(e) = capture_from(step, &response, &mut state) {
            report.aborted = Some(format!("{e:#}"));
            break;
        }
    }
    Meta {
        base_url: client.base_url().to_string(),
        recorded_at: jiff::Timestamp::now().to_string(),
        fixed_now: options.fixed_now.clone(),
        steps: report.outcomes.len(),
    }
    .write(out_dir)?;
    Ok(report)
}

/// Runs the scenario against `base_url` and compares every response to its cassette.
///
/// Mismatches are reported per step in the [`Report`], not as errors.
///
/// # Errors
///
/// Returns `Err` when the client cannot be built, `_meta.json` cannot be read, or a step cannot be resolved or
/// sent.
pub async fn replay(
    base_url: &str,
    cassette_dir: &Path,
    scenario: &Scenario,
    options: &Options,
) -> Result<Report> {
    let client = HttpClient::new(base_url)?;
    let mut state = scenario.state();
    let mut report = Report::new("replay");
    if let Some(meta) = Meta::read(cassette_dir)? {
        if meta.steps != scenario.steps.len() && !options.quiet {
            eprintln!(
                "note: the cassettes hold {} steps, the scenario has {}",
                meta.steps,
                scenario.steps.len()
            );
        }
        if let Some(now) = &meta.fixed_now {
            if !options.quiet {
                eprintln!("note: cassettes recorded with the fixed clock {now}; the server under test needs the same");
            }
        }
    }
    for (i, step) in scenario.steps.iter().enumerate() {
        let index = i + 1;
        let mut mismatches = Vec::new();
        let cassette = match Cassette::read_step(cassette_dir, index, &step.name) {
            Ok(c) => c,
            Err(e) => {
                progress(options, index, &step.name, "no cassette");
                report.outcomes.push(StepOutcome {
                    index,
                    name: step.name.clone(),
                    status: None,
                    mismatches: vec![Mismatch::Error(format!("{e:#}"))],
                });
                report.aborted = Some(format!("{e:#}"));
                break;
            }
        };
        let request =
            resolve(step, &state).with_context(|| format!("step {index:03}-{}", step.name))?;
        if let Some(drift) = compare_requests(&cassette.request, &request.recorded) {
            mismatches.push(drift);
        }
        let response = client
            .send(&request)
            .await
            .with_context(|| format!("step {index:03}-{}", step.name))?;
        mismatches.extend(compare_responses(
            cassette.compare,
            &cassette.volatile,
            &cassette.response,
            &response,
        ));
        progress(
            options,
            index,
            &step.name,
            &format!(
                "{} {}",
                response.status,
                if mismatches.is_empty() {
                    "ok"
                } else {
                    "MISMATCH"
                }
            ),
        );
        report.outcomes.push(StepOutcome {
            index,
            name: step.name.clone(),
            status: Some(response.status),
            mismatches,
        });
        if let Err(e) = capture_from(step, &response, &mut state) {
            report.aborted = Some(format!("{e:#}"));
            break;
        }
    }
    Ok(report)
}

/// Runs the scenario against two servers and compares their responses to each other.
///
/// `left` is a running server to diff against and `right` the server under test; differences are reported per
/// step in the [`Report`].
///
/// # Errors
///
/// Returns `Err` when a client cannot be built or a step cannot be resolved or sent to either server.
pub async fn diff(
    left: &str,
    right: &str,
    scenario: &Scenario,
    options: &Options,
) -> Result<Report> {
    let left_client = HttpClient::new(left)?;
    let right_client = HttpClient::new(right)?;
    let mut left_state = scenario.state();
    let mut right_state = scenario.state();
    let mut report = Report::new("diff");
    for (i, step) in scenario.steps.iter().enumerate() {
        let index = i + 1;
        let left_request = resolve(step, &left_state)
            .with_context(|| format!("step {index:03}-{} (left)", step.name))?;
        let left_response = left_client
            .send(&left_request)
            .await
            .with_context(|| format!("step {index:03}-{} (left)", step.name))?;
        let right_request = resolve(step, &right_state)
            .with_context(|| format!("step {index:03}-{} (right)", step.name))?;
        let right_response = right_client
            .send(&right_request)
            .await
            .with_context(|| format!("step {index:03}-{} (right)", step.name))?;
        let mut mismatches = Vec::new();
        if let Some(drift) = compare_requests(&left_request.recorded, &right_request.recorded) {
            // Only differs when the two servers handed out different ids earlier.
            mismatches.push(drift);
        }
        mismatches.extend(compare_responses(
            step.compare,
            &step.volatile,
            &left_response,
            &right_response,
        ));
        progress(
            options,
            index,
            &step.name,
            &format!(
                "{} / {} {}",
                left_response.status,
                right_response.status,
                if mismatches.is_empty() {
                    "ok"
                } else {
                    "MISMATCH"
                }
            ),
        );
        report.outcomes.push(StepOutcome {
            index,
            name: step.name.clone(),
            status: Some(right_response.status),
            mismatches,
        });
        let left_capture = capture_from(step, &left_response, &mut left_state);
        let right_capture = capture_from(step, &right_response, &mut right_state);
        if let Err(e) = left_capture.and(right_capture) {
            report.aborted = Some(format!("{e:#}"));
            break;
        }
    }
    Ok(report)
}

fn capture_from(
    step: &Step,
    response: &crate::cassette::RecordedResponse,
    state: &mut State,
) -> Result<()> {
    let raw: Vec<u8> = match &response.body {
        Some(crate::cassette::RecordedBody::Json(v)) => serde_json::to_vec(v)?,
        Some(crate::cassette::RecordedBody::Text(t)) => t.as_bytes().to_vec(),
        Some(crate::cassette::RecordedBody::Base64(b)) => crate::cassette::base64_decode(b)?,
        Some(crate::cassette::RecordedBody::Multipart(_)) | None => Vec::new(),
    };
    capture(step, response.status, &raw, response.json(), state)
}

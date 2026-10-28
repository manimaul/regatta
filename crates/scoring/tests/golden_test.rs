//! A database-free golden check against the approved files.
//!
//! The approved files (`tests/golden/approved`) are the reference output over the production snapshot with
//! `now` = the dump instant:
//!
//! - every `race-<id>.json` is rebuilt from its own inputs: the cards are recomputed with
//!   [`card`] from each `resultRecord` (with the certificate found on the boat, when it is
//!   there) and compared field by field, and [`report`] is re-run over the file's cards with
//!   their places zeroed and compared byte for byte;
//! - every `standings-<series>-<year>.json` is recomputed with [`standings`] from the race
//!   reports it lists and compared byte for byte.
//!
//! The full golden test (the dump in a container, the server's own data access) is
//! `crates/server/tests/golden.rs`; this one proves the pure scoring given the server's inputs.

use jiff::Timestamp;
use pretty_assertions::assert_eq;
use regatta_common::{to_kotlin_pretty_json, RaceReport, RaceReportCard, StandingsSeries};
use regatta_scoring::{card, report, standings};
use std::path::{Path, PathBuf};

const APPROVED_DIR: &str = "tests/golden/approved";

/// The instant the snapshot was taken.
const DUMP_INSTANT: &str = "2026-10-14T02:10:11Z";

fn approved_dir() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR")).join(APPROVED_DIR)
}

fn approved_files(prefix: &str) -> Vec<PathBuf> {
    let mut files: Vec<PathBuf> = std::fs::read_dir(approved_dir())
        .unwrap_or_else(|e| panic!("{}: {e}", approved_dir().display()))
        .map(|e| e.unwrap().path())
        .filter(|p| {
            let name = p.file_name().unwrap().to_string_lossy();
            name.starts_with(prefix) && name.ends_with(".json")
        })
        .collect();
    files.sort();
    files
}

fn read<T: serde::de::DeserializeOwned>(path: &Path) -> (String, T) {
    let text = std::fs::read_to_string(path).unwrap_or_else(|e| panic!("{}: {e}", path.display()));
    let value = serde_json::from_str(&text).unwrap_or_else(|e| panic!("{}: {e}", path.display()));
    (text, value)
}

fn race_file(id: i64) -> PathBuf {
    approved_dir().join(format!("race-{id}.json"))
}

/// The first differing line of two texts, with its number, for the failure message.
fn first_difference(expected: &str, actual: &str) -> String {
    for (n, (e, a)) in expected.lines().zip(actual.lines()).enumerate() {
        if e != a {
            return format!("line {}:\n  expected: {e}\n  actual:   {a}", n + 1);
        }
    }
    format!(
        "same prefix; expected {} lines, actual {} lines",
        expected.lines().count(),
        actual.lines().count()
    )
}

fn cards_of(report: &RaceReport) -> Vec<&RaceReportCard> {
    report
        .class_reports
        .iter()
        .flat_map(|c| c.bracket_report.iter())
        .flat_map(|b| b.cards.iter())
        .collect()
}

fn unplaced(card: &RaceReportCard) -> RaceReportCard {
    RaceReportCard {
        place_in_bracket: 0,
        place_in_bracket_orc: 0,
        place_in_class: 0,
        place_in_class_orc: 0,
        ..card.clone()
    }
}

/// Every card of every approved race report is what [`card`] computes from its record.
#[test]
fn race_report_cards_are_rebuilt_from_their_records() {
    let files = approved_files("race-");
    assert!(
        !files.is_empty(),
        "no race-*.json under {}",
        approved_dir().display()
    );
    let (mut cards, mut with_cert, mut without_cert, mut orc_tcf_from_file) = (0, 0, 0, 0);
    let mut failures = Vec::new();
    for path in &files {
        let name = path.file_name().unwrap().to_string_lossy().to_string();
        let (_, approved): (String, RaceReport) = read(path);
        for expected in cards_of(&approved) {
            cards += 1;
            let record = &expected.result_record;
            let class_start = approved.class_start(record.bracket.class_id);
            // the certificate is not in the file; the boat carries its certificates, usually the one used
            let cert = record.result.orc_ref.as_deref().and_then(|reference| {
                record
                    .boat_skipper
                    .boat
                    .as_ref()
                    .and_then(|boat| boat.orc_certs.iter().find(|c| c.ref_no == reference))
            });
            let mut actual = card(
                record.clone(),
                class_start,
                &approved.race_schedule.race,
                cert,
            );
            match (&record.result.orc_ref, cert) {
                (None, _) => without_cert += 1,
                (Some(_), Some(_)) => with_cert += 1,
                (Some(_), None) => {
                    // no certificate to compute the ORC side from: take it from the file, check the rest
                    orc_tcf_from_file += 1;
                    actual.orc_tcf = expected.orc_tcf;
                    actual.corrected_orc_time = expected.corrected_orc_time;
                    actual.orc_dxt_id = expected.orc_dxt_id;
                }
            }
            let expected = unplaced(expected);
            if actual != expected {
                failures.push(format!(
                    "{name} {}: {}",
                    expected.boat_name,
                    first_difference(
                        &to_kotlin_pretty_json(&expected).unwrap(),
                        &to_kotlin_pretty_json(&actual).unwrap()
                    )
                ));
            }
        }
    }
    assert!(
        failures.is_empty(),
        "{} cards differ:\n{}",
        failures.len(),
        failures.join("\n")
    );
    println!(
        "{} files, {cards} cards: {without_cert} without an ORC certificate, {with_cert} with the certificate from the boat, {orc_tcf_from_file} with the ORC factor taken from the file",
        files.len()
    );
    assert!(with_cert > 0, "no ORC card had its certificate on the boat");
}

/// Every approved race report is what [`report`] computes from the schedule and its own cards
/// (places zeroed), byte for byte.
#[test]
fn race_reports_are_rebuilt_byte_for_byte() {
    let files = approved_files("race-");
    let mut failures = Vec::new();
    let mut kinds = std::collections::BTreeMap::new();
    for path in &files {
        let name = path.file_name().unwrap().to_string_lossy().to_string();
        let (expected, approved): (String, RaceReport) = read(path);
        for class_report in &approved.class_reports {
            *kinds
                .entry(class_report.race_class.rating_type.name())
                .or_insert(0) += 1;
        }
        let cards: Vec<RaceReportCard> = cards_of(&approved).into_iter().map(unplaced).collect();
        let actual = to_kotlin_pretty_json(&report(approved.race_schedule.clone(), cards)).unwrap();
        if actual != expected {
            failures.push(format!("{name}: {}", first_difference(&expected, &actual)));
        }
    }
    assert!(
        failures.is_empty(),
        "{} of {} race reports differ:\n{}",
        failures.len(),
        files.len(),
        failures.join("\n")
    );
    println!(
        "{} race reports rebuilt; class reports by rating type: {kinds:?}",
        files.len()
    );
}

/// Every approved series standings is what [`standings`] computes from its race reports with
/// `now` = the dump instant, byte for byte.
#[test]
fn series_standings_are_rebuilt_byte_for_byte() {
    let files = approved_files("standings-");
    assert!(
        !files.is_empty(),
        "no standings-*.json under {}",
        approved_dir().display()
    );
    let now: Timestamp = DUMP_INSTANT.parse().unwrap();
    let mut failures = Vec::new();
    let mut race_count = 0;
    for path in &files {
        let name = path.file_name().unwrap().to_string_lossy().to_string();
        let (expected, approved): (String, StandingsSeries) = read(path);
        let reports: Vec<RaceReport> = approved
            .races
            .iter()
            .map(|race| read::<RaceReport>(&race_file(race.id)).1)
            .collect();
        race_count += reports.len();
        let actual = standings(approved.series.clone(), approved.year, &reports, now);
        let actual = to_kotlin_pretty_json(&actual).unwrap();
        if actual != expected {
            failures.push(format!("{name}: {}", first_difference(&expected, &actual)));
        }
    }
    assert!(
        failures.is_empty(),
        "{} of {} standings differ:\n{}",
        failures.len(),
        files.len(),
        failures.join("\n")
    );
    println!(
        "{} standings rebuilt from {race_count} race reports",
        files.len()
    );
}

/// The standings files list every race file (so the standings check covers all 118 reports).
#[test]
fn every_race_report_is_in_a_standings_file() {
    let listed: std::collections::BTreeSet<i64> = approved_files("standings-")
        .iter()
        .flat_map(|path| {
            read::<StandingsSeries>(path)
                .1
                .races
                .iter()
                .map(|r| r.id)
                .collect::<Vec<_>>()
        })
        .collect();
    let all: std::collections::BTreeSet<i64> = approved_files("race-")
        .iter()
        .map(|path| read::<RaceReport>(path).1.race_schedule.race.id)
        .collect();
    let unlisted: Vec<_> = all.difference(&listed).collect();
    println!(
        "{} race reports, {} in a series standings file; not in one: {unlisted:?}",
        all.len(),
        listed.len()
    );
    assert_eq!(listed, all.intersection(&listed).copied().collect());
}

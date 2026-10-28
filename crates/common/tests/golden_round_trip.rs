//! Every approved golden file parses into the typed structs and re-serializes byte for byte.
//!
//! This pins the field order, defaults and number and time formats of every type the reports and standings reach.

use regatta_common::{to_kotlin_pretty_json, RaceReport, StandingsSeries};
use std::path::{Path, PathBuf};

/// The approved golden files, relative to this crate.
const APPROVED_DIR: &str = "../scoring/tests/golden/approved";

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

fn round_trip<T: serde::de::DeserializeOwned + serde::Serialize>(files: &[PathBuf]) -> Vec<String> {
    let mut failures = Vec::new();
    for path in files {
        let name = path.file_name().unwrap().to_string_lossy().to_string();
        let expected = std::fs::read_to_string(path).unwrap();
        let value: T = match serde_json::from_str(&expected) {
            Ok(v) => v,
            Err(e) => {
                failures.push(format!("{name}: does not parse: {e}"));
                continue;
            }
        };
        let actual = to_kotlin_pretty_json(&value).unwrap();
        if actual != expected {
            failures.push(format!("{name}: {}", first_difference(&expected, &actual)));
        }
    }
    failures
}

#[test]
fn race_reports_round_trip_byte_for_byte() {
    let files = approved_files("race-");
    assert!(
        !files.is_empty(),
        "no race-*.json under {}",
        approved_dir().display()
    );
    let failures = round_trip::<RaceReport>(&files);
    assert!(
        failures.is_empty(),
        "{} of {} race reports differ:\n{}",
        failures.len(),
        files.len(),
        failures.join("\n")
    );
}

#[test]
fn standings_round_trip_byte_for_byte() {
    let files = approved_files("standings-");
    assert!(
        !files.is_empty(),
        "no standings-*.json under {}",
        approved_dir().display()
    );
    let failures = round_trip::<StandingsSeries>(&files);
    assert!(
        failures.is_empty(),
        "{} of {} standings differ:\n{}",
        failures.len(),
        files.len(),
        failures.join("\n")
    );
}

#[test]
fn every_approved_file_is_covered() {
    let all = approved_files("");
    let covered = approved_files("race-").len() + approved_files("standings-").len();
    assert_eq!(
        all.len(),
        covered,
        "approved files that are neither race-*.json nor standings-*.json"
    );
    assert_eq!(
        all.len(),
        140,
        "approved file count (update when the snapshot grows)"
    );
}

/// Typed parse and a second parse of the re-serialized text agree (no information lost either way).
#[test]
fn race_report_values_survive_a_second_round() {
    let path = approved_dir().join("race-1.json");
    let text = std::fs::read_to_string(&path).unwrap();
    let first: RaceReport = serde_json::from_str(&text).unwrap();
    let second: RaceReport = serde_json::from_str(&to_kotlin_pretty_json(&first).unwrap()).unwrap();
    assert_eq!(first, second);
    assert_eq!(first.race_schedule.race.id, 1);
    assert!(first.race_schedule.start_time().is_some());
}

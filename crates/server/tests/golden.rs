//! T1 (= `GoldenTest.kt`): every race report and every series standings over the production
//! snapshot, through the server's own data access, byte for byte against the approved files.
//! No re-approval here: a mismatch writes `<name>.received.json` next to the approved file
//! (git-ignored) and fails with the list.
//!
//! The approved files were written by the Kotlin `GoldenTest` under Gradle, whose JVM ran in the
//! host's zone (`TimeZone.setDefault(UTC)` is in the server's `main`, not in the test), so pgjdbc
//! read the `timestamp without time zone` columns as Pacific wall-clock times: race-1.json's
//! start is `2024-01-21T02:00:00Z` where the dump holds `2024-01-20 18:00:00` and the contract
//! cassettes (recorded from the UTC server) say `2024-01-20T18:00:00Z`. This test therefore reads
//! the snapshot in `America/Los_Angeles` ([`Db::with_naive_time_zone`]); the production path is
//! UTC (D7).

mod support;

use jiff::Timestamp;
use regatta_common::{to_kotlin_pretty_json, RaceReport, StandingsSeries};
use regatta_service::db::time::PACIFIC;
use regatta_service::reporter::{get_report, get_standings_report};
use regatta_service::{startup, Db};
use std::path::PathBuf;

const RACE_FILES: usize = 118;
const STANDINGS_FILES: usize = 22;

struct Approvals {
    dir: PathBuf,
    mismatches: Vec<String>,
}

impl Approvals {
    fn new() -> Approvals {
        let dir = support::repo_path(support::APPROVED);
        assert!(dir.is_dir(), "{} is not a directory", dir.display());
        Approvals {
            dir,
            mismatches: Vec::new(),
        }
    }

    fn check(&mut self, name: &str, content: &str) {
        let approved = self.dir.join(format!("{name}.json"));
        let received = self.dir.join(format!("{name}.received.json"));
        match std::fs::read_to_string(&approved) {
            Ok(expected) if expected == content => {
                let _ = std::fs::remove_file(&received);
            }
            Ok(_) => {
                std::fs::write(&received, content).expect("writing the received file");
                self.mismatches.push(format!("{name} (differs)"));
            }
            Err(_) => {
                std::fs::write(&received, content).expect("writing the received file");
                self.mismatches.push(format!("{name} (no approved file)"));
            }
        }
    }

    fn assert_all_approved(&self) {
        assert!(
            self.mismatches.is_empty(),
            "{} golden output(s) differ from {}. Compare the .received.json files:\n  {}",
            self.mismatches.len(),
            self.dir.display(),
            self.mismatches.join("\n  ")
        );
    }
}

fn years(list: &[String]) -> Vec<i32> {
    let mut years: Vec<i32> = list.iter().map(|y| y.parse().unwrap()).collect();
    years.sort();
    years
}

#[tokio::test(flavor = "multi_thread")]
async fn golden_reports_and_standings_match_approved() {
    let golden = support::start().await;
    let pacific = jiff::tz::TimeZone::get(PACIFIC).unwrap();
    let db = &Db::with_naive_time_zone(golden.pool.clone(), pacific);

    // GoldenTest.schemaVersionIsCurrentAfterStartup
    assert_eq!(
        startup::schema_version(&golden.pool).await.unwrap(),
        startup::SCHEMA_VERSION
    );

    let years = years(&db.all_years().await.unwrap());
    assert!(!years.is_empty(), "the snapshot has no race years");

    // GoldenTest.raceReportsMatchApproved
    let mut approvals = Approvals::new();
    let mut race_count = 0;
    for year in &years {
        for schedule in db.all_races(*year).await.unwrap() {
            let id = schedule.race.id;
            let report: RaceReport = get_report(db, id)
                .await
                .unwrap()
                .unwrap_or_else(|| panic!("no report for race {id}"));
            approvals.check(
                &format!("race-{id}"),
                &to_kotlin_pretty_json(&report).unwrap(),
            );
            race_count += 1;
        }
    }
    println!("checked {race_count} race reports for years {years:?}");

    // GoldenTest.seriesStandingsMatchApproved
    let now: Timestamp = support::DUMP_INSTANT.parse().unwrap();
    let mut standings_count = 0;
    for series in db.all_series().await.unwrap() {
        for year in &years {
            let standings: Option<StandingsSeries> =
                get_standings_report(db, series.id, *year, now)
                    .await
                    .unwrap();
            // a series with no races in a year has nothing to characterise
            if let Some(standings) = standings.filter(|s| !s.races.is_empty()) {
                approvals.check(
                    &format!("standings-{}-{year}", series.id),
                    &to_kotlin_pretty_json(&standings).unwrap(),
                );
                standings_count += 1;
            }
        }
    }
    println!("checked {standings_count} series standings for years {years:?}");

    approvals.assert_all_approved();
    assert_eq!(race_count, RACE_FILES, "race reports checked");
    assert_eq!(standings_count, STANDINGS_FILES, "series standings checked");
}

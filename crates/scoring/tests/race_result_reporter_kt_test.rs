//! = `RaceResultReporterKtTest.kt`.

use pretty_assertions::assert_eq;
use regatta_common::FinishCode;
use regatta_scoring::fixture::*;
use std::collections::HashMap;

/// Ordering of finish, HOC and non-finishers in one bracket. DNS has no race record and is
/// scored in the series standings, so it is not part of a race report.
#[test]
fn test_hoc() {
    let report = race(|r| {
        r.cruising_class("Windseekers", |c| {
            c.bracket("A", |b| {
                b.boat("DNF place 4a", None).code(FinishCode::Dnf);
                b.boat("HOC1 place 2", None).hoc(1);
                b.boat("RET place 4", None).code(FinishCode::Ret);
                b.boat("DNF place 4b", None).code(FinishCode::Dnf);
                b.boat("HOC2 place 3", None).hoc(2);
                b.boat("finish place 1", None).finishes("20:30:00");
            })
        })
    })
    .report(&HashMap::new());

    report.assert_class_scores(&[
        ("finish place 1", 1),
        ("HOC1 place 2", 2),
        ("HOC2 place 3", 3),
        ("DNF place 4a", 4),
        ("DNF place 4b", 4),
        ("RET place 4", 4),
    ]);
    report.assert_bracket_scores(&[
        ("finish place 1", 1),
        ("HOC1 place 2", 2),
        ("HOC2 place 3", 3),
        ("DNF place 4a", 4),
        ("DNF place 4b", 4),
        ("RET place 4", 4),
    ]);
}

/// GSI 14.1.2: a boat with no record for a race scores the number of starters plus one.
#[test]
fn gsi_14_1_2_dns_scores_starters_plus_one() {
    let standings = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 48).finishes("20:00:00");
                    b.boat("B", 72).finishes("20:10:00");
                    b.boat("C", 90).code(FinishCode::Dnf);
                })
            })
        });
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 48).finishes("20:00:00");
                    b.boat("B", 72).finishes("20:10:00");
                })
            })
        });
    })
    .standings();

    assert_eq!(2, standings.races.len());
    assert_eq!(vec![3.0, 3.0], standings.bracket_race_scores("C"));
    standings.assert_bracket_totals(&[("A", 2), ("B", 4), ("C", 6)]);
    standings.assert_class_totals(&[("A", 2), ("B", 4), ("C", 6)]);
}

/// The series builder's rcVolunteer adds a DNS_RC record in the boat's usual class and bracket.
#[test]
fn rc_volunteer_enters_the_boat_as_dns_rc() {
    let fixture = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 48).finishes("20:00:00");
                })
            })
        });
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("B", 72).finishes("20:00:00");
                })
            })
        });
        s.rc_volunteer("A", 2);
    });
    let entry = fixture.races[1]
        .results
        .iter()
        .find(|r| r.boat_skipper.boat.as_ref().map(|b| b.name.as_str()) == Some("A"))
        .unwrap();
    assert_eq!(FinishCode::DnsRc, entry.result.finish_code);
    assert_eq!(fixture.races[0].results[0].bracket.id, entry.bracket.id);
    assert_eq!(
        Some(FinishCode::DnsRc),
        fixture.standings().row("A").race_standings[1].finish_code
    );
}

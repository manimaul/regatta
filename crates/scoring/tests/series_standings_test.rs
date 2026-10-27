//! = `SeriesStandingsTest.kt`: series standings rules, GSI 14.4 / 14.5 / 1.4.1.2 with the D1, D2,
//! D3, D5 and D8 decisions in docs/explore_implemented/test_audit_and_plan.md. Unless noted,
//! every race is in the PHRF class "Fast", bracket I, with equal ratings, so finishing order is
//! placing order.

use jiff::SignedDuration;
use pretty_assertions::assert_eq;
use regatta_common::FinishCode::*;
use regatta_common::{OrcCertificate, RaceStatus, RatingType, StandingsSeries};
use regatta_scoring::fixture::*;
use std::collections::HashMap;

/// Enters the boats as timed finishers in the given order, one minute apart.
fn finishers(b: &mut BracketBuilder<'_>, boats: &[&str]) {
    for (i, name) in boats.iter().enumerate() {
        b.boat(name, 50).finishes(&format!("19:0{i}:00"));
    }
}

fn fast_race(s: &mut SeriesBuilder, boats: &[&str]) {
    s.race(|r| r.phrf_class("Fast", |c| c.bracket("I", |b| finishers(b, boats))))
}

fn phrf_only() -> BoatOpts<'static> {
    BoatOpts {
        rating_type: Some(RatingType::Phrf),
        ..Default::default()
    }
}

fn day() -> SignedDuration {
    SignedDuration::from_hours(24)
}

fn throw_outs(standings: &StandingsSeries, boat: &str) -> usize {
    standings
        .row(boat)
        .race_standings
        .iter()
        .filter(|r| r.throw_out)
        .count()
}

// ---- DNS (GSI 14.1.2)

#[test]
fn gsi_14_1_2_dns_scores_starters_plus_one_in_bracket_and_class() {
    let standings = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                    b.boat("B", 50).finishes("19:01:00");
                    b.boat("C", 50).finishes("19:02:00");
                });
                c.bracket("II", |b| {
                    b.boat("X", 50).finishes("19:03:00");
                    b.boat("Y", 50).finishes("19:04:00");
                });
            })
        });
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                    b.boat("B", 50).finishes("19:01:00");
                });
                c.bracket("II", |b| {
                    b.boat("X", 50).finishes("19:03:00");
                    b.boat("Y", 50).finishes("19:04:00");
                });
            })
        });
    })
    .standings();
    assert_eq!(vec![3.0, 3.0], standings.bracket_race_scores("C")); // 3rd, then 2 starters + 1
    assert_eq!(vec![3.0, 5.0], standings.class_race_scores("C")); // 3rd, then 4 starters + 1
}

/// D6: a DNS record scores the same as no record, and does not count as a starter for the others.
#[test]
fn d6_dns_record_scores_like_no_record() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]);
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    finishers(b, &["A", "B"]);
                    b.boat("C", 50).code(Dns);
                })
            })
        });
        fast_race(s, &["A", "B"]);
    })
    .standings();
    assert_eq!(vec![3.0, 3.0, 3.0], standings.bracket_race_scores("C"));
    assert_eq!(vec![1.0, 1.0, 1.0], standings.bracket_race_scores("A"));
}

/// D3: an RC volunteer is not a starter, so the DNS score of other boats does not count it.
#[test]
fn d3_dns_does_not_count_the_rc_volunteer_as_a_starter() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C", "D"]);
        fast_race(s, &["A", "B"]); // C is the RC volunteer, D is absent
        s.rc_volunteer("C", 2);
    })
    .standings();
    assert_eq!(3.0, standings.bracket_race_scores("D")[1]);
    assert_eq!(3.0, standings.class_race_scores("D")[1]);
}

/// GSI 1.4.1.2: the RC score does not affect the other boats, in scores or places.
#[test]
fn gsi_1_4_1_2_rc_volunteer_does_not_change_other_boats() {
    let build = |with_rc: bool| {
        series(|s| {
            fast_race(s, &["A", "B", "C", "D"]);
            fast_race(s, &["A", "C"]); // B absent, or the RC volunteer; D absent
            fast_race(s, &["A", "B", "D"]); // C absent
            if with_rc {
                s.rc_volunteer("B", 2);
            }
        })
        .standings()
    };
    let without = build(false);
    let with = build(true);
    for boat in ["A", "C", "D"] {
        assert_eq!(
            without.bracket_race_scores(boat),
            with.bracket_race_scores(boat),
            "{boat}"
        );
        assert_eq!(
            without.class_race_scores(boat),
            with.class_race_scores(boat),
            "{boat}"
        );
        assert_eq!(
            without.bracket_places()[boat],
            with.bracket_places()[boat],
            "{boat}"
        );
    }
}

// ---- throw-out (GSI 14.4, D2)

#[test]
fn gsi_14_4_five_started_races_drop_the_worst_score() {
    let standings = series(|s| {
        for _ in 0..5 {
            fast_race(s, &["A", "B"]);
        }
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 4), ("B", 8)]);
    standings.assert_class_totals(&[("A", 4), ("B", 8)]);
    assert_eq!(1, throw_outs(&standings, "B"));
}

#[test]
fn gsi_14_4_four_races_have_no_throw_out() {
    let standings = series(|s| {
        for _ in 0..4 {
            fast_race(s, &["A", "B"]);
        }
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 4), ("B", 8)]);
    assert_eq!(0, throw_outs(&standings, "B"));
}

#[test]
fn throw_out_drops_the_single_worst_score() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]);
        fast_race(s, &["A", "B", "C"]);
        fast_race(s, &["B", "C"]); // A DNS = 3
        fast_race(s, &["A", "B", "C"]);
        fast_race(s, &["A", "B", "C"]);
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 4), ("B", 7), ("C", 11)]);
    assert!(standings.row("A").race_standings[2].throw_out);
}

/// D2: a race with no results scores nothing and does not count toward the throw-out threshold.
#[test]
fn d2_race_with_no_results_scores_nothing_and_is_not_started() {
    let standings = series(|s| {
        for _ in 0..4 {
            fast_race(s, &["A", "B"]);
        }
        s.race(|r| r.phrf_class("Fast", |c| c.bracket("I", |_| {}))); // cancelled, or not yet sailed
    })
    .standings();
    assert_eq!(5, standings.races.len());
    standings.assert_bracket_totals(&[("A", 4), ("B", 8)]); // four started races: no throw-out, nothing for the fifth
}

/// D2: a race with no results is "not yet sailed" while its end is in the future, and "cancelled" once it is past.
#[test]
fn d2_race_status_depends_on_now() {
    let fixture = series(|s| {
        fast_race(s, &["A", "B"]);
        s.race(|r| r.phrf_class("Fast", |c| c.bracket("I", |_| {})));
    });
    let before = fixture.standings_with(
        Some(
            fixture.races[1]
                .schedule
                .start_time()
                .unwrap()
                .checked_sub(day())
                .unwrap(),
        ),
        &HashMap::new(),
    );
    let after = fixture.standings_with(
        Some(
            fixture.races[1]
                .schedule
                .end_time()
                .unwrap()
                .checked_add(day())
                .unwrap(),
        ),
        &HashMap::new(),
    );
    let statuses = |s: &StandingsSeries| -> Vec<RaceStatus> {
        s.row("A").race_standings.iter().map(|r| r.status).collect()
    };
    assert_eq!(
        vec![RaceStatus::Sailed, RaceStatus::NotYetSailed],
        statuses(&before)
    );
    assert_eq!(
        vec![RaceStatus::Sailed, RaceStatus::Cancelled],
        statuses(&after)
    );
    let cell = &after.row("A").race_standings[1];
    assert!(!cell.bracket_sailed);
    assert!(!cell.class_sailed);
    assert_eq!(0.0, cell.place_in_bracket);
    assert!(!cell.throw_out);
}

/// D2: a race sailed by other boats but by nobody in this bracket is "sailed" as a race, but not for the bracket.
#[test]
fn d2_race_sailed_elsewhere_is_sailed_but_not_for_the_group() {
    let standings = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                });
                c.bracket("II", |b| {
                    b.boat("X", 50).finishes("19:02:00");
                });
            })
        });
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                });
                c.bracket("II", |_| {});
            })
        });
    })
    .standings();
    let cell = &standings.row("X").race_standings[1];
    assert_eq!(RaceStatus::Sailed, cell.status);
    assert!(!cell.bracket_sailed);
    assert!(cell.class_sailed);
    assert_eq!(2.0, cell.place_in_class);
}

/// D2: an RC volunteer alone in a bracket does not make the race sailed for it.
#[test]
fn d2_rc_volunteer_alone_is_not_a_sailed_race() {
    let standings = series(|s| {
        fast_race(s, &["A", "B"]);
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).code(DnsRc);
                })
            })
        });
    })
    .standings();
    assert!(!standings.row("A").race_standings[1].bracket_sailed);
    standings.assert_bracket_totals(&[("A", 1), ("B", 2)]);
}

/// D2: the last-race tiebreak uses the last race sailed for the group.
#[test]
fn d2_tiebreak_uses_the_last_sailed_race() {
    let standings = series(|s| {
        fast_race(s, &["A", "B"]);
        fast_race(s, &["B", "A"]);
        s.race(|r| r.phrf_class("Fast", |c| c.bracket("I", |_| {})));
    })
    .standings();
    assert_eq!(places(&[("B", 1), ("A", 2)]), standings.bracket_places());
}

/// D2: sailed is decided per bracket, so the same race can count for the class but not for the bracket.
#[test]
fn d2_race_not_sailed_in_a_bracket_scores_nothing_there_but_counts_in_the_class() {
    let standings = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                    b.boat("B", 50).finishes("19:01:00");
                });
                c.bracket("II", |b| {
                    b.boat("X", 50).finishes("19:02:00");
                });
            })
        });
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                    b.boat("B", 50).finishes("19:01:00");
                });
                c.bracket("II", |_| {});
            })
        });
    })
    .standings();
    standings.assert_bracket_totals(&[("X", 1)]); // race 2 was not sailed in bracket II
    standings.assert_class_totals(&[("X", 3 + 3)]); // in the class, race 2 had 2 starters: DNS = 3
}

/// D2: the throw-out is chosen per scoring group: worst bracket score for the bracket total, worst class score for the class total.
#[test]
fn d2_throw_out_is_chosen_per_scoring_group() {
    fn fast_race(s: &mut SeriesBuilder, a: &str, b: &str, z: &str, x: &str, y: &str) {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |br| {
                    br.boat("A", 50).finishes(a);
                    br.boat("B", 50).finishes(b);
                    br.boat("Z", 50).finishes(z);
                });
                c.bracket("II", |br| {
                    br.boat("X", 50).finishes(x);
                    br.boat("Y", 50).finishes(y);
                });
            })
        })
    }
    let standings = series(|s| {
        fast_race(
            s, "19:00:00", "19:03:00", "19:04:00", "19:01:00", "19:02:00",
        ); // B: bracket 2, class 4
        fast_race(
            s, "19:00:00", "19:02:00", "19:01:00", "19:05:00", "19:06:00",
        ); // B: bracket 3, class 3
        for _ in 0..3 {
            fast_race(
                s, "19:00:00", "19:01:00", "19:02:00", "19:05:00", "19:06:00",
            ); // B: 2, 2
        }
    })
    .standings();
    assert_eq!(
        vec![2.0, 3.0, 2.0, 2.0, 2.0],
        standings.bracket_race_scores("B")
    );
    assert_eq!(
        vec![4.0, 3.0, 2.0, 2.0, 2.0],
        standings.class_race_scores("B")
    );
    standings.assert_bracket_totals(&[("B", 8)]); // drops the 3 in race 2
    standings.assert_class_totals(&[("B", 9)]); // drops the 4 in race 1
}

// ---- RC volunteer (GSI 1.4.1.2, D1)

/// D1: the RC race scores the average of the boat's other races, and the total uses it.
#[test]
fn d1_rc_volunteer_scores_the_average_of_the_other_races() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C", "D"]); // B 2
        fast_race(s, &["A", "C", "D"]); // B RC
        fast_race(s, &["C", "D", "A", "B"]); // B 4
        s.rc_volunteer("B", 2);
    })
    .standings();
    assert_eq!(vec![2.0, 3.0, 4.0], standings.bracket_race_scores("B"));
    standings.assert_bracket_totals(&[("B", 9)]);
    standings.assert_class_totals(&[("B", 9)]);
}

/// D1: the average is fractional, to the nearest tenth of a point (RRS A10(a)).
#[test]
fn d1_rc_average_is_fractional() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]); // B 2
        fast_race(s, &["A", "C"]); // B RC
        fast_race(s, &["A", "C", "B"]); // B 3
        s.rc_volunteer("B", 2);
    })
    .standings();
    assert_eq!(vec![2.0, 2.5, 3.0], standings.bracket_race_scores("B"));
    standings.assert_bracket_totals(&[("B", 7.5)]);
}

/// D1: DNS races count in the average.
#[test]
fn d1_rc_average_includes_dns_races() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]); // B 2
        fast_race(s, &["A", "C"]); // B RC
        fast_race(s, &["A", "C"]); // B DNS 3
        s.rc_volunteer("B", 2);
    })
    .standings();
    standings.assert_bracket_totals(&[("B", 2.0 + 2.5 + 3.0)]);
}

/// D1 order of operations: the throw-out is chosen from the other races first, and the average leaves it out.
#[test]
fn d1_rc_average_leaves_out_the_throw_out() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C", "D", "E"]); // B 2
        fast_race(s, &["A", "C", "D", "E"]); // B RC
        fast_race(s, &["A", "C", "D", "E", "B"]); // B 5, the throw-out
        fast_race(s, &["A", "B", "C", "D", "E"]); // B 2
        fast_race(s, &["A", "B", "C", "D", "E"]); // B 2
        s.rc_volunteer("B", 2);
    })
    .standings();
    assert_eq!(
        vec![2.0, 2.0, 5.0, 2.0, 2.0],
        standings.bracket_race_scores("B")
    );
    assert!(standings.row("B").race_standings[2].throw_out);
    standings.assert_bracket_totals(&[("B", 8)]);
}

/// D1: the average of the remaining races is never worse than the thrown-out score, so the RC race is never the throw-out.
#[test]
fn d1_rc_race_is_never_the_throw_out() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]); // B 2
        fast_race(s, &["A", "C"]); // B RC
        fast_race(s, &["A", "C", "B"]); // B 3
        fast_race(s, &["A", "C", "B"]); // B 3
        fast_race(s, &["A", "C", "B"]); // B 3
        s.rc_volunteer("B", 2);
    })
    .standings();
    // one 3 is thrown out; the average of 2, 3, 3 is 2.67, rounded to 2.7
    assert!(!standings.row("B").race_standings[1].throw_out);
    standings.assert_bracket_totals(&[("B", 2.0 + 2.7 + 3.0 + 3.0)]);
}

/// D1 / RRS A10(a): the average is rounded to the nearest tenth of a point, 0.05 upward.
#[test]
fn d1_rc_average_rounds_to_the_nearest_tenth_half_up() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C", "D", "E"]); // B 2
        fast_race(s, &["A", "C", "D", "E"]); // B RC
        fast_race(s, &["A", "C", "B", "D", "E"]); // B 3
        fast_race(s, &["A", "C", "B", "D", "E"]); // B 3
        fast_race(s, &["A", "C", "B", "D", "E"]); // B 3
        fast_race(s, &["A", "C", "D", "E", "B"]); // B 5, the throw-out
        s.rc_volunteer("B", 2);
    })
    .standings();
    // the average of 2, 3, 3, 3 is 2.75, which rounds up to 2.8, not to 2.7 or 2.75
    assert_eq!(
        vec![2.0, 2.8, 3.0, 3.0, 3.0, 5.0],
        standings.bracket_race_scores("B")
    );
    standings.assert_bracket_totals(&[("B", 2.0 + 2.8 + 3.0 + 3.0 + 3.0)]);
}

/// D1: with no other scored race, the RC race scores DNS (starters + 1), not DNF (finishers + 1).
#[test]
fn d1_rc_with_no_other_race_scores_dns() {
    let standings = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    finishers(b, &["A"]);
                    b.boat("C", 50).code(Dnf);
                    b.boat("B", 50).code(DnsRc);
                })
            })
        });
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 1), ("C", 2), ("B", 3)]);
}

/// D1: once per series; the earliest RC race gets the average and later ones score DNS, which then counts in the average.
#[test]
fn d1_rc_is_once_per_series() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]); // B 2
        fast_race(s, &["A", "C"]); // B RC: average of 2 and 3
        fast_race(s, &["A", "C"]); // B RC again: DNS 3
        s.rc_volunteer("B", 2);
        s.rc_volunteer("B", 3);
    })
    .standings();
    assert_eq!(vec![2.0, 2.5, 3.0], standings.bracket_race_scores("B"));
    standings.assert_bracket_totals(&[("B", 7.5)]);
}

// ---- tiebreak (GSI 14.5, D5)

#[test]
fn gsi_14_5_tie_goes_to_the_better_last_race() {
    let standings = series(|s| {
        fast_race(s, &["A", "B"]);
        fast_race(s, &["B", "A"]);
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 3), ("B", 3)]);
    assert_eq!(places(&[("B", 1), ("A", 2)]), standings.bracket_places());
    assert_eq!(places(&[("B", 1), ("A", 2)]), standings.class_places());
}

#[test]
fn gsi_14_5_three_way_tie_is_ordered_by_the_last_race() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]);
        fast_race(s, &["B", "C", "A"]);
        fast_race(s, &["C", "A", "B"]);
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 6), ("B", 6), ("C", 6)]);
    assert_eq!(
        places(&[("C", 1), ("A", 2), ("B", 3)]),
        standings.bracket_places()
    );
}

/// D5: an equal last race is broken by the next-to-last race, and so on back.
#[test]
fn d5_equal_last_race_goes_back_to_earlier_races() {
    let dnf_race = |s: &mut SeriesBuilder| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    finishers(b, &["C"]);
                    b.boat("A", 50).code(Dnf);
                    b.boat("B", 50).code(Dnf);
                })
            })
        })
    };
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]); // A1 B2 C3
        fast_race(s, &["B", "A", "C"]); // A2 B1 C3
        dnf_race(s); // A2 B2 C1
        dnf_race(s); // A2 B2 C1
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 7), ("B", 7), ("C", 8)]);
    // last two races equal; race 2 says B
    assert_eq!(
        places(&[("B", 1), ("A", 2), ("C", 3)]),
        standings.bracket_places()
    );
    assert_eq!(
        places(&[("B", 1), ("A", 2), ("C", 3)]),
        standings.class_places()
    );
}

/// D5: when every race is equal the boats share the place and the next place skips.
#[test]
fn d5_fully_tied_boats_share_the_place() {
    let dnf_race = |s: &mut SeriesBuilder| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    finishers(b, &["C"]);
                    b.boat("A", 50).code(Dnf);
                    b.boat("B", 50).code(Dnf);
                })
            })
        })
    };
    let standings = series(|s| {
        dnf_race(s); // C1 A2 B2, D DNS 4
        dnf_race(s); // C1 A2 B2, D DNS 4
        fast_race(s, &["C", "D"]); // C1 D2, A B DNS 3
    })
    .standings();
    standings.assert_bracket_totals(&[("C", 3), ("A", 7), ("B", 7), ("D", 10)]);
    assert_eq!(
        places(&[("C", 1), ("A", 2), ("B", 2), ("D", 4)]),
        standings.bracket_places()
    );
    assert_eq!(
        places(&[("C", 1), ("A", 2), ("B", 2), ("D", 4)]),
        standings.class_places()
    );
}

/// D5 / GSI 1.4.1.2: a race where either tied boat has an RC score is skipped in the tiebreak.
#[test]
fn d5_rc_race_is_skipped_in_the_tiebreak() {
    let standings = series(|s| {
        fast_race(s, &["A", "B", "C"]); // A1 B2 C3
        fast_race(s, &["C", "B", "A"]); // A3 B2 C1
        fast_race(s, &["C", "A"]); // A2, B RC: average 2, C1
        s.rc_volunteer("B", 3);
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 6), ("B", 6), ("C", 5)]);
    // the last race is skipped for the A/B tie; race 2 says B
    assert_eq!(
        places(&[("C", 1), ("B", 2), ("A", 3)]),
        standings.bracket_places()
    );
}

/// D5: the result does not depend on the input order of the boats.
#[test]
fn d5_places_do_not_depend_on_input_order() {
    let build = |order: &[&str]| {
        series(|s| {
            for _ in 0..2 {
                s.race(|r| {
                    r.phrf_class("Fast", |c| {
                        c.bracket("I", |b| {
                            for boat in order {
                                b.boat(boat, 50).code(Dnf);
                            }
                            finishers(b, &["C"]);
                        })
                    })
                });
            }
        })
        .standings()
        .bracket_places()
    };
    assert_eq!(places(&[("C", 1), ("A", 2), ("B", 2)]), build(&["A", "B"]));
    assert_eq!(build(&["A", "B"]), build(&["B", "A"]));
}

// ---- totals, grouping, single race

#[test]
fn class_totals_use_class_scores_and_bracket_totals_use_bracket_scores() {
    let standings = series(|s| {
        for _ in 0..2 {
            s.race(|r| {
                r.phrf_class("Fast", |c| {
                    c.bracket("I", |b| {
                        b.boat("A", 50).finishes("19:00:00");
                        b.boat("B", 50).finishes("19:03:00");
                    });
                    c.bracket("II", |b| {
                        b.boat("X", 50).finishes("19:01:00");
                        b.boat("Y", 50).finishes("19:02:00");
                    });
                })
            });
        }
    })
    .standings();
    standings.assert_bracket_totals(&[("A", 2), ("B", 4), ("X", 2), ("Y", 4)]);
    standings.assert_class_totals(&[("A", 2), ("X", 4), ("Y", 6), ("B", 8)]);
    assert_eq!(
        places(&[("A", 1), ("B", 2), ("X", 1), ("Y", 2)]),
        standings.bracket_places()
    );
    assert_eq!(
        places(&[("A", 1), ("X", 2), ("Y", 3), ("B", 4)]),
        standings.class_places()
    );
}

/// D8: a boat that changes skipper keeps one standings row, showing the latest skipper.
#[test]
fn d8_standings_are_grouped_by_boat_not_by_boat_and_skipper() {
    let standings = series(|s| {
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat_with(
                        "A",
                        50,
                        BoatOpts {
                            skipper: Some("Ann Owner"),
                            ..Default::default()
                        },
                    )
                    .finishes("19:00:00");
                    b.boat("B", 50).finishes("19:01:00");
                })
            })
        });
        s.race(|r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat_with(
                        "A",
                        50,
                        BoatOpts {
                            skipper: Some("Bob Crew"),
                            ..Default::default()
                        },
                    )
                    .finishes("19:00:00");
                    b.boat("B", 50).finishes("19:01:00");
                })
            })
        });
    })
    .standings();
    assert_eq!(1, standings.standings.len());
    assert_eq!(1, standings.standings[0].standings.len());
    let rows = &standings.standings[0].standings[0].standings;
    assert_eq!(2, rows.len());
    assert_eq!(
        Some("Bob Crew".to_string()),
        standings
            .row("A")
            .boat_skipper
            .skipper
            .as_ref()
            .map(|p| p.full_name())
    );
    standings.assert_bracket_totals(&[("A", 2), ("B", 4)]);
}

#[test]
fn single_race_series() {
    let standings = series(|s| fast_race(s, &["A", "B", "C"])).standings();
    standings.assert_bracket_totals(&[("A", 1), ("B", 2), ("C", 3)]);
    assert_eq!(
        places(&[("A", 1), ("B", 2), ("C", 3)]),
        standings.bracket_places()
    );
    assert_eq!(1, standings.row("A").race_standings.len());
    assert!(!standings.row("A").race_standings[0].throw_out);
}

// ---- ORC series scoring (D9, orc_series_standings.md)

/// Certificates for the ORC tests: with a B factor of 600 the race scores five-band Low/Medium.
fn certs() -> HashMap<String, OrcCertificate> {
    [("slow", 1.0), ("fast", 0.5), ("mid", 0.8)]
        .into_iter()
        .map(|(name, tot)| {
            (
                name.to_string(),
                OrcCertificate {
                    ref_no: name.into(),
                    five_band_wl_low_med_tot: tot,
                    ..Default::default()
                },
            )
        })
        .collect()
}

/// An ORC/PHRF boat with a PHRF rating of 50 (TCF 1 at B 600, so the PHRF order is the finish order) and a certificate.
fn orc_boat(b: &mut BracketBuilder<'_>, name: &str, cert: &str, finish: &str) {
    b.boat(name, 50).finishes(finish).orc(cert);
}

/// A race of the ORC/PHRF class "Fast", bracket I, starting at 18:00.
fn orc_race(s: &mut SeriesBuilder, block: impl FnOnce(&mut BracketBuilder<'_>)) {
    s.race_with(
        RaceOpts {
            start: "18:00".into(),
            ..RaceOpts::default()
        },
        |r| r.orc_phrf_class("Fast", |c| c.bracket("I", block)),
    )
}

/// The standard ORC race of these tests. Elapsed times A 60, C 70, B 90 minutes give the PHRF
/// order A, C, B; the ORC corrected times A 60, C 56, B 45 give the ORC order B, C, A.
fn standard_orc_race(s: &mut SeriesBuilder) {
    orc_race(s, |b| {
        orc_boat(b, "A", "slow", "19:00:00");
        orc_boat(b, "B", "fast", "19:30:00");
        orc_boat(b, "C", "mid", "19:10:00");
    })
}

#[test]
fn d9_orc_series_total_is_the_sum_of_orc_race_scores() {
    let standings = series(|s| {
        for _ in 0..3 {
            orc_race(s, |b| {
                orc_boat(b, "A", "slow", "19:00:00");
                orc_boat(b, "B", "fast", "19:30:00");
            });
        }
    })
    .standings_with(None, &certs());
    standings.assert_bracket_totals(&[("A", 3), ("B", 6)]);
    standings.assert_bracket_totals_orc(&[("A", 6), ("B", 3)]);
    standings.assert_class_totals_orc(&[("A", 6), ("B", 3)]);
    assert_eq!(places(&[("A", 1), ("B", 2)]), standings.bracket_places());
    assert_eq!(
        places(&[("A", 2), ("B", 1)]),
        standings.bracket_places_orc()
    );
    assert_eq!(places(&[("A", 2), ("B", 1)]), standings.class_places_orc());
    assert_eq!(RatingType::OrcPhrf, standings.row("A").rating_type);
    // the PHRF side is what a PHRF-only fleet of the same races scores
    let phrf_only = series(|s| {
        for _ in 0..3 {
            fast_race(s, &["A", "B"]);
        }
    })
    .standings();
    assert_eq!(phrf_only.bracket_totals(), standings.bracket_totals());
    assert_eq!(phrf_only.bracket_places(), standings.bracket_places());
}

/// GSI 14.1.2 / D9: a boat with no ORC entry scores the ORC starters + 1 in the ORC groups, and the PHRF starters + 1 in the PHRF ones.
#[test]
fn d9_orc_dns_scores_orc_starters_plus_one() {
    let standings = series(|s| {
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "19:30:00");
            b.boat_with("P", 50, phrf_only()).finishes("19:40:00");
        });
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            b.boat_with("P", 50, phrf_only()).finishes("19:40:00");
        });
    })
    .standings_with(None, &certs());
    assert_eq!(vec![1.0, 2.0], standings.bracket_race_scores_orc("B")); // 1st, then 1 ORC starter + 1
    assert_eq!(vec![2.0, 3.0], standings.bracket_race_scores("B")); // 2nd, then 2 PHRF starters + 1
    assert!(standings.row("B").race_standings[1].non_starter_orc);
    assert!(!standings.row("B").race_standings[0].non_starter_orc);
}

/// D2 / D9: a race with no ORC starter is not sailed for the ORC groups, even though it is sailed for the PHRF ones.
#[test]
fn d9_orc_race_without_orc_starters_is_not_sailed_for_orc() {
    let standings = series(|s| {
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "19:30:00");
            b.boat_with("P", 50, phrf_only()).finishes("19:40:00");
        });
        orc_race(s, |b| {
            b.boat_with("P", 50, phrf_only()).finishes("19:00:00");
        });
    })
    .standings_with(None, &certs());
    let second = &standings.row("A").race_standings[1];
    assert!(!second.bracket_sailed_orc);
    assert!(!second.class_sailed_orc);
    assert!(second.bracket_sailed);
    assert!(!second.non_starter_orc);
    assert_eq!(vec![2.0, 0.0], standings.bracket_race_scores_orc("A"));
    assert_eq!(vec![1.0, 0.0], standings.bracket_race_scores_orc("B"));
    standings.assert_bracket_totals_orc(&[("A", 2), ("B", 1)]);
    standings.assert_bracket_totals(&[("A", 1 + 2), ("B", 2 + 2)]); // PHRF DNS in race 2: 1 starter + 1
}

/// GSI 14.4 / D9: each rating throws out its own worst race, which can be a different race.
#[test]
fn d9_orc_throw_out_is_chosen_separately_from_phrf() {
    let standings = series(|s| {
        for _ in 0..3 {
            standard_orc_race(s);
        }
        // A absent: PHRF DNS = 5 starters + 1 = 6, ORC DNS = 2 ORC starters + 1 = 3
        orc_race(s, |b| {
            orc_boat(b, "B", "fast", "19:00:00");
            orc_boat(b, "C", "mid", "19:10:00");
            for (i, p) in ["P1", "P2", "P3"].into_iter().enumerate() {
                b.boat_with(p, 50, phrf_only())
                    .finishes(&format!("19:2{i}:00"));
            }
        });
        // A first under PHRF, last under ORC: corrected B 45, D 50, C 56, A 60
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "19:30:00");
            orc_boat(b, "C", "mid", "19:10:00");
            orc_boat(b, "D", "fast", "19:40:00");
        });
    })
    .standings_with(None, &certs());
    assert_eq!(
        vec![1.0, 1.0, 1.0, 6.0, 1.0],
        standings.bracket_race_scores("A")
    );
    assert_eq!(
        vec![3.0, 3.0, 3.0, 3.0, 4.0],
        standings.bracket_race_scores_orc("A")
    );
    let cells = &standings.row("A").race_standings;
    assert_eq!(
        vec![false, false, false, true, false],
        cells.iter().map(|c| c.throw_out).collect::<Vec<_>>()
    );
    assert_eq!(
        vec![false, false, false, false, true],
        cells.iter().map(|c| c.throw_out_orc).collect::<Vec<_>>()
    );
    standings.assert_bracket_totals(&[("A", 4)]);
    standings.assert_bracket_totals_orc(&[("A", 12)]);
}

/// D1 / D9: the RC volunteer's ORC score is the average of its other ORC race scores.
#[test]
fn d9_orc_rc_volunteer_averages_orc_scores() {
    let standings = series(|s| {
        standard_orc_race(s); // A: PHRF 1, ORC 3
        orc_race(s, |b| {
            orc_boat(b, "B", "fast", "19:30:00");
            orc_boat(b, "C", "mid", "19:10:00");
        }); // A RC
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "19:30:00");
            orc_boat(b, "C", "mid", "19:10:00");
            orc_boat(b, "D", "fast", "19:40:00");
        }); // A: PHRF 1, ORC 4
        s.rc_volunteer("A", 2);
    })
    .standings_with(None, &certs());
    assert_eq!(vec![1.0, 1.0, 1.0], standings.bracket_race_scores("A"));
    assert_eq!(vec![3.0, 3.5, 4.0], standings.bracket_race_scores_orc("A"));
    standings.assert_bracket_totals(&[("A", 3)]);
    standings.assert_bracket_totals_orc(&[("A", 10.5)]);
}

/// GSI 14.5 / D5 / D9: an ORC tie is broken on the ORC scores of the last race, whatever the PHRF standings say.
#[test]
fn d9_orc_tiebreak_uses_orc_race_scores() {
    let standings = series(|s| {
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "20:10:00");
        }); // ORC A 60, B 65: A 1, B 2
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "19:30:00");
        }); // ORC A 60, B 45: B 1, A 2
    })
    .standings_with(None, &certs());
    standings.assert_bracket_totals_orc(&[("A", 3), ("B", 3)]);
    assert_eq!(
        places(&[("B", 1), ("A", 2)]),
        standings.bracket_places_orc()
    );
    assert_eq!(places(&[("B", 1), ("A", 2)]), standings.class_places_orc());
    assert_eq!(places(&[("A", 1), ("B", 2)]), standings.bracket_places()); // PHRF: A wins both races
}

/// D9: a PHRF-only boat in an ORC class is not scored under ORC and takes no ORC place.
#[test]
fn d9_orc_places_are_among_orc_rows_only() {
    let standings = series(|s| {
        for _ in 0..2 {
            orc_race(s, |b| {
                orc_boat(b, "A", "slow", "19:00:00");
                orc_boat(b, "B", "fast", "19:30:00");
                b.boat_with("P", 50, phrf_only()).finishes("19:40:00");
            });
        }
    })
    .standings_with(None, &certs());
    let p = standings.row("P");
    assert_eq!(RatingType::Phrf, p.rating_type);
    assert_eq!(0, p.place_in_bracket_orc);
    assert_eq!(0, p.place_in_class_orc);
    assert_eq!(0.0, p.total_score_bracket_orc);
    assert_eq!(vec![0.0, 0.0], standings.bracket_race_scores_orc("P"));
    assert_eq!(
        places(&[("B", 1), ("A", 2), ("P", 0)]),
        standings.bracket_places_orc()
    );
    assert_eq!(
        places(&[("A", 1), ("B", 2), ("P", 3)]),
        standings.bracket_places()
    );
}

/// D9: outside an ORC class nothing is scored under ORC.
#[test]
fn d9_phrf_only_class_has_no_orc_fields_set() {
    let standings = series(|s| {
        for _ in 0..2 {
            fast_race(s, &["A", "B"]);
        }
    })
    .standings();
    for row in standings.rows() {
        assert_eq!(RatingType::Phrf, row.rating_type);
        assert_eq!(0.0, row.total_score_bracket_orc);
        assert_eq!(0.0, row.total_score_class_orc);
        assert_eq!(0, row.place_in_bracket_orc);
        assert_eq!(0, row.place_in_class_orc);
        for it in &row.race_standings {
            assert_eq!(0.0, it.place_in_bracket_orc);
            assert_eq!(0.0, it.place_in_class_orc);
            assert!(
                !(it.bracket_sailed_orc
                    || it.class_sailed_orc
                    || it.throw_out_orc
                    || it.throw_out_class_orc
                    || it.non_starter_orc)
            );
        }
    }
}

/// D9 with the 2025→2026 switch: before 2026 a rated class scores as PHRF only, whatever its results say.
#[test]
fn d9_orc_fields_are_not_set_before_2026() {
    let standings = series_with(
        SeriesOpts {
            year: 2025,
            first_date: Some("2025-06-17".into()),
            ..SeriesOpts::default()
        },
        |s| {
            for _ in 0..2 {
                orc_race(s, |b| {
                    orc_boat(b, "A", "slow", "19:00:00");
                    orc_boat(b, "B", "fast", "19:30:00");
                });
            }
        },
    )
    .standings_with(None, &certs());
    assert_eq!(1, standings.standings.len());
    assert_eq!(
        RatingType::Phrf,
        standings.standings[0].race_class.rating_type
    );
    standings.assert_bracket_totals(&[("A", 2), ("B", 4)]);
    standings.assert_bracket_totals_orc(&[("A", 0), ("B", 0)]);
    assert_eq!(
        places(&[("A", 0), ("B", 0)]),
        standings.bracket_places_orc()
    );
    assert_eq!(vec![0.0, 0.0], standings.bracket_race_scores_orc("A"));
}

/// D9 / R1: an ORC-only boat is not scored under PHRF, and its row follows the PHRF-placed rows, by ORC place.
#[test]
fn d9_orc_only_row_has_no_phrf_fields_set_and_sorts_after_the_phrf_rows() {
    let standings = series(|s| {
        for _ in 0..2 {
            orc_race(s, |b| {
                orc_boat(b, "A", "slow", "19:00:00");
                b.boat_with(
                    "O",
                    None,
                    BoatOpts {
                        rating_type: Some(RatingType::Orc),
                        ..Default::default()
                    },
                )
                .finishes("18:50:00")
                .orc("fast");
            });
        }
    })
    .standings_with(None, &certs());
    let o = standings.row("O");
    assert_eq!(RatingType::Orc, o.rating_type);
    assert_eq!(0.0, o.total_score_bracket);
    assert_eq!(0.0, o.total_score_class);
    assert_eq!(0, o.place_in_bracket);
    assert_eq!(0, o.place_in_class);
    assert_eq!(vec![0.0, 0.0], standings.bracket_race_scores("O"));
    standings.assert_bracket_totals_orc(&[("O", 2), ("A", 4)]); // O corrects to 25 minutes, A to 60
    assert_eq!(
        places(&[("O", 1), ("A", 2)]),
        standings.bracket_places_orc()
    );
    assert_eq!(places(&[("A", 1), ("O", 0)]), standings.bracket_places());
    assert_eq!(1, standings.standings.len());
    assert_eq!(1, standings.standings[0].standings.len());
    assert_eq!(
        vec!["A".to_string(), "O".to_string()],
        standings.standings[0].standings[0]
            .standings
            .iter()
            .map(|r| r.boat_skipper.boat.as_ref().unwrap().name.clone())
            .collect::<Vec<_>>()
    );
}

/// D9 / R1: an ORC-only boat is not a PHRF starter, so it does not raise the PHRF DNS score of a boat that stayed home.
#[test]
fn d9_orc_only_boat_does_not_count_as_a_phrf_starter() {
    let orc_only = || BoatOpts {
        rating_type: Some(RatingType::Orc),
        ..Default::default()
    };
    let standings = series(|s| {
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            b.boat_with("P", 50, phrf_only()).finishes("19:10:00");
            b.boat_with("O", None, orc_only())
                .finishes("18:50:00")
                .orc("fast");
        });
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            b.boat_with("O", None, orc_only())
                .finishes("18:50:00")
                .orc("fast");
        });
    })
    .standings_with(None, &certs());
    assert_eq!(vec![2.0, 2.0], standings.bracket_race_scores("P")); // 2nd, then 1 PHRF starter + 1
}

/// D9: a boat that raced PHRF-only before getting a certificate is scored under both, with ORC DNS for the earlier races.
#[test]
fn d9_boat_that_switches_to_orc_phrf_mid_series_scores_dns_in_orc_before_the_switch() {
    let standings = series(|s| {
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            b.boat_with("B", 50, phrf_only()).finishes("19:30:00");
        });
        orc_race(s, |b| {
            orc_boat(b, "A", "slow", "19:00:00");
            orc_boat(b, "B", "fast", "19:30:00");
        });
    })
    .standings_with(None, &certs());
    let b = standings.row("B");
    assert_eq!(RatingType::OrcPhrf, b.rating_type);
    assert_eq!(vec![2.0, 2.0], standings.bracket_race_scores("B"));
    assert_eq!(vec![2.0, 1.0], standings.bracket_race_scores_orc("B")); // 1 ORC starter + 1, then 1st
    assert_eq!(
        vec![true, false],
        b.race_standings
            .iter()
            .map(|r| r.non_starter_orc)
            .collect::<Vec<_>>()
    );
    assert_eq!(
        vec![false, false],
        b.race_standings
            .iter()
            .map(|r| r.non_starter)
            .collect::<Vec<_>>()
    );
}

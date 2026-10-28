//! Race placing rules, GSI 14.1 / 14.2 with the scoring decisions D2, D3, D4, D6 and D7.
//!
//! Every bare decision ID (`Dn`) in this file is a scoring decision of
//! `docs/explore_implemented/test_audit_and_plan.md`; R-rules are in
//! `docs/explore_implemented/orc_series_standings.md`.

use pretty_assertions::assert_eq;
use regatta_common::FinishCode::*;
use regatta_common::{
    Orc3Band, Orc5Band, OrcCertificate, OrcScoringOption, RaceReport, RatingType,
};
use regatta_scoring::fixture::*;
use std::collections::{HashMap, HashSet};

fn bracket_order(report: &RaceReport) -> Vec<String> {
    assert_eq!(1, report.class_reports.len(), "one class");
    assert_eq!(
        1,
        report.class_reports[0].bracket_report.len(),
        "one bracket"
    );
    report.class_reports[0].bracket_report[0]
        .cards
        .iter()
        .map(|c| c.boat_name.clone())
        .collect()
}

fn names(v: &[&str]) -> Vec<String> {
    v.iter().map(|s| s.to_string()).collect()
}

fn set(v: &[&str]) -> HashSet<String> {
    v.iter().map(|s| s.to_string()).collect()
}

fn phrf_only() -> BoatOpts<'static> {
    BoatOpts {
        rating_type: Some(RatingType::Phrf),
        ..Default::default()
    }
}

#[test]
fn ordering_corrected_time_then_hoc_then_code_weight() {
    let report = race_with(
        RaceOpts {
            b_factor: 600,
            ..RaceOpts::default()
        },
        |r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("NSC", None).code(Nsc);
                    b.boat("HOC2", None).hoc(2);
                    b.boat("DNF", None).code(Dnf);
                    b.boat("Slow rating fast boat", 150).finishes("19:00:00"); // 650/750 -> 48m
                    b.boat("HOC1", None).hoc(1);
                    b.boat("Fast rating slow boat", 0).finishes("18:50:00"); // 650/600 -> 54m10s
                    b.boat("RET", None).code(Ret);
                    b.boat("RC", None).code(DnsRc);
                })
            })
        },
    )
    .report(&HashMap::new());
    // timed finishers by corrected time, then HOC by position, then the non-finishers
    let order = bracket_order(&report);
    assert_eq!(
        names(&[
            "Slow rating fast boat",
            "Fast rating slow boat",
            "HOC1",
            "HOC2"
        ]),
        order[..4].to_vec()
    );
    assert_eq!(
        set(&["DNF", "RET", "NSC", "RC"]),
        order[4..].iter().cloned().collect::<HashSet<_>>()
    );
    report.assert_bracket_scores(&[
        ("Slow rating fast boat", 1),
        ("Fast rating slow boat", 2),
        ("HOC1", 3),
        ("HOC2", 4),
    ]);
}

/// GSI 14.1.1 / 14.1.3: DNF and RET score finishers + 1, and HOC boats are finishers (D3).
#[test]
fn gsi_14_1_1_dnf_and_ret_score_finishers_plus_one() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00");
                b.boat("H", None).hoc(1);
                b.boat("DNF", None).code(Dnf);
                b.boat("RET", None).code(Ret);
                b.boat("NSC", None).code(Nsc);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 2), ("H", 3), ("DNF", 4), ("RET", 4)]);
}

/// GSI 14.1.4 / D3: NSC scores the other starters + 1, the same as DNS, one worse than DNF when every other starter but
/// one finished.
#[test]
fn gsi_14_1_4_nsc_scores_starters_plus_one() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00");
                b.boat("DNF", None).code(Dnf);
                b.boat("NSC", None).code(Nsc);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("DNF", 3), ("NSC", 4)]);
}

/// D3: an RC volunteer is not a starter, so it does not raise the NSC score.
#[test]
fn d3_rc_volunteer_is_not_a_starter() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00");
                b.boat("NSC", None).code(Nsc);
                b.boat("RC 1", None).code(DnsRc);
                b.boat("RC 2", None).code(DnsRc);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 2), ("NSC", 3)]);
}

/// GSI 14.1.5 / D6: DSQ scores the other starters + 2, one worse than DNS; the DSQ boat is a starter for the others,
/// not for itself.
#[test]
fn gsi_14_1_5_dsq_scores_starters_plus_two() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00");
                b.boat("DNF", None).code(Dnf);
                b.boat("DSQ", None).code(Dsq);
                b.boat("RC", None).code(DnsRc);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 2), ("DNF", 3), ("DSQ", 5)]);
}

/// GSI 14.1.2 / D6: a DNS record scores starters + 1 and is not a starter, the same as having no record; the NSC boat
/// is a starter for it.
#[test]
fn gsi_14_1_2_dns_record_scores_starters_plus_one_and_is_not_a_starter() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00");
                b.boat("NSC", None).code(Nsc);
                b.boat("DNS", None).code(Dns);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 2), ("NSC", 3), ("DNS", 4)]);
}

/// D6 / D4: neither new code carries a penalty.
#[test]
fn d6_penalties_on_dns_and_dsq_are_ignored() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("DNS", None).code(Dns).penalty(2);
                b.boat("DSQ", None).code(Dsq).penalty(2);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("DNS", 3), ("DSQ", 3)]);
}

/// GSI 14.2: HOC boats are numbered by position on the course, after every timed finisher, whatever the input order.
#[test]
fn gsi_14_2_hoc_numbering() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("H3", None).hoc(3);
                b.boat("T2", None).finishes("19:30:00");
                b.boat("H1", None).hoc(1);
                b.boat("T1", None).finishes("19:00:00");
                b.boat("H2", None).hoc(2);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("T1", 1), ("T2", 2), ("H1", 3), ("H2", 4), ("H3", 5)]);
}

/// GSI 9.2 / 14.2: the penalised boat scores its finishing place plus the penalty.
#[test]
fn gsi_9_2_penalty_adds_to_the_finishing_place() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00").penalty(2);
                b.boat("B", None).finishes("19:01:00");
                b.boat("C", None).finishes("19:02:00");
                b.boat("D", None).finishes("19:03:00");
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 3)]);
}

/// D4 / RRS 44.3(c): no other boat's score changes, so ties are allowed.
#[test]
fn d4_penalty_leaves_other_boats_unchanged() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00").penalty(2);
                b.boat("B", None).finishes("19:01:00");
                b.boat("C", None).finishes("19:02:00");
                b.boat("D", None).finishes("19:03:00");
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 3), ("B", 2), ("C", 3), ("D", 4)]);
}

/// D4: a penalty is capped at the DNF score.
#[test]
fn d4_penalty_is_capped_at_dnf() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00").penalty(5);
                b.boat("DNF", None).code(Dnf);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 3), ("DNF", 3)]);
}

/// D4: several penalties are applied independently against each boat's own unpenalised place.
#[test]
fn d4_several_penalties_apply_independently() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00").penalty(2);
                b.boat("B", None).finishes("19:01:00").penalty(2);
                b.boat("C", None).finishes("19:02:00");
                b.boat("D", None).finishes("19:03:00");
                b.boat("E", None).finishes("19:04:00");
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 3), ("B", 4), ("C", 3), ("D", 4), ("E", 5)]);
}

/// D4: a penalty on a non-finisher is ignored; DNF, RET and NSC keep their code score.
#[test]
fn d4_penalty_on_a_non_finisher_is_ignored() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:01:00");
                b.boat("RET", None).code(Ret).penalty(1);
                b.boat("DNF", None).code(Dnf).penalty(2);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 2), ("RET", 3), ("DNF", 3)]);
}

/// D4: a penalty on an HOC finisher counts, since HOC is a finishing position (GSI 14.2).
#[test]
fn d4_penalty_on_hoc_adds_to_its_position() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("H1", None).hoc(1).penalty(1);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("H1", 3)]);
}

/// D7: equal corrected times (to the millisecond) share the place, and the next place skips.
#[test]
fn d7_equal_corrected_times_share_the_place_and_the_next_skips() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("B", None).finishes("19:00:00");
                b.boat("C", None).finishes("19:00:01");
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 1), ("C", 3)]);
}

#[test]
fn corrected_times_a_millisecond_apart_are_not_tied() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00.001");
                b.boat("B", None).finishes("19:00:00.000");
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("B", 1), ("A", 2)]);
}

/// D2: starters but no finishers: DNF and RET score 0 + 1 = 1, read literally from 14.1.1.
#[test]
fn starters_but_no_finishers_score_one() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).code(Dnf);
                b.boat("B", None).code(Ret);
            })
        })
    })
    .report(&HashMap::new());
    report.assert_bracket_scores(&[("A", 1), ("B", 1)]);
}

#[test]
fn class_places_span_brackets_and_bracket_places_do_not() {
    let report = race_with(
        RaceOpts {
            b_factor: 600,
            ..RaceOpts::default()
        },
        |r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("I-1", 50).finishes("19:00:00");
                    b.boat("I-2", 50).finishes("19:02:00");
                    b.boat("I-DNF", 50).code(Dnf);
                });
                c.bracket("II", |b| {
                    b.boat("II-1", 50).finishes("19:01:00");
                    b.boat("II-2", 50).finishes("19:03:00");
                });
            })
        },
    )
    .report(&HashMap::new());
    report.assert_bracket_scores(&[
        ("I-1", 1),
        ("I-2", 2),
        ("I-DNF", 3),
        ("II-1", 1),
        ("II-2", 2),
    ]);
    report.assert_class_scores(&[
        ("I-1", 1),
        ("II-1", 2),
        ("I-2", 3),
        ("II-2", 4),
        ("I-DNF", 5),
    ]);
}

/// R3 (orc_series_standings.md): the class report carries the race's ORC scoring option and bands, which the race
/// view's heading shows.
#[test]
fn class_report_carries_the_races_orc_scoring_option_and_bands() {
    let report = race_with(
        RaceOpts {
            date: Some("2026-06-17".into()),
            b_factor: 600,
            orc_scoring_option: OrcScoringOption::TripleNumberAllPurpose,
            ..RaceOpts::default()
        },
        |r| {
            r.orc_phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00");
                })
            });
            r.phrf_class("Half Fast", |c| {
                c.bracket("II", |b| {
                    b.boat("B", 100).finishes("19:00:00");
                })
            });
            r.cruising_class("WS", |c| {
                c.bracket("All", |b| {
                    b.boat("C", None).finishes("19:00:00");
                })
            });
        },
    )
    .report(&HashMap::new());
    assert_eq!(3, report.class_reports.len());
    for it in &report.class_reports {
        assert_eq!(
            OrcScoringOption::TripleNumberAllPurpose,
            it.orc_scoring_option,
            "{}",
            it.race_class.name
        );
        assert_eq!(Orc3Band::Low, it.orc3_band, "{}", it.race_class.name);
        assert_eq!(Orc5Band::LowMedium, it.orc5_band, "{}", it.race_class.name);
        assert_eq!(
            Some(format!("Low - {}", Orc3Band::Low.desc())),
            it.orc_band_label(),
            "{}",
            it.race_class.name
        );
    }
}

/// R1 (orc_series_standings.md): in a rated class each rating is placed among its own entries.
/// An ORC-only boat has no PHRF place and is not a PHRF starter or finisher, and a PHRF-only
/// boat has no ORC place. The bracket lists the PHRF-placed boats first, then the ORC-only ones.
#[test]
fn orc_places_are_among_orc_entries_and_phrf_places_among_phrf_entries() {
    let certs: HashMap<String, OrcCertificate> = [
        (
            "slow".to_string(),
            OrcCertificate {
                ref_no: "slow".into(),
                five_band_wl_low_med_tot: 1.0,
                ..Default::default()
            },
        ),
        (
            "fast".to_string(),
            OrcCertificate {
                ref_no: "fast".into(),
                five_band_wl_low_med_tot: 0.5,
                ..Default::default()
            },
        ),
    ]
    .into_iter()
    .collect();
    let report = race_with(
        RaceOpts {
            date: Some("2026-06-17".into()),
            b_factor: 600,
            ..RaceOpts::default()
        },
        |r| {
            r.orc_phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("Both A", 50).finishes("19:00:00").orc("slow"); // PHRF 1h, ORC 1h
                    b.boat("Both B", 50).finishes("19:30:00").orc("fast"); // PHRF 1h30, ORC 45m
                    b.boat_with("PHRF only", 50, phrf_only())
                        .finishes("19:10:00");
                    b.boat_with("PHRF DNF", 50, phrf_only()).code(Dnf);
                    b.boat_with(
                        "ORC only",
                        None,
                        BoatOpts {
                            rating_type: Some(RatingType::Orc),
                            ..Default::default()
                        },
                    )
                    .finishes("18:50:00")
                    .orc("fast"); // ORC 25m
                })
            })
        },
    )
    .report(&certs);
    // PHRF finishers are A, PHRF only and B, so DNF = 4; the ORC-only finisher is not counted
    report.assert_bracket_scores(&[
        ("Both A", 1),
        ("PHRF only", 2),
        ("Both B", 3),
        ("PHRF DNF", 4),
        ("ORC only", 0),
    ]);
    report.assert_class_scores(&[
        ("Both A", 1),
        ("PHRF only", 2),
        ("Both B", 3),
        ("PHRF DNF", 4),
        ("ORC only", 0),
    ]);
    let orc = scores(&[
        ("ORC only", 1.0),
        ("Both B", 2.0),
        ("Both A", 3.0),
        ("PHRF only", 0.0),
        ("PHRF DNF", 0.0),
    ]);
    assert_eq!(orc, report.bracket_scores_orc());
    assert_eq!(orc, report.class_scores_orc());
    assert_eq!(
        names(&["Both A", "PHRF only", "Both B", "PHRF DNF", "ORC only"]),
        bracket_order(&report)
    );
}

#[test]
fn orphan_results_land_in_the_incorrectly_classed_bucket() {
    let report = race(|r| {
        r.cruising_class("WS", |c| {
            c.bracket("All", |b| {
                b.boat("A", None).finishes("19:00:00");
                b.boat("Lost 2", None).orphan().finishes("19:30:00");
                b.boat("Lost 1", None).orphan().finishes("19:20:00");
            })
        })
    })
    .report(&HashMap::new());
    let orphans: Vec<_> = report
        .class_reports
        .iter()
        .filter(|c| c.race_class.name == "Incorrectly classed")
        .collect();
    assert_eq!(1, orphans.len());
    let orphans = orphans[0];
    assert_eq!(1, orphans.bracket_report.len());
    assert_eq!(
        "Invalid result records",
        orphans.bracket_report[0].bracket.name
    );
    // an orphan has no class start, so no elapsed time and no meaningful place
    assert_eq!(
        set(&["Lost 1", "Lost 2"]),
        orphans.bracket_report[0]
            .cards
            .iter()
            .map(|c| c.boat_name.clone())
            .collect::<HashSet<_>>()
    );
    assert_eq!(
        vec![None, None],
        orphans.bracket_report[0]
            .cards
            .iter()
            .map(|c| c.elapsed_time)
            .collect::<Vec<_>>()
    );
    let ws: Vec<_> = report
        .class_reports
        .iter()
        .filter(|c| c.race_class.name == "WS")
        .collect();
    assert_eq!(1, ws.len());
    assert_eq!(1, ws[0].bracket_report.len());
    assert_eq!(
        names(&["A"]),
        ws[0].bracket_report[0]
            .cards
            .iter()
            .map(|c| c.boat_name.clone())
            .collect::<Vec<_>>()
    );
    report.assert_bracket_scores(&[("A", 1)]);
}

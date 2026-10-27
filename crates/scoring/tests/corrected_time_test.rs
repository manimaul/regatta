//! = `CorrectedTimeTest.kt`: elapsed time, PHRF time-on-time (GSI 14.3), ORC correction and the season switch.

use jiff::{SignedDuration, Timestamp};
use pretty_assertions::assert_eq;
use regatta_common::{
    display, ClassSchedule, FinishCode, OrcCertificate, OrcScoringOption, PhrfBFactor, RaceClass,
    RaceResult, RatingType,
};
use regatta_scoring::effective_race_class;
use regatta_scoring::fixture::*;
use std::collections::HashMap;

fn hours(h: i64) -> SignedDuration {
    SignedDuration::from_hours(h)
}

fn certs(entries: &[(&str, OrcCertificate)]) -> HashMap<String, OrcCertificate> {
    entries
        .iter()
        .map(|(k, v)| (k.to_string(), v.clone()))
        .collect()
}

fn whole_millis(d: Option<SignedDuration>) -> Option<i64> {
    d.map(|d| d.as_millis() as i64)
}

#[test]
fn elapsed_time_is_from_the_boats_own_class_start() {
    let report = race_on("2026-06-17", "18:00", |r| {
        r.race_class("Fast", RatingType::Phrf, Some("18:00"), |c| {
            c.bracket("I", |b| {
                b.boat("A", 50).finishes("20:00:00");
            })
        });
        r.race_class("Half Fast", RatingType::Phrf, Some("18:05"), |c| {
            c.bracket("II", |b| {
                b.boat("B", 50).finishes("20:00:00");
            })
        });
    })
    .report(&HashMap::new());
    assert_eq!(Some(hours(2)), report.card("A").elapsed_time);
    assert_eq!(
        Some(hours(1) + SignedDuration::from_mins(55)),
        report.card("B").elapsed_time
    );
    assert_eq!(
        report.class_start(report.card("B").result_record.bracket.class_id),
        report.card("B").start_time
    );
}

/// ToT: corrected = elapsed × 650 / (B + rating), for each published B-factor.
#[test]
fn time_on_time_for_each_b_factor() {
    for b in PhrfBFactor::ENTRIES {
        let report = race_with(
            RaceOpts {
                b_factor: b.cf(),
                ..RaceOpts::default()
            },
            |r| {
                r.phrf_class("Fast", |c| {
                    c.bracket("I", |b| {
                        b.boat("A", 48).finishes("19:00:00");
                    })
                })
            },
        )
        .report(&HashMap::new());
        let card = report.card("A");
        let tcf = 650.0 / (f64::from(b.cf()) + 48.0);
        assert_eq!(tcf, card.phrf_tcf, "{}", b.name());
        assert_eq!(
            Some((3_600_000.0 * tcf) as i64),
            whole_millis(card.corrected_phrf_time),
            "{}",
            b.name()
        );
    }
}

#[test]
fn time_on_time_known_values() {
    let report = race_with(
        RaceOpts {
            b_factor: 600,
            ..RaceOpts::default()
        },
        |r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("Even", 50).finishes("19:00:00"); // 650 / 650 = 1.0
                    b.boat("Fast", 48).finishes("19:00:00"); // 650 / 648
                })
            })
        },
    )
    .report(&HashMap::new());
    assert_eq!(1.0, report.card("Even").phrf_tcf);
    assert_eq!(
        Some(3_600_000),
        whole_millis(report.card("Even").corrected_phrf_time)
    );
    assert_eq!(
        Some(3_611_111),
        whole_millis(report.card("Fast").corrected_phrf_time)
    );
}

#[test]
fn cruising_tcf_is_one() {
    let report = race_with(
        RaceOpts {
            b_factor: 480,
            ..RaceOpts::default()
        },
        |r| {
            r.cruising_class_with("Windseekers", true, None, |c| {
                c.bracket("A", |b| {
                    b.boat("A", 48).finishes("19:00:00");
                })
            })
        },
    )
    .report(&HashMap::new());
    assert_eq!(1.0, report.card("A").phrf_tcf);
    assert_eq!(Some(hours(1)), report.card("A").corrected_phrf_time);
}

/// D8 legacy behavior: a PHRF result saved without a rating scores with TCF 1.0.
#[test]
fn null_rating_on_legacy_row_gets_tcf_one() {
    let report = race_with(
        RaceOpts {
            b_factor: 600,
            ..RaceOpts::default()
        },
        |r| {
            r.phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", None).finishes("19:00:00");
                })
            })
        },
    )
    .report(&HashMap::new());
    assert_eq!(None, report.card("A").phrf_rating);
    assert_eq!(1.0, report.card("A").phrf_tcf);
    assert_eq!(Some(hours(1)), report.card("A").corrected_phrf_time);
}

#[test]
fn finish_before_start_gives_negative_elapsed_time() {
    let report = race_on("2026-06-17", "18:00", |r| {
        r.phrf_class("Fast", |c| {
            c.bracket("I", |b| {
                b.boat("A", 50).finishes("17:00:00");
            })
        })
    })
    .report(&HashMap::new());
    assert_eq!(Some(hours(-1)), report.card("A").elapsed_time);
    assert_eq!(Some(hours(-1)), report.card("A").corrected_phrf_time);
}

#[test]
fn elapsed_time_over_24_hours() {
    let report = race_on("2026-06-17", "18:00", |r| {
        r.phrf_class("Fast", |c| {
            c.bracket("I", |b| {
                b.boat("A", 50).finishes("2026-06-18 19:00:00");
            })
        })
    })
    .report(&HashMap::new());
    assert_eq!(Some(hours(25)), report.card("A").elapsed_time);
    assert_eq!(
        Some("25h:0m:0s".to_string()),
        report.card("A").elapsed_time.map(display)
    );
}

#[test]
fn millisecond_precision_is_kept() {
    let report = race_on("2026-06-17", "18:00", |r| {
        r.phrf_class("Fast", |c| {
            c.bracket("I", |b| {
                b.boat("A", 50).finishes("20:00:00.250");
                b.boat("B", 50).finishes("20:00:00.251");
            })
        })
    })
    .report(&HashMap::new());
    assert_eq!(
        Some(hours(2) + SignedDuration::from_millis(250)),
        report.card("A").elapsed_time
    );
    assert_eq!(
        Some(7_200_250),
        whole_millis(report.card("A").corrected_phrf_time)
    );
    assert_eq!(
        Some(7_200_251),
        whole_millis(report.card("B").corrected_phrf_time)
    );
    report.assert_bracket_scores(&[("A", 1), ("B", 2)]);
}

#[test]
fn orc_corrected_time_alongside_phrf_for_orc_phrf() {
    let cert = OrcCertificate {
        ref_no: "C1".into(),
        five_band_wl_low_med_tot: 0.9,
        five_band_wl_med_tot: 0.8,
        ..Default::default()
    };
    let report = race_with(
        RaceOpts {
            b_factor: 600,
            orc_scoring_option: OrcScoringOption::FiveBandWindwardLeeward,
            ..RaceOpts::default()
        },
        |r| {
            r.orc_phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00").orc("C1");
                    b.boat_with(
                        "B",
                        50,
                        BoatOpts {
                            rating_type: Some(RatingType::Phrf),
                            ..Default::default()
                        },
                    )
                    .finishes("19:00:00");
                })
            })
        },
    )
    .report(&certs(&[("C1", cert)]));
    let a = report.card("A");
    assert_eq!(1.0, a.phrf_tcf);
    assert_eq!(Some(hours(1)), a.corrected_phrf_time);
    assert_eq!(0.9, a.orc_tcf); // B 600 -> Orc5Band.LowMedium
    assert_eq!(Some(3_240_000), whole_millis(a.corrected_orc_time));
    let b = report.card("B");
    assert_eq!(1.0, b.orc_tcf);
    assert_eq!(Some(hours(1)), b.corrected_orc_time);
}

fn class_schedule(rating_type: RatingType, start: Timestamp) -> ClassSchedule {
    ClassSchedule {
        race_class: RaceClass {
            id: 1,
            name: "Fast".into(),
            rating_type,
            ..Default::default()
        },
        start_date: start,
        ..Default::default()
    }
}

/// The 2026 season starts in Pacific time: 2025-12-31 23:00 Pacific is 2026-01-01 07:00 UTC but still 2025.
#[test]
fn season_switch_uses_pacific_year() {
    let late_new_years_eve = pacific("2025-12-31", "23:00");
    let early_new_year = pacific("2026-01-01", "00:30");
    assert_eq!(
        RatingType::Phrf,
        effective_race_class(&class_schedule(RatingType::OrcPhrf, late_new_years_eve)).rating_type
    );
    assert_eq!(
        RatingType::OrcPhrf,
        effective_race_class(&class_schedule(RatingType::OrcPhrf, early_new_year)).rating_type
    );
}

#[test]
fn season_switch_before_2026_scores_rated_classes_as_phrf_only() {
    let in_2025 = pacific("2025-06-18", "18:00");
    assert_eq!(
        RatingType::Phrf,
        effective_race_class(&class_schedule(RatingType::Orc, in_2025)).rating_type
    );
    assert_eq!(
        RatingType::Phrf,
        effective_race_class(&class_schedule(RatingType::OrcPhrf, in_2025)).rating_type
    );
    assert_eq!(
        RatingType::Phrf,
        effective_race_class(&class_schedule(RatingType::Phrf, in_2025)).rating_type
    );
    assert_eq!(
        RatingType::CruisingFlyingSails,
        effective_race_class(&class_schedule(RatingType::CruisingFlyingSails, in_2025)).rating_type
    );
    assert_eq!(
        RatingType::CruisingNonFlyingSails,
        effective_race_class(&class_schedule(RatingType::CruisingNonFlyingSails, in_2025))
            .rating_type
    );
}

#[test]
fn season_switch_from_2026_keeps_the_class_as_is() {
    let in_2026 = pacific("2026-06-17", "18:00");
    for rt in RatingType::ENTRIES {
        assert_eq!(
            rt,
            effective_race_class(&class_schedule(rt, in_2026)).rating_type,
            "{}",
            rt.name()
        );
    }
}

#[test]
fn season_switch_with_no_start_scores_as_phrf_only() {
    let no_start = ClassSchedule {
        race_class: RaceClass {
            id: 1,
            rating_type: RatingType::OrcPhrf,
            ..Default::default()
        },
        ..Default::default()
    };
    assert_eq!(
        RatingType::Phrf,
        effective_race_class(&no_start).rating_type
    );
}

/// The card carries the certificate's ORC file id so the results table can link the reference number.
#[test]
fn card_carries_the_certificate_file_id() {
    let linked = OrcCertificate {
        ref_no: "C1".into(),
        five_band_wl_low_med_tot: 0.9,
        dxt_id: Some(264182),
        ..Default::default()
    };
    let unresolved = OrcCertificate {
        ref_no: "C2".into(),
        five_band_wl_low_med_tot: 0.9,
        ..Default::default()
    };
    let report = race_with(
        RaceOpts {
            date: Some("2026-06-17".into()),
            ..RaceOpts::default()
        },
        |r| {
            r.orc_phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00").orc("C1");
                    b.boat("B", 50).finishes("19:00:00").orc("C2");
                    b.boat_with(
                        "P",
                        50,
                        BoatOpts {
                            rating_type: Some(RatingType::Phrf),
                            ..Default::default()
                        },
                    )
                    .finishes("19:00:00");
                })
            })
        },
    )
    .report(&certs(&[("C1", linked), ("C2", unresolved)]));
    assert_eq!(Some(264182), report.card("A").orc_dxt_id);
    assert_eq!(
        Some("https://data.orc.org/public/WPub.dll/CC/264182".to_string()),
        report.card("A").orc_cert_url()
    );
    assert_eq!(None, report.card("B").orc_dxt_id);
    assert_eq!(None, report.card("B").orc_cert_url());
    assert_eq!(None, report.card("P").orc_cert_url());
}

/// Before 2026 the season switch only relabels the class as PHRF, which is what hides the ORC
/// columns in the web view. ORC places are still computed from each result's own rating type,
/// so hasOrcResults and the ORC places are set in both years.
#[test]
fn season_switch_relabels_the_class_but_still_computes_orc_places() {
    let cert = OrcCertificate {
        ref_no: "C1".into(),
        five_band_wl_low_med_tot: 0.9,
        ..Default::default()
    };
    let certs = certs(&[("C1", cert)]);
    let in_2025 = race_with(
        RaceOpts {
            date: Some("2025-06-18".into()),
            ..RaceOpts::default()
        },
        |r| {
            r.orc_phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00").orc("C1");
                })
            })
        },
    )
    .report(&certs);
    assert_eq!(1, in_2025.class_reports.len());
    assert_eq!(
        RatingType::Phrf,
        in_2025.class_reports[0].race_class.rating_type
    );
    assert!(in_2025.class_reports[0].has_orc_results);
    assert_eq!(Some(&1.0), in_2025.class_scores_orc().get("A"));
    let in_2026 = race_with(
        RaceOpts {
            date: Some("2026-06-17".into()),
            ..RaceOpts::default()
        },
        |r| {
            r.orc_phrf_class("Fast", |c| {
                c.bracket("I", |b| {
                    b.boat("A", 50).finishes("19:00:00").orc("C1");
                })
            })
        },
    )
    .report(&certs);
    assert_eq!(1, in_2026.class_reports.len());
    assert_eq!(
        RatingType::OrcPhrf,
        in_2026.class_reports[0].race_class.rating_type
    );
    assert!(in_2026.class_reports[0].has_orc_results);
    assert_eq!(Some(&1.0), in_2026.class_scores_orc().get("A"));
}

/// The Kotlin test drives `RaceResultsTable.rowToResult` with an Exposed `ResultRow`; that row
/// mapper belongs to the server crate (Phase 3). The body checks the same fallback on the
/// shared model's default (`RaceResult.finish_code` from `finish`), which is what the mapper
/// relies on.
///
/// F6: 733 rows in the production dump have a NULL finish_code. The row mapper infers TIME from a finish time, else RET.
#[test]
#[ignore = "RaceResultsTable.rowToResult is the database row mapper, ported with the server crate (Phase 3)"]
fn null_finish_code_falls_back_to_time_or_ret() {
    let finish = pacific("2026-06-17", "20:00");
    let timed: RaceResult = serde_json::from_str(&format!(
        r#"{{"id":1,"raceId":2,"boatId":3,"finish":"{}","ratingType":"PHRF","raceClassId":4,"bracketId":5}}"#,
        regatta_common::serde_kotlin::format_instant(finish)
    ))
    .unwrap();
    assert_eq!(FinishCode::Time, timed.finish_code);
    assert_eq!(Some(finish), timed.finish);
    let no_time: RaceResult = serde_json::from_str(
        r#"{"id":1,"raceId":2,"boatId":3,"finish":null,"ratingType":"PHRF","raceClassId":4,"bracketId":5}"#,
    )
    .unwrap();
    assert_eq!(FinishCode::Ret, no_time.finish_code);
    assert_eq!(None, no_time.finish);
}

/// See [`null_finish_code_falls_back_to_time_or_ret`].
#[test]
#[ignore = "RaceResultsTable.rowToResult is the database row mapper, ported with the server crate (Phase 3)"]
fn stored_finish_code_wins() {
    let finish = pacific("2026-06-17", "20:00");
    let dnf: RaceResult = serde_json::from_str(&format!(
        r#"{{"finish":"{}","finishCode":"DNF"}}"#,
        regatta_common::serde_kotlin::format_instant(finish)
    ))
    .unwrap();
    assert_eq!(FinishCode::Dnf, dnf.finish_code);
    let rc: RaceResult = serde_json::from_str(r#"{"finish":null,"finishCode":"DNS_RC"}"#).unwrap();
    assert_eq!(FinishCode::DnsRc, rc.finish_code);
}

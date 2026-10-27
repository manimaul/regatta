//! = `StandingsLabelsTest.kt`: series standings cell text and decoration (utils/standings_report.rs).
#![cfg(not(target_arch = "wasm32"))]

use regatta_app::utils::standings_report::*;
use regatta_common::{FinishCode, RaceStatus, RatingType, StandingsBoatSkipper, StandingsRace};

#[derive(Clone)]
struct R {
    code: Option<FinishCode>,
    bracket: f64,
    klass: f64,
    throw_out: bool,
    hoc: Option<i32>,
    bracket_sailed: bool,
    class_sailed: bool,
    status: RaceStatus,
    throw_out_class: Option<bool>,
    bracket_orc: f64,
    class_orc: f64,
    throw_out_orc: bool,
    throw_out_class_orc: Option<bool>,
    bracket_sailed_orc: bool,
    class_sailed_orc: Option<bool>,
    non_starter_orc: bool,
}

impl Default for R {
    fn default() -> Self {
        R {
            code: Some(FinishCode::Time),
            bracket: 3.0,
            klass: 5.0,
            throw_out: false,
            hoc: None,
            bracket_sailed: true,
            class_sailed: true,
            status: RaceStatus::Sailed,
            throw_out_class: None,
            bracket_orc: 0.0,
            class_orc: 0.0,
            throw_out_orc: false,
            throw_out_class_orc: None,
            bracket_sailed_orc: false,
            class_sailed_orc: None,
            non_starter_orc: false,
        }
    }
}

fn race(r: R) -> StandingsRace {
    StandingsRace {
        finish: r.code == Some(FinishCode::Time),
        place_in_bracket: r.bracket,
        place_in_class: r.klass,
        throw_out: r.throw_out,
        throw_out_class: r.throw_out_class.unwrap_or(r.throw_out),
        finish_code: r.code,
        hoc_position: r.hoc,
        non_starter: r.code.is_none(),
        bracket_sailed: r.bracket_sailed,
        class_sailed: r.class_sailed,
        status: r.status,
        place_in_bracket_orc: r.bracket_orc,
        place_in_class_orc: r.class_orc,
        throw_out_orc: r.throw_out_orc,
        throw_out_class_orc: r.throw_out_class_orc.unwrap_or(r.throw_out_orc),
        bracket_sailed_orc: r.bracket_sailed_orc,
        class_sailed_orc: r.class_sailed_orc.unwrap_or(r.bracket_sailed_orc),
        non_starter_orc: r.non_starter_orc,
    }
}

fn plain_race() -> StandingsRace {
    race(R::default())
}

fn coded(code: FinishCode) -> StandingsRace {
    race(R {
        code: Some(code),
        ..R::default()
    })
}

/// An ORC/PHRF race cell: PHRF 3 / 5, ORC 2 / 2, both groups sailed.
fn orc_race(
    code: Option<FinishCode>,
    throw_out: bool,
    throw_out_orc: bool,
    non_starter_orc: bool,
    bracket_sailed_orc: bool,
) -> StandingsRace {
    race(R {
        code,
        throw_out,
        bracket_orc: 2.0,
        class_orc: 2.0,
        throw_out_orc,
        bracket_sailed_orc,
        non_starter_orc,
        ..R::default()
    })
}

fn orc() -> StandingsRace {
    orc_race(Some(FinishCode::Time), false, false, false, true)
}

const RED: &[&str] = &["text-danger-fg"];

fn bold(text: &str, classes: &[&'static str]) -> ScoreSegment {
    ScoreSegment::bold(text, classes.to_vec())
}

fn plain(text: &str) -> ScoreSegment {
    ScoreSegment::new(text)
}

fn line(segments: Vec<ScoreSegment>, suffix: &str) -> ScoreLine {
    ScoreLine::new(segments, suffix)
}

fn row(total_bracket: f64, total_class: f64) -> StandingsBoatSkipper {
    StandingsBoatSkipper {
        total_score_bracket: total_bracket,
        total_score_class: total_class,
        ..StandingsBoatSkipper::default()
    }
}

#[test]
fn score_text() {
    assert_eq!(bracket_score_text(&plain_race()), "3");
    assert_eq!(class_score_text(&plain_race()), ", 5");
}

/// D1: an RC volunteer average is fractional and shown with one or two decimals.
#[test]
fn fractional_scores_are_formatted() {
    assert_eq!(
        bracket_score_text(&race(R {
            code: Some(FinishCode::DnsRc),
            bracket: 3.5,
            ..R::default()
        })),
        "3.5"
    );
    assert_eq!(
        class_score_text(&race(R {
            code: Some(FinishCode::DnsRc),
            klass: 10.0 / 3.0,
            ..R::default()
        })),
        ", 3.33"
    );
    assert_eq!(
        bracket_score_text(&race(R {
            bracket: 4.0,
            ..R::default()
        })),
        "4"
    );
    let r = row(10.5, 32.0 / 3.0);
    assert_eq!(total_bracket_text(&r), "10.5, ");
    assert_eq!(total_class_text(&r), "10.67");
}

#[test]
fn plain_score_has_no_decoration() {
    assert!(bracket_score_classes(&plain_race()).is_empty());
    assert!(class_score_classes(&plain_race()).is_empty());
}

#[test]
fn throw_out_is_red() {
    let r = race(R {
        throw_out: true,
        ..R::default()
    });
    assert_eq!(bracket_score_classes(&r), RED);
    assert_eq!(class_score_classes(&r), RED);
}

#[test]
fn code_suffix_per_finish_code() {
    assert_eq!(code_suffix(&coded(FinishCode::Time)), "");
    assert_eq!(code_suffix(&coded(FinishCode::Ret)), " RET");
    assert_eq!(code_suffix(&coded(FinishCode::Dnf)), " DNF");
    assert_eq!(code_suffix(&coded(FinishCode::Nsc)), " NSC");
    assert_eq!(
        code_suffix(&race(R {
            code: Some(FinishCode::Hoc),
            hoc: Some(3),
            ..R::default()
        })),
        " HOC3"
    );
    assert_eq!(code_suffix(&coded(FinishCode::DnsRc)), " DNS RC Volunteer");
    assert_eq!(code_suffix(&coded(FinishCode::Dns)), " DNS");
    assert_eq!(code_suffix(&coded(FinishCode::Dsq)), " DSQ");
    assert_eq!(
        code_suffix(&race(R {
            code: None,
            ..R::default()
        })),
        " DNS"
    );
    for code in FinishCode::ENTRIES {
        code_suffix(&race(R {
            code: Some(code),
            hoc: Some(1),
            ..R::default()
        })); // every code has a suffix
    }
}

#[test]
fn status_text_replaces_the_cell_for_unsailed_races() {
    assert_eq!(status_text_phrf(&plain_race()), None);
    assert_eq!(
        status_text_phrf(&race(R {
            code: None,
            status: RaceStatus::NotYetSailed,
            bracket_sailed: false,
            class_sailed: false,
            ..R::default()
        })),
        Some("Not yet sailed")
    );
    assert_eq!(
        status_text_phrf(&race(R {
            code: None,
            status: RaceStatus::Cancelled,
            bracket_sailed: false,
            class_sailed: false,
            ..R::default()
        })),
        Some("Cancelled")
    );
    assert_eq!(
        status_text_phrf(&race(R {
            code: None,
            bracket_sailed: false,
            class_sailed: false,
            ..R::default()
        })),
        Some("Not sailed")
    );
    // sailed in the class but not the bracket: the cell still shows, with a dash for the bracket
    assert_eq!(
        status_text_phrf(&race(R {
            code: None,
            bracket_sailed: false,
            class_sailed: true,
            ..R::default()
        })),
        None
    );
}

#[test]
fn unsailed_group_shows_a_dash() {
    assert_eq!(
        bracket_score_text(&race(R {
            code: None,
            bracket_sailed: false,
            ..R::default()
        })),
        "-"
    );
    assert_eq!(
        class_score_text(&race(R {
            code: None,
            class_sailed: false,
            ..R::default()
        })),
        ", -"
    );
}

#[test]
fn class_throw_out_decorates_only_the_class_score() {
    let r = race(R {
        throw_out: false,
        throw_out_class: Some(true),
        ..R::default()
    });
    assert!(bracket_score_classes(&r).is_empty());
    assert_eq!(class_score_classes(&r), RED);
}

#[test]
fn totals() {
    let r = row(12.0, 17.0);
    assert_eq!(total_bracket_text(&r), "12, ");
    assert_eq!(total_class_text(&r), "17");
}

// ---- D9: the ORC side

#[test]
fn orc_score_text_and_decoration() {
    assert_eq!(bracket_score_text_orc(&orc()), "2");
    assert_eq!(class_score_text_orc(&orc()), ", 2");
    assert_eq!(
        bracket_score_text_orc(&race(R {
            bracket_sailed_orc: false,
            ..R::default()
        })),
        "-"
    );
    assert_eq!(
        class_score_text_orc(&race(R {
            class_sailed_orc: Some(false),
            ..R::default()
        })),
        ", -"
    );
    assert!(bracket_score_classes_orc(&orc()).is_empty());
    assert_eq!(
        bracket_score_classes_orc(&orc_race(Some(FinishCode::Time), false, true, false, true)),
        RED
    );
    assert_eq!(
        class_score_classes_orc(&orc_race(Some(FinishCode::Time), false, true, false, true)),
        RED
    );
}

/// Without the ORC gate the race cell is the single unlabeled line it has always been, whatever the row's rating.
#[test]
fn score_lines_without_orc_is_the_single_unlabeled_line() {
    assert_eq!(
        score_lines(&orc(), false, true, RatingType::OrcPhrf),
        vec![line(vec![bold("3", &[]), bold(", 5", &[])], "")]
    );
    assert_eq!(
        score_lines(
            &orc_race(Some(FinishCode::Dnf), false, false, false, true),
            false,
            false,
            RatingType::Phrf
        ),
        vec![line(vec![bold("3", &[])], " DNF")]
    );
    assert_eq!(
        score_lines(
            &race(R {
                throw_out: true,
                ..R::default()
            }),
            false,
            false,
            RatingType::CruisingFlyingSails
        ),
        vec![line(vec![bold("3", RED)], "")]
    );
}

#[test]
fn score_lines_per_rating() {
    // both sides, the finish code on the last line
    assert_eq!(
        score_lines(
            &orc_race(Some(FinishCode::Dnf), false, false, false, true),
            true,
            true,
            RatingType::OrcPhrf
        ),
        vec![
            line(vec![bold("3", &[]), bold(", 5", &[])], " (PHRF)"),
            line(vec![bold("2", &[]), bold(", 2", &[])], " (ORC) DNF"),
        ]
    );
    // a PHRF-only row in an ORC class
    assert_eq!(
        score_lines(
            &orc_race(Some(FinishCode::Dnf), false, false, false, true),
            true,
            false,
            RatingType::Phrf
        ),
        vec![line(vec![bold("3", &[])], " (PHRF) DNF")]
    );
    // an ORC-only row
    assert_eq!(
        score_lines(&orc(), true, false, RatingType::Orc),
        vec![line(vec![bold("2", &[])], " (ORC)")]
    );
}

/// The PHRF and ORC throw-outs can be different races, so each line carries its own decoration.
#[test]
fn score_lines_decorate_each_side_on_its_own() {
    let phrf_out = score_lines(
        &orc_race(Some(FinishCode::Time), true, false, false, true),
        true,
        false,
        RatingType::OrcPhrf,
    );
    assert_eq!(
        phrf_out,
        vec![
            line(vec![bold("3", RED)], " (PHRF)"),
            line(vec![bold("2", &[])], " (ORC)")
        ]
    );
    let orc_out = score_lines(
        &orc_race(Some(FinishCode::Time), false, true, false, true),
        true,
        false,
        RatingType::OrcPhrf,
    );
    assert_eq!(
        orc_out,
        vec![
            line(vec![bold("3", &[])], " (PHRF)"),
            line(vec![bold("2", RED)], " (ORC)")
        ]
    );
}

/// A boat with a finish but no ORC entry that day is DNS on the ORC line only.
#[test]
fn score_lines_mark_an_orc_non_starter_on_the_orc_line() {
    let lines = score_lines(
        &orc_race(Some(FinishCode::Time), false, false, true, true),
        true,
        false,
        RatingType::OrcPhrf,
    );
    assert_eq!(
        lines.iter().map(|l| l.suffix.as_str()).collect::<Vec<_>>(),
        vec![" (PHRF)", " (ORC) DNS"]
    );
    // with no record at all both lines say DNS
    let none = score_lines(
        &orc_race(None, false, false, true, true),
        true,
        false,
        RatingType::OrcPhrf,
    );
    assert_eq!(
        none.iter().map(|l| l.suffix.as_str()).collect::<Vec<_>>(),
        vec![" (PHRF) DNS", " (ORC) DNS"]
    );
}

#[test]
fn score_lines_show_a_dash_for_an_unsailed_orc_group() {
    let lines = score_lines(
        &orc_race(Some(FinishCode::Time), false, false, false, false),
        true,
        true,
        RatingType::OrcPhrf,
    );
    assert_eq!(lines[1].segments, vec![bold("-", &[]), bold(", -", &[])]);
}

#[test]
fn status_text_per_rating() {
    // an ORC-only row in a race its ORC group did not sail
    assert_eq!(
        status_text(
            &race(R {
                code: None,
                bracket_sailed_orc: false,
                ..R::default()
            }),
            RatingType::Orc,
            true
        ),
        Some("Not sailed")
    );
    assert_eq!(
        status_text(
            &race(R {
                code: None,
                bracket_sailed_orc: true,
                ..R::default()
            }),
            RatingType::Orc,
            true
        ),
        None
    );
    // an ORC/PHRF row shows the cell when either side sailed
    assert_eq!(
        status_text(
            &race(R {
                code: None,
                bracket_sailed: false,
                class_sailed: false,
                bracket_sailed_orc: true,
                ..R::default()
            }),
            RatingType::OrcPhrf,
            true
        ),
        None
    );
    assert_eq!(
        status_text(
            &race(R {
                code: None,
                bracket_sailed: false,
                class_sailed: false,
                ..R::default()
            }),
            RatingType::OrcPhrf,
            true
        ),
        Some("Not sailed")
    );
    // outside the gate only the PHRF side counts
    assert_eq!(
        status_text(
            &race(R {
                code: None,
                bracket_sailed: false,
                class_sailed: false,
                bracket_sailed_orc: true,
                ..R::default()
            }),
            RatingType::OrcPhrf,
            false
        ),
        Some("Not sailed")
    );
}

#[test]
fn total_lines_per_rating() {
    let r = StandingsBoatSkipper {
        total_score_bracket: 12.0,
        total_score_class: 17.0,
        total_score_bracket_orc: 9.0,
        total_score_class_orc: 11.0,
        rating_type: RatingType::OrcPhrf,
        ..StandingsBoatSkipper::default()
    };
    assert_eq!(
        total_lines(&r, false, false),
        vec![line(vec![plain("17")], "")]
    );
    assert_eq!(
        total_lines(&r, false, true),
        vec![line(vec![bold("12, ", &[]), plain("17")], "")]
    );
    assert_eq!(
        total_lines(&r, true, true),
        vec![
            line(vec![bold("12, ", &[]), plain("17")], " (PHRF)"),
            line(vec![bold("9, ", &[]), plain("11")], " (ORC)")
        ]
    );
    assert_eq!(
        total_lines(&r, true, false),
        vec![
            line(vec![plain("17")], " (PHRF)"),
            line(vec![plain("11")], " (ORC)")
        ]
    );
    let orc_row = StandingsBoatSkipper {
        rating_type: RatingType::Orc,
        ..r.clone()
    };
    assert_eq!(
        total_lines(&orc_row, true, false),
        vec![line(vec![plain("11")], " (ORC)")]
    );
    let phrf_row = StandingsBoatSkipper {
        rating_type: RatingType::Phrf,
        ..r.clone()
    };
    assert_eq!(
        total_lines(&phrf_row, true, false),
        vec![line(vec![plain("17")], " (PHRF)")]
    );
}

#[test]
fn place_lines_per_rating() {
    let r = StandingsBoatSkipper {
        place_in_bracket: 2,
        place_in_class: 4,
        place_in_bracket_orc: 1,
        place_in_class_orc: 3,
        rating_type: RatingType::OrcPhrf,
        ..StandingsBoatSkipper::default()
    };
    assert_eq!(bracket_place_lines(&r, false), vec!["2"]);
    assert_eq!(class_place_lines(&r, false), vec!["4"]);
    assert_eq!(bracket_place_lines(&r, true), vec!["2(PHRF), ", "1(ORC)"]);
    assert_eq!(class_place_lines(&r, true), vec!["4(PHRF), ", "3(ORC)"]);
    assert_eq!(
        bracket_place_lines(
            &StandingsBoatSkipper {
                rating_type: RatingType::Phrf,
                ..r.clone()
            },
            true
        ),
        vec!["2(PHRF)"]
    );
    assert_eq!(
        bracket_place_lines(
            &StandingsBoatSkipper {
                rating_type: RatingType::Orc,
                ..r.clone()
            },
            true
        ),
        vec!["1(ORC)"]
    );
    // an ORC/PHRF row without an ORC place yet falls back to the PHRF place
    assert_eq!(
        bracket_place_lines(
            &StandingsBoatSkipper {
                place_in_bracket_orc: 0,
                ..r.clone()
            },
            true
        ),
        vec!["2(PHRF)"]
    );
}

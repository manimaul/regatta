//! Tests of the race results labels in `crates/app/src/utils/race_report.rs`: finish, elapsed and corrected time
//! text, and the place columns.
#![cfg(not(target_arch = "wasm32"))]

use jiff::{SignedDuration, Timestamp};
use regatta_app::utils::date_util::ts_time_str;
use regatta_app::utils::race_report::*;
use regatta_common::{
    FinishCode, RaceReportCard, RaceResult, RaceResultBoatBracket, RaceResultFull, RatingType,
};

fn start() -> Timestamp {
    Timestamp::from_second(1_780_000_000).unwrap()
}

fn finish() -> Timestamp {
    start() + SignedDuration::from_secs(2 * 3600 + 30 * 60 + 15)
}

fn card(
    code: FinishCode,
    finish_time: Option<Timestamp>,
    start_time: Option<Timestamp>,
    hoc: Option<i32>,
    penalty: Option<i32>,
) -> RaceReportCard {
    RaceReportCard {
        result_record: RaceResultBoatBracket {
            result: RaceResult {
                finish: finish_time,
                finish_code: code,
                hoc_position: hoc,
                penalty,
                ..RaceResult::default()
            },
            ..RaceResultBoatBracket::default()
        },
        start_time,
        finish_time,
        hoc_position: hoc,
        penalty,
        ..RaceReportCard::default()
    }
}

#[test]
fn finish_code_text_for_every_code() {
    assert_eq!(finish_code_text(FinishCode::Time, None), "TIME");
    assert_eq!(finish_code_text(FinishCode::Ret, None), "RET");
    assert_eq!(finish_code_text(FinishCode::Dnf, None), "DNF");
    assert_eq!(finish_code_text(FinishCode::Nsc, None), "NSC");
    assert_eq!(finish_code_text(FinishCode::Hoc, Some(3)), "HOC 3");
    assert_eq!(finish_code_text(FinishCode::Hoc, None), "HOC ");
    assert_eq!(
        finish_code_text(FinishCode::DnsRc, None),
        "DNS RC Volunteer"
    );
    assert_eq!(finish_code_text(FinishCode::Dns, None), "DNS");
    assert_eq!(finish_code_text(FinishCode::Dsq, None), "DSQ");
    for code in FinishCode::ENTRIES {
        finish_code_text(code, Some(1)); // every code has a label
    }
}

#[test]
fn card_finish_text_shows_the_local_finish_time() {
    assert_eq!(
        card_finish_text(&card(
            FinishCode::Time,
            Some(finish()),
            Some(start()),
            None,
            None
        )),
        ts_time_str(finish())
    );
}

#[test]
fn card_finish_text_appends_the_penalty() {
    assert_eq!(
        card_finish_text(&card(
            FinishCode::Time,
            Some(finish()),
            Some(start()),
            None,
            Some(2)
        )),
        format!("{} P+2", ts_time_str(finish()))
    );
}

#[test]
fn card_finish_text_without_a_start_falls_back_to_the_code() {
    assert_eq!(
        card_finish_text(&card(FinishCode::Time, Some(finish()), None, None, None)),
        "TIME"
    );
    assert_eq!(
        card_finish_text(&card(FinishCode::Dnf, None, Some(start()), None, None)),
        "DNF"
    );
    assert_eq!(
        card_finish_text(&card(FinishCode::Hoc, None, Some(start()), Some(2), None)),
        "HOC 2"
    );
}

#[test]
fn full_result_finish_text() {
    let full = RaceResultFull {
        finish: Some(finish()),
        finish_code: FinishCode::Time,
        penalty: Some(1),
        ..RaceResultFull::default()
    };
    assert_eq!(
        full_finish_text(&full, Some(start())),
        format!("{} P+1", ts_time_str(finish()))
    );
    assert_eq!(full_finish_text(&full, None), "TIME");
    let rc = RaceResultFull {
        finish_code: FinishCode::DnsRc,
        ..RaceResultFull::default()
    };
    assert_eq!(full_finish_text(&rc, Some(start())), "DNS RC Volunteer");
}

#[test]
fn elapsed_text_test() {
    assert_eq!(elapsed_text(&RaceReportCard::default()), "n/a");
    let c = RaceReportCard {
        elapsed_time: Some(SignedDuration::from_secs(2 * 3600 + 30 * 60 + 15)),
        ..RaceReportCard::default()
    };
    assert_eq!(elapsed_text(&c), "2h:30m:15s");
}

#[test]
fn corrected_time_text_with_tcf_to_four_decimals() {
    let c = RaceReportCard {
        phrf_tcf: 650.0 / 648.0,
        orc_tcf: 0.98765432,
        corrected_phrf_time: Some(SignedDuration::from_hours(1)),
        corrected_orc_time: Some(SignedDuration::from_secs(59 * 60 + 15)),
        ..RaceReportCard::default()
    };
    assert_eq!(cor_time_phrf_text(&c), "1h:0m:0s (1.0031 PHRF)");
    assert_eq!(cor_time_orc_text(&c), "0h:59m:15s (0.9877 ORC)");
}

#[test]
fn corrected_time_text_without_a_time_is_na() {
    let phrf = RaceReportCard {
        phrf_tcf: 1.5,
        ..RaceReportCard::default()
    };
    let orc = RaceReportCard {
        orc_tcf: 1.5,
        ..RaceReportCard::default()
    };
    assert_eq!(cor_time_phrf_text(&phrf), "n/a");
    assert_eq!(cor_time_orc_text(&orc), "n/a");
}

fn placed(rating_type: RatingType, orc: bool) -> RaceReportCard {
    RaceReportCard {
        result_record: RaceResultBoatBracket {
            result: RaceResult {
                rating_type,
                ..RaceResult::default()
            },
            ..RaceResultBoatBracket::default()
        },
        place_in_bracket: 3,
        place_in_class: 2,
        place_in_bracket_orc: if orc { 4 } else { 0 },
        place_in_class_orc: if orc { 5 } else { 0 },
        ..RaceReportCard::default()
    }
}

#[test]
fn multi_bracket_places_without_orc_are_the_bare_numbers() {
    for rt in RatingType::ENTRIES {
        assert_eq!(
            bracket_place_lines(&placed(rt, true), false),
            vec!["3"],
            "{}",
            rt.name()
        );
        assert_eq!(
            class_place_lines(&placed(rt, true), false),
            vec!["2"],
            "{}",
            rt.name()
        );
    }
}

#[test]
fn multi_bracket_places_for_phrf_in_an_orc_class() {
    assert_eq!(
        bracket_place_lines(&placed(RatingType::Phrf, true), true),
        vec!["3(PHRF)"]
    );
    assert_eq!(
        class_place_lines(&placed(RatingType::Phrf, true), true),
        vec!["2(PHRF)"]
    );
}

#[test]
fn multi_bracket_places_for_orc() {
    assert_eq!(
        bracket_place_lines(&placed(RatingType::Orc, true), true),
        vec!["4(ORC)"]
    );
    assert_eq!(
        class_place_lines(&placed(RatingType::Orc, true), true),
        vec!["5(ORC)"]
    );
}

#[test]
fn multi_bracket_places_for_orc_phrf_bracket_column() {
    assert_eq!(
        bracket_place_lines(&placed(RatingType::OrcPhrf, true), true),
        vec!["3(PHRF), ", "4(ORC)"]
    );
    // without an ORC place it reads as PHRF
    assert_eq!(
        bracket_place_lines(&placed(RatingType::OrcPhrf, false), true),
        vec!["3(PHRF)"]
    );
    assert_eq!(
        class_place_lines(&placed(RatingType::OrcPhrf, false), true),
        vec!["2(PHRF)"]
    );
}

/// D7 (`test_audit_and_plan.md`): the class column shows the ORC place in class, not in bracket.
#[test]
fn d7_multi_bracket_orc_phrf_class_column_shows_class_orc_place() {
    assert_eq!(
        class_place_lines(&placed(RatingType::OrcPhrf, true), true),
        vec!["2(PHRF), ", "5(ORC)"]
    );
}

#[test]
fn multi_bracket_places_for_cruising_in_an_orc_class_are_empty() {
    assert!(bracket_place_lines(&placed(RatingType::CruisingFlyingSails, true), true).is_empty());
    assert!(class_place_lines(&placed(RatingType::CruisingNonFlyingSails, true), true).is_empty());
}

#[test]
fn single_bracket_places() {
    assert_eq!(
        single_bracket_place_lines(&placed(RatingType::Phrf, true)),
        vec!["3(PHRF)"]
    );
    assert_eq!(
        single_bracket_place_lines(&placed(RatingType::Orc, true)),
        vec!["4(ORC)"]
    );
    assert_eq!(
        single_bracket_place_lines(&placed(RatingType::OrcPhrf, true)),
        vec!["3(PHRF), ", "4(ORC)"]
    );
    assert_eq!(
        single_bracket_place_lines(&placed(RatingType::OrcPhrf, false)),
        vec!["3(PHRF)"]
    );
    assert_eq!(
        single_bracket_place_lines(&placed(RatingType::CruisingFlyingSails, true)),
        vec!["3"]
    );
    assert_eq!(
        single_bracket_place_lines(&placed(RatingType::CruisingNonFlyingSails, true)),
        vec!["3"]
    );
}

//! = `RaceConfigTest.kt`: race config edits shared by the RC page, the races editor and the Config modal (utils/race_config.rs, R4a).
#![cfg(not(target_arch = "wasm32"))]

use jiff::Timestamp;
use regatta_app::utils::race_config::{
    time_limit, time_limit_text, with_class_start, with_orc_option, with_phrf_b_factor,
};
use regatta_common::{
    ClassSchedule, Orc3Band, Orc5Band, OrcScoringOption, Race, RaceClass, RaceSchedule,
};

fn ts(s: &str) -> Timestamp {
    s.parse().unwrap()
}

fn schedule() -> RaceSchedule {
    RaceSchedule {
        race: Race {
            name: "R".to_string(),
            phrf_b_factor: 600,
            orc_scoring_option: OrcScoringOption::FiveBandWindwardLeeward,
            ..Race::default()
        },
        schedule: vec![
            ClassSchedule {
                race_class: RaceClass {
                    id: 1,
                    name: "Fast".to_string(),
                    ..RaceClass::default()
                },
                start_date: ts("2026-06-17T17:00:00Z"),
                ..ClassSchedule::default()
            },
            ClassSchedule {
                race_class: RaceClass {
                    id: 2,
                    name: "Half Fast".to_string(),
                    ..RaceClass::default()
                },
                start_date: ts("2026-06-17T17:05:00Z"),
                ..ClassSchedule::default()
            },
        ],
        ..RaceSchedule::default()
    }
}

#[test]
fn b_factor_derives_both_bands() {
    let r = with_phrf_b_factor(&schedule(), Some(550)).race;
    assert_eq!(r.phrf_b_factor, 550);
    assert_eq!(r.orc3_band, Orc3Band::Medium);
    assert_eq!(r.orc5_band, Orc5Band::Medium);
    assert_eq!(
        with_phrf_b_factor(&schedule(), Some(480)).race.orc5_band,
        Orc5Band::High
    );
}

#[test]
fn null_b_factor_is_the_default() {
    let r = with_phrf_b_factor(&schedule(), None).race;
    assert_eq!(r.phrf_b_factor, 600);
    assert_eq!(r.orc5_band, Orc5Band::LowMedium);
}

#[test]
fn a_five_band_choice_sets_the_b_factor_and_derives_the_three_band() {
    let r = with_orc_option(
        &schedule(),
        OrcScoringOption::FiveBandWindwardLeeward,
        Orc3Band::Low,
        Orc5Band::High,
    )
    .race;
    assert_eq!(r.phrf_b_factor, 480);
    assert_eq!(r.orc5_band, Orc5Band::High);
    assert_eq!(r.orc3_band, Orc3Band::High);
}

#[test]
fn a_three_band_choice_sets_the_b_factor_and_derives_the_five_band() {
    let r = with_orc_option(
        &schedule(),
        OrcScoringOption::TripleNumberAllPurpose,
        Orc3Band::Medium,
        Orc5Band::Low,
    )
    .race;
    assert_eq!(
        r.orc_scoring_option,
        OrcScoringOption::TripleNumberAllPurpose
    );
    assert_eq!(r.phrf_b_factor, 550);
    assert_eq!(r.orc3_band, Orc3Band::Medium);
    assert_eq!(r.orc5_band, Orc5Band::Medium);
}

#[test]
fn a_single_number_choice_keeps_the_b_factor() {
    let r = with_orc_option(
        &schedule(),
        OrcScoringOption::SingleNumberAllPurpose,
        Orc3Band::High,
        Orc5Band::High,
    )
    .race;
    assert_eq!(r.phrf_b_factor, 600);
    assert_eq!(
        r.orc_scoring_option,
        OrcScoringOption::SingleNumberAllPurpose
    );
    assert_eq!(r.orc3_band, Orc3Band::Low);
    assert_eq!(r.orc5_band, Orc5Band::LowMedium);
}

#[test]
fn class_start_changes_only_that_class() {
    let start = ts("2026-06-17T17:30:00Z");
    let s0 = schedule();
    let s = with_class_start(&s0, &s0.schedule[1], start);
    assert_eq!(s0.schedule[0], s.schedule[0]);
    assert_eq!(s.schedule[1].start_date, start);
    assert_eq!(s0.schedule[1].end_date, s.schedule[1].end_date);
    assert_eq!(s0.race, s.race);
}

#[test]
fn class_start_keeps_the_time_limit() {
    let mut s0 = schedule();
    s0.schedule[0].end_date = ts("2026-06-17T21:00:00Z");
    assert_eq!(
        time_limit_text(time_limit(&s0.schedule[0]).unwrap()),
        "4:00"
    );
    let s = with_class_start(&s0, &s0.schedule[0], ts("2026-06-17T17:05:00Z"));
    assert_eq!(s.schedule[0].start_date, ts("2026-06-17T17:05:00Z"));
    assert_eq!(s.schedule[0].end_date, ts("2026-06-17T21:05:00Z"));
    assert_eq!(time_limit_text(time_limit(&s.schedule[0]).unwrap()), "4:00");
}

#[test]
fn time_limit_text_pads_minutes() {
    assert_eq!(time_limit_text(jiff::SignedDuration::from_mins(95)), "1:35");
    assert!(time_limit(&schedule().schedule[0]).is_none());
}

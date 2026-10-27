//! = `RaceValidationTest.kt`: validateRace guards the POST race and POST raceSchedule routes (R4a, orc_series_standings.md).

use pretty_assertions::assert_eq;
use regatta_common::{Orc3Band, Orc5Band, OrcScoringOption, Race};
use regatta_scoring::validate_race;

fn race(
    cf: i32,
    option: OrcScoringOption,
    orc3_band: Option<Orc3Band>,
    orc5_band: Option<Orc5Band>,
) -> Race {
    Race {
        name: "R".into(),
        phrf_b_factor: cf,
        orc_scoring_option: option,
        orc3_band: orc3_band.unwrap_or_else(|| Orc3Band::from_phrf_b_factor(cf)),
        orc5_band: orc5_band.unwrap_or_else(|| Orc5Band::from_phrf_b_factor(cf)),
        ..Default::default()
    }
}

#[test]
fn every_band_is_accepted_with_its_own_b_factor() {
    for band in Orc3Band::ENTRIES {
        let r = race(
            band.phrf_b_factor(),
            OrcScoringOption::TripleNumberAllPurpose,
            Some(band),
            None,
        );
        assert_eq!(None, validate_race(&r), "3-band {}", band.name());
    }
    for band in Orc5Band::ENTRIES {
        let r = race(
            band.phrf_b_factor(),
            OrcScoringOption::FiveBandWindwardLeeward,
            None,
            Some(band),
        );
        assert_eq!(None, validate_race(&r), "5-band {}", band.name());
    }
}

#[test]
fn r4a_rejects_a_5_band_option_whose_band_does_not_follow_the_b_factor() {
    assert_eq!(
        Some("PHRF B factor 550 is Medium, but the ORC wind band is Low/Medium".to_string()),
        validate_race(&race(
            550,
            OrcScoringOption::FiveBandWindwardLeeward,
            None,
            Some(Orc5Band::LowMedium)
        ))
    );
}

#[test]
fn r4a_rejects_a_3_band_option_whose_band_does_not_follow_the_b_factor() {
    assert_eq!(
        Some("PHRF B factor 550 is Medium, but the ORC wind band is Low".to_string()),
        validate_race(&race(
            550,
            OrcScoringOption::TripleNumberAllPurpose,
            Some(Orc3Band::Low),
            None
        ))
    );
}

/// Only the band in use is checked: the RC page leaves the other band as it was.
#[test]
fn the_unused_band_may_disagree() {
    assert_eq!(
        None,
        validate_race(&race(
            550,
            OrcScoringOption::FiveBandWindwardLeeward,
            Some(Orc3Band::Low),
            None
        ))
    );
    assert_eq!(
        None,
        validate_race(&race(
            550,
            OrcScoringOption::TripleNumberWindwardLeeward,
            None,
            Some(Orc5Band::LowMedium)
        ))
    );
}

#[test]
fn a_single_number_option_is_accepted_with_any_bands() {
    assert_eq!(
        None,
        validate_race(&race(
            550,
            OrcScoringOption::SingleNumberAllPurpose,
            Some(Orc3Band::Low),
            Some(Orc5Band::High)
        ))
    );
}

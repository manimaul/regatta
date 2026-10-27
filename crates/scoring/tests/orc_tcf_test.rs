//! = `OrcTcfTest.kt`: orcTcf picks one certificate field per scoring option and wind band. Every field has a distinct value here.

use pretty_assertions::assert_eq;
use regatta_common::OrcScoringOption::*;
use regatta_common::{Orc3Band, Orc5Band, OrcCertificate, OrcScoringOption};
use regatta_scoring::orc_tcf;
use std::collections::HashSet;

fn cert() -> OrcCertificate {
    OrcCertificate {
        all_purpose_tot: 1.001,
        wl_single_number_tot: 1.002,
        triple_number_all_purpose_low_tot: 1.011,
        triple_number_all_purpose_med_tot: 1.012,
        triple_number_all_purpose_hi_tot: 1.013,
        triple_number_wl_low_tot: 1.021,
        triple_number_wl_med_tot: 1.022,
        triple_number_wl_hi_tot: 1.023,
        single_number_predominant_upwind_tot: 1.031,
        single_number_predominant_reaching_tot: 1.032,
        single_number_predominant_downwind_tot: 1.033,
        predominant_upwind_low_tot: 1.041,
        predominant_upwind_low_med_tot: 1.042,
        predominant_upwind_med_tot: 1.043,
        predominant_upwind_med_hi_tot: 1.044,
        predominant_upwind_hi_tot: 1.045,
        predominant_downwind_low_tot: 1.051,
        predominant_downwind_low_med_tot: 1.052,
        predominant_downwind_med_tot: 1.053,
        predominant_downwind_med_hi_tot: 1.054,
        predominant_downwind_hi_tot: 1.055,
        predominant_reaching_low_tot: 1.061,
        predominant_reaching_low_med_tot: 1.062,
        predominant_reaching_med_tot: 1.063,
        predominant_reaching_med_hi_tot: 1.064,
        predominant_reaching_hi_tot: 1.065,
        five_band_wl_low_tot: 1.071,
        five_band_wl_low_med_tot: 1.072,
        five_band_wl_med_tot: 1.073,
        five_band_wl_med_hi_tot: 1.074,
        five_band_wl_hi_tot: 1.075,
        us_wl6040_l_tot: 1.081,
        us_wl6040_lm_tot: 1.082,
        us_wl6040_m_tot: 1.083,
        us_wl6040_mh_tot: 1.084,
        us_wl6040_h_tot: 1.085,
        five_band_all_purpose_low_tot: 1.091,
        five_band_all_purpose_low_med_tot: 1.092,
        five_band_all_purpose_med_tot: 1.093,
        five_band_all_purpose_med_hi_tot: 1.094,
        five_band_all_purpose_hi_tot: 1.095,
        ..Default::default()
    }
}

const SINGLE_NUMBER: [(OrcScoringOption, f64); 5] = [
    (SingleNumberAllPurpose, 1.001),
    (SingleNumberWindwardLeeward, 1.002),
    (SingleNumberPredominantUpwind, 1.031),
    (SingleNumberPredominantReaching, 1.032),
    (SingleNumberPredominantDownwind, 1.033),
];

const THREE_BAND: [(OrcScoringOption, [f64; 3]); 2] = [
    (TripleNumberAllPurpose, [1.011, 1.012, 1.013]),
    (TripleNumberWindwardLeeward, [1.021, 1.022, 1.023]),
];

const FIVE_BAND: [(OrcScoringOption, [f64; 5]); 6] = [
    (PredominantUpwind, [1.041, 1.042, 1.043, 1.044, 1.045]),
    (PredominantDownwind, [1.051, 1.052, 1.053, 1.054, 1.055]),
    (PredominantReaching, [1.061, 1.062, 1.063, 1.064, 1.065]),
    (FiveBandWindwardLeeward, [1.071, 1.072, 1.073, 1.074, 1.075]),
    (WindwardLeeward6040, [1.081, 1.082, 1.083, 1.084, 1.085]),
    (FiveBandAllPurpose, [1.091, 1.092, 1.093, 1.094, 1.095]),
];

#[test]
fn every_option_is_covered() {
    let covered: HashSet<OrcScoringOption> = SINGLE_NUMBER
        .iter()
        .map(|(o, _)| *o)
        .chain(THREE_BAND.iter().map(|(o, _)| *o))
        .chain(FIVE_BAND.iter().map(|(o, _)| *o))
        .collect();
    let all: HashSet<OrcScoringOption> = OrcScoringOption::ENTRIES.into_iter().collect();
    assert_eq!(all, covered);
}

#[test]
fn single_number_options_ignore_the_bands() {
    let cert = cert();
    for (option, expected) in SINGLE_NUMBER {
        assert_eq!(
            Ok(expected),
            orc_tcf(None, None, option, &cert),
            "{}",
            option.name()
        );
        for b3 in Orc3Band::ENTRIES {
            for b5 in Orc5Band::ENTRIES {
                assert_eq!(
                    Ok(expected),
                    orc_tcf(Some(b3), Some(b5), option, &cert),
                    "{} {b3:?} {b5:?}",
                    option.name()
                );
            }
        }
    }
}

#[test]
fn three_band_options_follow_orc3_band() {
    let cert = cert();
    for (option, expected) in THREE_BAND {
        for (i, b3) in Orc3Band::ENTRIES.into_iter().enumerate() {
            // the 5-band value must not matter
            for b5 in Orc5Band::ENTRIES {
                assert_eq!(
                    Ok(expected[i]),
                    orc_tcf(Some(b3), Some(b5), option, &cert),
                    "{} {b3:?} {b5:?}",
                    option.name()
                );
            }
            assert_eq!(
                Ok(expected[i]),
                orc_tcf(Some(b3), None, option, &cert),
                "{} {b3:?}",
                option.name()
            );
        }
    }
}

#[test]
fn five_band_options_follow_orc5_band() {
    let cert = cert();
    for (option, expected) in FIVE_BAND {
        for (i, b5) in Orc5Band::ENTRIES.into_iter().enumerate() {
            // the 3-band value must not matter
            for b3 in Orc3Band::ENTRIES {
                assert_eq!(
                    Ok(expected[i]),
                    orc_tcf(Some(b3), Some(b5), option, &cert),
                    "{} {b3:?} {b5:?}",
                    option.name()
                );
            }
            assert_eq!(
                Ok(expected[i]),
                orc_tcf(None, Some(b5), option, &cert),
                "{} {b5:?}",
                option.name()
            );
        }
    }
}

/// Kotlin's `requireNotNull` throws `IllegalArgumentException`; here the missing band is an `Err`.
#[test]
fn banded_options_require_their_band() {
    let cert = cert();
    for (option, _) in THREE_BAND {
        assert!(
            orc_tcf(None, Some(Orc5Band::Low), option, &cert).is_err(),
            "{}",
            option.name()
        );
    }
    for (option, _) in FIVE_BAND {
        assert!(
            orc_tcf(Some(Orc3Band::Low), None, option, &cert).is_err(),
            "{}",
            option.name()
        );
    }
}

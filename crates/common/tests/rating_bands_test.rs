//! = `RatingBandsTest.kt`: band edges of the PHRF B-factor and the ORC wind bands derived from it.

use regatta_common::{Orc3Band, Orc5Band, OrcCertificate, OrcScoringOption, PhrfBFactor};

#[test]
fn phrf_b_factor_above_calm_is_still_calm() {
    assert_eq!(PhrfBFactor::Calm, PhrfBFactor::from_b_factor(651));
    assert_eq!(PhrfBFactor::Calm, PhrfBFactor::from_b_factor(1000));
}

#[test]
fn phrf_b_factor_edges() {
    assert_eq!(PhrfBFactor::Hvy, PhrfBFactor::from_b_factor(480));
    assert_eq!(PhrfBFactor::Hvy, PhrfBFactor::from_b_factor(515));
    assert_eq!(PhrfBFactor::Avg, PhrfBFactor::from_b_factor(516));
    assert_eq!(PhrfBFactor::Avg, PhrfBFactor::from_b_factor(550));
    assert_eq!(PhrfBFactor::Avg, PhrfBFactor::from_b_factor(575));
    assert_eq!(PhrfBFactor::Light, PhrfBFactor::from_b_factor(576));
    assert_eq!(PhrfBFactor::Light, PhrfBFactor::from_b_factor(600));
    assert_eq!(PhrfBFactor::Light, PhrfBFactor::from_b_factor(625));
    assert_eq!(PhrfBFactor::Calm, PhrfBFactor::from_b_factor(626));
    assert_eq!(PhrfBFactor::Calm, PhrfBFactor::from_b_factor(650));
}

#[test]
fn orc3_band_edges() {
    for cf in 0..=700 {
        let expected = if cf >= 600 {
            Orc3Band::Low
        } else if cf >= 550 {
            Orc3Band::Medium
        } else {
            Orc3Band::High
        };
        assert_eq!(expected, Orc3Band::from_phrf_b_factor(cf), "cf = {cf}");
    }
}

#[test]
fn orc3_band_from_each_phrf_b_factor() {
    assert_eq!(
        Orc3Band::Low,
        Orc3Band::from_phrf_b_factor(PhrfBFactor::Calm.cf())
    );
    assert_eq!(
        Orc3Band::Low,
        Orc3Band::from_phrf_b_factor(PhrfBFactor::Light.cf())
    );
    assert_eq!(
        Orc3Band::Medium,
        Orc3Band::from_phrf_b_factor(PhrfBFactor::Avg.cf())
    );
    assert_eq!(
        Orc3Band::High,
        Orc3Band::from_phrf_b_factor(PhrfBFactor::Hvy.cf())
    );
}

#[test]
fn orc5_band_edges() {
    for cf in 0..=700 {
        let expected = if cf >= 650 {
            Orc5Band::Low
        } else if cf >= 600 {
            Orc5Band::LowMedium
        } else if cf >= 550 {
            Orc5Band::Medium
        } else if cf >= 510 {
            Orc5Band::MediumHigh
        } else {
            Orc5Band::High
        };
        assert_eq!(expected, Orc5Band::from_phrf_b_factor(cf), "cf = {cf}");
    }
}

#[test]
fn orc5_band_from_each_phrf_b_factor() {
    assert_eq!(
        Orc5Band::Low,
        Orc5Band::from_phrf_b_factor(PhrfBFactor::Calm.cf())
    );
    assert_eq!(
        Orc5Band::LowMedium,
        Orc5Band::from_phrf_b_factor(PhrfBFactor::Light.cf())
    );
    assert_eq!(
        Orc5Band::Medium,
        Orc5Band::from_phrf_b_factor(PhrfBFactor::Avg.cf())
    );
    assert_eq!(
        Orc5Band::High,
        Orc5Band::from_phrf_b_factor(PhrfBFactor::Hvy.cf())
    );
}

fn cert(five_band_wl_low_med_tot: f64) -> OrcCertificate {
    OrcCertificate {
        five_band_wl_low_med_tot,
        ..Default::default()
    }
}

/// virtualPhrf = 500 / (5-band W/L low-medium TOT) - 550, rounded.
#[test]
fn virtual_phrf_from_low_medium_tot() {
    assert_eq!(-50, cert(1.0).virtual_phrf());
    assert_eq!(75, cert(0.8).virtual_phrf());
    assert_eq!(219, cert(0.65).virtual_phrf()); // 769.23 - 550
    assert_eq!(-133, cert(1.2).virtual_phrf()); // 416.67 - 550
}

#[test]
fn virtual_phrf_ignores_other_tot_fields() {
    let cert = OrcCertificate {
        five_band_wl_low_med_tot: 1.0,
        all_purpose_tot: 0.5,
        five_band_wl_med_tot: 0.5,
        ..Default::default()
    };
    assert_eq!(-50, cert.virtual_phrf());
}

#[test]
fn orc_band_label_for_every_option() {
    let band5 = Orc5Band::MediumHigh;
    let band3 = Orc3Band::Medium;
    for option in OrcScoringOption::ENTRIES {
        let label = option.orc_band_label(band5, band3);
        let name = option.name();
        match option {
            OrcScoringOption::SingleNumberAllPurpose
            | OrcScoringOption::SingleNumberWindwardLeeward
            | OrcScoringOption::SingleNumberPredominantUpwind
            | OrcScoringOption::SingleNumberPredominantReaching
            | OrcScoringOption::SingleNumberPredominantDownwind => {
                assert_eq!(None, label, "{name}");
                assert!(!option.uses_3_band(), "{name}");
                assert!(!option.uses_5_band(), "{name}");
            }
            OrcScoringOption::TripleNumberAllPurpose
            | OrcScoringOption::TripleNumberWindwardLeeward => {
                assert_eq!(Some("Medium - 9 to 14 kts".to_string()), label, "{name}");
                assert!(option.uses_3_band(), "{name}");
                assert!(!option.uses_5_band(), "{name}");
            }
            OrcScoringOption::PredominantUpwind
            | OrcScoringOption::PredominantDownwind
            | OrcScoringOption::PredominantReaching
            | OrcScoringOption::FiveBandWindwardLeeward
            | OrcScoringOption::WindwardLeeward6040
            | OrcScoringOption::FiveBandAllPurpose => {
                assert_eq!(
                    Some("Medium/High - 13 to 17 kts".to_string()),
                    label,
                    "{name}"
                );
                assert!(!option.uses_3_band(), "{name}");
                assert!(option.uses_5_band(), "{name}");
            }
        }
    }
    assert_eq!(OrcScoringOption::ENTRIES.len(), 13);
}

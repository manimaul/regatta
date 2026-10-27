//! = `RaceValidation.kt` and `ResultValidation.kt`. The messages are what the clients show,
//! verbatim.

use regatta_common::{Orc3Band, Orc5Band, Race, RaceResult};

/// Checks a race before it is saved. Returns an error message for the client, or null when the
/// race is acceptable. R4a (orc_series_standings.md): the ORC wind band the scoring option uses
/// must be the one the PHRF B factor maps to, so the ORC time correction and the PHRF one agree
/// on the wind. Only the band in use is checked; a single-number option has none. Nothing is
/// corrected here: the client is told what to change.
pub fn validate_race(race: &Race) -> Option<String> {
    let cf = race.phrf_b_factor;
    if race.orc_scoring_option.uses_3_band() {
        let expected = Orc3Band::from_phrf_b_factor(cf);
        if race.orc3_band != expected {
            return Some(format!(
                "PHRF B factor {cf} is {}, but the ORC wind band is {}",
                expected.label(),
                race.orc3_band.label()
            ));
        }
    }
    if race.orc_scoring_option.uses_5_band() {
        let expected = Orc5Band::from_phrf_b_factor(cf);
        if race.orc5_band != expected {
            return Some(format!(
                "PHRF B factor {cf} is {}, but the ORC wind band is {}",
                expected.label(),
                race.orc5_band.label()
            ));
        }
    }
    None
}

/// Checks a result before it is saved. Returns an error message for the client, or null when the
/// result is acceptable. D8: a PHRF or ORC/PHRF result needs a PHRF rating, and R2
/// (orc_series_standings.md): an ORC or ORC/PHRF result needs an ORC certificate, or it would
/// score with a time correction factor of 1.
pub fn validate_result(result: &RaceResult) -> Option<String> {
    if result.rating_type.is_phrf() && result.phrf_rating.is_none() {
        return Some(format!(
            "A {} result needs a PHRF rating",
            result.rating_type.label()
        ));
    }
    if result.rating_type.is_orc() && result.orc_ref.is_none() {
        return Some(format!(
            "An {} result needs an ORC certificate",
            result.rating_type.label()
        ));
    }
    None
}

//! Save-time checks of a race, a result and a boat.
//!
//! The REST routes and the server functions call these before writing and answer 400 with the message; the
//! messages are what the clients show, verbatim.

use regatta_common::{Boat, Orc3Band, Orc5Band, Race, RaceResult};

/// Checks a race before it is saved, returning the message to show the user, or `None` if it is valid.
///
/// R4a (`orc_series_standings.md`): the ORC wind band the scoring option uses must be the one the PHRF B factor
/// maps to, so the ORC time correction and the PHRF one agree on the wind. Only the band in use is checked; a
/// single-number option has none. Nothing is corrected here; the user is told what to change.
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

/// Checks a result before it is saved, returning the message to show the user, or `None` if it is valid.
///
/// D8 (`test_audit_and_plan.md`): a PHRF or ORC/PHRF result needs a PHRF rating. R2 (`orc_series_standings.md`):
/// an ORC or ORC/PHRF result needs an ORC certificate. Without them the result would score with a time
/// correction factor of 1.
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

/// Checks a boat before it is saved, returning the message to show the user, or `None` if it is valid.
///
/// The same rule as [`validate_result`]: a PHRF or ORC/PHRF boat needs a PHRF rating, an ORC or ORC/PHRF boat
/// needs an ORC certificate, or its results could not be scored. The rule is [`Boat::missing_rating`], which the
/// boat form also checks before it saves.
pub fn validate_boat(boat: &Boat) -> Option<String> {
    boat.missing_rating()
}

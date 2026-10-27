//! = `RaceConfig.kt`: race config edits shared by the RC page, the races editor and the results
//! editor's Config modal (R4a, orc_series_standings.md). Pure, covered by
//! `tests/race_config_test.rs`. The PHRF B factor is the authority on the wind: the ORC bands
//! always follow it, and choosing a band sets the B factor.

use jiff::Timestamp;
use regatta_common::{
    ClassSchedule, Orc3Band, Orc5Band, OrcScoringOption, RaceSchedule, CORRECTION_FACTOR_DEFAULT,
};

/// Sets the PHRF B factor and derives both ORC wind bands from it. `None` is the default.
pub fn with_phrf_b_factor(schedule: &RaceSchedule, cf: Option<i32>) -> RaceSchedule {
    let factor = cf.unwrap_or(CORRECTION_FACTOR_DEFAULT);
    let mut out = schedule.clone();
    out.race.phrf_b_factor = factor;
    out.race.orc3_band = Orc3Band::from_phrf_b_factor(factor);
    out.race.orc5_band = Orc5Band::from_phrf_b_factor(factor);
    out
}

/// Sets the ORC scoring option and bands. The band the option uses sets the PHRF B factor, and the
/// other band is derived from that factor; a single-number option keeps the B factor and derives
/// both bands from it.
pub fn with_orc_option(
    schedule: &RaceSchedule,
    option: OrcScoringOption,
    orc3_band: Orc3Band,
    orc5_band: Orc5Band,
) -> RaceSchedule {
    let factor = if option.uses_3_band() {
        orc3_band.phrf_b_factor()
    } else if option.uses_5_band() {
        orc5_band.phrf_b_factor()
    } else {
        schedule.race.phrf_b_factor
    };
    let mut out = schedule.clone();
    out.race.phrf_b_factor = factor;
    out.race.orc_scoring_option = option;
    out.race.orc3_band = if option.uses_3_band() {
        orc3_band
    } else {
        Orc3Band::from_phrf_b_factor(factor)
    };
    out.race.orc5_band = if option.uses_5_band() {
        orc5_band
    } else {
        Orc5Band::from_phrf_b_factor(factor)
    };
    out
}

/// Moves one class's start; the other classes are untouched.
pub fn with_class_start(
    schedule: &RaceSchedule,
    class_schedule: &ClassSchedule,
    start: Timestamp,
) -> RaceSchedule {
    let mut out = schedule.clone();
    for cs in &mut out.schedule {
        if cs.race_class.id == class_schedule.race_class.id {
            cs.start_date = start;
        }
    }
    out
}

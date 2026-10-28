//! = `RaceConfig.kt`: race config edits shared by the RC page, the races editor and the results
//! editor's Config modal (R4a, orc_series_standings.md). Pure, covered by
//! `tests/race_config_test.rs`. The PHRF B factor is the authority on the wind: the ORC bands
//! always follow it, and choosing a band sets the B factor.

use jiff::{SignedDuration, Timestamp};
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

/// The class's time limit: its end less its start, when both are set and the end is later.
pub fn time_limit(class_schedule: &ClassSchedule) -> Option<SignedDuration> {
    let start = class_schedule.race_start()?;
    let end = class_schedule.race_end()?;
    (end > start).then(|| end.duration_since(start))
}

/// A time limit as `H:MM`.
pub fn time_limit_text(limit: SignedDuration) -> String {
    let minutes = limit.as_secs() / 60;
    format!("{}:{:02}", minutes / 60, minutes % 60)
}

/// Moves one class's start and keeps its time limit fixed by moving the end with it; the other
/// classes are untouched. A class without a time limit keeps its end.
pub fn with_class_start(
    schedule: &RaceSchedule,
    class_schedule: &ClassSchedule,
    start: Timestamp,
) -> RaceSchedule {
    let mut out = schedule.clone();
    for cs in &mut out.schedule {
        if cs.race_class.id == class_schedule.race_class.id {
            if let Some(end) = time_limit(cs).and_then(|limit| start.checked_add(limit).ok()) {
                cs.end_date = end;
            }
            cs.start_date = start;
        }
    }
    out
}

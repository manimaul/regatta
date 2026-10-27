//! The database wrappers of `RaceResultReporter` (`getReport`, `getStandingsReport`,
//! `reduceToCard`): load the schedule, the results and their certificates, build the cards and
//! call the pure functions of `regatta-scoring`.

use crate::db::Db;
use anyhow::{anyhow, Result};
use jiff::Timestamp;
use regatta_common::*;
use regatta_scoring::{card, report, standings};
use std::collections::HashMap;

/// `getStandingsReport`: the series' race reports in the year, scored as of `now`
/// (`FixedClock.now()` on the REST side, the dump instant in the golden test).
pub async fn get_standings_report(
    db: &Db,
    series_id: i64,
    year: i32,
    now: Timestamp,
) -> Result<Option<StandingsSeries>> {
    let race_ids = db.series_races(series_id, year).await?;
    let mut reports = Vec::with_capacity(race_ids.len());
    for schedule in db.find_race_schedules(&race_ids).await? {
        reports.push(get_report_for(db, schedule).await?);
    }
    let Some(series) = db.find_series(series_id).await? else {
        return Ok(None);
    };
    Ok(Some(standings(series, year, &reports, now)))
}

/// `getReport(raceId)`: fetches the schedule and the results, and builds the cards.
pub async fn get_report(db: &Db, race_id: i64) -> Result<Option<RaceReport>> {
    match db.find_race_schedule(race_id).await? {
        Some(schedule) => Ok(Some(get_report_for(db, schedule).await?)),
        None => Ok(None),
    }
}

/// `getReport(raceSchedule)` for a schedule already loaded: the results and their ORC
/// certificates, then the cards.
pub async fn get_report_for(db: &Db, race_schedule: RaceSchedule) -> Result<RaceReport> {
    let schedules: HashMap<i64, &ClassSchedule> = race_schedule
        .schedule
        .iter()
        .map(|s| (s.race_class.id, s))
        .collect();
    let records = db.results_boat_bracket_by_race_id(&race_schedule).await?;
    let mut refs: Vec<String> = Vec::new();
    for r in &records {
        if let Some(orc_ref) = &r.result.orc_ref {
            if !refs.contains(orc_ref) {
                refs.push(orc_ref.clone());
            }
        }
    }
    let certs = db.find_certificates(&refs).await?;
    let mut cards = Vec::with_capacity(records.len());
    for record in records {
        cards.push(reduce_to_card(record, &race_schedule, &schedules, &certs)?);
    }
    Ok(report(race_schedule, cards))
}

/// `reduceToCard`: the certificate by `orcRef` (an error when a result names one that is not
/// stored) and the class start by the bracket's class id.
fn reduce_to_card(
    record: RaceResultBoatBracket,
    race_schedule: &RaceSchedule,
    class_schedules: &HashMap<i64, &ClassSchedule>,
    certs: &HashMap<String, OrcCertificate>,
) -> Result<RaceReportCard> {
    let orc_cert = match &record.result.orc_ref {
        Some(orc_ref) => Some(certs.get(orc_ref).ok_or_else(|| {
            anyhow!(
                "could not find certificate for result id {} ref = {orc_ref}",
                record.result.id
            )
        })?),
        None => None,
    };
    let class_start = class_schedules
        .get(&record.bracket.class_id)
        .map(|s| s.start_date);
    Ok(card(record, class_start, &race_schedule.race, orc_cert))
}

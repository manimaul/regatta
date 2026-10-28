//! The `raceresults` table: one result per boat and race, with its finish, rating, finish code and penalty.

use super::time::{from_pg, to_pg};
use super::{boat, enum_from_name, orc_table, person, race};
use anyhow::{anyhow, Result};
use jiff::tz::TimeZone;
use jiff::Timestamp;
use regatta_common::*;
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

#[derive(FromRow)]
struct Row {
    id: i64,
    race_id: i64,
    boat_id: i64,
    end_date: Option<String>,
    phrf_rating: Option<i32>,
    orc_ref: Option<String>,
    rating_type: String,
    hoc: Option<i32>,
    class_id: i64,
    bracket_id: i64,
    finish_code: Option<String>,
    penalty: Option<i32>,
}

const COLUMNS: &str = "raceresults.id, raceresults.race_id, raceresults.boat_id, raceresults.end_date::text AS end_date, \
                       raceresults.phrf_rating, raceresults.orc_ref, raceresults.rating_type, raceresults.hoc, \
                       raceresults.class_id, raceresults.bracket_id, raceresults.finish_code, raceresults.penalty";

/// The result joined to its boat (`b_*`, see [`boat::COLUMNS`]).
#[derive(FromRow)]
struct JoinedRow {
    #[sqlx(flatten)]
    result: Row,
    b_id: i64,
    b_name: String,
    b_sail_number: String,
    b_boat_type: String,
    b_phrf_rating: Option<i32>,
    b_rating_type: String,
    b_skipper_id: Option<i64>,
}

const BOAT_COLUMNS: &str = "boat.id AS b_id, boat.name AS b_name, boat.sail_number AS b_sail_number, boat.boat_type AS b_boat_type, \
                            boat.phrf_rating AS b_phrf_rating, boat.rating_type AS b_rating_type, boat.skipper_id AS b_skipper_id";

impl JoinedRow {
    fn boat_row(&self) -> boat::Row {
        boat::Row {
            id: self.b_id,
            name: self.b_name.clone(),
            sail_number: self.b_sail_number.clone(),
            boat_type: self.b_boat_type.clone(),
            phrf_rating: self.b_phrf_rating,
            rating_type: self.b_rating_type.clone(),
        }
    }
}

/// Returns the stored finish code, or for a legacy row without one, `TIME` with a finish and `RET` without.
fn finish_code(code: &Option<String>, finish: Option<Timestamp>) -> Result<FinishCode> {
    match code {
        Some(c) => enum_from_name(c),
        None => Ok(if finish.is_some() {
            FinishCode::Time
        } else {
            FinishCode::Ret
        }),
    }
}

fn to_result(row: &Row, tz: &TimeZone) -> Result<RaceResult> {
    let finish = row
        .end_date
        .as_deref()
        .map(|t| from_pg(t, tz))
        .transpose()?;
    Ok(RaceResult {
        id: row.id,
        race_id: row.race_id,
        boat_id: row.boat_id,
        finish,
        phrf_rating: row.phrf_rating,
        orc_ref: row.orc_ref.clone(),
        hoc_position: row.hoc,
        bracket_id: row.bracket_id,
        race_class_id: row.class_id,
        penalty: row.penalty,
        rating_type: enum_from_name(&row.rating_type)?,
        finish_code: finish_code(&row.finish_code, finish)?,
    })
}

fn to_result_full(
    row: &Row,
    boats: &HashMap<i64, BoatSkipper>,
    certs: &HashMap<String, OrcCertificate>,
    tz: &TimeZone,
) -> Result<RaceResultFull> {
    let finish = row
        .end_date
        .as_deref()
        .map(|t| from_pg(t, tz))
        .transpose()?;
    Ok(RaceResultFull {
        id: row.id,
        race_id: row.race_id,
        boat: boats
            .get(&row.boat_id)
            .cloned()
            .ok_or_else(|| anyhow!("no boat {} for result {}", row.boat_id, row.id))?,
        finish,
        phrf_rating: row.phrf_rating,
        orc_certificate: row.orc_ref.as_ref().and_then(|r| certs.get(r).cloned()),
        hoc_position: row.hoc,
        bracket_id: row.bracket_id,
        race_class_id: row.class_id,
        penalty: row.penalty,
        rating_type: enum_from_name(&row.rating_type)?,
        finish_code: finish_code(&row.finish_code, finish)?,
    })
}

/// Returns the number of results of race `race_id`.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn count(conn: &mut PgConnection, race_id: i64) -> Result<i64> {
    Ok(counts_by_race(conn, &[race_id])
        .await?
        .get(&race_id)
        .copied()
        .unwrap_or(0))
}

/// Returns the results per race, one grouped query for all of `race_ids`; races with no results are absent.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn counts_by_race(
    conn: &mut PgConnection,
    race_ids: &[i64],
) -> Result<HashMap<i64, i64>> {
    if race_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<(i64, i64)> = sqlx::query_as(
        "SELECT race_id, count(id) FROM raceresults WHERE race_id = ANY($1) GROUP BY race_id",
    )
    .bind(race_ids)
    .fetch_all(conn)
    .await?;
    Ok(rows.into_iter().collect())
}

/// Inserts the result or updates the one of the same boat and race, returning it as stored.
///
/// The finish time is stored only for a TIME result and the HOC position only for an HOC one.
///
/// # Errors
///
/// Returns `Err` if the statement fails or the stored row does not parse.
pub async fn upsert_result(
    conn: &mut PgConnection,
    result: &RaceResult,
    tz: &TimeZone,
) -> Result<Option<RaceResult>> {
    let finish = result
        .finish
        .filter(|_| result.finish_code == FinishCode::Time)
        .map(|t| to_pg(t, tz));
    let hoc = result
        .hoc_position
        .filter(|_| result.finish_code == FinishCode::Hoc);
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "INSERT INTO raceresults (race_id, boat_id, end_date, phrf_rating, orc_ref, rating_type, hoc, class_id, bracket_id, finish_code, penalty) \
         VALUES ($1, $2, $3::text::timestamp, $4, $5, $6, $7, $8, $9, $10, $11) \
         ON CONFLICT (boat_id, race_id) DO UPDATE SET end_date = EXCLUDED.end_date, phrf_rating = EXCLUDED.phrf_rating, \
         orc_ref = EXCLUDED.orc_ref, rating_type = EXCLUDED.rating_type, hoc = EXCLUDED.hoc, class_id = EXCLUDED.class_id, \
         bracket_id = EXCLUDED.bracket_id, finish_code = EXCLUDED.finish_code, penalty = EXCLUDED.penalty \
         RETURNING {COLUMNS}"
    )))
    .bind(result.race_id)
    .bind(result.boat_id)
    .bind(finish)
    .bind(result.phrf_rating)
    .bind(&result.orc_ref)
    .bind(result.rating_type.name())
    .bind(hoc)
    .bind(result.race_class_id)
    .bind(result.bracket_id)
    .bind(result.finish_code.name())
    .bind(result.penalty)
    .fetch_all(conn)
    .await?;
    match rows.as_slice() {
        [row] => Ok(Some(to_result(row, tz)?)),
        _ => Ok(None),
    }
}

/// Returns the number of results of boat `boat_id`.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_count(conn: &mut PgConnection, boat_id: i64) -> Result<i64> {
    Ok(race_counts_by_boat(conn, &[boat_id])
        .await?
        .get(&boat_id)
        .copied()
        .unwrap_or(0))
}

/// Returns the results per boat, one grouped query for all of `boat_ids`; boats with no results are absent.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_counts_by_boat(
    conn: &mut PgConnection,
    boat_ids: &[i64],
) -> Result<HashMap<i64, i64>> {
    if boat_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<(i64, i64)> = sqlx::query_as(
        "SELECT boat_id, count(id) FROM raceresults WHERE boat_id = ANY($1) GROUP BY boat_id",
    )
    .bind(boat_ids)
    .fetch_all(conn)
    .await?;
    Ok(rows.into_iter().collect())
}

/// Returns the results of race `race_id` with their boat, skipper and bracket in `race_schedule`.
///
/// `race_schedule` is the schedule of that same race, loaded once by the caller. The boat carries the result's
/// rating type and PHRF rating; a result whose bracket is not in the schedule gets the default bracket (id 0).
/// Four statements for any number of results: the results joined to boat, then the boats' race counts, ORC
/// certificates and skippers.
///
/// # Errors
///
/// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
pub async fn results_boat_bracket_by_race_id(
    conn: &mut PgConnection,
    race_id: i64,
    race_schedule: &RaceSchedule,
    tz: &TimeZone,
) -> Result<Vec<RaceResultBoatBracket>> {
    let rows: Vec<JoinedRow> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS}, {BOAT_COLUMNS} FROM raceresults \
         INNER JOIN race ON race.id = raceresults.race_id \
         INNER JOIN boat ON boat.id = raceresults.boat_id \
         WHERE raceresults.race_id = $1"
    )))
    .bind(race_id)
    .fetch_all(&mut *conn)
    .await?;
    let mut boat_ids: Vec<i64> = Vec::new();
    let mut skipper_ids: Vec<i64> = Vec::new();
    for row in &rows {
        if !boat_ids.contains(&row.result.boat_id) {
            boat_ids.push(row.result.boat_id);
        }
        if let Some(s) = row.b_skipper_id {
            if !skipper_ids.contains(&s) {
                skipper_ids.push(s);
            }
        }
    }
    let race_counts = race_counts_by_boat(conn, &boat_ids).await?;
    let certs = orc_table::find_certificates_by_boat(conn, &boat_ids).await?;
    let skippers = person::find_people(conn, &skipper_ids).await?;
    let mut out = Vec::with_capacity(rows.len());
    for row in &rows {
        let result = to_result(&row.result, tz)?;
        let boat_id = row.result.boat_id;
        let mut b = boat::to_boat(
            &row.boat_row(),
            race_counts.get(&boat_id).copied().unwrap_or(0),
            certs.get(&boat_id).cloned().unwrap_or_default(),
        )?;
        b.rating_type = result.rating_type;
        b.phrf_rating = result.phrf_rating;
        let bracket = find_boat_bracket(conn, race_schedule, &result)
            .await?
            .unwrap_or_default();
        out.push(RaceResultBoatBracket {
            result,
            boat_skipper: BoatSkipper {
                boat: Some(b),
                skipper: row.b_skipper_id.and_then(|s| skippers.get(&s).cloned()),
            },
            bracket,
        });
    }
    Ok(out)
}

/// Returns the results of race `race_id`, in table order.
///
/// # Errors
///
/// Returns `Err` if the query fails or a stored value does not parse (enum name, timestamp).
pub async fn results_by_race_id(
    conn: &mut PgConnection,
    race_id: i64,
    tz: &TimeZone,
) -> Result<Vec<RaceResult>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM raceresults WHERE raceresults.race_id = $1"
    )))
    .bind(race_id)
    .fetch_all(conn)
    .await?;
    rows.iter().map(|r| to_result(r, tz)).collect()
}

/// Returns the schedule of race `race_id` with its full results, or `None` if the race does not exist.
///
/// The boats and certificates load in bulk.
///
/// # Errors
///
/// - Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
/// - Returns `Err` if a result's boat does not exist.
pub async fn schedule_results_by_race_id(
    conn: &mut PgConnection,
    race_id: i64,
    tz: &TimeZone,
) -> Result<Option<RaceScheduleResults>> {
    let Some(race_schedule) = race::find_race_schedule(conn, race_id, tz).await? else {
        return Ok(None);
    };
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM raceresults WHERE raceresults.race_id = $1"
    )))
    .bind(race_id)
    .fetch_all(&mut *conn)
    .await?;
    let mut boat_ids: Vec<i64> = Vec::new();
    let mut refs: Vec<String> = Vec::new();
    for row in &rows {
        if !boat_ids.contains(&row.boat_id) {
            boat_ids.push(row.boat_id);
        }
        if let Some(r) = &row.orc_ref {
            if !refs.contains(r) {
                refs.push(r.clone());
            }
        }
    }
    let boats = boat::find_boat_skippers(conn, &boat_ids).await?;
    let certs = orc_table::find_certificates(conn, &refs).await?;
    let results = rows
        .iter()
        .map(|r| to_result_full(r, &boats, &certs, tz))
        .collect::<Result<Vec<_>>>()?;
    Ok(Some(RaceScheduleResults {
        race_schedule,
        results,
    }))
}

/// Deletes the result `id`, returning the rows deleted.
///
/// # Errors
///
/// Returns `Err` if the statement fails.
pub async fn delete_result(conn: &mut PgConnection, id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM raceresults WHERE id = $1")
        .bind(id)
        .execute(conn)
        .await?
        .rows_affected())
}

/// Returns the result's bracket in the schedule, found by its bracket id, or `None` if the schedule lacks it.
///
/// A result's bracket id is always set (0 when missing), so the lookup is only by id, never by rating; a result
/// whose bracket is not in the schedule is an orphan, which the race report lists under "Incorrectly classed".
/// `conn` is unused.
///
/// # Errors
///
/// Never returns `Err`; the `Result` keeps the signature of the other queries.
pub async fn find_boat_bracket(
    conn: &mut PgConnection,
    race: &RaceSchedule,
    result: &RaceResult,
) -> Result<Option<Bracket>> {
    let by_id = race
        .schedule
        .iter()
        .find_map(|sch| sch.brackets.iter().find(|b| b.id == result.bracket_id));
    if let Some(bracket) = by_id {
        return Ok(Some(bracket.clone()));
    }
    let _ = conn;
    Ok(None)
}

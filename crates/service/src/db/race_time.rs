//! = `RaceTimeTable.kt`: the class start and end times of a race.

use super::time::{from_pg, pacific_year, to_pg};
use anyhow::Result;
use jiff::tz::TimeZone;
use regatta_common::RaceTime;
use sqlx::{FromRow, PgConnection};
use std::collections::{BTreeSet, HashMap};

#[derive(FromRow)]
struct Row {
    race_id: i64,
    class_id: i64,
    start_date: String,
    end_date: String,
}

const COLUMNS: &str =
    "race_id, class_id, start_date::text AS start_date, end_date::text AS end_date";

fn to_time(row: &Row, tz: &TimeZone) -> Result<RaceTime> {
    Ok(RaceTime {
        start_date: from_pg(&row.start_date, tz)?,
        end_date: from_pg(&row.end_date, tz)?,
        class_id: row.class_id,
        race_id: row.race_id,
    })
}

/// Every season year with a scheduled race, by the Pacific calendar (D8), newest first.
pub async fn all_years(conn: &mut PgConnection, tz: &TimeZone) -> Result<Vec<String>> {
    let rows: Vec<(String, String)> =
        sqlx::query_as("SELECT start_date::text, end_date::text FROM racetime")
            .fetch_all(conn)
            .await?;
    let mut years = BTreeSet::new();
    for (start, end) in rows {
        years.insert(pacific_year(from_pg(&start, tz)?)?);
        years.insert(pacific_year(from_pg(&end, tz)?)?);
    }
    Ok(years.into_iter().rev().map(|y| y.to_string()).collect())
}

pub async fn delete_race(conn: &mut PgConnection, race_id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM racetime WHERE race_id = $1")
        .bind(race_id)
        .execute(conn)
        .await?
        .rows_affected())
}

/// Replaces the race and class pair's row.
pub async fn update_race_time(
    conn: &mut PgConnection,
    time: &RaceTime,
    tz: &TimeZone,
) -> Result<Option<RaceTime>> {
    sqlx::query("DELETE FROM racetime WHERE race_id = $1 AND class_id = $2")
        .bind(time.race_id)
        .bind(time.class_id)
        .execute(&mut *conn)
        .await?;
    let row: Option<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "INSERT INTO racetime (race_id, class_id, start_date, end_date) \
         VALUES ($1, $2, $3::text::timestamp, $4::text::timestamp) RETURNING {COLUMNS}"
    )))
    .bind(time.race_id)
    .bind(time.class_id)
    .bind(to_pg(time.start_date, tz))
    .bind(to_pg(time.end_date, tz))
    .fetch_optional(conn)
    .await?;
    row.as_ref().map(|r| to_time(r, tz)).transpose()
}

/// The class start and end times of `race_ids`, keyed by race id to class id, in one query.
/// A race and class pair with more than one row is left out, as it is not a usable schedule.
pub async fn find_by_races(
    conn: &mut PgConnection,
    race_ids: &[i64],
    tz: &TimeZone,
) -> Result<HashMap<(i64, i64), RaceTime>> {
    if race_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM racetime WHERE race_id = ANY($1)"
    )))
    .bind(race_ids)
    .fetch_all(conn)
    .await?;
    let mut grouped: HashMap<(i64, i64), Vec<RaceTime>> = HashMap::new();
    for row in &rows {
        let time = to_time(row, tz)?;
        grouped
            .entry((time.race_id, time.class_id))
            .or_default()
            .push(time);
    }
    Ok(grouped
        .into_iter()
        .filter_map(|(key, times)| match times.as_slice() {
            [time] => Some((key, time.clone())),
            _ => None,
        })
        .collect())
}

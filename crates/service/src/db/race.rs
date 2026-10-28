//! The `race` table, the race schedules assembled from it, and the season year queries.
//!
//! A [`RaceSchedule`] is the race with its series, RC volunteer, result count and, per class, the class times and
//! brackets ([`find_race_schedules`]). Season years follow the Pacific calendar (D8 in
//! `docs/explore_implemented/test_audit_and_plan.md`).

use super::time::{to_pg, year_bounds};
use super::{
    bracket, enum_from_name, image, person, race_bracket_junction as junction, race_class,
    race_results, race_time, series,
};
use anyhow::Result;
use jiff::tz::TimeZone;
use regatta_common::*;
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

#[derive(FromRow)]
struct Row {
    id: i64,
    name: String,
    series_id: Option<i64>,
    rc_id: Option<i64>,
    correction_factor: Option<i32>,
    orc_option: Option<String>,
    orc_3: Option<String>,
    orc_5: Option<String>,
}

const COLUMNS: &str = "id, name, series_id, rc_id, correction_factor, orc_option, orc_3, orc_5";

fn to_race(row: &Row, report_image: Option<String>) -> Result<Race> {
    Ok(Race {
        id: row.id,
        name: row.name.clone(),
        series_id: row.series_id,
        rc_id: row.rc_id,
        report_image,
        phrf_b_factor: row.correction_factor.unwrap_or(CORRECTION_FACTOR_DEFAULT),
        orc_scoring_option: match &row.orc_option {
            Some(o) => enum_from_name(o)?,
            None => OrcScoringOption::FiveBandWindwardLeeward,
        },
        orc3_band: match &row.orc_3 {
            Some(o) => enum_from_name(o)?,
            None => Orc3Band::from_phrf_b_factor(CORRECTION_FACTOR_DEFAULT),
        },
        orc5_band: match &row.orc_5 {
            Some(o) => enum_from_name(o)?,
            None => Orc5Band::from_phrf_b_factor(CORRECTION_FACTOR_DEFAULT),
        },
    })
}

/// Builds a [`Race`] from its row with its report image name (one extra query).
async fn to_race_with_image(conn: &mut PgConnection, row: &Row) -> Result<Race> {
    let image = image::get_race_report_image_name(conn, row.id).await?;
    to_race(row, image)
}

/// Returns the number of races in the series.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_count_for_series(conn: &mut PgConnection, series_id: i64) -> Result<i64> {
    Ok(race_counts_by_series(conn, &[series_id])
        .await?
        .get(&series_id)
        .copied()
        .unwrap_or(0))
}

/// Returns the races per series, one grouped query for all of `series_ids`; series with no races are absent.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_counts_by_series(
    conn: &mut PgConnection,
    series_ids: &[i64],
) -> Result<HashMap<i64, i64>> {
    if series_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<(Option<i64>, i64)> = sqlx::query_as(
        "SELECT series_id, count(id) FROM race WHERE series_id = ANY($1) GROUP BY series_id",
    )
    .bind(series_ids)
    .fetch_all(conn)
    .await?;
    Ok(rows
        .into_iter()
        .filter_map(|(series, count)| series.map(|s| (s, count)))
        .collect())
}

/// Inserts the race (id 0) or upserts it by id, returning it as stored with its report image name.
///
/// The name is trimmed.
///
/// # Errors
///
/// Returns `Err` if a statement fails or a stored ORC option or band name is unknown.
pub async fn upsert_race(conn: &mut PgConnection, race: &Race) -> Result<Option<Race>> {
    let set = "name = EXCLUDED.name, series_id = EXCLUDED.series_id, rc_id = EXCLUDED.rc_id, \
               correction_factor = EXCLUDED.correction_factor, orc_option = EXCLUDED.orc_option, \
               orc_3 = EXCLUDED.orc_3, orc_5 = EXCLUDED.orc_5";
    let rows: Vec<Row> = if race.id > 0 {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO race (id, name, series_id, rc_id, correction_factor, orc_option, orc_3, orc_5) \
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8) ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(race.id)
        .bind(race.name.trim())
        .bind(race.series_id)
        .bind(race.rc_id)
        .bind(race.phrf_b_factor)
        .bind(race.orc_scoring_option.name())
        .bind(race.orc3_band.name())
        .bind(race.orc5_band.name())
        .fetch_all(&mut *conn)
        .await?
    } else {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO race (name, series_id, rc_id, correction_factor, orc_option, orc_3, orc_5) \
             VALUES ($1, $2, $3, $4, $5, $6, $7) ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(race.name.trim())
        .bind(race.series_id)
        .bind(race.rc_id)
        .bind(race.phrf_b_factor)
        .bind(race.orc_scoring_option.name())
        .bind(race.orc3_band.name())
        .bind(race.orc5_band.name())
        .fetch_all(&mut *conn)
        .await?
    };
    match rows.as_slice() {
        [row] => Ok(Some(to_race_with_image(conn, row).await?)),
        _ => Ok(None),
    }
}

/// Stores one class's time, then its bracket junctions; `true` when every bracket went in.
async fn insert_class_schedule(
    conn: &mut PgConnection,
    race_id: i64,
    schedule: &ClassSchedule,
    tz: &TimeZone,
) -> Result<bool> {
    let time = race_time::update_race_time(
        conn,
        &RaceTime {
            start_date: schedule.start_date,
            end_date: schedule.end_date,
            class_id: schedule.race_class.id,
            race_id,
        },
        tz,
    )
    .await?;
    let Some(time) = time else {
        return Ok(false);
    };
    tracing::debug!("inserted race time {time:?}");
    let count =
        junction::set_brackets(conn, race_id, schedule.race_class.id, &schedule.brackets).await?;
    Ok(count == schedule.brackets.len())
}

/// Replaces the race's times and junctions, upserts the race, then reads the schedule back.
///
/// `None` if the race row did not come back. A class whose brackets did not all go in is not reported.
///
/// # Errors
///
/// Returns `Err` if a statement fails or a stored value does not parse (enum name, timestamp).
pub async fn insert_schedule(
    conn: &mut PgConnection,
    schedule: &RaceSchedule,
    tz: &TimeZone,
) -> Result<Option<RaceSchedule>> {
    race_time::delete_race(conn, schedule.race.id).await?;
    junction::delete_race(conn, schedule.race.id).await?;
    let Some(race) = upsert_race(conn, &schedule.race).await? else {
        return Ok(None);
    };
    for class_schedule in &schedule.schedule {
        insert_class_schedule(conn, race.id, class_schedule, tz).await?;
    }
    find_race_schedule(conn, race.id, tz).await
}

/// Returns the schedule of race `race_id`, or `None` if it does not exist (see [`find_race_schedules`]).
///
/// # Errors
///
/// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
pub async fn find_race_schedule(
    conn: &mut PgConnection,
    race_id: i64,
    tz: &TimeZone,
) -> Result<Option<RaceSchedule>> {
    Ok(find_race_schedules(conn, &[race_id], tz)
        .await?
        .into_iter()
        .next())
}

/// Returns the schedules of `race_ids`, in that order, skipping ids with no race row.
///
/// Fourteen statements however many races are asked for: the races, their report image names, the bracket
/// junctions, the brackets and classes those name (with race counts), the class times, the result counts, the
/// series (with race counts) and the RC volunteers.
///
/// Classes are ordered by class sort then class id, and brackets within a class by min rating then id, so the
/// order does not depend on how the junction rows happen to be stored. A class without exactly one time row is
/// left out of the schedule.
///
/// # Errors
///
/// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
pub async fn find_race_schedules(
    conn: &mut PgConnection,
    race_ids: &[i64],
    tz: &TimeZone,
) -> Result<Vec<RaceSchedule>> {
    let mut ids: Vec<i64> = Vec::new();
    for id in race_ids {
        if !ids.contains(id) {
            ids.push(*id);
        }
    }
    if ids.is_empty() {
        return Ok(Vec::new());
    }
    let race_rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM race WHERE id = ANY($1)"
    )))
    .bind(&ids)
    .fetch_all(&mut *conn)
    .await?;
    let race_rows: HashMap<i64, &Row> = race_rows.iter().map(|r| (r.id, r)).collect();
    let image_names = image::report_image_names(conn, &ids).await?;
    let junction_rows = junction::select_for_races(conn, &ids).await?;
    let mut junctions: HashMap<i64, Vec<&junction::JunctionRow>> = HashMap::new();
    for j in &junction_rows {
        junctions.entry(j.race_id).or_default().push(j);
    }
    let mut bracket_ids: Vec<i64> = Vec::new();
    for j in &junction_rows {
        if !bracket_ids.contains(&j.bracket_id) {
            bracket_ids.push(j.bracket_id);
        }
    }
    let brackets = bracket::find_brackets(conn, &bracket_ids).await?;
    let mut class_ids: Vec<i64> = Vec::new();
    for b in brackets.values() {
        if !class_ids.contains(&b.class_id) {
            class_ids.push(b.class_id);
        }
    }
    let classes = race_class::find_classes(conn, &class_ids).await?;
    let times = race_time::find_by_races(conn, &ids, tz).await?;
    let result_counts = race_results::counts_by_race(conn, &ids).await?;
    let series_ids: Vec<i64> = {
        let mut v = Vec::new();
        for r in race_rows.values() {
            if let Some(s) = r.series_id {
                if !v.contains(&s) {
                    v.push(s);
                }
            }
        }
        v
    };
    let series_map = series::find_series(conn, &series_ids).await?;
    let rc_ids: Vec<i64> = {
        let mut v = Vec::new();
        for r in race_rows.values() {
            if let Some(p) = r.rc_id {
                if !v.contains(&p) {
                    v.push(p);
                }
            }
        }
        v
    };
    let people = person::find_people(conn, &rc_ids).await?;

    let mut out = Vec::with_capacity(ids.len());
    for race_id in ids {
        let Some(row) = race_rows.get(&race_id) else {
            continue;
        };
        let race = to_race(row, image_names.get(&race_id).cloned())?;
        let mut race_brackets: Vec<&Bracket> = junctions
            .get(&race_id)
            .map(|js| {
                js.iter()
                    .filter_map(|j| brackets.get(&j.bracket_id))
                    .collect()
            })
            .unwrap_or_default();
        race_brackets.sort_by(|a, b| {
            let class_sort = |x: &Bracket| classes.get(&x.class_id).map(|c| c.sort);
            class_sort(a)
                .cmp(&class_sort(b))
                .then(a.class_id.cmp(&b.class_id))
                .then(a.min_rating.total_cmp(&b.min_rating))
                .then(a.id.cmp(&b.id))
        });
        // group by class, the classes in first-seen order
        let mut class_order: Vec<i64> = Vec::new();
        let mut by_class: HashMap<i64, Vec<Bracket>> = HashMap::new();
        for b in race_brackets {
            if !by_class.contains_key(&b.class_id) {
                class_order.push(b.class_id);
            }
            by_class.entry(b.class_id).or_default().push(b.clone());
        }
        let mut schedule = Vec::new();
        for class_id in class_order {
            if let Some(time) = times.get(&(race_id, class_id)) {
                schedule.push(ClassSchedule {
                    race_class: classes.get(&class_id).cloned().unwrap_or_default(),
                    brackets: by_class.remove(&class_id).unwrap_or_default(),
                    start_date: time.start_date,
                    end_date: time.end_date,
                });
            }
        }
        out.push(RaceSchedule {
            result_count: result_counts.get(&race_id).copied().unwrap_or(0),
            series: race.series_id.and_then(|s| series_map.get(&s).cloned()),
            rc: race.rc_id.and_then(|p| people.get(&p).cloned()),
            race,
            schedule,
        });
    }
    Ok(out)
}

/// Returns the race `race_id` with its report image name, or `None` if it does not exist.
///
/// # Errors
///
/// Returns `Err` if a query fails or a stored ORC option or band name is unknown.
pub async fn find_race(conn: &mut PgConnection, race_id: i64) -> Result<Option<Race>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM race WHERE id = $1"
    )))
    .bind(race_id)
    .fetch_all(&mut *conn)
    .await?;
    match rows.as_slice() {
        [row] => Ok(Some(to_race_with_image(conn, row).await?)),
        _ => Ok(None),
    }
}

/// Deletes the race row, returning the rows deleted; delete its times and junctions first.
///
/// # Errors
///
/// Returns `Err` if the statement fails (a result or image still references the race, say).
pub async fn delete_race(conn: &mut PgConnection, race_id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM race WHERE id = $1")
        .bind(race_id)
        .execute(conn)
        .await?
        .rows_affected())
}

/// Returns the schedules of the races with a class start in the Pacific calendar year, by start time.
///
/// The sort is stable over the order `SELECT DISTINCT` returns.
///
/// # Errors
///
/// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
pub async fn all_races(
    conn: &mut PgConnection,
    year: i32,
    tz: &TimeZone,
) -> Result<Vec<RaceSchedule>> {
    let (start, end) = year_bounds(year)?;
    let ids: Vec<i64> = sqlx::query_scalar(
        "SELECT DISTINCT race_id FROM racetime WHERE start_date >= $1::text::timestamp AND start_date < $2::text::timestamp",
    )
    .bind(to_pg(start, tz))
    .bind(to_pg(end, tz))
    .fetch_all(&mut *conn)
    .await?;
    let mut schedules = find_race_schedules(conn, &ids, tz).await?;
    schedules.sort_by_key(|s| s.start_time());
    Ok(schedules)
}

/// Returns the ids of the series' races with a class start in the Pacific calendar year.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn series_races(
    conn: &mut PgConnection,
    series_id: i64,
    year: i32,
    tz: &TimeZone,
) -> Result<Vec<i64>> {
    let (start, end) = year_bounds(year)?;
    Ok(sqlx::query_scalar(
        "SELECT DISTINCT race_id FROM racetime JOIN race r on r.id = racetime.race_id \
         WHERE r.series_id = $1 AND start_date >= $2::text::timestamp AND start_date < $3::text::timestamp",
    )
    .bind(series_id)
    .bind(to_pg(start, tz))
    .bind(to_pg(end, tz))
    .fetch_all(conn)
    .await?)
}

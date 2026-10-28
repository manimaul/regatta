//! The `racebracketjunction` table: which brackets sail in which race under which class.

use anyhow::Result;
use regatta_common::Bracket;
use sqlx::{FromRow, PgConnection};
use std::collections::{HashMap, HashSet};

/// One junction row: `bracket_id` sails in `race_id` under `class_id`.
#[derive(Debug, Clone, FromRow)]
pub struct JunctionRow {
    pub race_id: i64,
    pub class_id: i64,
    pub bracket_id: i64,
}

/// Returns the number of distinct races the bracket sailed in.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_count_for_bracket(conn: &mut PgConnection, bracket_id: i64) -> Result<i64> {
    Ok(race_counts_by_bracket(conn, &[bracket_id])
        .await?
        .get(&bracket_id)
        .copied()
        .unwrap_or(0))
}

/// Returns the number of distinct races the class sailed in.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_count_for_class(conn: &mut PgConnection, class_id: i64) -> Result<i64> {
    Ok(race_counts_by_class(conn, &[class_id])
        .await?
        .get(&class_id)
        .copied()
        .unwrap_or(0))
}

/// Returns the distinct races per bracket, one grouped query for all of `bracket_ids`.
///
/// Brackets with no races are absent.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_counts_by_bracket(
    conn: &mut PgConnection,
    bracket_ids: &[i64],
) -> Result<HashMap<i64, i64>> {
    if bracket_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<(i64, i64)> = sqlx::query_as(
        "SELECT bracket_id, count(DISTINCT race_id) FROM racebracketjunction WHERE bracket_id = ANY($1) GROUP BY bracket_id",
    )
    .bind(bracket_ids)
    .fetch_all(conn)
    .await?;
    Ok(rows.into_iter().collect())
}

/// Returns the distinct races per class, one grouped query for all of `class_ids`.
///
/// Classes with no races are absent.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn race_counts_by_class(
    conn: &mut PgConnection,
    class_ids: &[i64],
) -> Result<HashMap<i64, i64>> {
    if class_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<(i64, i64)> = sqlx::query_as(
        "SELECT class_id, count(DISTINCT race_id) FROM racebracketjunction WHERE class_id = ANY($1) GROUP BY class_id",
    )
    .bind(class_ids)
    .fetch_all(conn)
    .await?;
    Ok(rows.into_iter().collect())
}

/// Replaces the brackets of a race and class pair, returning the rows inserted (one per distinct bracket id).
///
/// # Errors
///
/// Returns `Err` if a statement fails.
pub async fn set_brackets(
    conn: &mut PgConnection,
    race_id: i64,
    class_id: i64,
    list: &[Bracket],
) -> Result<usize> {
    let deleted =
        sqlx::query("DELETE FROM racebracketjunction WHERE race_id = $1 AND class_id = $2")
            .bind(race_id)
            .bind(class_id)
            .execute(&mut *conn)
            .await?
            .rows_affected();
    tracing::debug!("deleted bracket race junctions {deleted}");
    let mut seen = HashSet::new();
    let mut count = 0usize;
    for bracket in list {
        if !seen.insert(bracket.id) {
            continue;
        }
        count += sqlx::query(
            "INSERT INTO racebracketjunction (bracket_id, race_id, class_id) VALUES ($1, $2, $3)",
        )
        .bind(bracket.id)
        .bind(race_id)
        .bind(class_id)
        .execute(&mut *conn)
        .await?
        .rows_affected() as usize;
    }
    Ok(count)
}

/// Returns the junction rows of `race_ids` in table order, in one query.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn select_for_races(
    conn: &mut PgConnection,
    race_ids: &[i64],
) -> Result<Vec<JunctionRow>> {
    if race_ids.is_empty() {
        return Ok(Vec::new());
    }
    Ok(sqlx::query_as(
        "SELECT race_id, class_id, bracket_id FROM racebracketjunction WHERE race_id = ANY($1)",
    )
    .bind(race_ids)
    .fetch_all(conn)
    .await?)
}

/// Deletes every junction row of the race, returning the rows deleted.
///
/// # Errors
///
/// Returns `Err` if the statement fails.
pub async fn delete_race(conn: &mut PgConnection, race_id: i64) -> Result<u64> {
    Ok(
        sqlx::query("DELETE FROM racebracketjunction WHERE race_id = $1")
            .bind(race_id)
            .execute(conn)
            .await?
            .rows_affected(),
    )
}

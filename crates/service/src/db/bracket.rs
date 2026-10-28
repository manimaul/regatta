//! The `bracket` table: the rating bands of a class, with their race counts.
//!
//! A bracket belongs to one class; a small set of them exists in the system.

use super::race_bracket_junction as junction;
use anyhow::Result;
use regatta_common::{Bracket, RaceClassBrackets};
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

#[derive(FromRow)]
pub(crate) struct Row {
    id: i64,
    name: String,
    description: Option<String>,
    active: bool,
    min_r: f32,
    max_r: f32,
    race_class: i64,
}

const COLUMNS: &str = "id, name, description, active, min_r, max_r, race_class";

fn to_bracket(row: &Row, number_of_races: i64) -> Bracket {
    Bracket {
        id: row.id,
        name: row.name.clone(),
        description: row.description.clone(),
        active: row.active,
        number_of_races,
        min_rating: row.min_r,
        max_rating: row.max_r,
        class_id: row.race_class,
    }
}

/// Hydrates rows with their race counts from one grouped query.
async fn hydrate(conn: &mut PgConnection, rows: &[Row]) -> Result<Vec<Bracket>> {
    let ids: Vec<i64> = rows.iter().map(|r| r.id).collect();
    let counts = junction::race_counts_by_bracket(conn, &ids).await?;
    Ok(rows
        .iter()
        .map(|r| to_bracket(r, counts.get(&r.id).copied().unwrap_or(0)))
        .collect())
}

/// Inserts the bracket (id 0) or upserts it by id, returning it as stored with its race count.
///
/// # Errors
///
/// Returns `Err` if a statement fails.
pub async fn upsert_bracket(conn: &mut PgConnection, item: &Bracket) -> Result<Option<Bracket>> {
    let set =
        "name = EXCLUDED.name, description = EXCLUDED.description, active = EXCLUDED.active, \
               min_r = EXCLUDED.min_r, max_r = EXCLUDED.max_r, race_class = EXCLUDED.race_class";
    let description = item.description.as_deref().map(str::trim);
    let row: Option<Row> = if item.id > 0 {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO bracket (id, name, description, active, min_r, max_r, race_class) \
             VALUES ($1, $2, $3, $4, $5, $6, $7) ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(item.id)
        .bind(item.name.trim())
        .bind(description)
        .bind(item.active)
        .bind(item.min_rating)
        .bind(item.max_rating)
        .bind(item.class_id)
        .fetch_optional(&mut *conn)
        .await?
    } else {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO bracket (name, description, active, min_r, max_r, race_class) \
             VALUES ($1, $2, $3, $4, $5, $6) ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(item.name.trim())
        .bind(description)
        .bind(item.active)
        .bind(item.min_rating)
        .bind(item.max_rating)
        .bind(item.class_id)
        .fetch_optional(&mut *conn)
        .await?
    };
    match row {
        Some(row) => {
            let count = junction::race_count_for_bracket(conn, row.id).await?;
            Ok(Some(to_bracket(&row, count)))
        }
        None => Ok(None),
    }
}

/// Deletes the class's brackets missing from the list, then upserts the listed ones, returning them as stored.
///
/// # Errors
///
/// Returns `Err` if a statement fails (a deleted bracket still referenced by a result, say).
pub async fn upsert_brackets(
    conn: &mut PgConnection,
    brackets: &RaceClassBrackets,
) -> Result<Vec<Bracket>> {
    let existing = find_class_brackets(conn, brackets.race_class.id).await?;
    for bracket in existing {
        if !brackets.brackets.iter().any(|b| b.id == bracket.id) {
            delete_bracket(conn, bracket.id).await?;
        }
    }
    let mut out = Vec::with_capacity(brackets.brackets.len());
    for bracket in &brackets.brackets {
        if let Some(b) = upsert_bracket(conn, bracket).await? {
            out.push(b);
        }
    }
    Ok(out)
}

/// Deletes the bracket `bracket_id`, returning the rows deleted.
///
/// # Errors
///
/// Returns `Err` if the statement fails.
pub async fn delete_bracket(conn: &mut PgConnection, bracket_id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM bracket WHERE id = $1")
        .bind(bracket_id)
        .execute(conn)
        .await?
        .rows_affected())
}

/// Returns the brackets of class `class_id`, sorted by min rating.
///
/// # Errors
///
/// Returns `Err` if a query fails.
pub async fn find_class_brackets(conn: &mut PgConnection, class_id: i64) -> Result<Vec<Bracket>> {
    Ok(find_class_brackets_of(conn, &[class_id])
        .await?
        .remove(&class_id)
        .unwrap_or_default())
}

/// Returns the brackets of each class in `class_ids`, sorted by min rating; two statements for any number of them.
///
/// # Errors
///
/// Returns `Err` if a query fails.
pub async fn find_class_brackets_of(
    conn: &mut PgConnection,
    class_ids: &[i64],
) -> Result<HashMap<i64, Vec<Bracket>>> {
    if class_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM bracket WHERE race_class = ANY($1)"
    )))
    .bind(class_ids)
    .fetch_all(&mut *conn)
    .await?;
    let mut grouped: HashMap<i64, Vec<Bracket>> = HashMap::new();
    for bracket in hydrate(conn, &rows).await? {
        grouped.entry(bracket.class_id).or_default().push(bracket);
    }
    for brackets in grouped.values_mut() {
        brackets.sort_by(|a, b| a.min_rating.total_cmp(&b.min_rating));
    }
    Ok(grouped)
}

/// Returns the bracket `bracket_id` with its race count, or `None` if it does not exist.
///
/// # Errors
///
/// Returns `Err` if a query fails.
pub async fn find_bracket(conn: &mut PgConnection, bracket_id: i64) -> Result<Option<Bracket>> {
    Ok(find_brackets(conn, &[bracket_id])
        .await?
        .remove(&bracket_id))
}

/// Returns the brackets with `bracket_ids`, by id; two statements for any number of brackets.
///
/// # Errors
///
/// Returns `Err` if a query fails.
pub async fn find_brackets(
    conn: &mut PgConnection,
    bracket_ids: &[i64],
) -> Result<HashMap<i64, Bracket>> {
    if bracket_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM bracket WHERE id = ANY($1)"
    )))
    .bind(bracket_ids)
    .fetch_all(&mut *conn)
    .await?;
    Ok(hydrate(conn, &rows)
        .await?
        .into_iter()
        .map(|b| (b.id, b))
        .collect())
}

//! = `SeriesTable.kt`.

use super::race;
use anyhow::Result;
use regatta_common::Series;
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

#[derive(FromRow)]
struct Row {
    id: i64,
    name: String,
    sort: i32,
    active: bool,
}

const COLUMNS: &str = "id, name, sort, active";

fn to_series(row: &Row, race_count: i64) -> Series {
    Series {
        id: row.id,
        name: row.name.clone(),
        sort: row.sort,
        race_count,
        active: row.active,
    }
}

/// Hydrates rows with their race counts from one grouped query.
async fn hydrate(conn: &mut PgConnection, rows: &[Row]) -> Result<Vec<Series>> {
    let ids: Vec<i64> = rows.iter().map(|r| r.id).collect();
    let counts = race::race_counts_by_series(conn, &ids).await?;
    Ok(rows
        .iter()
        .map(|r| to_series(r, counts.get(&r.id).copied().unwrap_or(0)))
        .collect())
}

pub async fn select_all_series(conn: &mut PgConnection) -> Result<Vec<Series>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM series ORDER BY sort"
    )))
    .fetch_all(&mut *conn)
    .await?;
    hydrate(conn, &rows).await
}

pub async fn select_series(conn: &mut PgConnection, id: i64) -> Result<Option<Series>> {
    Ok(find_series(conn, &[id]).await?.remove(&id))
}

/// The series with `ids`, by id; two statements for any number of series.
pub async fn find_series(conn: &mut PgConnection, ids: &[i64]) -> Result<HashMap<i64, Series>> {
    if ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM series WHERE id = ANY($1)"
    )))
    .bind(ids)
    .fetch_all(&mut *conn)
    .await?;
    Ok(hydrate(conn, &rows)
        .await?
        .into_iter()
        .map(|s| (s.id, s))
        .collect())
}

pub async fn delete_series(conn: &mut PgConnection, id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM series WHERE id = $1")
        .bind(id)
        .execute(conn)
        .await?
        .rows_affected())
}

pub async fn select_by_name(conn: &mut PgConnection, name: &str) -> Result<Vec<Series>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM series WHERE name ILIKE $1"
    )))
    .bind(format!("%{name}%"))
    .fetch_all(&mut *conn)
    .await?;
    hydrate(conn, &rows).await
}

pub async fn upsert_series(conn: &mut PgConnection, list: &[Series]) -> Result<Vec<Series>> {
    let mut out = Vec::with_capacity(list.len());
    for series in list {
        let row: Option<Row> = if series.id > 0 {
            sqlx::query_as(sqlx::AssertSqlSafe(format!(
                "INSERT INTO series (id, name, sort, active) VALUES ($1, $2, $3, $4) \
                 ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, sort = EXCLUDED.sort, active = EXCLUDED.active \
                 RETURNING {COLUMNS}"
            )))
            .bind(series.id)
            .bind(series.name.trim())
            .bind(series.sort)
            .bind(series.active)
            .fetch_optional(&mut *conn)
            .await?
        } else {
            sqlx::query_as(sqlx::AssertSqlSafe(format!(
                "INSERT INTO series (name, sort, active) VALUES ($1, $2, $3) \
                 ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, sort = EXCLUDED.sort, active = EXCLUDED.active \
                 RETURNING {COLUMNS}"
            )))
            .bind(series.name.trim())
            .bind(series.sort)
            .bind(series.active)
            .fetch_optional(&mut *conn)
            .await?
        };
        if let Some(row) = row {
            let count = race::race_count_for_series(conn, row.id).await?;
            out.push(to_series(&row, count));
        }
    }
    Ok(out)
}

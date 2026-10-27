//! = `AuthTable.kt`.

use anyhow::Result;
use regatta_common::AuthRecord;
use sqlx::{FromRow, PgConnection};

#[derive(FromRow)]
struct Row {
    id: i64,
    hash: String,
    user_name: String,
}

const COLUMNS: &str = "id, hash, user_name";

fn to_record(row: &Row) -> AuthRecord {
    AuthRecord {
        id: row.id,
        hash: row.hash.clone(),
        user_name: row.user_name.clone(),
    }
}

/// `AuthTable.selectAll().count() > 0`: false means bootstrap mode.
pub async fn admin_exists(conn: &mut PgConnection) -> Result<bool> {
    let count: i64 = sqlx::query_scalar("SELECT count(*) FROM auth")
        .fetch_one(conn)
        .await?;
    Ok(count > 0)
}

fn single(rows: Vec<Row>) -> Option<AuthRecord> {
    match rows.as_slice() {
        [row] => Some(to_record(row)),
        _ => None,
    }
}

pub async fn get_auth_by_name(
    conn: &mut PgConnection,
    user_name: &str,
) -> Result<Option<AuthRecord>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM auth WHERE user_name = $1"
    )))
    .bind(user_name)
    .fetch_all(conn)
    .await?;
    Ok(single(rows))
}

pub async fn get_auth(conn: &mut PgConnection, id: i64) -> Result<Option<AuthRecord>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM auth WHERE id = $1"
    )))
    .bind(id)
    .fetch_all(conn)
    .await?;
    Ok(single(rows))
}

pub async fn save_auth(conn: &mut PgConnection, record: &AuthRecord) -> Result<Option<AuthRecord>> {
    let set = "hash = EXCLUDED.hash, user_name = EXCLUDED.user_name";
    let row: Option<Row> = if record.id > 0 {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO auth (id, hash, user_name) VALUES ($1, $2, $3) \
             ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(record.id)
        .bind(&record.hash)
        .bind(&record.user_name)
        .fetch_optional(conn)
        .await?
    } else {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO auth (hash, user_name) VALUES ($1, $2) \
             ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(&record.hash)
        .bind(&record.user_name)
        .fetch_optional(conn)
        .await?
    };
    Ok(row.as_ref().map(to_record))
}

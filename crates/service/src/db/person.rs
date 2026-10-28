//! The `person` table: skippers and RC volunteers.

use super::kotlin_str_cmp;
use anyhow::Result;
use regatta_common::Person;
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

#[derive(FromRow)]
pub(crate) struct Row {
    pub id: i64,
    pub first: String,
    pub last: String,
    pub club_member: bool,
    pub active: bool,
}

const COLUMNS: &str = "id, first, last, club_member, active";

pub(crate) fn to_person(row: &Row) -> Person {
    Person {
        id: row.id,
        first: row.first.clone(),
        last: row.last.clone(),
        club_member: row.club_member,
        active: row.active,
    }
}

/// Returns the person `id`, or `None` if it does not exist.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn select_person(conn: &mut PgConnection, id: i64) -> Result<Option<Person>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM person WHERE id = $1"
    )))
    .bind(id)
    .fetch_all(conn)
    .await?;
    Ok(match rows.as_slice() {
        [row] => Some(to_person(row)),
        _ => None,
    })
}

/// Returns the people with `ids`, by id, in one query.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn find_people(conn: &mut PgConnection, ids: &[i64]) -> Result<HashMap<i64, Person>> {
    if ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM person WHERE id = ANY($1)"
    )))
    .bind(ids)
    .fetch_all(conn)
    .await?;
    Ok(rows.iter().map(|r| (r.id, to_person(r))).collect())
}

/// Returns the people whose first or last name contains `name`, case-insensitively.
///
/// The search is `first ILIKE %name% OR last ILIKE %name%`; pattern characters in `name` pass through.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn find_person(conn: &mut PgConnection, name: &str) -> Result<Vec<Person>> {
    let pattern = format!("%{name}%");
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM person WHERE first ILIKE $1 OR last ILIKE $2"
    )))
    .bind(&pattern)
    .bind(&pattern)
    .fetch_all(conn)
    .await?;
    Ok(rows.iter().map(to_person).collect())
}

/// Inserts the person (id 0) or upserts it by id, returning it as stored; names are trimmed.
///
/// # Errors
///
/// Returns `Err` if the statement fails.
pub async fn upsert_person(conn: &mut PgConnection, person: &Person) -> Result<Option<Person>> {
    let set = "first = EXCLUDED.first, last = EXCLUDED.last, club_member = EXCLUDED.club_member, active = EXCLUDED.active";
    let row: Option<Row> = if person.id > 0 {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO person (id, first, last, club_member, active) VALUES ($1, $2, $3, $4, $5) \
             ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(person.id)
        .bind(person.first.trim())
        .bind(person.last.trim())
        .bind(person.club_member)
        .bind(person.active)
        .fetch_optional(conn)
        .await?
    } else {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO person (first, last, club_member, active) VALUES ($1, $2, $3, $4) \
             ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(person.first.trim())
        .bind(person.last.trim())
        .bind(person.club_member)
        .bind(person.active)
        .fetch_optional(conn)
        .await?
    };
    Ok(row.as_ref().map(to_person))
}

/// Returns everyone, sorted by first name in UTF-16 order ([`kotlin_str_cmp`]), stably over the table order.
///
/// # Errors
///
/// Returns `Err` if the query fails.
pub async fn select_all_people(conn: &mut PgConnection) -> Result<Vec<Person>> {
    let rows: Vec<Row> =
        sqlx::query_as(sqlx::AssertSqlSafe(format!("SELECT {COLUMNS} FROM person")))
            .fetch_all(conn)
            .await?;
    let mut people: Vec<Person> = rows.iter().map(to_person).collect();
    people.sort_by(|a, b| kotlin_str_cmp(&a.first, &b.first));
    Ok(people)
}

/// Deletes the person `id`, returning the rows deleted; unlink their boats first ([`super::boat::remove_person`]).
///
/// # Errors
///
/// Returns `Err` if the statement fails (a boat or race still references the person, say).
pub async fn delete_person(conn: &mut PgConnection, id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM person WHERE id = $1")
        .bind(id)
        .execute(conn)
        .await?
        .rows_affected())
}

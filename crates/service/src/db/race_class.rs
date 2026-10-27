//! = `RaceClassTable.kt`.

use super::{bracket, enum_from_name, race_bracket_junction as junction};
use anyhow::Result;
use regatta_common::{RaceClass, RaceClassBrackets};
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

#[derive(FromRow)]
struct Row {
    id: i64,
    name: String,
    sort: i32,
    rating_type: String,
    active: bool,
}

const COLUMNS: &str = "id, name, sort, rating_type, active";

fn to_class(row: &Row, number_of_races: i64) -> Result<RaceClass> {
    Ok(RaceClass {
        id: row.id,
        name: row.name.clone(),
        sort: row.sort,
        rating_type: enum_from_name(&row.rating_type)?,
        number_of_races,
        active: row.active,
    })
}

/// Hydrates rows with their race counts from one grouped query.
async fn hydrate(conn: &mut PgConnection, rows: &[Row]) -> Result<Vec<RaceClass>> {
    let ids: Vec<i64> = rows.iter().map(|r| r.id).collect();
    let counts = junction::race_counts_by_class(conn, &ids).await?;
    rows.iter()
        .map(|r| to_class(r, counts.get(&r.id).copied().unwrap_or(0)))
        .collect()
}

async fn with_brackets(
    conn: &mut PgConnection,
    classes: Vec<RaceClass>,
) -> Result<Vec<RaceClassBrackets>> {
    let ids: Vec<i64> = classes.iter().map(|c| c.id).collect();
    let mut brackets = bracket::find_class_brackets_of(conn, &ids).await?;
    Ok(classes
        .into_iter()
        .map(|race_class| {
            let brackets = brackets.remove(&race_class.id).unwrap_or_default();
            RaceClassBrackets {
                race_class,
                brackets,
            }
        })
        .collect())
}

/// Every class with its brackets, in sort order; four statements however many classes there are.
pub async fn all_classes(conn: &mut PgConnection) -> Result<Vec<RaceClassBrackets>> {
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM raceclass ORDER BY sort"
    )))
    .fetch_all(&mut *conn)
    .await?;
    let classes = hydrate(conn, &rows).await?;
    with_brackets(conn, classes).await
}

pub async fn upsert_class_brackets(
    conn: &mut PgConnection,
    body: &RaceClassBrackets,
) -> Result<Option<RaceClassBrackets>> {
    let Some(race_class) = upsert_class(conn, &body.race_class).await? else {
        return Ok(None);
    };
    let brackets = bracket::upsert_brackets(
        conn,
        &RaceClassBrackets {
            race_class: race_class.clone(),
            brackets: body
                .brackets
                .iter()
                .map(|b| Bracketed::with_class(b, race_class.id))
                .collect(),
        },
    )
    .await?;
    Ok(Some(RaceClassBrackets {
        race_class,
        brackets,
    }))
}

/// `Bracket.copy(classId = ...)`.
struct Bracketed;
impl Bracketed {
    fn with_class(b: &regatta_common::Bracket, class_id: i64) -> regatta_common::Bracket {
        regatta_common::Bracket {
            class_id,
            ..b.clone()
        }
    }
}

pub async fn upsert_class(conn: &mut PgConnection, item: &RaceClass) -> Result<Option<RaceClass>> {
    let set = "name = EXCLUDED.name, active = EXCLUDED.active, rating_type = EXCLUDED.rating_type, sort = EXCLUDED.sort";
    let row: Option<Row> = if item.id > 0 {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO raceclass (id, name, active, rating_type, sort) VALUES ($1, $2, $3, $4, $5) \
             ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(item.id)
        .bind(item.name.trim())
        .bind(item.active)
        .bind(item.rating_type.name())
        .bind(item.sort)
        .fetch_optional(&mut *conn)
        .await?
    } else {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO raceclass (name, active, rating_type, sort) VALUES ($1, $2, $3, $4) \
             ON CONFLICT (id) DO UPDATE SET {set} RETURNING {COLUMNS}"
        )))
        .bind(item.name.trim())
        .bind(item.active)
        .bind(item.rating_type.name())
        .bind(item.sort)
        .fetch_optional(&mut *conn)
        .await?
    };
    match row {
        Some(row) => {
            let count = junction::race_count_for_class(conn, row.id).await?;
            Ok(Some(to_class(&row, count)?))
        }
        None => Ok(None),
    }
}

/// Upserts every class and answers each with its brackets, sorted by `sort`.
pub async fn upsert_class_list(
    conn: &mut PgConnection,
    list: &[RaceClass],
) -> Result<Vec<RaceClassBrackets>> {
    let mut classes = Vec::with_capacity(list.len());
    for item in list {
        if let Some(c) = upsert_class(conn, item).await? {
            classes.push(c);
        }
    }
    let mut out = with_brackets(conn, classes).await?;
    out.sort_by_key(|c| c.race_class.sort);
    Ok(out)
}

pub async fn delete_class(conn: &mut PgConnection, class_id: i64) -> Result<u64> {
    Ok(sqlx::query("DELETE FROM raceclass WHERE id = $1")
        .bind(class_id)
        .execute(conn)
        .await?
        .rows_affected())
}

pub async fn select_by_id(conn: &mut PgConnection, id: i64) -> Result<Option<RaceClass>> {
    Ok(find_classes(conn, &[id]).await?.remove(&id))
}

/// The classes with `class_ids`, by id; two statements for any number of classes.
pub async fn find_classes(
    conn: &mut PgConnection,
    class_ids: &[i64],
) -> Result<HashMap<i64, RaceClass>> {
    if class_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM raceclass WHERE id = ANY($1)"
    )))
    .bind(class_ids)
    .fetch_all(&mut *conn)
    .await?;
    Ok(hydrate(conn, &rows)
        .await?
        .into_iter()
        .map(|c| (c.id, c))
        .collect())
}

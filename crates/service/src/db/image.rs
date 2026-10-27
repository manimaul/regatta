//! = `ImageTable.kt`: the race report images (`hash` is the file name).

use anyhow::Result;
use sqlx::{PgConnection, Row};
use std::collections::HashMap;

pub async fn get_image(conn: &mut PgConnection, name: &str) -> Result<Option<Vec<u8>>> {
    let rows = sqlx::query("SELECT data FROM image WHERE hash = $1")
        .bind(name)
        .fetch_all(conn)
        .await?;
    Ok(match rows.as_slice() {
        [row] => Some(row.try_get::<Vec<u8>, _>("data")?),
        _ => None,
    })
}

/// The report image name of race `id`, without reading the image bytes.
pub async fn get_race_report_image_name(
    conn: &mut PgConnection,
    id: i64,
) -> Result<Option<String>> {
    Ok(report_image_names(conn, &[id]).await?.remove(&id))
}

/// The report image name of each race in `race_ids` that has exactly one, in one query that skips the bytes.
pub async fn report_image_names(
    conn: &mut PgConnection,
    race_ids: &[i64],
) -> Result<HashMap<i64, String>> {
    if race_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows = sqlx::query("SELECT hash, race_id FROM image WHERE race_id = ANY($1)")
        .bind(race_ids)
        .fetch_all(conn)
        .await?;
    let mut grouped: HashMap<i64, Vec<String>> = HashMap::new();
    for row in rows {
        grouped
            .entry(row.try_get("race_id")?)
            .or_default()
            .push(row.try_get("hash")?);
    }
    Ok(grouped
        .into_iter()
        .filter_map(|(race, names)| match names.as_slice() {
            [name] => Some((race, name.clone())),
            _ => None,
        })
        .collect())
}

pub async fn save_race_report_image(
    conn: &mut PgConnection,
    race_id: i64,
    name: &str,
    image: &[u8],
) -> Result<()> {
    sqlx::query("DELETE FROM image WHERE race_id = $1")
        .bind(race_id)
        .execute(&mut *conn)
        .await?;
    sqlx::query("INSERT INTO image (race_id, hash, data) VALUES ($1, $2, $3)")
        .bind(race_id)
        .bind(name)
        .bind(image)
        .execute(conn)
        .await?;
    Ok(())
}

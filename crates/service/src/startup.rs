//! The startup schema check: connect, apply the baseline schema to an empty database, and refuse any other version.
//!
//! D6 in `docs/explore_implemented/rust_port.md`: the server serves schema version 5 only (or 15, which older fresh
//! installs wrote for the same schema). Nothing is ever altered or upgraded here. The entry points are
//! [`connect`] and [`prepare`], called by the server's `main`.

use anyhow::{anyhow, bail, Context, Result};
use sqlx::postgres::PgPoolOptions;
use sqlx::PgPool;

/// The schema applied to an empty database: `migrations/0001_baseline.sql`, which ends by writing version 5.
pub const BASELINE_SQL: &str = include_str!("../../../migrations/0001_baseline.sql");

/// The schema version this server serves.
pub const SCHEMA_VERSION: i32 = 5;
/// The version older fresh installs wrote (`SCHEMA_VERSION + 10`) for the same schema; accepted as 5.
pub const FRESH_INSTALL_VERSION: i32 = 15;

/// The hint that ends the startup error for a missing or unsupported schema version: how to reach version 5.
pub const RUN_KOTLIN_FIRST: &str =
    "run the last Kotlin release of the regatta server first to bring the schema to version 5";

/// Opens a connection pool of up to 10 connections to `db_url`.
///
/// # Errors
///
/// Returns `Err` if the database cannot be reached; the message carries the URL with its password redacted.
pub async fn connect(db_url: &str) -> Result<PgPool> {
    PgPoolOptions::new()
        .max_connections(10)
        .connect(db_url)
        .await
        .with_context(|| format!("connecting to {}", redact(db_url)))
}

/// Applies the baseline to an empty database, then checks the schema version and returns it.
///
/// No `metadata` table means an empty database, which gets [`BASELINE_SQL`]. The version must then be
/// [`SCHEMA_VERSION`] or [`FRESH_INSTALL_VERSION`]. Logs `db migration version = N`.
///
/// # Errors
///
/// - Returns `Err` if a query fails or the baseline cannot be applied.
/// - Returns `Err` if `metadata.version` is missing, not an integer, or not a supported version.
pub async fn prepare(pool: &PgPool) -> Result<i32> {
    let has_metadata: bool = sqlx::query_scalar(
        "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'metadata')",
    )
    .fetch_one(pool)
    .await
    .context("checking for the metadata table")?;
    if !has_metadata {
        tracing::info!(
            "no metadata table: applying the baseline schema (version {SCHEMA_VERSION})"
        );
        sqlx::raw_sql(BASELINE_SQL)
            .execute(pool)
            .await
            .context("applying migrations/0001_baseline.sql")?;
    }
    let version = schema_version(pool).await?;
    tracing::info!("db migration version = {version}");
    match version {
        SCHEMA_VERSION | FRESH_INSTALL_VERSION => Ok(version),
        other => bail!(
            "unsupported database schema version {other} (this server serves version {SCHEMA_VERSION}): {RUN_KOTLIN_FIRST}"
        ),
    }
}

/// Reads `metadata.version` as an integer.
///
/// # Errors
///
/// Returns `Err` if the query fails, there is no version row, or its value is not an integer.
pub async fn schema_version(pool: &PgPool) -> Result<i32> {
    let value: Option<String> =
        sqlx::query_scalar("SELECT value FROM metadata WHERE name = 'version'")
            .fetch_optional(pool)
            .await
            .context("reading metadata.version")?;
    let value = value.ok_or_else(|| anyhow!("no metadata version row: {RUN_KOTLIN_FIRST}"))?;
    value
        .trim()
        .parse::<i32>()
        .with_context(|| format!("metadata.version {value:?} is not an integer"))
}

/// The URL without its password, for log lines.
fn redact(url: &str) -> String {
    match (url.find("://"), url.rfind('@')) {
        (Some(scheme), Some(at)) if at > scheme => {
            let creds = &url[scheme + 3..at];
            match creds.find(':') {
                Some(colon) => format!(
                    "{}{}:***{}",
                    &url[..scheme + 3],
                    &creds[..colon],
                    &url[at..]
                ),
                None => url.to_string(),
            }
        }
        _ => url.to_string(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn password_is_redacted() {
        assert_eq!(
            redact("postgres://regatta_admin:mysecretpassword@localhost:5432/regatta"),
            "postgres://regatta_admin:***@localhost:5432/regatta"
        );
        assert_eq!(
            redact("postgres://localhost/regatta"),
            "postgres://localhost/regatta"
        );
    }

    #[test]
    fn baseline_ends_with_the_version_row() {
        assert!(BASELINE_SQL
            .trim_end()
            .ends_with("INSERT INTO metadata (name, value) VALUES ('version', '5');"));
    }
}

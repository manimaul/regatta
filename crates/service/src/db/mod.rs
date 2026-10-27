//! The database service layer (= `db/Database.kt` and the `*Table.kt` objects), hand-written
//! SQL over sqlx (D2). One module per Kotlin table object; [`Db`] carries the `RegattaDatabase`
//! functions under the same names in snake_case. Every write that Kotlin ran as one `dbQuery`
//! runs in one transaction here.
//!
//! Schema caveats (Phase 0): production databases carry historical object names and a different
//! column order, so no statement names a sequence or constraint (`ON CONFLICT (columns)`,
//! `RETURNING` named columns) and no query reads `SELECT *` positionally. `timestamp without time
//! zone` columns are UTC (D7): they travel as text and are tagged UTC in [`time`].

pub mod auth_table;
pub mod boat;
pub mod bracket;
pub mod image;
pub mod orc_table;
pub mod person;
pub mod race;
pub mod race_bracket_junction;
pub mod race_class;
pub mod race_results;
pub mod race_time;
pub mod series;
pub mod time;

use anyhow::{Context, Result};
use jiff::tz::TimeZone;
use regatta_common::*;
use serde::de::DeserializeOwned;
use sqlx::{PgConnection, PgPool};
use std::collections::HashMap;

/// `dbQuery` for writes. Exposed's `suspendTransaction` re-runs the block on an `SQLException`
/// up to `defaultMaxAttempts` (3) times; a failing insert therefore draws three sequence values,
/// which decides the ids of the records created afterwards (the contract cassettes record them).
/// The same retry happens here for a database error: each attempt is a fresh transaction.
macro_rules! write {
    ($self:ident, |$c:ident| $body:expr) => {{
        let mut attempt = 1;
        loop {
            match $self.write_once(async |$c| $body).await {
                Ok(value) => break Ok(value),
                Err(e) if attempt < MAX_ATTEMPTS && is_database_error(&e) => {
                    tracing::debug!("transaction attempt {attempt} failed, retrying: {e:#}");
                    attempt += 1;
                }
                Err(e) => break Err(e),
            }
        }
    }};
}

/// `RatingType.valueOf(...)` and the other enum `valueOf`s: the stored name to the enum, an error
/// for a name the enum does not have.
pub fn enum_from_name<T: DeserializeOwned>(name: &str) -> Result<T> {
    serde_json::from_value(serde_json::Value::String(name.to_string()))
        .with_context(|| format!("unknown enum name {name:?}"))
}

/// Kotlin's `String.compareTo`: by UTF-16 code units.
pub fn kotlin_str_cmp(a: &str, b: &str) -> std::cmp::Ordering {
    a.encode_utf16().cmp(b.encode_utf16())
}

/// Exposed's `defaultMaxAttempts`.
const MAX_ATTEMPTS: u32 = 3;

/// Whether the error chain holds an error the database reported (Exposed's `SQLException`).
fn is_database_error(error: &anyhow::Error) -> bool {
    error.chain().any(|cause| {
        matches!(
            cause.downcast_ref::<sqlx::Error>(),
            Some(sqlx::Error::Database(_))
        )
    })
}

/// The `RegattaDatabase` object: a pool and the service functions.
#[derive(Clone)]
pub struct Db {
    pool: PgPool,
    /// The zone the `timestamp without time zone` columns are read and written in (see [`time`]).
    tz: TimeZone,
}

impl Db {
    /// Production: naive timestamps are UTC (D7, what the Kotlin server's `main` forces).
    pub fn new(pool: PgPool) -> Db {
        Db::with_naive_time_zone(pool, TimeZone::UTC)
    }

    /// Naive timestamps read in `tz`, the JVM default zone of the Kotlin process being
    /// reproduced (the Gradle golden test ran in the host's Pacific zone).
    pub fn with_naive_time_zone(pool: PgPool, tz: TimeZone) -> Db {
        Db { pool, tz }
    }

    pub fn naive_time_zone(&self) -> &TimeZone {
        &self.tz
    }

    pub fn pool(&self) -> &PgPool {
        &self.pool
    }

    /// `dbQuery` for reads: one connection, no transaction needed.
    async fn read<T>(&self, f: impl AsyncFnOnce(&mut PgConnection) -> Result<T>) -> Result<T> {
        let mut conn = self.pool.acquire().await?;
        f(&mut conn).await
    }

    /// One attempt of a `dbQuery` write: every statement in one transaction, committed on success.
    /// Callers go through the `write!` macro, which retries like Exposed.
    async fn write_once<T>(
        &self,
        f: impl AsyncFnOnce(&mut PgConnection) -> Result<T>,
    ) -> Result<T> {
        let mut tx = self.pool.begin().await?;
        match f(&mut tx).await {
            Ok(value) => {
                tx.commit().await?;
                Ok(value)
            }
            Err(e) => {
                tx.rollback().await.ok();
                Err(e)
            }
        }
    }

    // Series ------------------------

    pub async fn all_series(&self) -> Result<Vec<Series>> {
        self.read(async |c| series::select_all_series(c).await)
            .await
    }
    pub async fn find_series(&self, id: i64) -> Result<Option<Series>> {
        self.read(async |c| series::select_series(c, id).await)
            .await
    }
    pub async fn delete_series(&self, id: i64) -> Result<u64> {
        write!(self, |c| series::delete_series(c, id).await)
    }
    pub async fn find_series_by_name(&self, name: &str) -> Result<Vec<Series>> {
        self.read(async |c| series::select_by_name(c, name).await)
            .await
    }
    pub async fn upsert_series(&self, list: &[Series]) -> Result<Vec<Series>> {
        write!(self, |c| series::upsert_series(c, list).await)
    }

    // Person ------------------------

    pub async fn find_person(&self, id: i64) -> Result<Option<Person>> {
        self.read(async |c| person::select_person(c, id).await)
            .await
    }
    pub async fn find_person_by_name(&self, name: &str) -> Result<Vec<Person>> {
        self.read(async |c| person::find_person(c, name).await)
            .await
    }
    pub async fn upsert_person(&self, p: &Person) -> Result<Option<Person>> {
        write!(self, |c| person::upsert_person(c, p).await)
    }
    pub async fn all_people(&self) -> Result<Vec<Person>> {
        self.read(async |c| person::select_all_people(c).await)
            .await
    }
    /// Unlinks the person's boats, then deletes the person (one transaction).
    pub async fn delete_person(&self, id: i64) -> Result<u64> {
        write!(self, |c| {
            boat::remove_person(c, id).await?;
            person::delete_person(c, id).await
        })
    }

    // Boat ------------------------

    pub async fn find_boat(&self, id: i64) -> Result<Option<BoatSkipper>> {
        self.read(async |c| boat::find_boat_skipper(c, id).await)
            .await
    }
    pub async fn upsert_boat(&self, b: &Boat) -> Result<Option<Boat>> {
        write!(self, |c| boat::upsert_boat(c, b).await)
    }
    pub async fn all_boats(&self) -> Result<Vec<BoatSkipper>> {
        self.read(async |c| boat::select_all_boats(c).await).await
    }
    pub async fn find_boat_for_person(&self, person_id: i64) -> Result<Option<BoatSkipper>> {
        self.read(async |c| boat::find_boat_for_skipper(c, person_id).await)
            .await
    }
    pub async fn delete_boat(&self, id: i64) -> Result<u64> {
        write!(self, |c| boat::delete_boat(c, id).await)
    }

    // Auth ------------------------

    pub async fn admin_exists(&self) -> Result<bool> {
        self.read(async |c| auth_table::admin_exists(c).await).await
    }
    pub async fn get_auth_by_name(&self, user_name: &str) -> Result<Option<AuthRecord>> {
        self.read(async |c| auth_table::get_auth_by_name(c, user_name).await)
            .await
    }
    pub async fn get_auth(&self, id: i64) -> Result<Option<AuthRecord>> {
        self.read(async |c| auth_table::get_auth(c, id).await).await
    }
    pub async fn save_auth(&self, record: &AuthRecord) -> Result<Option<AuthRecord>> {
        write!(self, |c| auth_table::save_auth(c, record).await)
    }

    // Race Bracket ------------------------

    pub async fn find_bracket(&self, id: i64) -> Result<Option<Bracket>> {
        self.read(async |c| bracket::find_bracket(c, id).await)
            .await
    }
    pub async fn upsert_bracket(&self, b: &Bracket) -> Result<Option<Bracket>> {
        write!(self, |c| bracket::upsert_bracket(c, b).await)
    }
    pub async fn delete_bracket(&self, id: i64) -> Result<u64> {
        write!(self, |c| bracket::delete_bracket(c, id).await)
    }

    // Race Class ------------------------

    pub async fn upsert_class_brackets(
        &self,
        body: &RaceClassBrackets,
    ) -> Result<Option<RaceClassBrackets>> {
        write!(self, |c| race_class::upsert_class_brackets(c, body).await)
    }
    pub async fn upsert_class_list(&self, list: &[RaceClass]) -> Result<Vec<RaceClassBrackets>> {
        write!(self, |c| race_class::upsert_class_list(c, list).await)
    }
    pub async fn all_classes(&self) -> Result<Vec<RaceClassBrackets>> {
        self.read(async |c| race_class::all_classes(c).await).await
    }
    pub async fn delete_class(&self, id: i64) -> Result<u64> {
        write!(self, |c| race_class::delete_class(c, id).await)
    }
    pub async fn find_class(&self, id: i64) -> Result<Option<RaceClass>> {
        self.read(async |c| race_class::select_by_id(c, id).await)
            .await
    }

    // Race ------------------------

    /// The schedules of the year's races (Pacific calendar, D8), by start time.
    pub async fn all_races(&self, year: i32) -> Result<Vec<RaceSchedule>> {
        self.read(async |c| race::all_races(c, year, &self.tz).await)
            .await
    }
    /// The ids of the series' races in the year.
    pub async fn series_races(&self, series_id: i64, year: i32) -> Result<Vec<i64>> {
        self.read(async |c| race::series_races(c, series_id, year, &self.tz).await)
            .await
    }
    pub async fn find_race(&self, id: i64) -> Result<Option<Race>> {
        self.read(async |c| race::find_race(c, id).await).await
    }
    pub async fn find_race_schedule(&self, id: i64) -> Result<Option<RaceSchedule>> {
        self.read(async |c| race::find_race_schedule(c, id, &self.tz).await)
            .await
    }
    pub async fn find_race_schedules(&self, ids: &[i64]) -> Result<Vec<RaceSchedule>> {
        self.read(async |c| race::find_race_schedules(c, ids, &self.tz).await)
            .await
    }
    pub async fn find_certificate(&self, ref_no: Option<&str>) -> Result<Option<OrcCertificate>> {
        self.read(async |c| orc_table::find_certificate(c, ref_no).await)
            .await
    }
    pub async fn find_certificates(
        &self,
        refs: &[String],
    ) -> Result<HashMap<String, OrcCertificate>> {
        self.read(async |c| orc_table::find_certificates(c, refs).await)
            .await
    }
    pub async fn all_certificates(&self) -> Result<Vec<OrcCertificate>> {
        self.read(async |c| orc_table::all_certificates(c).await)
            .await
    }
    /// Rows changed; `Err(InvalidCertificate)` in the chain when the certificate fails validation.
    pub async fn update_certificate(&self, cert: &OrcCertificate) -> Result<u64> {
        write!(self, |c| orc_table::update_cert(c, cert).await)
    }
    pub async fn upsert_race(&self, r: &Race) -> Result<Option<Race>> {
        write!(self, |c| race::upsert_race(c, r).await)
    }
    /// Junction rows, class times and the race, in one transaction.
    pub async fn delete_race(&self, id: i64) -> Result<u64> {
        write!(self, |c| {
            race_bracket_junction::delete_race(c, id).await?;
            race_time::delete_race(c, id).await?;
            race::delete_race(c, id).await
        })
    }
    pub async fn save_race_report_image(
        &self,
        race_id: i64,
        file_name: &str,
        bytes: &[u8],
    ) -> Result<()> {
        write!(self, |c| image::save_race_report_image(
            c, race_id, file_name, bytes
        )
        .await)
    }
    pub async fn get_race_report_image(&self, file_name: &str) -> Result<Option<Vec<u8>>> {
        self.read(async |c| image::get_image(c, file_name).await)
            .await
    }

    // Race Times ------------------------

    pub async fn all_years(&self) -> Result<Vec<String>> {
        self.read(async |c| race_time::all_years(c, &self.tz).await)
            .await
    }

    // Results ------------------------

    pub async fn delete_result(&self, id: i64) -> Result<u64> {
        write!(self, |c| race_results::delete_result(c, id).await)
    }
    pub async fn results_by_race_id(&self, race_id: i64) -> Result<Vec<RaceResult>> {
        self.read(async |c| race_results::results_by_race_id(c, race_id, &self.tz).await)
            .await
    }
    pub async fn schedule_results_by_race_id(
        &self,
        race_id: i64,
    ) -> Result<Option<RaceScheduleResults>> {
        self.read(async |c| race_results::schedule_results_by_race_id(c, race_id, &self.tz).await)
            .await
    }
    /// The results of the schedule's race, placed against that schedule.
    pub async fn results_boat_bracket_by_race_id(
        &self,
        race_schedule: &RaceSchedule,
    ) -> Result<Vec<RaceResultBoatBracket>> {
        self.read(async |c| {
            race_results::results_boat_bracket_by_race_id(
                c,
                race_schedule.race.id,
                race_schedule,
                &self.tz,
            )
            .await
        })
        .await
    }
    pub async fn upsert_result(&self, result: &RaceResult) -> Result<Option<RaceResult>> {
        write!(self, |c| race_results::upsert_result(c, result, &self.tz)
            .await)
    }
    pub async fn result_count(&self, race_id: i64) -> Result<i64> {
        self.read(async |c| race_results::count(c, race_id).await)
            .await
    }
    pub async fn insert_schedule(&self, schedule: &RaceSchedule) -> Result<Option<RaceSchedule>> {
        write!(self, |c| race::insert_schedule(c, schedule, &self.tz).await)
    }
}

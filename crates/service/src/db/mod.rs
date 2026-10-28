//! The data layer: hand-written SQL over sqlx, one module per table, behind the service functions of [`Db`].
//!
//! The REST handlers, the server functions and [`crate::reporter`] call [`Db`]; the table modules take a
//! `&mut PgConnection` so that [`Db`] can run several of them in one transaction. The design decisions are in
//! `docs/explore_implemented/rust_port.md` (D2: sqlx with hand-written SQL, D6: the frozen schema, D7: UTC
//! timestamps).
//!
//! - Every write runs in one transaction, retried on a database error (see the `write!` macro).
//! - Production databases carry historical object names and a different column order, so no statement names a
//!   sequence or constraint (`ON CONFLICT (columns)`, `RETURNING` named columns) and no query reads `SELECT *`
//!   positionally.
//! - `timestamp without time zone` columns are UTC (D7): they travel as text (`::text`) and are tagged with the
//!   zone in [`time`].
//! - Enums are stored by name and read back with [`enum_from_name`].

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

/// Runs a write in a transaction, retried in a fresh transaction up to [`MAX_ATTEMPTS`] times on a database error.
///
/// A failing insert therefore draws three sequence values, which fixes the ids of the records created afterwards;
/// the contract cassettes (`crates/contract/cassettes`) record those ids, so the retry count must not change.
/// Errors that are not database errors (a validation failure, say) are not retried.
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

/// Parses a stored enum name (`PHRF`, `DNS_RC`, ...) into its enum through the enum's serde names.
///
/// # Errors
///
/// Returns `Err` if the enum has no variant of that name.
pub fn enum_from_name<T: DeserializeOwned>(name: &str) -> Result<T> {
    serde_json::from_value(serde_json::Value::String(name.to_string()))
        .with_context(|| format!("unknown enum name {name:?}"))
}

/// Orders two strings by UTF-16 code units, not by bytes.
///
/// This is the order the golden files and contract cassettes were recorded in; it differs from `str::cmp` for
/// characters outside the Basic Multilingual Plane.
pub fn kotlin_str_cmp(a: &str, b: &str) -> std::cmp::Ordering {
    a.encode_utf16().cmp(b.encode_utf16())
}

/// How many times the `write!` macro attempts a write; the contract cassettes' ids depend on it.
const MAX_ATTEMPTS: u32 = 3;

/// Tells whether the error chain holds an error the database reported, the kind the `write!` macro retries.
fn is_database_error(error: &anyhow::Error) -> bool {
    error.chain().any(|cause| {
        matches!(
            cause.downcast_ref::<sqlx::Error>(),
            Some(sqlx::Error::Database(_))
        )
    })
}

/// The database service: a connection pool and every query and write the server makes.
///
/// Reads run on one pooled connection; writes run in one transaction each, retried by the `write!` macro. Cheap to
/// clone (the pool is shared).
#[derive(Clone)]
pub struct Db {
    pool: PgPool,
    /// The zone the `timestamp without time zone` columns are read and written in (see [`time`]).
    tz: TimeZone,
}

impl Db {
    /// Creates the production service, which reads naive timestamps as UTC (D7 in `rust_port.md`).
    pub fn new(pool: PgPool) -> Db {
        Db::with_naive_time_zone(pool, TimeZone::UTC)
    }

    /// Creates a service that reads and writes naive timestamps in `tz` instead of UTC.
    ///
    /// The golden test reads the dump in Pacific time, the zone the approved files were recorded in
    /// (`crates/server/tests/golden.rs`).
    pub fn with_naive_time_zone(pool: PgPool, tz: TimeZone) -> Db {
        Db { pool, tz }
    }

    /// The zone naive timestamps are read and written in: UTC in production.
    pub fn naive_time_zone(&self) -> &TimeZone {
        &self.tz
    }

    pub fn pool(&self) -> &PgPool {
        &self.pool
    }

    /// Runs a read on one pooled connection, without a transaction.
    async fn read<T>(&self, f: impl AsyncFnOnce(&mut PgConnection) -> Result<T>) -> Result<T> {
        let mut conn = self.pool.acquire().await?;
        f(&mut conn).await
    }

    /// Runs one attempt of a write: every statement in one transaction, committed on success, rolled back on error.
    ///
    /// Callers go through the `write!` macro, which retries it.
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

    /// Returns every series with its race count, by `sort`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn all_series(&self) -> Result<Vec<Series>> {
        self.read(async |c| series::select_all_series(c).await)
            .await
    }
    /// Returns the series `id` with its race count, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn find_series(&self, id: i64) -> Result<Option<Series>> {
        self.read(async |c| series::select_series(c, id).await)
            .await
    }
    /// Deletes the series `id`, returning the number of rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt (a race still referencing the series, say).
    pub async fn delete_series(&self, id: i64) -> Result<u64> {
        write!(self, |c| series::delete_series(c, id).await)
    }
    /// Returns the series whose name contains `name`, case-insensitively (`ILIKE`; pattern characters pass through).
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn find_series_by_name(&self, name: &str) -> Result<Vec<Series>> {
        self.read(async |c| series::select_by_name(c, name).await)
            .await
    }
    /// Inserts the series with id 0 and updates the others, in one transaction, returning them as stored.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt.
    pub async fn upsert_series(&self, list: &[Series]) -> Result<Vec<Series>> {
        write!(self, |c| series::upsert_series(c, list).await)
    }

    // Person ------------------------

    /// Returns the person `id`, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn find_person(&self, id: i64) -> Result<Option<Person>> {
        self.read(async |c| person::select_person(c, id).await)
            .await
    }
    /// Returns the people whose first or last name contains `name`, case-insensitively.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn find_person_by_name(&self, name: &str) -> Result<Vec<Person>> {
        self.read(async |c| person::find_person(c, name).await)
            .await
    }
    /// Inserts the person (id 0) or updates it, returning it as stored.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt.
    pub async fn upsert_person(&self, p: &Person) -> Result<Option<Person>> {
        write!(self, |c| person::upsert_person(c, p).await)
    }
    /// Returns everyone, sorted by first name.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn all_people(&self) -> Result<Vec<Person>> {
        self.read(async |c| person::select_all_people(c).await)
            .await
    }
    /// Unlinks the person's boats, then deletes the person (one transaction), returning the rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt.
    pub async fn delete_person(&self, id: i64) -> Result<u64> {
        write!(self, |c| {
            boat::remove_person(c, id).await?;
            person::delete_person(c, id).await
        })
    }

    // Boat ------------------------

    /// Returns the boat `id` with its skipper, race count and certificates, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or the stored rating type is unknown.
    pub async fn find_boat(&self, id: i64) -> Result<Option<BoatSkipper>> {
        self.read(async |c| boat::find_boat_skipper(c, id).await)
            .await
    }
    /// Inserts the boat (id 0) or updates it, replacing its linked certificates, returning it as stored.
    ///
    /// # Errors
    ///
    /// - Returns [`orc_table::InvalidCertificate`] in the chain if a certificate has a zero rating; nothing is
    ///   written.
    /// - Returns `Err` if a statement fails on every attempt.
    pub async fn upsert_boat(&self, b: &Boat) -> Result<Option<Boat>> {
        write!(self, |c| boat::upsert_boat(c, b).await)
    }
    /// Returns every boat with its skipper, sorted for the boats page (see [`boat::select_all_boats`]).
    ///
    /// # Errors
    ///
    /// - Returns `Err` if a query fails or a stored rating type is unknown.
    /// - Returns `Err` if an ORC boat has no certificate, so it cannot be sorted.
    pub async fn all_boats(&self) -> Result<Vec<BoatSkipper>> {
        self.read(async |c| boat::select_all_boats(c).await).await
    }
    /// Returns the one boat the person skippers, or `None` when they skipper none or several.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or the stored rating type is unknown.
    pub async fn find_boat_for_person(&self, person_id: i64) -> Result<Option<BoatSkipper>> {
        self.read(async |c| boat::find_boat_for_skipper(c, person_id).await)
            .await
    }
    /// Unlinks the boat's certificates, then deletes the boat, returning the rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt (results still referencing the boat, say).
    pub async fn delete_boat(&self, id: i64) -> Result<u64> {
        write!(self, |c| boat::delete_boat(c, id).await)
    }

    // Auth ------------------------

    /// Tells whether any admin exists; `false` means bootstrap mode, where every request is admin.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn admin_exists(&self) -> Result<bool> {
        self.read(async |c| auth_table::admin_exists(c).await).await
    }
    /// Returns the admin record of `user_name`, or `None` if there is not exactly one.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn get_auth_by_name(&self, user_name: &str) -> Result<Option<AuthRecord>> {
        self.read(async |c| auth_table::get_auth_by_name(c, user_name).await)
            .await
    }
    /// Returns the admin record `id`, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn get_auth(&self, id: i64) -> Result<Option<AuthRecord>> {
        self.read(async |c| auth_table::get_auth(c, id).await).await
    }
    /// Inserts the admin record (id 0) or updates it, returning it as stored.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt.
    pub async fn save_auth(&self, record: &AuthRecord) -> Result<Option<AuthRecord>> {
        write!(self, |c| auth_table::save_auth(c, record).await)
    }

    // Race Bracket ------------------------

    /// Returns the bracket `id` with its race count, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn find_bracket(&self, id: i64) -> Result<Option<Bracket>> {
        self.read(async |c| bracket::find_bracket(c, id).await)
            .await
    }
    /// Inserts the bracket (id 0) or updates it, returning it as stored.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt.
    pub async fn upsert_bracket(&self, b: &Bracket) -> Result<Option<Bracket>> {
        write!(self, |c| bracket::upsert_bracket(c, b).await)
    }
    /// Deletes the bracket `id`, returning the rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt.
    pub async fn delete_bracket(&self, id: i64) -> Result<u64> {
        write!(self, |c| bracket::delete_bracket(c, id).await)
    }

    // Race Class ------------------------

    /// Upserts a class and replaces its brackets with `body.brackets`, deleting the ones left out.
    ///
    /// `None` if the class row did not come back.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt or the stored rating type is unknown.
    pub async fn upsert_class_brackets(
        &self,
        body: &RaceClassBrackets,
    ) -> Result<Option<RaceClassBrackets>> {
        write!(self, |c| race_class::upsert_class_brackets(c, body).await)
    }
    /// Upserts every class in `list`, returning each with its brackets, by `sort`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt or a stored rating type is unknown.
    pub async fn upsert_class_list(&self, list: &[RaceClass]) -> Result<Vec<RaceClassBrackets>> {
        write!(self, |c| race_class::upsert_class_list(c, list).await)
    }
    /// Returns every class with its brackets, by `sort`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or a stored rating type is unknown.
    pub async fn all_classes(&self) -> Result<Vec<RaceClassBrackets>> {
        self.read(async |c| race_class::all_classes(c).await).await
    }
    /// Deletes the class `id`, returning the rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt (brackets still referencing the class, say).
    pub async fn delete_class(&self, id: i64) -> Result<u64> {
        write!(self, |c| race_class::delete_class(c, id).await)
    }
    /// Returns the class `id` with its race count, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or the stored rating type is unknown.
    pub async fn find_class(&self, id: i64) -> Result<Option<RaceClass>> {
        self.read(async |c| race_class::select_by_id(c, id).await)
            .await
    }

    // Race ------------------------

    /// Returns the schedules of the races with a class start in `year`, by start time.
    ///
    /// The year follows the Pacific calendar (D8 in `test_audit_and_plan.md`).
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
    pub async fn all_races(&self, year: i32) -> Result<Vec<RaceSchedule>> {
        self.read(async |c| race::all_races(c, year, &self.tz).await)
            .await
    }
    /// Returns the ids of the series' races with a class start in `year` (Pacific calendar).
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn series_races(&self, series_id: i64, year: i32) -> Result<Vec<i64>> {
        self.read(async |c| race::series_races(c, series_id, year, &self.tz).await)
            .await
    }
    /// Returns the race `id` with its report image name, or `None` if it does not exist.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or a stored ORC option or band name is unknown.
    pub async fn find_race(&self, id: i64) -> Result<Option<Race>> {
        self.read(async |c| race::find_race(c, id).await).await
    }
    /// Returns the schedule of race `id`, or `None` if it does not exist (see [`race::find_race_schedules`]).
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
    pub async fn find_race_schedule(&self, id: i64) -> Result<Option<RaceSchedule>> {
        self.read(async |c| race::find_race_schedule(c, id, &self.tz).await)
            .await
    }
    /// Returns the schedules of `ids` in that order, skipping ids with no race (see [`race::find_race_schedules`]).
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
    pub async fn find_race_schedules(&self, ids: &[i64]) -> Result<Vec<RaceSchedule>> {
        self.read(async |c| race::find_race_schedules(c, ids, &self.tz).await)
            .await
    }
    /// Returns the stored certificate `ref_no`, or `None` if it is not stored or `ref_no` is `None`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails or the stored JSON does not decode.
    pub async fn find_certificate(&self, ref_no: Option<&str>) -> Result<Option<OrcCertificate>> {
        self.read(async |c| orc_table::find_certificate(c, ref_no).await)
            .await
    }
    /// Returns the stored certificates with the reference numbers `refs`, by reference number.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails or a stored JSON does not decode.
    pub async fn find_certificates(
        &self,
        refs: &[String],
    ) -> Result<HashMap<String, OrcCertificate>> {
        self.read(async |c| orc_table::find_certificates(c, refs).await)
            .await
    }
    /// Returns every stored certificate.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails or a stored JSON does not decode.
    pub async fn all_certificates(&self) -> Result<Vec<OrcCertificate>> {
        self.read(async |c| orc_table::all_certificates(c).await)
            .await
    }
    /// Replaces the stored certificate of the same reference number, keeping its boat link; returns the rows changed.
    ///
    /// # Errors
    ///
    /// - Returns [`orc_table::InvalidCertificate`] in the chain if the certificate has a zero rating.
    /// - Returns `Err` if the statement fails on every attempt.
    pub async fn update_certificate(&self, cert: &OrcCertificate) -> Result<u64> {
        write!(self, |c| orc_table::update_cert(c, cert).await)
    }
    /// Inserts the race (id 0) or updates it, returning it as stored.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt or a stored ORC option or band name is unknown.
    pub async fn upsert_race(&self, r: &Race) -> Result<Option<Race>> {
        write!(self, |c| race::upsert_race(c, r).await)
    }
    /// Deletes the race with its bracket junctions and class times, in one transaction; returns the race rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt (results still referencing the race, say).
    pub async fn delete_race(&self, id: i64) -> Result<u64> {
        write!(self, |c| {
            race_bracket_junction::delete_race(c, id).await?;
            race_time::delete_race(c, id).await?;
            race::delete_race(c, id).await
        })
    }
    /// Replaces the race's report image with `bytes` stored under `file_name`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt.
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
    /// Returns the bytes of the report image stored as `file_name`, or `None` if there is not exactly one.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn get_race_report_image(&self, file_name: &str) -> Result<Option<Vec<u8>>> {
        self.read(async |c| image::get_image(c, file_name).await)
            .await
    }

    // Race Times ------------------------

    /// Returns every season year with a scheduled race (Pacific calendar), newest first.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails or a stored timestamp does not parse.
    pub async fn all_years(&self) -> Result<Vec<String>> {
        self.read(async |c| race_time::all_years(c, &self.tz).await)
            .await
    }

    // Results ------------------------

    /// Deletes the result `id`, returning the rows deleted.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt.
    pub async fn delete_result(&self, id: i64) -> Result<u64> {
        write!(self, |c| race_results::delete_result(c, id).await)
    }
    /// Returns the results of race `race_id`, in table order.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails or a stored value does not parse (enum name, timestamp).
    pub async fn results_by_race_id(&self, race_id: i64) -> Result<Vec<RaceResult>> {
        self.read(async |c| race_results::results_by_race_id(c, race_id, &self.tz).await)
            .await
    }
    /// Returns the schedule of race `race_id` with its full results, or `None` if the race does not exist.
    ///
    /// # Errors
    ///
    /// - Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
    /// - Returns `Err` if a result's boat does not exist.
    pub async fn schedule_results_by_race_id(
        &self,
        race_id: i64,
    ) -> Result<Option<RaceScheduleResults>> {
        self.read(async |c| race_results::schedule_results_by_race_id(c, race_id, &self.tz).await)
            .await
    }
    /// Returns the results of the schedule's race with their boat, skipper and bracket in that schedule.
    ///
    /// # Errors
    ///
    /// Returns `Err` if a query fails or a stored value does not parse (enum name, timestamp).
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
    /// Inserts the result or updates the one of the same boat and race, returning it as stored.
    ///
    /// The finish time is stored only for a TIME result and the HOC position only for an HOC one.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the statement fails on every attempt or the stored row does not parse.
    pub async fn upsert_result(&self, result: &RaceResult) -> Result<Option<RaceResult>> {
        write!(self, |c| race_results::upsert_result(c, result, &self.tz)
            .await)
    }
    /// Returns the number of results of race `race_id`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the query fails.
    pub async fn result_count(&self, race_id: i64) -> Result<i64> {
        self.read(async |c| race_results::count(c, race_id).await)
            .await
    }
    /// Saves a race with its class times and brackets, replacing the old ones, and returns the schedule read back.
    ///
    /// `None` if the race row did not come back. See [`race::insert_schedule`].
    ///
    /// # Errors
    ///
    /// Returns `Err` if a statement fails on every attempt or a stored value does not parse.
    pub async fn insert_schedule(&self, schedule: &RaceSchedule) -> Result<Option<RaceSchedule>> {
        write!(self, |c| race::insert_schedule(c, schedule, &self.tz).await)
    }
}

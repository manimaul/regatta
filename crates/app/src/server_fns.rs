//! The server functions the UI calls, one per data operation.
//!
//! The bodies (compiled only with the `ssr` feature) are thin adapters over `regatta_service`, the same layer the
//! REST handlers call, so the UI and `/v1/api` cannot drift. Every function is posted as JSON to `/api/<name>` (the
//! prefix is not `/v1/api`, so nothing collides with the REST contract).
//!
//! Admin-only functions check the `regatta_token` cookie (the same bearer value the REST side takes,
//! `base64(json(LoginResponse))`) through `ssr::require_admin`, bootstrap rule included (an empty `auth` table
//! lets everything through); a missing or bad cookie is `ServerFnError::ServerError("unauthorized")`
//! ([`UNAUTHORIZED`]) and the UI shows its logged-out state.
//!
//! Error texts are the REST texts: the `validate_race` / `validate_result` / `validate_boat` messages,
//! [`RACE_HAS_RESULTS_TEXT`], the foreign-key 409 text [`FOREIGN_KEY_TEXT`]; anything else is [`INTERNAL_TEXT`]
//! (the detail goes to the log, never to the client).

use leptos::prelude::*;
use leptos::server_fn::codec::Json;
use regatta_common::*;
use serde::{Deserialize, Serialize};

/// The auth cookie: `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure` behind TLS.
pub const AUTH_COOKIE: &str = "regatta_token";
/// The error text of an admin function called without a valid cookie.
pub const UNAUTHORIZED: &str = "unauthorized";
/// The REST side's 409 text for a delete that something still references.
pub const FOREIGN_KEY_TEXT: &str =
    "This record is still referenced by other records and cannot be deleted.";
/// The error text for any failure the client did not cause; the detail goes to the server log only.
pub const INTERNAL_TEXT: &str = "Internal server error";
/// The error text of [`delete_race`] for a race that still has results.
pub const RACE_HAS_RESULTS_TEXT: &str = "This race has recorded results and cannot be deleted.";
/// The error text [`login`] returns for a wrong password or an unknown user: the status line of the REST 401.
pub const LOGIN_FAILED: &str = "401 Unauthorized";

/// The session the auth cookie proves.
///
/// Holds the `LoginResponse` the REST side would accept as a bearer and the admin's user name, which travels with
/// the login so the server can render "Logged in as …".
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CurrentLogin {
    pub login: LoginResponse,
    pub user_name: String,
}

/// Returns the message of a server function failure as the UI shows it.
///
/// A `ServerError` gives its text verbatim (the REST plain-text bodies); any other error gives its display text.
pub fn error_message(e: &ServerFnError) -> String {
    match e {
        ServerFnError::ServerError(m) => m.clone(),
        other => other.to_string(),
    }
}

#[cfg(feature = "ssr")]
pub mod ssr {
    //! Server-side helpers the function bodies share: the database and clock from context, the auth cookie, the
    //! error mapping.

    use super::{CurrentLogin, AUTH_COOKIE, FOREIGN_KEY_TEXT, INTERNAL_TEXT, UNAUTHORIZED};
    use http::header::{COOKIE, SET_COOKIE};
    use http::request::Parts;
    use http::HeaderValue;
    use jiff::Timestamp;
    use leptos::prelude::*;
    use leptos_axum::ResponseOptions;
    use regatta_service::auth::{self, TokenCheck};
    pub use regatta_service::Db;

    /// `REGATTA_FIXED_NOW` as the server provides it in context (`None`: the wall clock).
    #[derive(Debug, Clone, Copy, PartialEq, Eq)]
    pub struct FixedNow(pub Option<Timestamp>);

    /// Returns the database the server put in context.
    ///
    /// # Errors
    ///
    /// Returns `Err` if no [`Db`] is in context (a render outside the server).
    pub fn db() -> Result<Db, ServerFnError> {
        use_context::<Db>()
            .ok_or_else(|| ServerFnError::ServerError("no database in context".to_string()))
    }

    /// Returns `REGATTA_FIXED_NOW` when the server set it, else the wall clock.
    pub fn now() -> Timestamp {
        regatta_service::config::now(use_context::<FixedNow>().and_then(|f| f.0))
    }

    /// Maps a service failure to the error the client sees.
    ///
    /// A foreign key violation (SQLSTATE 23503) is the client's doing and becomes [`FOREIGN_KEY_TEXT`], the REST 409
    /// text; anything else is logged and answered with [`INTERNAL_TEXT`].
    pub fn internal(e: anyhow::Error) -> ServerFnError {
        let fk = e.chain().any(|cause| {
            cause
                .downcast_ref::<sqlx::Error>()
                .and_then(|e| e.as_database_error())
                .and_then(|db| db.code())
                .is_some_and(|code| code == "23503")
        });
        if fk {
            tracing::warn!("rejected: record is still referenced by other records");
            ServerFnError::ServerError(FOREIGN_KEY_TEXT.to_string())
        } else {
            tracing::error!("server function failed: {e:#}");
            ServerFnError::ServerError(INTERNAL_TEXT.to_string())
        }
    }

    /// The error of an admin function called without a valid cookie ([`UNAUTHORIZED`]).
    pub fn unauthorized() -> ServerFnError {
        ServerFnError::ServerError(UNAUTHORIZED.to_string())
    }

    /// The `regatta_token` cookie of the request, if any.
    pub fn cookie_token() -> Option<String> {
        let parts = use_context::<Parts>()?;
        parts
            .headers
            .get_all(COOKIE)
            .iter()
            .filter_map(|v| v.to_str().ok())
            .flat_map(|header| header.split(';'))
            .filter_map(|pair| {
                let (name, value) = pair.trim().split_once('=')?;
                (name.trim() == AUTH_COOKIE)
                    .then(|| value.trim().to_string())
                    .filter(|v| !v.is_empty())
            })
            .next()
    }

    /// Tells whether the request came in over TLS, directly or behind a proxy that says so in `X-Forwarded-Proto`.
    ///
    /// The auth cookie is then `Secure`.
    fn is_https() -> bool {
        let Some(parts) = use_context::<Parts>() else {
            return false;
        };
        parts
            .headers
            .get("x-forwarded-proto")
            .and_then(|v| v.to_str().ok())
            .map(|v| v.trim().eq_ignore_ascii_case("https"))
            .unwrap_or_else(|| parts.uri.scheme_str() == Some("https"))
    }

    fn set_cookie_header(value: String) {
        if let Some(response) = use_context::<ResponseOptions>() {
            if let Ok(header) = HeaderValue::from_str(&value) {
                response.append_header(SET_COOKIE, header);
            }
        }
    }

    /// Sets the auth cookie to `token`, expiring with the login.
    pub fn set_auth_cookie(token: &str, expires: Timestamp) {
        let expires = expires.strftime("%a, %d %b %Y %H:%M:%S GMT");
        let mut cookie =
            format!("{AUTH_COOKIE}={token}; Path=/; Expires={expires}; HttpOnly; SameSite=Lax");
        if is_https() {
            cookie.push_str("; Secure");
        }
        set_cookie_header(cookie);
    }

    /// Clears the auth cookie.
    pub fn clear_auth_cookie() {
        let mut cookie = format!("{AUTH_COOKIE}=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        if is_https() {
            cookie.push_str("; Secure");
        }
        set_cookie_header(cookie);
    }

    /// Checks that the request may make admin changes.
    ///
    /// An empty `auth` table lets everything through (bootstrap mode); afterwards the auth cookie must hold a token
    /// that `regatta_service::auth::validate_admin_token` accepts.
    ///
    /// # Errors
    ///
    /// - [`unauthorized`] if the cookie is missing, unparseable or rejected.
    /// - [`INTERNAL_TEXT`] if the admin check query fails.
    pub async fn require_admin(db: &Db) -> Result<(), ServerFnError> {
        if !db.admin_exists().await.map_err(internal)? {
            return Ok(());
        }
        let token = cookie_token().ok_or_else(unauthorized)?;
        match auth::validate_admin_token(db, &token).await {
            Ok(TokenCheck::Admin) => Ok(()),
            Ok(TokenCheck::Rejected) | Err(_) => Err(unauthorized()),
        }
    }

    /// Returns the validated session of the auth cookie, with its raw token.
    ///
    /// `None` without a cookie, with an unparseable or rejected token, or while no admin exists (nothing to be
    /// logged in as).
    ///
    /// # Errors
    ///
    /// Returns `Err` with [`INTERNAL_TEXT`] if the auth record query fails.
    pub async fn session(db: &Db) -> Result<Option<(String, CurrentLogin)>, ServerFnError> {
        let Some(token) = cookie_token() else {
            return Ok(None);
        };
        let Ok(login) = auth::parse_token(&token) else {
            return Ok(None);
        };
        let Some(record) = db.get_auth(login.id).await.map_err(internal)? else {
            return Ok(None);
        };
        if !auth::validate_hash(&login, &record) {
            return Ok(None);
        }
        Ok(Some((
            token,
            CurrentLogin {
                login,
                user_name: record.user_name,
            },
        )))
    }

    /// Encodes the bearer token for a login response: `base64(json(LoginResponse))`.
    ///
    /// # Errors
    ///
    /// Returns `Err` with [`INTERNAL_TEXT`] if the response cannot be serialized.
    pub fn token_for(response: &regatta_common::LoginResponse) -> Result<String, ServerFnError> {
        use base64::prelude::{Engine, BASE64_STANDARD};
        let json = serde_json::to_vec(response).map_err(|e| internal(e.into()))?;
        Ok(BASE64_STANDARD.encode(json))
    }
}

// ---- auth --------------------------------------------------------------------------------------

/// Logs in: returns the `LoginResponse` and sets the auth cookie on the response.
///
/// # Errors
///
/// - [`LOGIN_FAILED`] for a wrong password or an unknown user.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = LoginCall, prefix = "/api", endpoint = "login", input = Json)]
pub async fn login(login: Login) -> Result<LoginResponse, ServerFnError> {
    let db = ssr::db()?;
    match regatta_service::auth::create_login_response(&db, &login, ssr::now())
        .await
        .map_err(ssr::internal)?
    {
        Some(response) => {
            ssr::set_auth_cookie(&ssr::token_for(&response)?, response.expires);
            Ok(response)
        }
        None => Err(ServerFnError::ServerError(LOGIN_FAILED.to_string())),
    }
}

/// Clears the auth cookie.
///
/// Never returns `Err`.
#[server(name = LogoutCall, prefix = "/api", endpoint = "logout", input = Json)]
pub async fn logout() -> Result<(), ServerFnError> {
    ssr::clear_auth_cookie();
    Ok(())
}

/// Creates or replaces an admin, open to anyone while the `auth` table is empty.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails or stores nothing.
#[server(name = CreateAdminCall, prefix = "/api", endpoint = "create_admin", input = Json)]
pub async fn create_admin(record: AuthRecord) -> Result<AuthRecord, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.save_auth(&record)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError(INTERNAL_TEXT.to_string()))
}

/// Returns the session the request's auth cookie proves, validated like a REST bearer; `None` when logged out.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = CurrentLoginCall, prefix = "/api", endpoint = "current_login", input = Json)]
pub async fn current_login() -> Result<Option<CurrentLogin>, ServerFnError> {
    let db = ssr::db()?;
    Ok(ssr::session(&db).await?.map(|(_, current)| current))
}

/// Returns the raw bearer token of the session, `None` when logged out.
///
/// Backs "Copy auth token": the `database/*.py` scripts take it as their bearer.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = AuthTokenCall, prefix = "/api", endpoint = "auth_token", input = Json)]
pub async fn auth_token() -> Result<Option<String>, ServerFnError> {
    let db = ssr::db()?;
    Ok(ssr::session(&db).await?.map(|(token, _)| token))
}

// ---- series ------------------------------------------------------------------------------------

/// Returns every series.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = AllSeriesCall, prefix = "/api", endpoint = "all_series", input = Json)]
pub async fn all_series() -> Result<Vec<Series>, ServerFnError> {
    ssr::db()?.all_series().await.map_err(ssr::internal)
}

/// Inserts or updates the series and returns them as stored.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostSeriesCall, prefix = "/api", endpoint = "post_series", input = Json)]
pub async fn post_series(series: Vec<Series>) -> Result<Vec<Series>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_series(&series).await.map_err(ssr::internal)
}

/// Deletes a series.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`FOREIGN_KEY_TEXT`] if other records still reference it.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeleteSeriesCall, prefix = "/api", endpoint = "delete_series", input = Json)]
pub async fn delete_series(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_series(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// Returns the standings of a series for a year, `None` for an unknown series.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = SeriesStandingsCall, prefix = "/api", endpoint = "series_standings", input = Json)]
pub async fn series_standings(
    id: i64,
    year: i32,
) -> Result<Option<StandingsSeries>, ServerFnError> {
    let db = ssr::db()?;
    regatta_service::reporter::get_standings_report(&db, id, year, ssr::now())
        .await
        .map_err(ssr::internal)
}

// ---- classes and brackets ----------------------------------------------------------------------

/// Returns every class with its brackets.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = AllClassesCall, prefix = "/api", endpoint = "all_classes", input = Json)]
pub async fn all_classes() -> Result<Vec<RaceClassBrackets>, ServerFnError> {
    ssr::db()?.all_classes().await.map_err(ssr::internal)
}

/// Saves a class and its brackets; brackets missing from the list are deleted.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostClassCall, prefix = "/api", endpoint = "post_class", input = Json)]
pub async fn post_class(
    class: RaceClassBrackets,
) -> Result<Option<RaceClassBrackets>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_class_brackets(&class)
        .await
        .map_err(ssr::internal)
}

/// Inserts or updates the classes and returns each with its brackets, sorted by `sort`.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostClassListCall, prefix = "/api", endpoint = "post_class_list", input = Json)]
pub async fn post_class_list(
    classes: Vec<RaceClass>,
) -> Result<Vec<RaceClassBrackets>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_class_list(&classes).await.map_err(ssr::internal)
}

/// Deletes a class.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`FOREIGN_KEY_TEXT`] if other records still reference it.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeleteClassCall, prefix = "/api", endpoint = "delete_class", input = Json)]
pub async fn delete_class(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_class(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// Deletes a bracket.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`FOREIGN_KEY_TEXT`] if other records still reference it.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeleteBracketCall, prefix = "/api", endpoint = "delete_bracket", input = Json)]
pub async fn delete_bracket(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_bracket(id).await.map_err(ssr::internal)?;
    Ok(())
}

// ---- races -------------------------------------------------------------------------------------

/// Returns every season year with a scheduled race (Pacific calendar), newest first.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = YearsCall, prefix = "/api", endpoint = "years", input = Json)]
pub async fn years() -> Result<Vec<String>, ServerFnError> {
    ssr::db()?.all_years().await.map_err(ssr::internal)
}

/// Returns the race schedules of a year.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = AllRacesCall, prefix = "/api", endpoint = "all_races", input = Json)]
pub async fn all_races(year: i32) -> Result<Vec<RaceSchedule>, ServerFnError> {
    ssr::db()?.all_races(year).await.map_err(ssr::internal)
}

/// Returns one race schedule, `None` for an unknown race.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = RaceScheduleCall, prefix = "/api", endpoint = "race_schedule", input = Json)]
pub async fn race_schedule(id: i64) -> Result<Option<RaceSchedule>, ServerFnError> {
    ssr::db()?
        .find_race_schedule(id)
        .await
        .map_err(ssr::internal)
}

/// Validates and saves a race schedule, returning it as stored.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - The `regatta_scoring::validate_race` message if the race is invalid.
/// - `"Could not save the race"` if nothing was stored.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostRaceScheduleCall, prefix = "/api", endpoint = "post_race_schedule", input = Json)]
pub async fn post_race_schedule(schedule: RaceSchedule) -> Result<RaceSchedule, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    if let Some(message) = regatta_scoring::validate_race(&schedule.race) {
        return Err(ServerFnError::ServerError(message));
    }
    db.insert_schedule(&schedule)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError("Could not save the race".to_string()))
}

/// Deletes a race that has no recorded results.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`RACE_HAS_RESULTS_TEXT`] while results are recorded for the race.
/// - [`FOREIGN_KEY_TEXT`] if other records still reference it.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeleteRaceCall, prefix = "/api", endpoint = "delete_race", input = Json)]
pub async fn delete_race(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    if db.result_count(id).await.map_err(ssr::internal)? > 0 {
        return Err(ServerFnError::ServerError(
            RACE_HAS_RESULTS_TEXT.to_string(),
        ));
    }
    db.delete_race(id).await.map_err(ssr::internal)?;
    Ok(())
}

// ---- results -----------------------------------------------------------------------------------

/// Returns a race schedule with its results, `None` for an unknown race.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = ScheduleResultsCall, prefix = "/api", endpoint = "schedule_results", input = Json)]
pub async fn schedule_results(race_id: i64) -> Result<Option<RaceScheduleResults>, ServerFnError> {
    ssr::db()?
        .schedule_results_by_race_id(race_id)
        .await
        .map_err(ssr::internal)
}

/// Validates and inserts or updates a race result, returning it as stored.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - The `regatta_scoring::validate_result` message if the result is invalid.
/// - `"Could not save the result"` if nothing was stored.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostResultCall, prefix = "/api", endpoint = "post_result", input = Json)]
pub async fn post_result(result: RaceResult) -> Result<RaceResult, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    if let Some(message) = regatta_scoring::validate_result(&result) {
        return Err(ServerFnError::ServerError(message));
    }
    db.upsert_result(&result)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError("Could not save the result".to_string()))
}

/// Deletes a race result.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeleteResultCall, prefix = "/api", endpoint = "delete_result", input = Json)]
pub async fn delete_result(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_result(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// Returns the scored report of a race, `None` for an unknown race.
///
/// # Errors
///
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = ReportCall, prefix = "/api", endpoint = "report", input = Json)]
pub async fn report(race_id: i64) -> Result<Option<RaceReport>, ServerFnError> {
    let db = ssr::db()?;
    regatta_service::reporter::get_report(&db, race_id)
        .await
        .map_err(ssr::internal)
}

// ---- people and boats (admin, like the REST routes) --------------------------------------------

/// Returns every person; admin only, like the REST route.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = AllPeopleCall, prefix = "/api", endpoint = "all_people", input = Json)]
pub async fn all_people() -> Result<Vec<Person>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.all_people().await.map_err(ssr::internal)
}

/// Inserts or updates a person, returning it as stored.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - `"Could not save the skipper"` if nothing was stored.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostPersonCall, prefix = "/api", endpoint = "post_person", input = Json)]
pub async fn post_person(person: Person) -> Result<Person, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_person(&person)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError("Could not save the skipper".to_string()))
}

/// Deletes a person.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`FOREIGN_KEY_TEXT`] if other records still reference it.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeletePersonCall, prefix = "/api", endpoint = "delete_person", input = Json)]
pub async fn delete_person(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_person(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// Returns every boat with its skipper; admin only, like the REST route.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = AllBoatsCall, prefix = "/api", endpoint = "all_boats", input = Json)]
pub async fn all_boats() -> Result<Vec<BoatSkipper>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.all_boats().await.map_err(ssr::internal)
}

/// Validates and inserts or updates a boat, returning it as stored.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - The `regatta_scoring::validate_boat` message if the boat is invalid.
/// - `"Could not save the boat"` if nothing was stored.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = PostBoatCall, prefix = "/api", endpoint = "post_boat", input = Json)]
pub async fn post_boat(boat: Boat) -> Result<Boat, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    if let Some(message) = regatta_scoring::validate_boat(&boat) {
        return Err(ServerFnError::ServerError(message));
    }
    db.upsert_boat(&boat)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError("Could not save the boat".to_string()))
}

/// Deletes a boat.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - [`FOREIGN_KEY_TEXT`] if other records still reference it.
/// - [`INTERNAL_TEXT`] if the service fails.
#[server(name = DeleteBoatCall, prefix = "/api", endpoint = "delete_boat", input = Json)]
pub async fn delete_boat(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_boat(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// Looks up an active ORC certificate on data.orc.org, `None` when ORC has no such active certificate.
///
/// The server makes the call because data.orc.org sends no CORS headers, so the browser cannot.
///
/// # Errors
///
/// - [`UNAUTHORIZED`] if the request is not an admin's (see `ssr::require_admin`).
/// - `"a reference number is required"` if `ref_no` is blank.
/// - [`INTERNAL_TEXT`] if the lookup fails.
#[server(name = OrcCertCall, prefix = "/api", endpoint = "orc_cert", input = Json)]
pub async fn orc_cert(ref_no: String) -> Result<Option<OrcCertificate>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    let ref_no = ref_no.trim();
    if ref_no.is_empty() {
        return Err(ServerFnError::ServerError(
            "a reference number is required".to_string(),
        ));
    }
    regatta_service::orc::lookup(ref_no)
        .await
        .map_err(ssr::internal)
}

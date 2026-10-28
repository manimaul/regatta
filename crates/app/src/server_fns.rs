//! = `utils/Api.kt`: one server function per `Api` call. The bodies (compiled only with the `ssr`
//! feature) are thin adapters over `regatta_service`, the same layer the REST handlers call, so the
//! UI and `/v1/api` cannot drift. Every function is posted as JSON to `/api/<name>` (the prefix is
//! not `/v1/api`, so nothing collides with the REST contract).
//!
//! Admin-only functions check the `regatta_token` cookie (the same bearer value the REST side
//! takes, `base64(json(LoginResponse))`) with `validate_admin_token`, bootstrap rule included (an
//! empty `auth` table lets everything through); a missing or bad cookie is
//! `ServerFnError::ServerError("unauthorized")` and the UI shows its logged-out state.
//!
//! Error texts are the REST texts: `validateRace` / `validateResult` messages, "This race has
//! recorded results and cannot be deleted.", the foreign-key 409 text; anything else is
//! "Internal server error" (the detail goes to the log, never to the client), like `StatusPages.kt`.

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
pub const INTERNAL_TEXT: &str = "Internal server error";
pub const RACE_HAS_RESULTS_TEXT: &str = "This race has recorded results and cannot be deleted.";
/// What `login` answers for a wrong password or an unknown user (= the REST 401's status line,
/// which `LoginViewModel` showed as `"${status} ${statusText}"`).
pub const LOGIN_FAILED: &str = "401 Unauthorized";

/// The session the cookie proves: the `LoginResponse` the REST side would accept as a bearer and
/// the admin's user name (Kotlin kept it in `localStorage["username"]`; the server renders
/// "Logged in as …", so it travels with the login).
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CurrentLogin {
    pub login: LoginResponse,
    pub user_name: String,
}

/// The message of a server function failure as the UI shows it: the `ServerError` text verbatim
/// (the REST plain-text bodies), anything else as it displays.
pub fn error_message(e: &ServerFnError) -> String {
    match e {
        ServerFnError::ServerError(m) => m.clone(),
        other => other.to_string(),
    }
}

#[cfg(feature = "ssr")]
pub mod ssr {
    //! The server-side helpers the function bodies share: the database and clock from context,
    //! the auth cookie, the error mapping.

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

    pub fn db() -> Result<Db, ServerFnError> {
        use_context::<Db>()
            .ok_or_else(|| ServerFnError::ServerError("no database in context".to_string()))
    }

    /// `FixedClock.now()`.
    pub fn now() -> Timestamp {
        regatta_service::config::now(use_context::<FixedNow>().and_then(|f| f.0))
    }

    /// = `StatusPages.kt`: a foreign key violation is the client's doing (409 text), anything else
    /// is logged and answered generically.
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

    /// Whether the request came in over TLS (directly or behind a proxy that says so): the
    /// cookie is then `Secure`.
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

    /// The bootstrap rule and the token check of `Token.validateAdminToken`, for the cookie: an
    /// empty `auth` table lets everything through; afterwards the cookie must hold a valid token.
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

    /// The validated session of the cookie, with its raw token: `None` without a cookie, with an
    /// unparseable or rejected token, or while no admin exists (nothing to be logged in as).
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

    /// The bearer token for a login response: `base64(json)`, what `localStorage` held.
    pub fn token_for(response: &regatta_common::LoginResponse) -> Result<String, ServerFnError> {
        use base64::prelude::{Engine, BASE64_STANDARD};
        let json = serde_json::to_vec(response).map_err(|e| internal(e.into()))?;
        Ok(BASE64_STANDARD.encode(json))
    }
}

// ---- auth (`Auth.kt` + `LoginViewModel.kt`) ------------------------------------------------

/// `POST login`: the `LoginResponse`, and the auth cookie set on the response. A wrong password
/// or unknown user is [`LOGIN_FAILED`].
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
#[server(name = LogoutCall, prefix = "/api", endpoint = "logout", input = Json)]
pub async fn logout() -> Result<(), ServerFnError> {
    ssr::clear_auth_cookie();
    Ok(())
}

/// `POST auth`: creates or replaces an admin (open while the `auth` table is empty).
#[server(name = CreateAdminCall, prefix = "/api", endpoint = "create_admin", input = Json)]
pub async fn create_admin(record: AuthRecord) -> Result<AuthRecord, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.save_auth(&record)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError(INTERNAL_TEXT.to_string()))
}

/// The session the request's cookie proves, validated like a REST bearer; `None` when logged out.
#[server(name = CurrentLoginCall, prefix = "/api", endpoint = "current_login", input = Json)]
pub async fn current_login() -> Result<Option<CurrentLogin>, ServerFnError> {
    let db = ssr::db()?;
    Ok(ssr::session(&db).await?.map(|(_, current)| current))
}

/// The raw bearer token of the session ("Copy auth token"; the Python scripts take it as the
/// bearer), `None` when logged out.
#[server(name = AuthTokenCall, prefix = "/api", endpoint = "auth_token", input = Json)]
pub async fn auth_token() -> Result<Option<String>, ServerFnError> {
    let db = ssr::db()?;
    Ok(ssr::session(&db).await?.map(|(token, _)| token))
}

// ---- series ------------------------------------------------------------------------------------

#[server(name = AllSeriesCall, prefix = "/api", endpoint = "all_series", input = Json)]
pub async fn all_series() -> Result<Vec<Series>, ServerFnError> {
    ssr::db()?.all_series().await.map_err(ssr::internal)
}

#[server(name = PostSeriesCall, prefix = "/api", endpoint = "post_series", input = Json)]
pub async fn post_series(series: Vec<Series>) -> Result<Vec<Series>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_series(&series).await.map_err(ssr::internal)
}

#[server(name = DeleteSeriesCall, prefix = "/api", endpoint = "delete_series", input = Json)]
pub async fn delete_series(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_series(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// `GET series/standings?id=&year=`: `None` for an unknown series.
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

#[server(name = AllClassesCall, prefix = "/api", endpoint = "all_classes", input = Json)]
pub async fn all_classes() -> Result<Vec<RaceClassBrackets>, ServerFnError> {
    ssr::db()?.all_classes().await.map_err(ssr::internal)
}

/// `POST raceClass`: the class and its brackets; brackets missing from the list are deleted.
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

#[server(name = PostClassListCall, prefix = "/api", endpoint = "post_class_list", input = Json)]
pub async fn post_class_list(
    classes: Vec<RaceClass>,
) -> Result<Vec<RaceClassBrackets>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_class_list(&classes).await.map_err(ssr::internal)
}

#[server(name = DeleteClassCall, prefix = "/api", endpoint = "delete_class", input = Json)]
pub async fn delete_class(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_class(id).await.map_err(ssr::internal)?;
    Ok(())
}

#[server(name = DeleteBracketCall, prefix = "/api", endpoint = "delete_bracket", input = Json)]
pub async fn delete_bracket(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_bracket(id).await.map_err(ssr::internal)?;
    Ok(())
}

// ---- races -------------------------------------------------------------------------------------

#[server(name = YearsCall, prefix = "/api", endpoint = "years", input = Json)]
pub async fn years() -> Result<Vec<String>, ServerFnError> {
    ssr::db()?.all_years().await.map_err(ssr::internal)
}

#[server(name = AllRacesCall, prefix = "/api", endpoint = "all_races", input = Json)]
pub async fn all_races(year: i32) -> Result<Vec<RaceSchedule>, ServerFnError> {
    ssr::db()?.all_races(year).await.map_err(ssr::internal)
}

#[server(name = RaceScheduleCall, prefix = "/api", endpoint = "race_schedule", input = Json)]
pub async fn race_schedule(id: i64) -> Result<Option<RaceSchedule>, ServerFnError> {
    ssr::db()?
        .find_race_schedule(id)
        .await
        .map_err(ssr::internal)
}

/// `POST raceSchedule`: the `validateRace` message as the error text, else the schedule as stored.
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

/// `DELETE race?id=`: refused with [`RACE_HAS_RESULTS_TEXT`] while results are recorded.
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

/// `GET scheduleResults?raceId=`: `None` for an unknown race.
#[server(name = ScheduleResultsCall, prefix = "/api", endpoint = "schedule_results", input = Json)]
pub async fn schedule_results(race_id: i64) -> Result<Option<RaceScheduleResults>, ServerFnError> {
    ssr::db()?
        .schedule_results_by_race_id(race_id)
        .await
        .map_err(ssr::internal)
}

/// `POST results`: the `validateResult` message as the error text, else the upserted result.
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

#[server(name = DeleteResultCall, prefix = "/api", endpoint = "delete_result", input = Json)]
pub async fn delete_result(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_result(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// `GET report?raceId=`: the race report, `None` for an unknown race.
#[server(name = ReportCall, prefix = "/api", endpoint = "report", input = Json)]
pub async fn report(race_id: i64) -> Result<Option<RaceReport>, ServerFnError> {
    let db = ssr::db()?;
    regatta_service::reporter::get_report(&db, race_id)
        .await
        .map_err(ssr::internal)
}

// ---- people and boats (admin, like the REST routes) --------------------------------------------

#[server(name = AllPeopleCall, prefix = "/api", endpoint = "all_people", input = Json)]
pub async fn all_people() -> Result<Vec<Person>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.all_people().await.map_err(ssr::internal)
}

#[server(name = PostPersonCall, prefix = "/api", endpoint = "post_person", input = Json)]
pub async fn post_person(person: Person) -> Result<Person, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.upsert_person(&person)
        .await
        .map_err(ssr::internal)?
        .ok_or_else(|| ServerFnError::ServerError("Could not save the skipper".to_string()))
}

#[server(name = DeletePersonCall, prefix = "/api", endpoint = "delete_person", input = Json)]
pub async fn delete_person(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_person(id).await.map_err(ssr::internal)?;
    Ok(())
}

#[server(name = AllBoatsCall, prefix = "/api", endpoint = "all_boats", input = Json)]
pub async fn all_boats() -> Result<Vec<BoatSkipper>, ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.all_boats().await.map_err(ssr::internal)
}

/// `POST boat`: the `validate_boat` message as the error text, else the upserted boat.
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

#[server(name = DeleteBoatCall, prefix = "/api", endpoint = "delete_boat", input = Json)]
pub async fn delete_boat(id: i64) -> Result<(), ServerFnError> {
    let db = ssr::db()?;
    ssr::require_admin(&db).await?;
    db.delete_boat(id).await.map_err(ssr::internal)?;
    Ok(())
}

/// `GET orcCert?refNo=`: the certificate looked up on data.orc.org by the server (the browser
/// cannot: no CORS there); `None` when ORC has no such active certificate.
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

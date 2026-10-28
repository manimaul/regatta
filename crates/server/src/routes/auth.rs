//! The REST login and admin-record routes: `POST login` and `POST auth`.

use crate::error::AppResult;
use crate::extract::{Admin, Json};
use crate::respond::{empty, json};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::{AuthRecord, Login};
use regatta_service::auth;

/// Answers `POST login` with a `LoginResponse`, or a bare 401 for a wrong password or an unknown user.
///
/// The 401 carries no `WWW-Authenticate` challenge, unlike the admin-route refusal. `expires` is 30 days from the
/// fixed clock when one is configured, else from the wall clock.
pub async fn login(State(state): State<AppState>, Json(login): Json<Login>) -> AppResult {
    match auth::create_login_response(&state.db, &login, state.now()).await? {
        Some(response) => Ok(json(&response)),
        None => Ok(empty(StatusCode::UNAUTHORIZED)),
    }
}

/// Creates or replaces an admin record for `POST auth`; open to any bearer value while the `auth` table is empty.
pub async fn post_auth(
    _admin: Admin,
    State(state): State<AppState>,
    Json(record): Json<AuthRecord>,
) -> AppResult {
    match state.db.save_auth(&record).await? {
        Some(saved) => Ok(json(&saved)),
        None => Ok(empty(StatusCode::INTERNAL_SERVER_ERROR)),
    }
}

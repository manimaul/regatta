use crate::error::AppResult;
use crate::extract::{Admin, Json};
use crate::respond::{empty, json};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::{AuthRecord, Login};
use regatta_service::auth;

/// `POST login`: the `LoginResponse`, or a bare 401 (no challenge header) for a wrong password
/// or an unknown user. `expires` is the fixed clock + 30 days when one is configured.
pub async fn login(State(state): State<AppState>, Json(login): Json<Login>) -> AppResult {
    match auth::create_login_response(&state.db, &login, state.now()).await? {
        Some(response) => Ok(json(&response)),
        None => Ok(empty(StatusCode::UNAUTHORIZED)),
    }
}

/// `POST auth`: creates or replaces an admin record (open while the `auth` table is empty).
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

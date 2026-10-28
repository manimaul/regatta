use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json, text};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::Boat;
use regatta_scoring::validate_boat;

pub async fn all_boats(_admin: Admin, State(state): State<AppState>) -> AppResult {
    Ok(json(&state.db.all_boats().await?))
}

/// `GET boat?id=` or `?person_id=`: an unknown `id` falls through to the `person_id` branch
/// (400 when that is absent), an unknown `person_id` is 204, neither parameter is 400.
pub async fn get_boat(_admin: Admin, State(state): State<AppState>, params: Params) -> AppResult {
    if let Some(id) = params.long("id")? {
        if let Some(boat) = state.db.find_boat(id).await? {
            return Ok(json(&boat));
        }
    }
    match params.long("person_id")? {
        Some(person_id) => match state.db.find_boat_for_person(person_id).await? {
            Some(boat) => Ok(json(&boat)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::BAD_REQUEST)),
    }
}

/// `POST boat`: `validate_boat` text as a 400, else the upserted boat.
pub async fn post_boat(
    _admin: Admin,
    State(state): State<AppState>,
    Json(boat): Json<Boat>,
) -> AppResult {
    if let Some(message) = validate_boat(&boat) {
        return Ok(text(StatusCode::BAD_REQUEST, &message));
    }
    match state.db.upsert_boat(&boat).await? {
        Some(saved) => Ok(json(&saved)),
        None => Ok(empty(StatusCode::INTERNAL_SERVER_ERROR)),
    }
}

pub async fn delete_boat(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => {
            state.db.delete_boat(id).await?;
            Ok(empty(StatusCode::OK))
        }
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

//! The REST person routes: `allSkippers`, `find/skipper` and `GET`/`POST`/`DELETE skipper`.

use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::Person;

pub async fn all_skippers(_admin: Admin, State(state): State<AppState>) -> AppResult {
    Ok(json(&state.db.all_people().await?))
}

pub async fn get_skipper(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => match state.db.find_person(id).await? {
            Some(person) => Ok(json(&person)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn find_skipper(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.get("name") {
        Some(name) => Ok(json(&state.db.find_person_by_name(name).await?)),
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn post_skipper(
    _admin: Admin,
    State(state): State<AppState>,
    Json(person): Json<Person>,
) -> AppResult {
    match state.db.upsert_person(&person).await? {
        Some(saved) => Ok(json(&saved)),
        None => Ok(empty(StatusCode::CONFLICT)),
    }
}

pub async fn delete_skipper(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => {
            state.db.delete_person(id).await?;
            Ok(empty(StatusCode::OK))
        }
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

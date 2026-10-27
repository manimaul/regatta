use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::{Bracket, RaceClass, RaceClassBrackets};

pub async fn get_class(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("id")? {
        Some(id) => match state.db.find_class(id).await? {
            Some(class) => Ok(json(&class)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn all_classes(State(state): State<AppState>) -> AppResult {
    Ok(json(&state.db.all_classes().await?))
}

/// `POST raceClass`: upserts the class and its brackets; brackets missing from the list are deleted.
pub async fn post_class(
    _admin: Admin,
    State(state): State<AppState>,
    Json(body): Json<RaceClassBrackets>,
) -> AppResult {
    match state.db.upsert_class_brackets(&body).await? {
        Some(rcb) => Ok(json(&rcb)),
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn post_class_list(
    _admin: Admin,
    State(state): State<AppState>,
    Json(body): Json<Vec<RaceClass>>,
) -> AppResult {
    Ok(json(&state.db.upsert_class_list(&body).await?))
}

pub async fn delete_class(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => {
            state.db.delete_class(id).await?;
            Ok(empty(StatusCode::OK))
        }
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn get_bracket(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("id")? {
        Some(id) => match state.db.find_bracket(id).await? {
            Some(bracket) => Ok(json(&bracket)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn post_bracket(
    _admin: Admin,
    State(state): State<AppState>,
    Json(body): Json<Bracket>,
) -> AppResult {
    match state.db.upsert_bracket(&body).await? {
        Some(bracket) => Ok(json(&bracket)),
        None => Ok(empty(StatusCode::INTERNAL_SERVER_ERROR)),
    }
}

pub async fn delete_bracket(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => {
            state.db.delete_bracket(id).await?;
            Ok(empty(StatusCode::OK))
        }
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

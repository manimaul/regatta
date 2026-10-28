//! The REST result routes: `results`, `scheduleResults`, `resultCount` and the race `report`.

use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json, text};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::RaceResult;
use regatta_scoring::validate_result;
use regatta_service::reporter;

/// Answers `GET results?raceId=` with the race's results: `200 []` for an unknown race, 204 without the parameter.
pub async fn get_results(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("raceId")? {
        Some(race_id) => Ok(json(&state.db.results_by_race_id(race_id).await?)),
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

/// Upserts the result for `POST results`, or answers the [`validate_result`] message as a 400 text.
pub async fn post_result(
    _admin: Admin,
    State(state): State<AppState>,
    Json(result): Json<RaceResult>,
) -> AppResult {
    if let Some(message) = validate_result(&result) {
        return Ok(text(StatusCode::BAD_REQUEST, &message));
    }
    match state.db.upsert_result(&result).await? {
        Some(saved) => Ok(json(&saved)),
        None => Ok(empty(StatusCode::CONFLICT)),
    }
}

pub async fn delete_result(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => {
            state.db.delete_result(id).await?;
            Ok(empty(StatusCode::OK))
        }
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn schedule_results(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("raceId")? {
        Some(race_id) => match state.db.schedule_results_by_race_id(race_id).await? {
            Some(results) => Ok(json(&results)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

/// Answers `GET resultCount?raceId=` with the count as JSON (`0` for an unknown race), 400 without the parameter.
pub async fn result_count(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("raceId")? {
        Some(race_id) => Ok(json(&state.db.result_count(race_id).await?)),
        None => Ok(empty(StatusCode::BAD_REQUEST)),
    }
}

/// Answers `GET report?raceId=` with the race report, 204 for an unknown race or no parameter.
pub async fn report(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("raceId")? {
        Some(race_id) => match reporter::get_report(&state.db, race_id).await? {
            Some(report) => Ok(json(&report)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

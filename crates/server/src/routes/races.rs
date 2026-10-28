//! The REST race routes: `years`, `allRaces`, `race` and `raceSchedule`.

use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json, text};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::{Race, RaceSchedule};
use regatta_scoring::validate_race;

pub async fn years(State(state): State<AppState>) -> AppResult {
    Ok(json(&state.db.all_years().await?))
}

/// Answers `GET allRaces?year=` with the year's races; a missing or non-numeric year is a 400, not a 500.
pub async fn all_races(State(state): State<AppState>, params: Params) -> AppResult {
    match params.number_or_none::<i32>("year") {
        Some(year) => Ok(json(&state.db.all_races(year).await?)),
        None => Ok(empty(StatusCode::BAD_REQUEST)),
    }
}

pub async fn get_race(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("id")? {
        Some(id) => match state.db.find_race(id).await? {
            Some(race) => Ok(json(&race)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

/// Upserts the race for `POST race`, or answers the [`validate_race`] message as a 400 text.
pub async fn post_race(
    _admin: Admin,
    State(state): State<AppState>,
    Json(race): Json<Race>,
) -> AppResult {
    if let Some(message) = validate_race(&race) {
        return Ok(text(StatusCode::BAD_REQUEST, &message));
    }
    match state.db.upsert_race(&race).await? {
        Some(saved) => Ok(json(&saved)),
        None => Ok(empty(StatusCode::CONFLICT)),
    }
}

/// Deletes the race for `DELETE race?id=`, refusing a race with results with a 409 text.
///
/// A missing or non-numeric id is a 204, not a 500, as the contract cassettes record.
pub async fn delete_race(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    let Some(id) = params.number_or_none::<i64>("id") else {
        return Ok(empty(StatusCode::NO_CONTENT));
    };
    if state.db.result_count(id).await? > 0 {
        return Ok(text(
            StatusCode::CONFLICT,
            "This race has recorded results and cannot be deleted.",
        ));
    }
    state.db.delete_race(id).await?;
    Ok(empty(StatusCode::OK))
}

pub async fn get_race_schedule(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("id")? {
        Some(id) => match state.db.find_race_schedule(id).await? {
            Some(schedule) => Ok(json(&schedule)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

/// Stores the race schedule for `POST raceSchedule`, or answers the [`validate_race`] message as a 400 text.
pub async fn post_race_schedule(
    _admin: Admin,
    State(state): State<AppState>,
    Json(schedule): Json<RaceSchedule>,
) -> AppResult {
    if let Some(message) = validate_race(&schedule.race) {
        return Ok(text(StatusCode::BAD_REQUEST, &message));
    }
    match state.db.insert_schedule(&schedule).await? {
        Some(saved) => Ok(json(&saved)),
        None => Ok(empty(StatusCode::BAD_REQUEST)),
    }
}

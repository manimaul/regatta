//! The REST series routes: `series`, `allSeries`, `find/series` and `series/standings`.

use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::Series;
use regatta_service::reporter;

/// Answers `GET series?id=` with the series, 204 without an id or for an unknown one.
pub async fn get_series(State(state): State<AppState>, params: Params) -> AppResult {
    match params.long("id")? {
        Some(id) => match state.db.find_series(id).await? {
            Some(series) => Ok(json(&series)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

pub async fn all_series(State(state): State<AppState>) -> AppResult {
    Ok(json(&state.db.all_series().await?))
}

/// Answers `GET find/series?name=` with the matching series: `200 []` for no match, 204 without a name.
pub async fn find_series(State(state): State<AppState>, params: Params) -> AppResult {
    match params.get("name") {
        Some(name) => Ok(json(&state.db.find_series_by_name(name).await?)),
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

/// Answers `GET series/standings?id=&year=` with the series standings, 204 without either or for an unknown series.
pub async fn standings(State(state): State<AppState>, params: Params) -> AppResult {
    if let Some(id) = params.long("id")? {
        if let Some(year) = params.int("year")? {
            if let Some(standings) =
                reporter::get_standings_report(&state.db, id, year, state.now()).await?
            {
                return Ok(json(&standings));
            }
        }
    }
    Ok(empty(StatusCode::NO_CONTENT))
}

pub async fn post_series(
    _admin: Admin,
    State(state): State<AppState>,
    Json(series): Json<Vec<Series>>,
) -> AppResult {
    Ok(json(&state.db.upsert_series(&series).await?))
}

/// Deletes the series for `DELETE series?id=`: 200 whether or not a row existed, 204 without an id.
pub async fn delete_series(
    _admin: Admin,
    State(state): State<AppState>,
    params: Params,
) -> AppResult {
    match params.long("id")? {
        Some(id) => {
            state.db.delete_series(id).await?;
            Ok(empty(StatusCode::OK))
        }
        None => Ok(empty(StatusCode::NO_CONTENT)),
    }
}

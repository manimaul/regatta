//! The REST race report image routes: the multipart upload and the download by file name.
//!
//! `POST image?raceId=` stores every `image/*` file part of a multipart body (other parts are ignored) and answers
//! 202. `GET image<name>` is one path segment with a literal prefix, as the contract cassettes record, so
//! `/v1/api/imagecontract.png` is the image and `/v1/api/image/<name>` is the site fallback.

use crate::error::{AppError, AppResult};
use crate::extract::{Admin, Params};
use crate::respond::{empty, with_type};
use crate::AppState;
use anyhow::anyhow;
use axum::extract::{FromRequest, Multipart, Path, Request, State};
use axum::http::StatusCode;

pub async fn upload(_admin: Admin, State(state): State<AppState>, request: Request) -> AppResult {
    let params = Params::parse(request.uri().query());
    let Some(race_id) = params.long("raceId")? else {
        return Ok(empty(StatusCode::BAD_REQUEST));
    };
    let mut multipart = Multipart::from_request(request, &state)
        .await
        .map_err(|e| AppError(anyhow!("receiveMultipart: {e}")))?;
    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|e| AppError(anyhow!("multipart part: {e}")))?
    {
        let is_image = field
            .content_type()
            .map(|ct| {
                ct.split('/')
                    .next()
                    .unwrap_or("")
                    .trim()
                    .eq_ignore_ascii_case("image")
            })
            .unwrap_or(false);
        if !is_image {
            continue;
        }
        // only a file part carries a file name; a bare form value is ignored
        let Some(file_name) = field.file_name().map(str::to_string) else {
            continue;
        };
        let bytes = field
            .bytes()
            .await
            .map_err(|e| AppError(anyhow!("multipart part body: {e}")))?;
        state
            .db
            .save_race_report_image(race_id, &file_name, &bytes)
            .await?;
    }
    Ok(empty(StatusCode::ACCEPTED))
}

/// Answers `GET image<name>` with the image bytes, typed by the file extension, or 404 for an unknown name.
pub async fn download(
    _admin: Admin,
    State(state): State<AppState>,
    Path(name): Path<String>,
) -> AppResult {
    serve(&state, &name).await
}

/// Answers `GET image`, the download route with an empty name, with 404.
pub async fn download_empty_name(_admin: Admin, State(state): State<AppState>) -> AppResult {
    serve(&state, "").await
}

async fn serve(state: &AppState, name: &str) -> AppResult {
    match state.db.get_race_report_image(name).await? {
        Some(bytes) => {
            let content_type = mime_guess::from_path(name)
                .first_or_octet_stream()
                .to_string();
            Ok(with_type(StatusCode::OK, &content_type, bytes))
        }
        None => Ok(empty(StatusCode::NOT_FOUND)),
    }
}

//! The race report image: `POST image?raceId=` with a multipart body whose `image/*` file parts
//! are stored (other parts are ignored), and `GET image<name>` (one path segment with a literal
//! prefix, so `/v1/api/image/<name>` is the SPA fallback).

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
        // a part with a file name is a `PartData.FileItem`; a bare form value is ignored
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

/// `GET image<name>`: the bytes with the content type of the file extension, 404 when unknown.
pub async fn download(
    _admin: Admin,
    State(state): State<AppState>,
    Path(name): Path<String>,
) -> AppResult {
    serve(&state, &name).await
}

/// `GET image` is the download route with an empty name: no such image, 404.
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

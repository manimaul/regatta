//! The router fallback: a file from the site root, else the server-rendered shell, always with 200.
//!
//! Files under the site root (`/pkg/*`, the assets) are served first, precompressed when `cargo leptos build
//! --precompress` left `.br`/`.gz` siblings. Every other unmatched route and method gets the shell rendered for its
//! path, so client routes deep-link; the contract cassettes' fallback steps pin the 200 and the `text/html` media type.
//!
//! The bundle under `/pkg` carries content hashes in its names when `hash-files` is on (a release build), so a found
//! file is cached for a year as immutable; without hashing it must be revalidated on every load, else a browser's
//! heuristic freshness can pair a new page with a stale bundle and hydration fails.

use crate::AppState;
use axum::body::Body;
use axum::extract::{Request, State};
use axum::http::{header, HeaderValue, StatusCode, Uri};
use axum::response::Response;

/// Serves a static file from the site root, else the shell rendered for the request path, always with 200.
///
/// An unknown page renders the router's `NotFound` view.
pub async fn site(uri: Uri, State(state): State<AppState>, request: Request<Body>) -> Response {
    let hashed_files = state.leptos_options.hash_files;
    let in_pkg = uri
        .path()
        .strip_prefix('/')
        .and_then(|path| path.strip_prefix(state.leptos_options.site_pkg_dir.as_ref()))
        .is_some_and(|rest| rest.starts_with('/'));
    let context = state.leptos_context();
    let handler = leptos_axum::file_and_error_handler_with_context::<AppState, _>(
        context,
        regatta_app::shell,
    );
    let mut response = handler(uri, State(state), request).await;
    // leptos marks the rendered shell 404, but the contract cassettes record 200 for every unknown path (a redirect
    // set while rendering keeps its status)
    if response.status() == StatusCode::NOT_FOUND {
        *response.status_mut() = StatusCode::OK;
    } else if in_pkg && response.status() == StatusCode::OK {
        response
            .headers_mut()
            .insert(header::CACHE_CONTROL, pkg_cache_control(hashed_files));
    }
    response
}

/// The `Cache-Control` of a file served from `/pkg`: immutable for a year when its name carries a content hash,
/// revalidated on every load otherwise.
fn pkg_cache_control(hashed_files: bool) -> HeaderValue {
    HeaderValue::from_static(if hashed_files {
        "public, max-age=31536000, immutable"
    } else {
        "no-cache"
    })
}

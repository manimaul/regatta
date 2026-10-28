//! The router fallback: a file from the site root, else the server-rendered shell, always with 200.
//!
//! Files under the site root (`/pkg/*`, the assets) are served first, precompressed when `cargo leptos build
//! --precompress` left `.br`/`.gz` siblings. Every other unmatched route and method gets the shell rendered for its
//! path, so client routes deep-link; the contract cassettes' fallback steps pin the 200 and the `text/html` media type.

use crate::AppState;
use axum::body::Body;
use axum::extract::{Request, State};
use axum::http::{StatusCode, Uri};
use axum::response::Response;

/// Serves a static file from the site root, else the shell rendered for the request path, always with 200.
///
/// An unknown page renders the router's `NotFound` view.
pub async fn site(uri: Uri, State(state): State<AppState>, request: Request<Body>) -> Response {
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
    }
    response
}

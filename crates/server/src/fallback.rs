//! Every unmatched route and method answers the server-rendered shell with 200 (Ktor's
//! `unhandled` handler and `staticResources(... "index.html")` did the same with the static
//! `index.html`; the contract's fallback steps compare status and media type). Files under the
//! site root (`/pkg/*`, the assets) are served first, precompressed when `cargo leptos build
//! --precompress` left `.br`/`.gz` siblings.

use crate::AppState;
use axum::body::Body;
use axum::extract::{Request, State};
use axum::http::{StatusCode, Uri};
use axum::response::Response;

/// A static file from the site root, else the shell rendered for the request path (the router's
/// `NotFound` view for an unknown page), always 200.
pub async fn site(uri: Uri, State(state): State<AppState>, request: Request<Body>) -> Response {
    let context = state.leptos_context();
    let handler = leptos_axum::file_and_error_handler_with_context::<AppState, _>(
        context,
        regatta_app::shell,
    );
    let mut response = handler(uri, State(state), request).await;
    // leptos marks the rendered shell 404; Ktor answered 200 for every unknown path (a redirect
    // set while rendering keeps its status)
    if response.status() == StatusCode::NOT_FOUND {
        *response.status_mut() = StatusCode::OK;
    }
    response
}

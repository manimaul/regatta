//! The HTTP layer of the `regatta` server: one axum router with three faces.
//!
//! - The REST API under `/v1/api` ([`routes`]): every handler a thin adapter over `regatta-service`, its statuses,
//!   bodies and headers pinned by the recorded exchanges in `crates/contract/cassettes`.
//! - The Leptos server functions under [`SERVER_FN_PREFIX`] that the UI calls (`crates/app/src/server_fns.rs`).
//! - The server-rendered UI (`regatta_app`), with the site files and the shell under the [`fallback`].
//!
//! [`build_router`] assembles them over an [`AppState`]; [`error`], [`extract`] and [`respond`] give the REST handlers
//! the request and response shapes the cassettes record. Those shapes are listed in "Quirks to reproduce" in
//! `docs/explore_implemented/rust_port.md`.

/// Stack size of the threads that render pages. Rendering a large screen (the Classes editor) in
/// a debug build nests far deeper than the 2 MiB a tokio worker gets by default and aborts the
/// process with a stack overflow; the server runtime and the SSR tests build their threads with
/// this size instead.
pub const RENDER_THREAD_STACK: usize = 32 * 1024 * 1024;

pub mod error;
pub mod extract;
pub mod fallback;
pub mod respond;
pub mod routes;

use axum::body::Body;
use axum::extract::{FromRef, Request, State};
use axum::response::IntoResponse;
use axum::routing::any;
use axum::Router;
use jiff::Timestamp;
use leptos::prelude::*;
use leptos_axum::{generate_route_list, LeptosRoutes};
use regatta_app::server_fns::ssr::FixedNow;
use regatta_service::Db;
use tower::ServiceBuilder;
use tower_http::compression::CompressionLayer;

/// The server-function prefix: `/api/<name>`, never `/v1/api` (the REST contract).
pub const SERVER_FN_PREFIX: &str = "/api";

/// The state every REST handler, server function and rendered page shares.
#[derive(Clone)]
pub struct AppState {
    pub db: Db,
    /// The `REGATTA_FIXED_NOW` test clock: when set, the `now` of series standings and of `LoginResponse.expires`.
    pub fixed_now: Option<Timestamp>,
    /// Where the site files are, the output name of the wasm bundle, dev reload: from cargo-leptos's
    /// `LEPTOS_*` environment, else the defaults of the root `Cargo.toml`'s metadata.
    pub leptos_options: LeptosOptions,
}

impl FromRef<AppState> for LeptosOptions {
    fn from_ref(state: &AppState) -> LeptosOptions {
        state.leptos_options.clone()
    }
}

impl AppState {
    pub fn new(db: Db, fixed_now: Option<Timestamp>) -> AppState {
        AppState {
            db,
            fixed_now,
            leptos_options: leptos_options(),
        }
    }

    /// Returns the fixed clock when one is configured, else the wall clock.
    pub fn now(&self) -> Timestamp {
        regatta_service::config::now(self.fixed_now)
    }

    /// Returns the context provider for server functions and rendered pages: the [`Db`] and the [`FixedNow`] clock.
    pub fn leptos_context(&self) -> impl Fn() + Clone + Send + Sync + 'static {
        let db = self.db.clone();
        let fixed_now = FixedNow(self.fixed_now);
        move || {
            provide_context(db.clone());
            provide_context(fixed_now);
        }
    }
}

/// Reports whether cargo-leptos (or the image's environment) set `LEPTOS_SITE_ADDR`.
///
/// When it did, that address wins over `REGATTA_BIND`.
pub fn leptos_env_configured() -> bool {
    std::env::var_os("LEPTOS_SITE_ADDR").is_some()
}

/// Returns the Leptos site options from the `LEPTOS_*` environment, else the workspace metadata defaults.
///
/// `get_configuration(None)` reads the `LEPTOS_*` variables cargo-leptos exports; run plainly (`cargo run`, the
/// tests) they are absent and the `[[workspace.metadata.leptos]]` defaults apply: output `regatta`, site root
/// `target/site`, `pkg`, `0.0.0.0:8888`.
pub fn leptos_options() -> LeptosOptions {
    match leptos::config::get_configuration(None) {
        Ok(conf) => conf.leptos_options,
        Err(_) => LeptosOptions::builder()
            .output_name("regatta")
            .site_root("target/site")
            .site_pkg_dir("pkg")
            .site_addr(([0, 0, 0, 0], 8888))
            .reload_port(3001)
            .env(leptos::config::Env::DEV)
            .build(),
    }
}

/// The whole application: path normalisation in front of the router.
pub type App = tower::util::MapRequest<Router, fn(Request) -> Request>;

/// Builds the whole server: REST API, server functions, UI routes and fallback, behind path normalisation.
///
/// In match order: the REST routes under `/v1/api`; the server functions under `/api`; the UI routes rendered by
/// Leptos; the site files and the rendered shell (`NotFound`) for everything else, always 200 (the contract
/// cassettes record 200 `text/html` for unknown paths). Compression (`Vary: Accept-Encoding`) and the collapse of
/// repeated slashes in the path (`/v1/api//series` answers like `/v1/api/series`) sit on top.
pub fn build_router(state: AppState) -> App {
    let routes = generate_route_list(regatta_app::App);
    for route in &routes {
        tracing::debug!("ui route {} ({:?})", route.path(), route.mode());
    }
    let options = state.leptos_options.clone();
    let router = Router::new()
        .nest("/v1/api", routes::api_router())
        .route(
            &format!("{SERVER_FN_PREFIX}/{{*fn_name}}"),
            any(server_fn_handler),
        )
        .leptos_routes_with_context(&state, routes, state.leptos_context(), move || {
            regatta_app::shell(options.clone())
        })
        .fallback(fallback::site)
        .method_not_allowed_fallback(fallback::site)
        .layer(CompressionLayer::new())
        .with_state(state);
    ServiceBuilder::new()
        .map_request(collapse_slashes as fn(Request) -> Request)
        .service(router)
}

/// `/api/<fn>`: the server functions of `regatta_app::server_fns`, with the database and the
/// clock in context (the request parts and the response options come from leptos_axum).
async fn server_fn_handler(
    State(state): State<AppState>,
    request: Request<Body>,
) -> impl IntoResponse {
    leptos_axum::handle_server_fns_with_context(state.leptos_context(), request).await
}

/// Collapses repeated slashes in the request path, keeping the query.
///
/// The contract cassettes record `/v1/api//series` answering like `/v1/api/series`; axum's router does not ignore
/// empty path segments on its own.
fn collapse_slashes(mut request: Request) -> Request {
    let uri = request.uri();
    let path = uri.path();
    if !path.contains("//") {
        return request;
    }
    let mut collapsed = String::with_capacity(path.len());
    let mut last_slash = false;
    for c in path.chars() {
        if c == '/' {
            if last_slash {
                continue;
            }
            last_slash = true;
        } else {
            last_slash = false;
        }
        collapsed.push(c);
    }
    let path_and_query = match uri.query() {
        Some(q) => format!("{collapsed}?{q}"),
        None => collapsed,
    };
    let mut parts = uri.clone().into_parts();
    if let Ok(pq) = path_and_query.parse() {
        parts.path_and_query = Some(pq);
        if let Ok(new_uri) = http::Uri::from_parts(parts) {
            *request.uri_mut() = new_uri;
        }
    }
    request
}

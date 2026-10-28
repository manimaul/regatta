#![recursion_limit = "512"]
//! The Leptos UI of the regatta site, rendered on the server and hydrated in the browser.
//!
//! The crate is compiled twice: with the `ssr` feature into `regatta-server`, which renders every page, and with
//! `hydrate` into `regatta-frontend`, the wasm bundle served from `/pkg`. Both builds must render the same markup
//! from the same state or hydration panics, so never put a block element inside `<p>`. Tailwind finds classes by
//! scanning these sources, so class names are literal strings, and components read only the `--rg-*` theme tokens
//! of `crates/app/style/app.css`. Design notes: `docs/explore_implemented/rust_port.md`.
//!
//! - [`shell`]: the HTML document around the app (theme script, stylesheets, CDN scripts).
//! - [`app`]: the page frame and the router.
//! - [`routes`]: one module per screen.
//! - [`components`]: the shared components and their `.rg-*` classes.
//! - [`state`]: UI state that holds no server data (login, alerts, route, theme).
//! - [`server_fns`]: the server functions the UI calls, over `regatta-service`.
//! - [`utils`]: pure helpers (dates, labels, report rows) and the browser shims.
//! - [`chart`]: the course chart over MapLibre GL.
//! - [`sortable`]: the SortableJS bindings for drag-and-drop lists.
//! - [`auth_hash`]: the password hashing the login form does before sending.
//! - [`cb`]: the [`cb::Cb`] callback prop type.
//! - [`app_style`]: the shared margin classes.

pub mod app;
pub mod app_style;
pub mod auth_hash;
pub mod cb;
pub mod chart;
pub mod components;
pub mod routes;
pub mod server_fns;
pub mod sortable;
pub mod state;
pub mod utils;

pub use app::App;
use leptos::prelude::*;
use leptos_meta::MetaTags;
use state::theme::Theme;

/// The inline script that applies the saved light/dark choice before first paint.
///
/// A fallback for a browser that has `localStorage["theme"]` but no `theme` cookie yet; the key name must stay
/// stable so existing users keep their saved theme.
pub const THEME_SCRIPT: &str = r#"try { if (localStorage.getItem("theme") === "dark") document.documentElement.setAttribute("data-theme", "dark"); } catch (e) {}"#;

pub const MAPLIBRE_CSS: &str =
    "https://cdn.jsdelivr.net/npm/maplibre-gl@4.7.1/dist/maplibre-gl.css";
pub const MAPLIBRE_JS: &str = "https://cdn.jsdelivr.net/npm/maplibre-gl@4.7.1/dist/maplibre-gl.js";
pub const SORTABLE_JS: &str = "https://cdn.jsdelivr.net/npm/sortablejs@1.15.2/Sortable.min.js";

/// Reads the `theme` cookie of the current request, when leptos_axum put the request parts in context.
///
/// Returns `None` on the client and in tests.
pub fn theme_from_cookie() -> Option<Theme> {
    #[cfg(feature = "ssr")]
    {
        let parts = use_context::<http::request::Parts>()?;
        parts
            .headers
            .get_all(http::header::COOKIE)
            .iter()
            .filter_map(|v| v.to_str().ok())
            .find_map(state::theme::theme_from_cookie_header)
    }
    #[cfg(not(feature = "ssr"))]
    {
        None
    }
}

/// Returns the theme the page starts in.
///
/// That is the request cookie on the server, the applied attribute or saved choice in the browser, light otherwise.
pub fn initial_theme() -> Theme {
    theme_from_cookie().unwrap_or_else(state::theme::initial_theme)
}

/// Renders the HTML document around the app.
///
/// `data-theme` comes from the `theme` cookie so the server renders the chosen theme without a flash; the CDN tags
/// load MapLibre and SortableJS, which are not bundled.
pub fn shell(options: LeptosOptions) -> impl IntoView {
    let theme = initial_theme();
    view! {
        <!DOCTYPE html>
        <html lang="en" data-theme=theme.attr()>
            <head>
                <meta charset="UTF-8"/>
                <meta name="author" content="William Kamp - https://github.com/manimaul"/>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <script inner_html=THEME_SCRIPT></script>
                <link rel="stylesheet" href=MAPLIBRE_CSS/>
                // a plain link: `<Stylesheet>` needs the meta context, which `App` provides inside <body>
                <link rel="stylesheet" href=format!("/{}/{}.css", options.site_pkg_dir, options.output_name)/>
                <AutoReload options=options.clone()/>
                <HydrationScripts options/>
                <MetaTags/>
            </head>
            <body>
                <App/>
                <script src=MAPLIBRE_JS></script>
                <script src=SORTABLE_JS></script>
            </body>
        </html>
    }
}

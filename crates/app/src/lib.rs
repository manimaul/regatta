#![recursion_limit = "512"]
//! regatta-app: the Leptos UI of the Rust port (= the Kotlin `web` module; see
//! docs/explore_unimplemented/rust_port.md, Phase 4). Compiled twice: with `ssr` into
//! regatta-server, with `hydrate` into regatta-frontend (wasm).
//!
//! Module map (Kotlin file -> Rust module): `Router.kt`/`Main.js.kt` -> [`app`], `index.html` ->
//! [`shell`], `components/*` -> [`components`], `components/routes/*` -> [`routes`],
//! `utils/Api.kt` -> [`server_fns`], `utils/*` -> [`utils`] / [`chart`] / [`auth_hash`],
//! `viewmodel/*` -> [`state`],
//! `lib/Sortable*.kt` -> [`sortable`], `styles/AppStyle.kt` -> [`app_style`].

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

/// = the inline script of `index.html`: applies the saved light/dark choice before first paint.
/// Kept as a fallback for a browser with a `localStorage["theme"]` but no `theme` cookie yet.
pub const THEME_SCRIPT: &str = r#"try { if (localStorage.getItem("theme") === "dark") document.documentElement.setAttribute("data-theme", "dark"); } catch (e) {}"#;

pub const MAPLIBRE_CSS: &str =
    "https://cdn.jsdelivr.net/npm/maplibre-gl@4.7.1/dist/maplibre-gl.css";
pub const MAPLIBRE_JS: &str = "https://cdn.jsdelivr.net/npm/maplibre-gl@4.7.1/dist/maplibre-gl.js";
pub const SORTABLE_JS: &str = "https://cdn.jsdelivr.net/npm/sortablejs@1.15.2/Sortable.min.js";

/// The `theme` cookie of the current request, when leptos_axum put the request parts in context.
/// `None` on the client and in tests.
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

/// The theme the page starts in: the request cookie on the server, the applied attribute or saved
/// choice in the browser, light otherwise.
pub fn initial_theme() -> Theme {
    theme_from_cookie().unwrap_or_else(state::theme::initial_theme)
}

/// = `index.html`: the document around the app. `data-theme` comes from the `theme` cookie so the
/// server renders the chosen theme without a flash; the CDN tags load MapLibre and SortableJS.
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

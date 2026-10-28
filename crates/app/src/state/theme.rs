//! The light/dark theme state and its persistence.
//!
//! The toggle writes `data-theme` on `<html>`, `localStorage["theme"]` and the `theme` cookie (D3,
//! `docs/explore_implemented/rust_port.md`), so the next server render starts in the chosen theme without a flash.
//! Components follow the theme only through the `--rg-*` tokens of `crates/app/style/app.css`.

use leptos::prelude::*;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Theme {
    #[default]
    Light,
    Dark,
}

impl Theme {
    pub const fn attr(self) -> &'static str {
        match self {
            Theme::Light => "light",
            Theme::Dark => "dark",
        }
    }

    /// The site defaults to light; only an explicit choice from the toggle switches it.
    pub fn from_value(value: Option<&str>) -> Theme {
        match value {
            Some("dark") => Theme::Dark,
            _ => Theme::Light,
        }
    }

    pub const fn other(self) -> Theme {
        match self {
            Theme::Light => Theme::Dark,
            Theme::Dark => Theme::Light,
        }
    }
}

/// The `localStorage` key of the saved theme; the inline script in the shell reads it before the bundle loads.
///
/// Must stay stable so existing users keep their saved choice.
pub const THEME_STORAGE_KEY: &str = "theme";
/// The theme cookie's name; [`crate::shell`] reads it to render `data-theme` on the server.
pub const THEME_COOKIE: &str = "theme";
/// The theme cookie's lifetime in seconds: one year.
pub const THEME_COOKIE_MAX_AGE: u32 = 31_536_000;

/// Formats the `Set-Cookie`-style value the toggle writes through `document.cookie`.
pub fn theme_cookie(theme: Theme) -> String {
    format!(
        "{THEME_COOKIE}={}; path=/; max-age={THEME_COOKIE_MAX_AGE}; SameSite=Lax",
        theme.attr()
    )
}

/// Parses the `theme` cookie's value out of a `Cookie:` header, `None` when absent.
pub fn theme_from_cookie_header(header: &str) -> Option<Theme> {
    header.split(';').find_map(|pair| {
        let (name, value) = pair.trim().split_once('=')?;
        (name.trim() == THEME_COOKIE).then(|| Theme::from_value(Some(value.trim())))
    })
}

/// The app-wide theme signal.
///
/// Provided by [`crate::App`] and read by the theme toggle, the chart controller and anything else that follows the
/// theme.
#[derive(Debug, Clone, Copy)]
pub struct ThemeState(pub RwSignal<Theme>);

pub fn provide_theme(initial: Theme) -> ThemeState {
    let state = ThemeState(RwSignal::new(initial));
    provide_context(state);
    apply(initial);
    state
}

pub fn use_theme() -> ThemeState {
    use_context::<ThemeState>().unwrap_or_else(|| provide_theme(initial_theme()))
}

impl ThemeState {
    pub fn theme(&self) -> Theme {
        self.0.get()
    }

    pub fn toggle(&self) {
        self.set_theme(self.0.get_untracked().other());
    }

    pub fn set_theme(&self, theme: Theme) {
        persist(theme);
        apply(theme);
        self.0.set(theme);
    }
}

/// Returns the browser's saved theme, or light.
///
/// Reads the `data-theme` attribute the shell or inline script already applied, then `localStorage`. On the server
/// the cookie's value is used instead (see [`crate::initial_theme`]).
pub fn initial_theme() -> Theme {
    crate::utils::browser::initial_theme()
}

fn apply(theme: Theme) {
    crate::utils::browser::set_document_theme(theme.attr());
}

fn persist(theme: Theme) {
    crate::utils::browser::local_storage_set(THEME_STORAGE_KEY, theme.attr());
    crate::utils::browser::set_cookie(&theme_cookie(theme));
}

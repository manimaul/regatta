//! = `ThemeViewModel.kt`, plus the `theme` cookie of the Rust port (rust_port.md D3): the toggle
//! writes `data-theme` on `<html>`, `localStorage["theme"]` and the cookie, so the next
//! server render starts in the chosen theme without a flash.

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

/// `localStorage` key; the inline script in the shell reads it before the bundle loads.
pub const THEME_STORAGE_KEY: &str = "theme";
/// Cookie name; `shell()` reads it to render `data-theme` on the server.
pub const THEME_COOKIE: &str = "theme";
/// One year, like the inline script's fallback expects.
pub const THEME_COOKIE_MAX_AGE: u32 = 31_536_000;

/// The `Set-Cookie`-style value the toggle writes through `document.cookie`.
pub fn theme_cookie(theme: Theme) -> String {
    format!(
        "{THEME_COOKIE}={}; path=/; max-age={THEME_COOKIE_MAX_AGE}; SameSite=Lax",
        theme.attr()
    )
}

/// The `theme` cookie's value out of a `Cookie:` header, or `None` when absent.
pub fn theme_from_cookie_header(header: &str) -> Option<Theme> {
    header.split(';').find_map(|pair| {
        let (name, value) = pair.trim().split_once('=')?;
        (name.trim() == THEME_COOKIE).then(|| Theme::from_value(Some(value.trim())))
    })
}

/// The app-wide theme signal (`themeViewModel`), provided by `App` and read by `ThemeToggle`,
/// the chart controller and anything else that follows the theme.
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

/// The browser's saved choice (the attribute the shell or inline script already applied, then
/// `localStorage`), or light. On the server `App` passes the cookie's value in instead.
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

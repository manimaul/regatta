//! The light/dark switch: a sun knob on the left in light mode sliding right to a moon in dark mode
//! (`.rg-theme-toggle`).
//!
//! It writes the choice through [`crate::state::theme`].

use crate::state::theme::{use_theme, Theme};
use leptos::either::Either;
use leptos::prelude::*;

#[component]
pub fn ThemeToggle() -> impl IntoView {
    let theme = use_theme();
    let dark = move || theme.theme() == Theme::Dark;
    view! {
        <button
            class="rg-theme-toggle"
            type="button"
            role="switch"
            aria-checked=move || dark().to_string()
            aria-label="Dark mode"
            title=move || if dark() { "Switch to light mode" } else { "Switch to dark mode" }
            on:click=move |_| theme.toggle()
        >
            <span class="rg-theme-toggle-knob">
                {move || if dark() { Either::Left(MoonIcon()) } else { Either::Right(SunIcon()) }}
            </span>
        </button>
    }
}

#[component]
fn SunIcon() -> impl IntoView {
    view! {
        <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <circle cx="12" cy="12" r="4" fill="currentColor"></circle>
            <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"></path>
        </svg>
    }
}

#[component]
fn MoonIcon() -> impl IntoView {
    view! {
        <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" fill="currentColor"></path>
        </svg>
    }
}

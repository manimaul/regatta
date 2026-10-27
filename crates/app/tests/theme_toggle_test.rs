//! = `ThemeToggleTest.kt`, the markup half: the switch's state attributes follow the theme, the cookie and
//! `data-theme` on the shell follow the cookie. The click is in `tests/wasm.rs`.
#![cfg(all(not(target_arch = "wasm32"), feature = "ssr"))]

mod common;

use common::*;
use leptos::prelude::*;
use regatta_app::components::ThemeToggle;
use regatta_app::state::theme::{provide_theme, theme_cookie, theme_from_cookie_header, Theme};

#[test]
fn defaults_to_light_with_no_saved_choice() {
    assert_eq!(Theme::from_value(None), Theme::Light);
    assert_eq!(Theme::from_value(Some("nonsense")), Theme::Light);
    assert_eq!(Theme::from_value(Some("dark")), Theme::Dark);
    assert_eq!(
        theme_from_cookie_header("a=1; theme=dark; b=2"),
        Some(Theme::Dark)
    );
    assert_eq!(theme_from_cookie_header("theme=light"), Some(Theme::Light));
    assert_eq!(theme_from_cookie_header("a=1"), None);
    assert_eq!(
        theme_cookie(Theme::Dark),
        "theme=dark; path=/; max-age=31536000; SameSite=Lax"
    );
}

#[test]
fn toggle_reflects_the_theme() {
    let light = render(|| {
        provide_theme(Theme::Light);
        view! { <ThemeToggle/> }
    });
    let toggle = tags_with_class(&light, "rg-theme-toggle")[0];
    assert_eq!(attr(toggle, "aria-checked"), Some("false"));
    assert_eq!(attr(toggle, "role"), Some("switch"));
    assert_eq!(attr(toggle, "title"), Some("Switch to dark mode"));
    assert_eq!(tags_with_class(&light, "rg-theme-toggle-knob").len(), 1);
    assert!(light.contains("<circle"), "sun knob in light mode");

    let dark = render(|| {
        provide_theme(Theme::Dark);
        view! { <ThemeToggle/> }
    });
    let toggle = tags_with_class(&dark, "rg-theme-toggle")[0];
    assert_eq!(attr(toggle, "aria-checked"), Some("true"));
    assert_eq!(attr(toggle, "title"), Some("Switch to light mode"));
    assert!(!dark.contains("<circle"), "moon knob in dark mode");
}

#[test]
fn shell_renders_data_theme_from_the_cookie_default_light() {
    // no request parts in context: the default
    let html = render(|| {
        provide_context(leptos_router::location::RequestUrl::new("/people"));
        regatta_app::shell(LeptosOptions::builder().output_name("regatta").build())
    });
    assert!(
        html.contains("<html lang=\"en\" data-theme=\"light\">"),
        "{}",
        &html[..200.min(html.len())]
    );
    assert!(
        html.contains(regatta_app::MAPLIBRE_CSS)
            && html.contains(regatta_app::MAPLIBRE_JS)
            && html.contains(regatta_app::SORTABLE_JS)
    );
    assert!(
        html.contains("localStorage.getItem(\"theme\")"),
        "the inline fallback script is kept"
    );
    // leptos_axum's streaming injects the <Title> through MetaTags; the plain render carries the frame
    assert!(
        html.contains("id=\"route_content\""),
        "the page frame renders inside the shell"
    );
    // D11: a screen renders in `SsrMode::Async` behind an empty Suspense fallback, so a synchronous
    // render carries the frame only; the streamed People page is covered by the server's SSR test.
    assert!(
        !html.contains("<h1>Skippers</h1>"),
        "the screen body is streamed, not rendered synchronously"
    );
}

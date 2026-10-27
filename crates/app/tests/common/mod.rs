//! Helpers for the SSR render tests: an owner for the signals and contexts, a router context for
//! components that navigate, and a few string-level DOM checks on the rendered HTML.
#![allow(dead_code)]

use std::sync::OnceLock;

use leptos::prelude::*;
use leptos_router::components::Router;
use leptos_router::location::RequestUrl;

/// Screens in `SsrMode::Async` spawn their resources while rendering, which needs a global
/// executor (leptos_axum initialises one on the server). The futures are never driven here: a
/// synchronous render only sees the fallbacks, which is what these tests assert on.
fn runtime() -> &'static tokio::runtime::Runtime {
    static RT: OnceLock<tokio::runtime::Runtime> = OnceLock::new();
    RT.get_or_init(|| {
        tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("tokio runtime")
    })
}

/// Renders a view to its HTML inside a fresh reactive owner (= `runTest { composition { ... } }`).
pub fn render<V: IntoView + 'static>(f: impl FnOnce() -> V) -> String {
    let _guard = runtime().enter();
    let _ = any_spawner::Executor::init_tokio(); // Err when already initialised: fine
    let owner = Owner::new();
    owner.set();
    let html = f().to_html();
    owner.cleanup();
    html
}

/// Renders inside a `<Router>` positioned at `path`, for components using the router hooks.
pub fn render_at<V: IntoView + 'static>(
    path: &'static str,
    f: impl FnOnce() -> V + Send + 'static,
) -> String {
    render(move || {
        provide_context(RequestUrl::new(path));
        view! { <Router>{f()}</Router> }
    })
}

/// The opening tags (`<tag ...>`) of the HTML, in order.
pub fn tags(html: &str) -> Vec<&str> {
    let mut out = Vec::new();
    let mut rest = html;
    while let Some(start) = rest.find('<') {
        let after = &rest[start..];
        let Some(end) = after.find('>') else { break };
        let tag = &after[..=end];
        if !tag.starts_with("</") && !tag.starts_with("<!") {
            out.push(tag);
        }
        rest = &after[end + 1..];
    }
    out
}

/// The opening tags that carry this class.
pub fn tags_with_class<'a>(html: &'a str, class: &str) -> Vec<&'a str> {
    tags(html)
        .into_iter()
        .filter(|t| classes_of(t).contains(&class))
        .collect()
}

/// The value of `attr="..."` on an opening tag.
pub fn attr<'a>(tag: &'a str, name: &str) -> Option<&'a str> {
    let needle = format!(" {name}=\"");
    let start = tag.find(&needle)? + needle.len();
    let end = tag[start..].find('"')? + start;
    Some(&tag[start..end])
}

/// The class list of an opening tag.
pub fn classes_of(tag: &str) -> Vec<&str> {
    attr(tag, "class")
        .map(|c| c.split_whitespace().collect())
        .unwrap_or_default()
}

/// Text content with tags stripped (whitespace kept as rendered).
pub fn text(html: &str) -> String {
    let mut out = String::new();
    let mut in_tag = false;
    for c in html.chars() {
        match c {
            '<' => in_tag = true,
            '>' => in_tag = false,
            _ if !in_tag => out.push(c),
            _ => {}
        }
    }
    out
}

/// Counts the occurrences of a substring.
pub fn count(html: &str, needle: &str) -> usize {
    html.matches(needle).count()
}

/// The HTML between the first `<tag ...>` with this class (any tag when `class` is empty) and its
/// closing tag (shallow: the first `</tag>`).
pub fn element_html<'a>(html: &'a str, tag_name: &str, class: &str) -> Option<&'a str> {
    let open = format!("<{tag_name}");
    let mut from = 0;
    while let Some(start) = html[from..].find(&open) {
        let start = from + start;
        let end_of_tag = html[start..].find('>')? + start;
        let tag = &html[start..=end_of_tag];
        let name_ends = tag[open.len()..]
            .chars()
            .next()
            .map(|c| c == ' ' || c == '>' || c == '/')
            .unwrap_or(false);
        if name_ends && (class.is_empty() || classes_of(tag).contains(&class)) {
            let close = format!("</{tag_name}>");
            let end = html[end_of_tag..].find(&close)? + end_of_tag;
            return Some(&html[start..end + close.len()]);
        }
        from = end_of_tag + 1;
    }
    None
}

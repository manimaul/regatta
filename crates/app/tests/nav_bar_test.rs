//! = `NavBarTest.kt`, the markup half: the hamburger, the collapse and the links. The toggle click is in `tests/wasm.rs`.
#![cfg(all(not(target_arch = "wasm32"), feature = "ssr"))]

mod common;

use common::*;
use leptos::prelude::*;
use regatta_app::components::NavBar;

fn link_texts(html: &str) -> Vec<String> {
    // class attributes render last in a tag, so split on the class and read the text after the tag's `>`
    html.split("class=\"rg-nav-link\">")
        .skip(1)
        .map(|s| s[..s.find("</button>").unwrap()].to_string())
        .collect()
}

#[test]
fn renders_closed_menu_with_public_links() {
    let html = render_at("/course", || view! { <NavBar/> });
    let toggler = tags_with_class(&html, "rg-navbar-toggler")[0];
    assert_eq!(attr(toggler, "aria-expanded"), Some("false"));
    assert_eq!(attr(toggler, "aria-controls"), Some("navbarNavDropdown"));
    let collapse = tags_with_class(&html, "rg-navbar-collapse")[0];
    assert_eq!(attr(collapse, "data-open"), Some("false"));
    // logged out: the public routes and the Admin link
    assert_eq!(link_texts(&html), vec!["Race Results", "Course", "Admin"]);
    let logo = tags_with_class(&html, "rg-navbar-logo")[0];
    assert_eq!(attr(logo, "src"), Some("/cyct_burgee.svg"));
    assert!(
        classes_of(logo).contains(&"h-[60px]"),
        "a height class, not a height attribute"
    );
    assert_eq!(tags_with_class(&html, "rg-theme-toggle").len(), 1);
}

#[test]
fn current_route_is_marked() {
    let html = render_at("/course", || view! { <NavBar/> });
    let current: Vec<&str> = tags_with_class(&html, "rg-nav-link")
        .into_iter()
        .filter(|t| attr(t, "aria-current") == Some("page"))
        .collect();
    assert_eq!(current.len(), 1);
    let html = render_at("/races/results", || view! { <NavBar/> });
    let links = tags_with_class(&html, "rg-nav-link");
    assert_eq!(
        attr(links[0], "aria-current"),
        Some("page"),
        "Race Results is current"
    );
    assert_eq!(attr(links[1], "aria-current"), None);
}

#[test]
fn nonav_renders_the_full_results_link_instead() {
    let html = render_at("/races/results/view/3?nonav", || view! { <NavBar/> });
    assert!(tags_with_class(&html, "rg-navbar").is_empty());
    let a = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<a"))
        .unwrap();
    assert_eq!(attr(a, "target"), Some("_blank"));
    assert_eq!(attr(a, "href"), Some("/races/results/view/3?nav"));
    assert_eq!(text(&html), "Full Regatta Results");
}

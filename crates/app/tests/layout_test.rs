//! Tests that the layout helpers emit Tailwind and `.rg-*` classes with Bootstrap's sizes.
#![cfg(all(not(target_arch = "wasm32"), feature = "ssr"))]

mod common;

use common::*;
use leptos::prelude::*;
use regatta_app::components::*;
use regatta_app::state::alerts::use_alerts;
use std::collections::BTreeSet;

/// Tells whether `cls` is in the `@source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}")` safelist of
/// `crates/app/style/app.css`.
///
/// Customizer spells these class names out, and Tailwind generates them because of that safelist: keep the two in sync.
fn safelisted(cls: &str) -> bool {
    let (space, rest) = cls.split_at(1);
    if !matches!(space, "m" | "p") {
        return false;
    }
    let rest = match rest.chars().next() {
        Some(c @ ('t' | 'b' | 's' | 'e' | 'x' | 'y')) => &rest[c.len_utf8()..],
        _ => rest,
    };
    matches!(rest, "-0" | "-1" | "-2" | "-4" | "-6" | "-12" | "-auto")
}

fn classes_by_id(html: &str, id: &str) -> BTreeSet<String> {
    let tag = tags(html)
        .into_iter()
        .find(|t| attr(t, "id") == Some(id))
        .expect(id);
    classes_of(tag).into_iter().map(str::to_string).collect()
}

fn set(items: &[&str]) -> BTreeSet<String> {
    items.iter().map(|s| s.to_string()).collect()
}

#[test]
fn every_spacing_combination_is_safelisted() {
    let sides: Vec<Option<RgSide>> = std::iter::once(None)
        .chain(RgSide::ENTRIES.into_iter().map(Some))
        .collect();
    let sizes: Vec<Option<RgSz>> = std::iter::once(None)
        .chain(RgSz::ENTRIES.into_iter().map(Some))
        .collect();
    for space in RgSpace::ENTRIES {
        for side in &sides {
            for size in &sizes {
                let customizer = Customizer::new().set(Some(space), *side, *size);
                for cls in customizer.get_classes() {
                    if size.is_some() {
                        assert!(
                            safelisted(cls),
                            "{space:?}/{side:?}/{size:?} -> '{cls}' is not in the app.css safelist"
                        );
                    }
                }
            }
        }
    }
}

#[test]
fn spacing_keeps_bootstrap_sizes() {
    let html = render(|| {
        view! {
            <RgDiv id="a" customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::B), Some(RgSz::S3))/>
            <RgDiv id="b" customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::Y), Some(RgSz::S4))/>
            <RgDiv id="c" customizer=Customizer::new().set(Some(RgSpace::P), None, Some(RgSz::S5))/>
            <RgDiv id="d" customizer=Customizer::new().set(Some(RgSpace::M), None, Some(RgSz::S2)).add_custom("grow").add_custom("flex").add_custom("flex-col")/>
        }
    });
    assert_eq!(
        classes_by_id(&html, "a"),
        set(&["mb-4"]),
        "Bootstrap mb-3 (1rem) is Tailwind mb-4"
    );
    assert_eq!(
        classes_by_id(&html, "b"),
        set(&["my-6"]),
        "Bootstrap my-4 (1.5rem) is Tailwind my-6"
    );
    assert_eq!(
        classes_by_id(&html, "c"),
        set(&["p-12"]),
        "Bootstrap p-5 (3rem) is Tailwind p-12"
    );
    assert_eq!(
        classes_by_id(&html, "d"),
        set(&["m-2", "grow", "flex", "flex-col"])
    );
}

#[test]
fn grid_container_and_position_classes() {
    let html = render(|| {
        view! {
            <RgGrid r#type=RgContainerType::ContainerFluid/>
            <RgGrid r#type=RgContainerType::ContainerMd position=RgPosition::FixedTop/>
        }
    });
    let grids: Vec<BTreeSet<String>> = tags(&html)
        .into_iter()
        .filter(|t| t.starts_with("<div"))
        .map(|t| classes_of(t).into_iter().map(str::to_string).collect())
        .collect();
    assert_eq!(
        grids[0],
        set(&["rg-container-fluid", "min-h-screen", "flex", "flex-col"])
    );
    assert_eq!(
        grids[1],
        set(&[
            "rg-container-md",
            "rg-fixed-top",
            "min-h-screen",
            "flex",
            "flex-col"
        ])
    );
    // classList.add() rejects names containing spaces, so each entry must be exactly one class
    for cls in RgContainerType::ENTRIES
        .iter()
        .map(|c| c.cls())
        .chain(RgPosition::ENTRIES.iter().map(|p| p.cls()))
    {
        assert!(
            cls.starts_with("rg-") && cls.chars().all(|c| c.is_ascii_lowercase() || c == '-'),
            "'{cls}' must be a single rg-* class"
        );
    }
}

#[test]
fn sortable_uses_list_group_classes() {
    let html = render(|| {
        view! { <RgSortable items=vec!["Harbor Series".to_string(), "Point Series".to_string()] name=|s: &String| s.clone() on_sort={|_: Vec<String>| {}}/> }
    });
    let ul = element_html(&html, "ul", "rg-list-group").expect("ul.rg-list-group");
    assert_eq!(tags_with_class(ul, "rg-list-group-item").len(), 2);
    assert!(
        tags_with_class(&html, "list-group").is_empty()
            && tags_with_class(&html, "list-group-item").is_empty(),
        "no Bootstrap classes left"
    );
}

#[test]
fn success_banner_uses_rg_alert() {
    let html = render(|| {
        let alerts = use_alerts();
        alerts.show_alert("Tailwind Test added!");
        view! { <Alerts/> }
    });
    let banner = tags_with_class(&html, "rg-alert-success");
    assert_eq!(banner.len(), 1);
    let b = banner[0];
    assert_eq!(attr(b, "role"), Some("alert"));
    let banner_html = element_html(&html, "div", "rg-alert-success").unwrap();
    assert_eq!(text(banner_html), "Tailwind Test added!");
    assert!(
        attr(b, "style").is_none(),
        "position comes from .rg-alert-success, not an inline style"
    );
    for bootstrap in ["alert", "alert-success", "fixed-top", "bi"] {
        assert!(
            tags_with_class(&html, bootstrap).is_empty(),
            "no Bootstrap classes left: {bootstrap}"
        );
    }
}

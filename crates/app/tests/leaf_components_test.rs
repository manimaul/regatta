//! = `LeafComponentsTest.kt`, the markup half: Bootstrap classes replaced by rg-*, public APIs unchanged.
//! The interaction half (open/select/dismiss, typing) is in `tests/wasm.rs`.
#![cfg(all(not(target_arch = "wasm32"), feature = "ssr"))]

mod common;

use common::*;
use jiff::Timestamp;
use leptos::prelude::*;
use regatta_app::components::*;

#[test]
fn dropdown_renders_closed_with_the_primary_toggle() {
    let html = render(|| {
        view! { <RgDropdown items=vec!["Alpha", "Bravo", "Charlie"] selected_item="Alpha" name=|s: &&str| s.to_string() handler=|_: &str| {}/> }
    });
    let root = element_html(&html, "div", "rg-dropdown").expect("div.rg-dropdown");
    let toggle = tags(root)
        .into_iter()
        .find(|t| t.starts_with("<button"))
        .expect("toggle");
    let classes = classes_of(toggle);
    assert!(classes.contains(&"rg-btn"));
    assert!(classes.contains(&"rg-btn-primary"));
    assert!(classes.contains(&"rg-dropdown-toggle"));
    assert_eq!(attr(toggle, "aria-expanded"), Some("false"));
    assert!(
        tags_with_class(&html, "rg-dropdown-menu").is_empty(),
        "closed until clicked"
    );
    assert_eq!(text(root), "Alpha");
}

#[test]
fn dropdown_with_one_item_is_disabled() {
    let html = render(|| {
        view! { <RgIdDropdown id="one" items=vec!["Only"] selected_item="Only" name=|s: &&str| s.to_string() handler=|_: &str| {}/> }
    });
    let root = tags(&html)
        .into_iter()
        .find(|t| attr(t, "id") == Some("one"))
        .expect("#one");
    assert!(classes_of(root).contains(&"rg-dropdown"));
    let toggle = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<button"))
        .expect("toggle");
    assert!(toggle.contains(" disabled"), "{toggle}");
}

#[test]
fn dropdown_none_shows_none_and_the_selected_name() {
    let html = render(|| {
        view! { <RgDropdownNone items=vec!["Zulu Tango", "Kahuna", "Grace E"] selected_item=Some("Kahuna") name=|s: &&str| s.to_string() handler={|_: Option<&str>| {}}/> }
    });
    assert_eq!(
        text(element_html(&html, "div", "rg-dropdown").unwrap()),
        "Kahuna"
    );
    let none = render(|| {
        view! { <RgDropdownNone items=vec!["Zulu Tango"] selected_item={None::<&str>} name=|s: &&str| s.to_string() handler={|_: Option<&str>| {}}/> }
    });
    assert_eq!(
        text(element_html(&none, "div", "rg-dropdown").unwrap()),
        "None"
    );
}

#[test]
fn button_classes_custom_first_then_style() {
    let html = render(|| {
        view! {
            <RgButton label="Keep" style=RgButtonStyle::PrimaryOutline click=|()| {}/>
            <RgButton label="Save" style=RgButtonStyle::Success closes_modal=true click=|()| {}/>
            <RgButton label="No" style=RgButtonStyle::Danger custom_classes="me-2" click=|()| {}/>
        }
    });
    let buttons: Vec<&str> = tags(&html)
        .into_iter()
        .filter(|t| t.starts_with("<button"))
        .collect();
    assert_eq!(
        attr(buttons[0], "class"),
        Some("rg-btn rg-btn-outline-primary")
    );
    assert_eq!(attr(buttons[1], "class"), Some("rg-btn rg-btn-success"));
    assert_eq!(attr(buttons[2], "class"), Some("me-2 rg-btn rg-btn-danger"));
    assert!(!buttons[0].contains("disabled"));
}

#[test]
fn number_input_renders_the_value_and_numeric_mode() {
    let html = render(|| {
        view! { <RgNumberInput label="PHRF Rating" value=Some(72.0) listener=|_: f64| {}/> }
    });
    let input = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<input"))
        .expect("input");
    assert!(classes_of(input).contains(&"rg-input"));
    assert_eq!(attr(input, "value"), Some("72"));
    assert_eq!(attr(input, "inputmode"), Some("numeric"));
    let label = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<label"))
        .expect("label");
    assert_eq!(
        attr(label, "for"),
        attr(input, "id"),
        "the label points at the input"
    );
}

#[test]
fn input_with_button_uses_input_group_and_outline_button() {
    let html = render(|| {
        view! { <RgInputWithButton label="ORC Reference Number" value="04560004KJP" btn_label="Add Cert" listener=|_: (String, bool)| {}/> }
    });
    let group_tag = tags_with_class(&html, "rg-input-group")[0];
    assert!(
        classes_of(group_tag).contains(&"mb-4"),
        "Bootstrap mb-3 (1rem) maps to Tailwind mb-4"
    );
    let group = element_html(&html, "div", "rg-input-group").unwrap();
    let input = tags(group)
        .into_iter()
        .find(|t| t.starts_with("<input"))
        .unwrap();
    assert!(classes_of(input).contains(&"rg-input"));
    assert_eq!(attr(input, "value"), Some("04560004KJP"));
    let button = tags(group)
        .into_iter()
        .find(|t| t.starts_with("<button"))
        .unwrap();
    assert_eq!(attr(button, "class"), Some("rg-btn rg-btn-outline-primary"));
}

#[test]
fn check_and_switch_render() {
    let html = render(|| {
        view! {
            <RgCheck label="Club member" checked=false listener=|_: bool| {}/>
            <RgSwitch id="sw" num=1 label="PHRF" check=true change=|_: bool| {}/>
        }
    });
    assert_eq!(tags_with_class(&html, "rg-check").len(), 2);
    let box_tag = tags_with_class(&html, "rg-checkbox")[0];
    assert_eq!(attr(box_tag, "type"), Some("checkbox"));
    let switch = tags_with_class(&html, "rg-switch")[0];
    assert_eq!(attr(switch, "id"), Some("sw1"));
    assert!(switch.contains(" checked"), "{switch}");
    let label = tags(&html)
        .into_iter()
        .filter(|t| t.starts_with("<label"))
        .find(|t| attr(t, "for") == Some("sw1"));
    assert!(label.is_some(), "the switch label points at sw1");
}

#[test]
fn time_uses_compact_input_group() {
    let html = render(|| {
        view! { <RgTime label="Start" date=Timestamp::now() show_seconds=true listener=|_: Timestamp| {}/> }
    });
    let group_tag = tags_with_class(&html, "rg-input-group")[0];
    assert!(classes_of(group_tag).contains(&"rg-input-group-sm"));
    let group = element_html(&html, "div", "rg-input-group").unwrap();
    assert_eq!(tags_with_class(group, "rg-input").len(), 3);
    let addons: Vec<String> = group
        .split("<span class=\"rg-input-addon\">")
        .skip(1)
        .map(|s| text(&s[..s.find("</span>").unwrap()]))
        .collect();
    assert_eq!(addons, vec!["Start", ":", ":"]);
    for bootstrap in ["form-control", "input-group", "input-group-text"] {
        assert!(
            tags_with_class(&html, bootstrap).is_empty(),
            "no Bootstrap classes left: {bootstrap}"
        );
    }
}

#[test]
fn spinner_is_an_inline_svg() {
    let html = render(|| view! { <RgSpinner/> });
    let svg = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<svg"))
        .unwrap();
    assert_eq!(attr(svg, "width"), Some("50"));
    assert!(html.contains("<animateTransform"));
    assert!(html.contains("stroke-dasharray=\"100.53096491487338\""));
}

#[test]
fn cell_lines_and_score_lines() {
    use regatta_app::utils::standings_report::{ScoreLine, ScoreSegment};
    let html = render(|| {
        view! {
            <td><CellLines lines=vec!["3(PHRF), ".to_string(), "4(ORC)".to_string()]/></td>
            <td><ScoreLines lines=vec![ScoreLine::new(vec![ScoreSegment::bold("3", vec!["text-danger-fg"]), ScoreSegment::new("17")], " (PHRF)")]/></td>
        }
    });
    assert_eq!(count(&html, "<br"), 1);
    assert!(html.contains("<b class=\"text-danger-fg\">3</b>"), "{html}");
    assert!(html.contains("<span>17</span>"), "{html}");
    assert!(text(&html).contains("3(PHRF), 4(ORC)"));
    assert!(text(&html).ends_with(" (PHRF)"), "{}", text(&html));
}

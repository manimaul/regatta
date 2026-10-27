//! = `ModalTest.kt`, the markup half: modals are native <dialog>s opened by id, with the panel / header / body /
//! footer structure and no Bootstrap data-bs-* attributes. Opening, closing and the confirmation flow are in `tests/wasm.rs`.
#![cfg(all(not(target_arch = "wasm32"), feature = "ssr"))]

mod common;

use common::*;
use leptos::prelude::*;
use regatta_app::components::*;
use regatta_app::state::alerts::{use_alerts, CONFIRMATION_MODAL_ID};

#[test]
fn modal_button_and_body_render_by_id() {
    let html = render(|| {
        view! {
            <RgModalButton id="m1" button_label="Open"/>
            <RgModalBody id="m1" modal_title="Edit boat" footer=Box::new(|| view! {
                <RgModalFooterRow>
                    <RgButton label="Clear" style=RgButtonStyle::PrimaryOutline click=|()| {}/>
                    <RgButton label="Save" style=RgButtonStyle::Success closes_modal=true click=|()| {}/>
                </RgModalFooterRow>
            }.into_any())>"body"</RgModalBody>
        }
    });
    let dialog = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<dialog"))
        .expect("dialog");
    assert!(classes_of(dialog).contains(&"rg-modal"));
    assert_eq!(attr(dialog, "id"), Some("m1"));
    assert_eq!(attr(dialog, "aria-labelledby"), Some("m1-title"));
    assert!(!dialog.contains(" open"), "closed until opened");
    let title = tags(&html)
        .into_iter()
        .find(|t| attr(t, "id") == Some("m1-title"))
        .expect("title");
    assert!(title.starts_with("<h5") && classes_of(title).contains(&"rg-modal-title"));
    assert_eq!(
        text(element_html(&html, "h5", "rg-modal-title").unwrap()),
        "Edit boat"
    );
    assert!(!html.contains("data-bs-"), "no Bootstrap data attributes");
    for cls in [
        "rg-modal-panel",
        "rg-modal-header",
        "rg-modal-close",
        "rg-modal-body",
        "rg-modal-footer",
        "rg-modal-footer-row",
    ] {
        assert_eq!(tags_with_class(&html, cls).len(), 1, "{cls}");
    }
    let close = tags_with_class(&html, "rg-modal-close")[0];
    assert_eq!(attr(close, "aria-label"), Some("Close"));
    assert_eq!(attr(close, "type"), Some("button"));
    let open = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<button"))
        .unwrap();
    assert_eq!(attr(open, "class"), Some("rg-btn rg-btn-outline-success"));
}

#[test]
fn rg_modal_numbers_its_dialog_and_large_panel_class() {
    let html = render(|| {
        view! {
            <RgModal button_label="Open" modal_title="Time">"body"</RgModal>
            <RgModalBody id="big" modal_title="Wide" large=true>"table"</RgModalBody>
        }
    });
    let dialogs: Vec<&str> = tags(&html)
        .into_iter()
        .filter(|t| t.starts_with("<dialog"))
        .collect();
    assert_eq!(dialogs.len(), 2);
    assert!(attr(dialogs[0], "id").unwrap().starts_with("modal-"));
    assert_eq!(tags_with_class(&html, "rg-modal-panel").len(), 2);
    assert_eq!(tags_with_class(&html, "rg-modal-panel-lg").len(), 1);
    assert_eq!(
        tags_with_class(&html, "rg-modal-footer").len(),
        0,
        "no footer when none is given"
    );
}

#[test]
fn confirmation_dialog_shows_the_question() {
    let html = render(|| {
        let alerts = use_alerts();
        alerts.confirm(
            "Are you sure?",
            Some("Delete Kahuna?".to_string()),
            |_: bool| {},
        );
        view! { <Alerts/> }
    });
    let dialog = tags(&html)
        .into_iter()
        .find(|t| t.starts_with("<dialog"))
        .expect("dialog");
    assert_eq!(attr(dialog, "id"), Some(CONFIRMATION_MODAL_ID));
    assert_eq!(
        text(element_html(&html, "h5", "rg-modal-title").unwrap()),
        "Are you sure?"
    );
    assert!(html.contains("<h6>Delete Kahuna?</h6>"), "{html}");
    let labels: Vec<String> = element_html(&html, "div", "rg-modal-footer-row")
        .unwrap()
        .split("</button>")
        .filter_map(|s| s.rfind('>').map(|i| s[i + 1..].to_string()))
        .filter(|s| !s.is_empty())
        .collect();
    assert_eq!(labels, vec!["No", "Yes"]);
}

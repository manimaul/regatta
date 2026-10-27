//! The interactive halves of `ModalTest.kt`, `LeafComponentsTest.kt`, `NavBarTest.kt` and
//! `ThemeToggleTest.kt`: dialogs open and close, dropdowns dismiss, the hamburger and the theme
//! toggle flip. Run in a browser with `wasm-pack test --headless --chrome crates/app --features hydrate`
//! (or compile-check with `cargo test -p regatta-app --features hydrate --target wasm32-unknown-unknown --no-run`).
#![cfg(target_arch = "wasm32")]

use leptos::prelude::*;
use leptos_router::components::Router;
use regatta_app::components::*;
use regatta_app::state::alerts::{use_alerts, CONFIRMATION_MODAL_ID};
use regatta_app::state::theme::{provide_theme, Theme, THEME_STORAGE_KEY};
use wasm_bindgen::JsCast;
use wasm_bindgen_test::*;
use web_sys::{
    Event, EventInit, HtmlDialogElement, HtmlElement, HtmlInputElement, KeyboardEvent,
    KeyboardEventInit, MouseEvent, MouseEventInit,
};

wasm_bindgen_test_configure!(run_in_browser);

fn mount<V: IntoView + 'static>(f: impl FnOnce() -> V + 'static) -> HtmlElement {
    let root = document()
        .create_element("div")
        .unwrap()
        .dyn_into::<HtmlElement>()
        .unwrap();
    document().body().unwrap().append_child(&root).unwrap();
    let r = root.clone();
    let handle = mount_to(r, f);
    std::mem::forget(handle);
    root
}

fn q(root: &HtmlElement, selector: &str) -> Option<HtmlElement> {
    root.query_selector(selector)
        .ok()
        .flatten()
        .and_then(|e| e.dyn_into::<HtmlElement>().ok())
}

fn qa(root: &HtmlElement, selector: &str) -> Vec<HtmlElement> {
    let list = root.query_selector_all(selector).unwrap();
    (0..list.length())
        .filter_map(|i| list.item(i))
        .filter_map(|n| n.dyn_into::<HtmlElement>().ok())
        .collect()
}

fn button(root: &HtmlElement, label: &str) -> HtmlElement {
    qa(root, "button")
        .into_iter()
        .find(|b| b.text_content().as_deref() == Some(label))
        .unwrap_or_else(|| panic!("button {label}"))
}

fn press_on(target: &web_sys::EventTarget) {
    let init = EventInit::new();
    init.set_bubbles(true);
    init.set_cancelable(true);
    target
        .dispatch_event(&Event::new_with_event_init_dict("pointerdown", &init).unwrap())
        .unwrap();
}

fn click_on(target: &web_sys::EventTarget) {
    let init = MouseEventInit::new();
    init.set_bubbles(true);
    target
        .dispatch_event(&MouseEvent::new_with_mouse_event_init_dict("click", &init).unwrap())
        .unwrap();
}

fn escape() -> KeyboardEvent {
    let init = KeyboardEventInit::new();
    init.set_key("Escape");
    init.set_bubbles(true);
    init.set_cancelable(true);
    let event = KeyboardEvent::new_with_keyboard_event_init_dict("keydown", &init).unwrap();
    document().body().unwrap().dispatch_event(&event).unwrap();
    event
}

fn type_into(input: &HtmlInputElement, text: &str) {
    input.set_value(text);
    let init = EventInit::new();
    init.set_bubbles(true);
    input
        .dispatch_event(&Event::new_with_event_init_dict("input", &init).unwrap())
        .unwrap();
}

async fn tick() {
    let promise = js_sys::Promise::new(&mut |resolve, _| {
        window()
            .set_timeout_with_callback_and_timeout_and_arguments_0(&resolve, 20)
            .unwrap();
    });
    let _ = wasm_bindgen_futures::JsFuture::from(promise).await;
}

// ---- ModalTest

#[wasm_bindgen_test]
async fn modal_button_opens_by_id_and_closes_modal_buttons_close() {
    let opened = RwSignal::new(0);
    let saved = RwSignal::new(0);
    let root = mount(move || {
        view! {
            <RgModalButton id="wm1" button_label="Open" open_action=move |()| opened.update(|o| *o += 1)/>
            <RgModalBody id="wm1" modal_title="Edit boat" footer=Box::new(move || view! {
                <RgModalFooterRow>
                    <RgButton label="Clear" style=RgButtonStyle::PrimaryOutline click=|()| {}/>
                    <RgButton label="Save" style=RgButtonStyle::Success closes_modal=true click=move |()| saved.update(|s| *s += 1)/>
                </RgModalFooterRow>
            }.into_any())>"body"</RgModalBody>
        }
    });
    tick().await;
    let dlg = q(&root, "dialog")
        .unwrap()
        .dyn_into::<HtmlDialogElement>()
        .unwrap();
    assert!(dlg.class_list().contains("rg-modal"));
    assert_eq!(
        document()
            .get_element_by_id("wm1-title")
            .unwrap()
            .text_content()
            .as_deref(),
        Some("Edit boat")
    );
    assert!(root
        .query_selector("[data-bs-toggle], [data-bs-target], [data-bs-dismiss]")
        .unwrap()
        .is_none());
    assert!(!dlg.open());

    button(&root, "Open").click();
    assert!(dlg.open());
    assert_eq!(opened.get_untracked(), 1);

    button(&dlg.clone().unchecked_into(), "Clear").click();
    assert!(
        dlg.open(),
        "buttons without closes_modal leave the modal open"
    );

    button(&dlg.clone().unchecked_into(), "Save").click();
    assert_eq!(
        saved.get_untracked(),
        1,
        "the click handler runs before the modal closes"
    );
    assert!(!dlg.open());
}

#[wasm_bindgen_test]
async fn close_button_and_backdrop_close() {
    let root = mount(|| view! { <RgModal button_label="Open" modal_title="Time">"body"</RgModal> });
    tick().await;
    let dlg = q(&root, "dialog")
        .unwrap()
        .dyn_into::<HtmlDialogElement>()
        .unwrap();
    let panel = q(&root, ".rg-modal-panel").unwrap();

    button(&root, "Open").click();
    button(&dlg.clone().unchecked_into(), "×").click();
    assert!(!dlg.open(), "× closes");

    button(&root, "Open").click();
    press_on(&panel);
    click_on(&dlg);
    assert!(
        dlg.open(),
        "a press that starts inside the panel and ends on the backdrop keeps it open"
    );

    press_on(&dlg);
    click_on(&dlg);
    assert!(!dlg.open(), "a backdrop click closes");
}

#[wasm_bindgen_test]
async fn confirmation_answers_and_dismissal() {
    let answer = RwSignal::new(None::<bool>);
    let alerts_cell = RwSignal::new(None::<regatta_app::state::alerts::AlertsState>);
    let root = mount(move || {
        let alerts = use_alerts();
        alerts_cell.set(Some(alerts));
        view! { <Alerts/> }
    });
    tick().await;
    let alerts = alerts_cell.get_untracked().unwrap();
    let dlg = q(&root, &format!("#{CONFIRMATION_MODAL_ID}"))
        .unwrap()
        .dyn_into::<HtmlDialogElement>()
        .unwrap();
    assert!(alerts.confirmation_modal_mounted.get_untracked());

    alerts.confirm(
        "Are you sure?",
        Some("Delete Kahuna?".to_string()),
        move |yes: bool| answer.set(Some(yes)),
    );
    tick().await;
    assert!(dlg.open() && alerts.confirmation.get_untracked().is_some());
    assert_eq!(
        document()
            .get_element_by_id(&format!("{CONFIRMATION_MODAL_ID}-title"))
            .unwrap()
            .text_content()
            .as_deref(),
        Some("Are you sure?")
    );

    button(&dlg.clone().unchecked_into(), "Yes").click();
    assert_eq!(answer.get_untracked(), Some(true));
    assert!(!dlg.open());
    assert!(
        alerts.confirmation.get_untracked().is_none(),
        "answered confirmation cleared"
    );

    // Esc / backdrop dismiss without answering (as with Bootstrap), and the stale question is dropped
    answer.set(None);
    alerts.confirm("Delete again?", None, move |yes: bool| {
        answer.set(Some(yes))
    });
    tick().await;
    assert!(dlg.open() && alerts.confirmation.get_untracked().is_some());
    dlg.close();
    tick().await;
    assert!(
        alerts.confirmation.get_untracked().is_none(),
        "dismissed confirmation cleared"
    );
    assert_eq!(answer.get_untracked(), None);
}

// ---- LeafComponentsTest

#[wasm_bindgen_test]
async fn dropdown_opens_selects_and_closes() {
    let picked = RwSignal::new(None::<String>);
    let root = mount(move || {
        view! { <RgDropdown items=vec!["Alpha".to_string(), "Bravo".to_string(), "Charlie".to_string()] selected_item="Alpha".to_string() name=|s: &String| s.clone() handler=move |s: String| picked.set(Some(s))/> }
    });
    tick().await;
    let toggle = q(&root, ".rg-dropdown > button").unwrap();
    assert!(
        toggle.class_list().contains("rg-btn")
            && toggle.class_list().contains("rg-btn-primary")
            && toggle.class_list().contains("rg-dropdown-toggle")
    );
    assert!(q(&root, ".rg-dropdown-menu").is_none());

    toggle.click();
    tick().await;
    let items = qa(&root, ".rg-dropdown-menu .rg-dropdown-item");
    assert_eq!(
        items
            .iter()
            .map(|i| i.text_content().unwrap_or_default())
            .collect::<Vec<_>>(),
        vec!["Alpha", "Bravo", "Charlie"]
    );
    assert_eq!(
        toggle.get_attribute("aria-expanded").as_deref(),
        Some("true")
    );

    items[1].click();
    tick().await;
    assert_eq!(picked.get_untracked().as_deref(), Some("Bravo"));
    assert!(q(&root, ".rg-dropdown-menu").is_none());
}

#[wasm_bindgen_test]
async fn dropdown_closes_on_outside_press_but_not_inside_press() {
    let root = mount(
        || view! { <RgDropdown items=vec!["Alpha", "Bravo"] selected_item="Alpha" name=|s: &&str| s.to_string() handler=|_: &str| {}/> },
    );
    tick().await;
    q(&root, ".rg-dropdown > button").unwrap().click();
    tick().await;

    press_on(&q(&root, ".rg-dropdown-menu").unwrap());
    tick().await;
    assert!(
        q(&root, ".rg-dropdown-menu").is_some(),
        "a press inside the menu keeps it open"
    );

    press_on(&document().body().unwrap());
    tick().await;
    assert!(
        q(&root, ".rg-dropdown-menu").is_none(),
        "a press outside closes it"
    );
}

#[wasm_bindgen_test]
async fn dropdown_closes_on_escape_and_consumes_it() {
    let root = mount(
        || view! { <RgDropdown items=vec!["Alpha", "Bravo"] selected_item="Alpha" name=|s: &&str| s.to_string() handler=|_: &str| {}/> },
    );
    tick().await;
    q(&root, ".rg-dropdown > button").unwrap().click();
    tick().await;

    let event = escape();
    tick().await;
    assert!(q(&root, ".rg-dropdown-menu").is_none());
    assert!(
        event.default_prevented(),
        "Escape is consumed so an enclosing <dialog> stays open"
    );

    let later = escape();
    assert!(
        !later.default_prevented(),
        "a closed dropdown no longer listens"
    );
}

#[wasm_bindgen_test]
async fn dropdown_none_filters_and_selects_none() {
    let picked = RwSignal::new(Some("unset".to_string()));
    let root = mount(move || {
        view! { <RgDropdownNone items=vec!["Zulu Tango".to_string(), "Kahuna".to_string(), "Grace E".to_string()] selected_item=Some("Kahuna".to_string()) name=|s: &String| s.clone() handler={move |s: Option<String>| picked.set(s)}/> }
    });
    tick().await;
    q(&root, ".rg-dropdown > button").unwrap().click();
    tick().await;
    assert!(q(
        &root,
        ".rg-dropdown-menu .rg-dropdown-filter > hr.rg-dropdown-divider"
    )
    .is_some());

    // the filter and its divider sit in a sticky wrapper so they stay put while a long list scrolls under them
    let filter = q(
        &root,
        ".rg-dropdown-menu .rg-dropdown-filter input.rg-input",
    )
    .unwrap()
    .dyn_into::<HtmlInputElement>()
    .unwrap();
    assert!(
        filter.class_list().contains("my-2"),
        "Bootstrap my-2 (.5rem) maps to Tailwind my-2"
    );
    type_into(&filter, "zulu");
    tick().await;
    assert_eq!(
        qa(&root, ".rg-dropdown-item")
            .iter()
            .map(|i| i.text_content().unwrap_or_default())
            .collect::<Vec<_>>(),
        vec!["None", "Zulu Tango"]
    );

    qa(&root, ".rg-dropdown-item")[0].click();
    tick().await;
    assert_eq!(picked.get_untracked(), None);
    assert!(q(&root, ".rg-dropdown-menu").is_none());

    // the filter is cleared when the menu closes
    q(&root, ".rg-dropdown > button").unwrap().click();
    tick().await;
    assert_eq!(qa(&root, ".rg-dropdown-item").len(), 4);
}

#[wasm_bindgen_test]
async fn number_input_reports_only_valid_numbers() {
    let seen = RwSignal::new(Vec::<f64>::new());
    let root = mount(
        move || view! { <RgNumberInput label="PHRF Rating" value=Some(72.0) listener=move |n: f64| seen.update(|s| s.push(n))/> },
    );
    tick().await;
    let input = q(&root, "input")
        .unwrap()
        .dyn_into::<HtmlInputElement>()
        .unwrap();
    assert!(input.class_list().contains("rg-input"));
    assert_eq!(input.value(), "72");
    type_into(&input, "-");
    type_into(&input, "-6");
    type_into(&input, "abc");
    assert_eq!(seen.get_untracked(), vec![-6.0]);
}

#[wasm_bindgen_test]
async fn input_with_button_reports_typing_and_clicks() {
    let events = RwSignal::new(Vec::<(String, bool)>::new());
    let value = RwSignal::new(String::new());
    let root = mount(move || {
        view! {
            <RgInputWithButton label="ORC Reference Number" value=value btn_label="Add Cert" listener={move |(v, clicked): (String, bool)| {
                events.update(|e| e.push((v.clone(), clicked)));
                if !clicked {
                    value.set(v);
                }
            }}/>
        }
    });
    tick().await;
    let input = q(&root, ".rg-input-group input.rg-input")
        .unwrap()
        .dyn_into::<HtmlInputElement>()
        .unwrap();
    type_into(&input, " 04560004KJP ");
    tick().await;
    button(&root, "Add Cert").click();
    assert_eq!(
        events.get_untracked(),
        vec![
            ("04560004KJP".to_string(), false),
            ("04560004KJP".to_string(), true)
        ]
    );
}

#[wasm_bindgen_test]
async fn check_and_switch_report() {
    let checked = RwSignal::new(false);
    let switched = RwSignal::new(None::<bool>);
    let root = mount(move || {
        view! {
            <RgCheck label="Club member" checked=false listener=move |v: bool| checked.set(v)/>
            <RgSwitch id="sw" num=1 label="PHRF" check=true change=move |v: bool| switched.set(Some(v))/>
        }
    });
    tick().await;
    let checks = qa(&root, ".rg-check");
    assert_eq!(checks.len(), 2);
    let init = EventInit::new();
    init.set_bubbles(true);

    let box_ = q(&checks[0], "input.rg-checkbox")
        .unwrap()
        .dyn_into::<HtmlInputElement>()
        .unwrap();
    box_.set_checked(true);
    box_.dispatch_event(&Event::new_with_event_init_dict("change", &init).unwrap())
        .unwrap();
    assert!(checked.get_untracked());

    let switch = q(&checks[1], "input.rg-switch")
        .unwrap()
        .dyn_into::<HtmlInputElement>()
        .unwrap();
    assert!(switch.checked());
    assert_eq!(
        q(&checks[1], "label")
            .unwrap()
            .get_attribute("for")
            .as_deref(),
        Some("sw1")
    );
    switch.set_checked(false);
    switch
        .dispatch_event(&Event::new_with_event_init_dict("change", &init).unwrap())
        .unwrap();
    assert_eq!(switched.get_untracked(), Some(false));
}

// ---- NavBarTest

#[wasm_bindgen_test]
async fn hamburger_toggles_and_link_click_closes_menu() {
    let start_url = window().location().href().unwrap();
    let root = mount(|| view! { <Router><NavBar/></Router> });
    tick().await;
    let toggler = q(&root, ".rg-navbar-toggler").unwrap();
    let collapse = q(&root, ".rg-navbar-collapse").unwrap();
    assert_eq!(
        collapse.get_attribute("data-open").as_deref(),
        Some("false")
    );
    assert_eq!(
        toggler.get_attribute("aria-expanded").as_deref(),
        Some("false")
    );

    toggler.click();
    tick().await;
    assert_eq!(collapse.get_attribute("data-open").as_deref(), Some("true"));
    assert_eq!(
        toggler.get_attribute("aria-expanded").as_deref(),
        Some("true")
    );

    toggler.click();
    tick().await;
    assert_eq!(
        collapse.get_attribute("data-open").as_deref(),
        Some("false"),
        "the hamburger closes it again"
    );

    toggler.click();
    tick().await;
    let course = qa(&root, ".rg-nav-link")
        .into_iter()
        .find(|l| l.text_content().as_deref() == Some("Course"))
        .unwrap();
    course.click();
    tick().await;
    assert_eq!(
        collapse.get_attribute("data-open").as_deref(),
        Some("false"),
        "choosing a link closes the menu"
    );
    assert_eq!(
        toggler.get_attribute("aria-expanded").as_deref(),
        Some("false")
    );
    assert!(window().location().pathname().unwrap().ends_with("/course"));
    window()
        .history()
        .unwrap()
        .replace_state_with_url(&wasm_bindgen::JsValue::NULL, "", Some(&start_url))
        .unwrap();
}

// ---- ThemeToggleTest

#[wasm_bindgen_test]
async fn toggle_switches_theme_and_remembers_it() {
    let storage = window().local_storage().unwrap().unwrap();
    storage.remove_item(THEME_STORAGE_KEY).unwrap();
    let root = mount(|| {
        provide_theme(Theme::Light);
        view! { <ThemeToggle/> }
    });
    tick().await;
    let html = document().document_element().unwrap();
    assert_eq!(
        html.get_attribute("data-theme").as_deref(),
        Some("light"),
        "defaults to light with no saved choice"
    );
    let toggle = q(&root, ".rg-theme-toggle").unwrap();
    assert_eq!(
        toggle.get_attribute("aria-checked").as_deref(),
        Some("false")
    );

    toggle.click();
    tick().await;
    assert_eq!(
        toggle.get_attribute("aria-checked").as_deref(),
        Some("true")
    );
    assert_eq!(html.get_attribute("data-theme").as_deref(), Some("dark"));
    assert_eq!(
        storage.get_item(THEME_STORAGE_KEY).unwrap().as_deref(),
        Some("dark")
    );
    assert!(
        document()
            .unchecked_into::<web_sys::HtmlDocument>()
            .cookie()
            .unwrap()
            .contains("theme=dark"),
        "the cookie follows the toggle"
    );
    assert_eq!(
        regatta_app::state::theme::initial_theme(),
        Theme::Dark,
        "a fresh load picks up the saved choice"
    );

    toggle.click();
    tick().await;
    assert_eq!(
        toggle.get_attribute("aria-checked").as_deref(),
        Some("false")
    );
    assert_eq!(html.get_attribute("data-theme").as_deref(), Some("light"));
    storage.remove_item(THEME_STORAGE_KEY).unwrap();
}

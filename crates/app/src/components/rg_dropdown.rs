//! = `RgDropdown.kt`: the dropdown buttons (`RgDropdown`, `RgIdDropdown`, `RgDropdownNone`) and
//! the typed pickers built on them. The menu is `position: fixed`, placed against its toggle and
//! capped to the room on screen; it dismisses on an outside press or Escape (consumed, so a
//! dropdown inside a modal closes only itself).

use crate::app_style;
use crate::cb::Cb;
use crate::components::rg_button::RgButtonStyle;
use crate::components::rg_form::RgInput;
use crate::utils::item_name_contains_filter;
use leptos::either::Either;
use leptos::prelude::*;
use regatta_common::{Person, RaceClass, RaceClassBrackets, Series};
use std::sync::Arc;

/// A `name(&T) -> String` prop; closures convert with `.into()` through `#[prop(into)]`.
#[derive(Clone)]
pub struct Namer<T>(pub Arc<dyn Fn(&T) -> String + Send + Sync>);

impl<T, F> From<F> for Namer<T>
where
    F: Fn(&T) -> String + Send + Sync + 'static,
{
    fn from(f: F) -> Self {
        Namer(Arc::new(f))
    }
}

impl<T> Namer<T> {
    pub fn call(&self, item: &T) -> String {
        (self.0)(item)
    }
}

/// Below this much room (px) the menu opens upward when there is more space above the toggle.
pub const MIN_MENU_HEIGHT: f64 = 160.0;
/// Gap (px) kept between the menu and the viewport edge.
pub const VIEWPORT_GAP: f64 = 16.0;
/// Gap (px) between the toggle and the menu.
pub const TOGGLE_GAP: f64 = 2.0;

#[cfg(feature = "hydrate")]
mod dom {
    use super::{MIN_MENU_HEIGHT, TOGGLE_GAP, VIEWPORT_GAP};
    use leptos::prelude::{document, window};
    use wasm_bindgen::prelude::*;
    use wasm_bindgen::JsCast;
    use web_sys::{Element, Event, HtmlElement, KeyboardEvent, Node};

    /// Places the fixed-position menu against its toggle button (the previous sibling) in viewport
    /// coordinates and caps it to the room left on screen, so a long list scrolls inside the menu
    /// rather than the page and nothing between the menu and the viewport (an overflow-x: auto table
    /// wrapper, say) can clip it. Opens upward when the toggle sits near the bottom of the screen with
    /// more room above.
    pub fn place_menu(menu: &HtmlElement) {
        let Some(toggle) = menu
            .previous_element_sibling()
            .map(|t| t.get_bounding_client_rect())
        else {
            return;
        };
        let viewport_height = window()
            .inner_height()
            .ok()
            .and_then(|v| v.as_f64())
            .unwrap_or(0.0);
        let viewport_width = window()
            .inner_width()
            .ok()
            .and_then(|v| v.as_f64())
            .unwrap_or(0.0);
        let below = viewport_height - toggle.bottom() - VIEWPORT_GAP;
        let above = toggle.top() - VIEWPORT_GAP;
        let open_above = below < MIN_MENU_HEIGHT && above > below;
        let style = menu.style();
        let _ = style.set_property(
            "max-height",
            &format!(
                "{}px",
                (if open_above { above } else { below }).max(MIN_MENU_HEIGHT)
            ),
        );
        if open_above {
            let _ = style.set_property("top", "auto");
            let _ = style.set_property(
                "bottom",
                &format!("{}px", viewport_height - toggle.top() + TOGGLE_GAP),
            );
        } else {
            let _ = style.set_property("bottom", "auto");
            let _ = style.set_property("top", &format!("{}px", toggle.bottom() + TOGGLE_GAP));
        }
        // keep the menu's right edge on screen; it is wider than the toggle more often than not
        let width = menu.get_bounding_client_rect().width();
        let _ = style.set_property(
            "left",
            &format!(
                "{}px",
                VIEWPORT_GAP.max(toggle.left().min(viewport_width - width - VIEWPORT_GAP))
            ),
        );
    }

    /// Window listeners that re-place the menu when the page scrolls or the window resizes.
    pub struct Reposition {
        scroll: Closure<dyn FnMut(Event)>,
        resize: Closure<dyn FnMut(Event)>,
    }

    impl Reposition {
        pub fn install(menu: HtmlElement) -> Reposition {
            let m = menu.clone();
            let scroll = Closure::<dyn FnMut(Event)>::new(move |event: Event| {
                let inside = event
                    .target()
                    .and_then(|t| t.dyn_into::<Node>().ok())
                    .map(|n| m.contains(Some(&n)))
                    .unwrap_or(false);
                if !inside {
                    place_menu(&m);
                }
            });
            let m = menu.clone();
            let resize = Closure::<dyn FnMut(Event)>::new(move |_: Event| place_menu(&m));
            let _ = window().add_event_listener_with_callback_and_bool(
                "scroll",
                scroll.as_ref().unchecked_ref(),
                true,
            );
            let _ = window()
                .add_event_listener_with_callback("resize", resize.as_ref().unchecked_ref());
            Reposition { scroll, resize }
        }
    }

    impl Drop for Reposition {
        fn drop(&mut self) {
            let _ = window().remove_event_listener_with_callback_and_bool(
                "scroll",
                self.scroll.as_ref().unchecked_ref(),
                true,
            );
            let _ = window().remove_event_listener_with_callback(
                "resize",
                self.resize.as_ref().unchecked_ref(),
            );
        }
    }

    /// Document listeners (capture phase, so they run before anything inside the page can stop the
    /// event) that dismiss the dropdown on a press outside `root` or on Escape.
    pub struct Dismiss {
        pointer: Closure<dyn FnMut(Event)>,
        key: Closure<dyn FnMut(KeyboardEvent)>,
    }

    impl Dismiss {
        pub fn install(root: Element, dismiss: impl Fn() + Clone + 'static) -> Dismiss {
            let d = dismiss.clone();
            let pointer = Closure::<dyn FnMut(Event)>::new(move |event: Event| {
                if let Some(target) = event.target().and_then(|t| t.dyn_into::<Node>().ok()) {
                    if !root.contains(Some(&target)) {
                        d();
                    }
                }
            });
            let key = Closure::<dyn FnMut(KeyboardEvent)>::new(move |event: KeyboardEvent| {
                if event.key() == "Escape" {
                    event.prevent_default();
                    event.stop_propagation();
                    dismiss();
                }
            });
            let doc = document();
            let _ = doc.add_event_listener_with_callback_and_bool(
                "pointerdown",
                pointer.as_ref().unchecked_ref(),
                true,
            );
            let _ = doc.add_event_listener_with_callback_and_bool(
                "keydown",
                key.as_ref().unchecked_ref(),
                true,
            );
            Dismiss { pointer, key }
        }
    }

    impl Drop for Dismiss {
        fn drop(&mut self) {
            let doc = document();
            let _ = doc.remove_event_listener_with_callback_and_bool(
                "pointerdown",
                self.pointer.as_ref().unchecked_ref(),
                true,
            );
            let _ = doc.remove_event_listener_with_callback_and_bool(
                "keydown",
                self.key.as_ref().unchecked_ref(),
                true,
            );
        }
    }
}

/// While `open`, closes the dropdown on a pointer press outside `root` or on Escape.
fn dismiss_on_outside_or_escape(
    open: Signal<bool>,
    root: NodeRef<leptos::html::Div>,
    dismiss: impl Fn() + Clone + Send + Sync + 'static,
) {
    #[cfg(feature = "hydrate")]
    {
        let listeners = StoredValue::new_local(None::<dom::Dismiss>);
        Effect::new(move |_| {
            if open.get() {
                if let Some(el) = root.get() {
                    let el: web_sys::Element = el.into();
                    listeners.set_value(Some(dom::Dismiss::install(el, dismiss.clone())));
                }
            } else {
                listeners.set_value(None);
            }
        });
        on_cleanup(move || {
            listeners.try_set_value(None);
        });
    }
    #[cfg(not(feature = "hydrate"))]
    {
        let _ = (open, root, dismiss);
    }
}

/// The open menu: an optional non-scrolling `filter` header above the scrolling `items` pane.
#[component]
fn DropdownMenu(
    #[prop(optional_no_strip)] filter: Option<Children>,
    children: Children,
) -> impl IntoView {
    let menu_ref = NodeRef::<leptos::html::Div>::new();
    #[cfg(feature = "hydrate")]
    {
        let reposition = StoredValue::new_local(None::<dom::Reposition>);
        Effect::new(move |_| {
            if let Some(menu) = menu_ref.get() {
                let menu: web_sys::HtmlElement = menu.into();
                dom::place_menu(&menu);
                // a fixed menu doesn't follow its toggle when the page (or the modal's dialog) scrolls or the window resizes
                reposition.set_value(Some(dom::Reposition::install(menu)));
            }
        });
        on_cleanup(move || {
            reposition.try_set_value(None);
        });
    }
    view! {
        <div class="rg-dropdown-menu" node_ref=menu_ref>
            {filter.map(|f| view! { <div class="rg-dropdown-filter">{f()}</div> })}
            <div class="rg-dropdown-items">{children()}</div>
        </div>
    }
}

fn item_label(selected: &str, item_name: String) -> impl IntoView {
    if selected == item_name {
        Either::Left(view! { <b>{item_name}</b> })
    } else {
        Either::Right(item_name)
    }
}

/// A picker with a "None" entry and an optional type-to-filter box.
#[component]
pub fn RgDropdownNone<T>(
    #[prop(into)] items: Signal<Vec<T>>,
    #[prop(into)] selected_item: Signal<Option<T>>,
    #[prop(into)] name: Namer<T>,
    #[prop(default = true)] show_filter: bool,
    #[prop(optional, into)] shor_name: Option<Namer<T>>,
    #[prop(into)] handler: Cb<Option<T>>,
) -> impl IntoView
where
    T: Clone + PartialEq + Send + Sync + 'static,
{
    let filter = RwSignal::new(String::new());
    let toggle = RwSignal::new(false);
    let root = NodeRef::<leptos::html::Div>::new();
    let n = name.clone();
    let selected = Memo::new(move |_| {
        selected_item
            .get()
            .map(|s| n.call(&s))
            .unwrap_or_else(|| "None".to_string())
    });
    let n = name.clone();
    let button_text = move || {
        selected_item
            .get()
            .map(|s| {
                shor_name
                    .as_ref()
                    .map(|f| f.call(&s))
                    .unwrap_or_else(|| n.call(&s))
            })
            .unwrap_or_else(|| "None".to_string())
    };
    dismiss_on_outside_or_escape(toggle.into(), root, move || {
        toggle.set(false);
        filter.set(String::new());
    });
    let toggle_class =
        super::join_classes(&[RgButtonStyle::Primary.classes(), "rg-dropdown-toggle"]);
    let choose = move |item: Option<T>| {
        toggle.set(false);
        filter.set(String::new());
        handler.run(item);
    };
    view! {
        <div class="rg-dropdown" node_ref=root>
            <button class=toggle_class aria-expanded=move || toggle.get().to_string() on:click=move |_| toggle.update(|t| *t = !*t)>
                {button_text}
            </button>
            {move || toggle.get().then(|| {
                let name = name.clone();
                let filter_box: Option<Children> = show_filter.then(|| {
                    Box::new(move || view! {
                        // Bootstrap's my-2 (.5rem) in Tailwind's scale; the wrapper supplies the horizontal spacing
                        <RgInput label="Type to filter" value=filter place_holder=true custom_classes="w-auto my-2" listener=move |v: String| filter.set(v)/>
                        <hr class="rg-dropdown-divider"/>
                    }.into_any()) as Children
                });
                view! {
                    <DropdownMenu filter=filter_box>
                        <button class="rg-dropdown-item" on:click=move |_| choose(None)>"None"</button>
                        {move || {
                            let name = name.clone();
                            items.get().into_iter().filter_map(move |item| {
                                let item_name = name.call(&item);
                                item_name_contains_filter(&item_name, &filter.get()).then(|| {
                                    view! {
                                        <button class="rg-dropdown-item" on:click=move |_| choose(Some(item.clone()))>
                                            {item_label(&selected.get(), item_name.clone())}
                                        </button>
                                    }
                                })
                            }).collect_view()
                        }}
                    </DropdownMenu>
                }
            })}
        </div>
    }
}

#[component]
pub fn RgDropdown<T>(
    #[prop(into)] items: Signal<Vec<T>>,
    #[prop(into)] selected_item: Signal<T>,
    #[prop(into)] name: Namer<T>,
    #[prop(into)] handler: Cb<T>,
) -> impl IntoView
where
    T: Clone + PartialEq + Send + Sync + 'static,
{
    view! { <RgIdDropdown items=items selected_item=selected_item name=name handler=handler/> }
}

#[component]
pub fn RgIdDropdown<T>(
    #[prop(optional, into)] id: Option<String>,
    #[prop(into)] items: Signal<Vec<T>>,
    #[prop(into)] selected_item: Signal<T>,
    #[prop(into)] name: Namer<T>,
    #[prop(into)] handler: Cb<T>,
) -> impl IntoView
where
    T: Clone + PartialEq + Send + Sync + 'static,
{
    let toggle = RwSignal::new(false);
    let root = NodeRef::<leptos::html::Div>::new();
    let n = name.clone();
    let selected = Memo::new(move |_| n.call(&selected_item.get()));
    dismiss_on_outside_or_escape(toggle.into(), root, move || toggle.set(false));
    let toggle_class =
        super::join_classes(&[RgButtonStyle::Primary.classes(), "rg-dropdown-toggle"]);
    view! {
        <div class="rg-dropdown" id=id node_ref=root>
            <button
                class=toggle_class
                aria-expanded=move || toggle.get().to_string()
                disabled=move || items.with(|i| i.len() <= 1)
                on:click=move |_| toggle.update(|t| *t = !*t)
            >
                {move || selected.get()}
            </button>
            {move || toggle.get().then(|| {
                let name = name.clone();
                view! {
                    <DropdownMenu>
                        {move || {
                            let name = name.clone();
                            items.get().into_iter().map(move |item| {
                                let item_name = name.call(&item);
                                view! {
                                    <button class="rg-dropdown-item" on:click=move |_| { toggle.set(false); handler.run(item.clone()); }>
                                        {item_label(&selected.get(), item_name.clone())}
                                    </button>
                                }
                            }).collect_view()
                        }}
                    </DropdownMenu>
                }
            })}
        </div>
    }
}

#[component]
pub fn RgSeriesDropdown(
    #[prop(into)] series_list: Signal<Vec<Series>>,
    #[prop(into)] series: Signal<Option<Series>>,
    #[prop(into)] handler: Cb<Series>,
) -> impl IntoView {
    view! {
        <RgDropdownNone
            items=series_list
            selected_item=series
            name=|s: &Series| s.name.clone()
            show_filter=true
            shor_name=Namer::from(|s: &Series| s.name.clone())
            handler={move |s: Option<Series>| if let Some(s) = s { handler.run(s) }}
        />
    }
}

#[component]
pub fn RgClassDropdown(
    #[prop(into)] items: Signal<Vec<RaceClassBrackets>>,
    #[prop(into)] current: Signal<Option<RaceClass>>,
    #[prop(into)] handler: Cb<Option<RaceClassBrackets>>,
) -> impl IntoView {
    let cb = Signal::derive(move || {
        let current_id = current.get().map(|c| c.id);
        items
            .get()
            .into_iter()
            .find(|i| Some(i.race_class.id) == current_id)
    });
    view! {
        <RgDropdownNone
            items=items
            selected_item=cb
            name=|c: &RaceClassBrackets| c.race_class.name.clone()
            handler={move |c: Option<RaceClassBrackets>| if c.is_some() { handler.run(c) }}
        />
    }
}

#[component]
pub fn RgSkipperDropdown(
    #[prop(into)] people: Signal<Vec<Person>>,
    #[prop(into)] person: Signal<Option<Person>>,
    #[prop(into)] handler: Cb<Option<Person>>,
) -> impl IntoView {
    let sorted = Signal::derive(move || {
        let mut p = people.get();
        p.sort_by_key(|a| a.full_name());
        p
    });
    view! {
        <label class=app_style::MARGIN_END><b>"Skipper"</b></label>
        <RgDropdownNone items=sorted selected_item=person name=|p: &Person| p.full_name() handler=handler/>
    }
}

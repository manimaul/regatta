//! Toggle switches.

use crate::cb::Cb;
use leptos::prelude::*;

/// A labelled toggle switch with element id `id` followed by `num`; `change` gets the new state.
#[component]
pub fn RgSwitch(
    #[prop(into)] id: String,
    num: usize,
    #[prop(into)] label: String,
    #[prop(into)] check: Signal<bool>,
    #[prop(into)] change: Cb<bool>,
) -> impl IntoView {
    let id = format!("{id}{num}");
    view! {
        <div class="rg-check">
            <input
                type="checkbox"
                class="rg-switch"
                id=id.clone()
                on:change=move |ev| change.run(event_target_checked(&ev))
                checked=move || check.get()
                prop:checked=move || check.get()
            />
            <label for=id>{label}</label>
        </div>
    }
}

/// One switch per item, labelled by `label` and checked by `check`; `change` gets the item and its new state.
///
/// The group takes the next element id from [`super::ElementIds`] so the server and the browser agree on the ids.
#[component]
pub fn RgSwitches<T>(
    #[prop(into)] items: Signal<Vec<T>>,
    #[prop(into)] check: Cb<T, bool>,
    #[prop(into)] label: Cb<T, String>,
    #[prop(into)] change: Cb<(T, bool)>,
) -> impl IntoView
where
    T: Clone + Send + Sync + 'static,
{
    let group = super::next_input_id();
    view! {
        {move || {
            let group = group.clone();
            items.get().into_iter().enumerate().map(|(i, t)| {
                let item = t.clone();
                let checked = Signal::derive(move || check.run(item.clone()));
                view! {
                    <RgSwitch id=group.clone() num=i label=label.run(t.clone()) check=checked change=move |on: bool| change.run((t.clone(), on))/>
                }
            }).collect_view()
        }}
    }
}

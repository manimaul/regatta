//! = `RgSwitch.kt`.

use crate::cb::Cb;
use leptos::prelude::*;

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

/// One switch per item. The Kotlin ids were a random salt per group; here the group takes the
/// next element id so the server and the browser agree.
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

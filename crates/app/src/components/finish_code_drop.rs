//! = `FinishCodeDrop.kt`.

use crate::cb::Cb;
use crate::components::rg_dropdown::RgDropdown;
use crate::utils::race_report::finish_code_text;
use leptos::prelude::*;
use regatta_common::FinishCode;

#[component]
pub fn FinishCodeDrop(
    #[prop(into)] selected: Signal<FinishCode>,
    #[prop(into)] hoc_position: Signal<Option<i32>>,
    #[prop(default = true)] show_hoc: bool,
    #[prop(optional, into)] custom_classes: super::Cls,
    #[prop(into)] handler: Cb<FinishCode>,
) -> impl IntoView {
    let items: Vec<FinishCode> = FinishCode::ENTRIES
        .into_iter()
        .filter(|c| show_hoc || *c != FinishCode::Hoc)
        .collect();
    let class = (!custom_classes.is_empty()).then(|| custom_classes.0.clone());
    leptos::html::div().attr("class", class).child(view! {
        <RgDropdown items=items selected_item=selected name=move |c: &FinishCode| finish_code_text(*c, hoc_position.get_untracked()) handler=handler/>
    })
}

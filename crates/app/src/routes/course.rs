//! The Course screen: the course marks picker and the chart.
//!
//! The map container renders empty on the server; the browser mounts MapLibre into it once hydrated.

use crate::app_style;
use crate::chart::chart_view_model::{marks, use_chart, Mark};
use crate::components::rg_dropdown::RgDropdown;
use leptos::prelude::*;

#[component]
pub fn Course() -> impl IntoView {
    let chart = use_chart();
    let container = NodeRef::<leptos::html::Div>::new();
    chart.mount(container);
    let selected = Signal::derive(move || chart.mark.get());
    view! {
        <div class=app_style::MARGIN_BOT>
            <RgDropdown
                items={marks().into_iter().map(Some).collect::<Vec<_>>()}
                selected_item=selected
                name={|m: &Option<Mark>| match m {
                    Some(m) => format!("{} {} - {}", m.letter, m.name, m.desc),
                    None => "None".to_string(),
                }}
                handler={move |m: Option<Mark>| chart.choose_mark(m)}
            />
        </div>
        <div class="grow" node_ref=container></div>
    }
}

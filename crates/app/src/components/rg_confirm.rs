//! Inline confirmation and acknowledgement prompts.

use crate::app_style;
use crate::cb::Cb;
use crate::components::rg_button::{RgButton, RgButtonStyle};
use leptos::prelude::*;

/// A question with No and Yes buttons; `handler` gets `true` for Yes.
#[component]
pub fn RgConfirm(
    #[prop(into)] msg: String,
    #[prop(optional, into)] sub_title: Option<String>,
    #[prop(into)] handler: Cb<bool>,
) -> impl IntoView {
    view! {
        <div>
            <h3>{msg}</h3>
            {sub_title.map(|s| view! { <p>{s}</p> })}
            <RgButton label="No" style=RgButtonStyle::Danger custom_classes=app_style::MARGIN_END click=move |()| handler.run(false)/>
            <RgButton label="Yes" style=RgButtonStyle::Primary custom_classes=app_style::MARGIN_START click=move |()| handler.run(true)/>
        </div>
    }
}

/// A message with an Ok button that calls `handler`.
#[component]
pub fn RgOk(
    #[prop(into)] title: String,
    #[prop(optional, into)] sub_title: Option<String>,
    #[prop(into)] handler: Cb<()>,
) -> impl IntoView {
    view! {
        <div>
            <h3>{title}</h3>
            {sub_title.map(|s| view! { <p>{s}</p> })}
            <RgButton label="Ok" style=RgButtonStyle::Primary click=move |()| handler.run(())/>
        </div>
    }
}

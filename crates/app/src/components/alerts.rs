//! The shared confirmation dialog and the success banner, rendered once by the page frame from
//! [`crate::state::alerts`].

use crate::components::modal::{RgModalBody, RgModalFooterRow};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::state::alerts::{use_alerts, CONFIRMATION_MODAL_ID};
use leptos::prelude::*;

#[component]
pub fn Alerts() -> impl IntoView {
    let alerts = use_alerts();
    let title = move || {
        alerts
            .confirmation
            .with(|c| c.as_ref().map(|c| c.msg.clone()).unwrap_or_default())
    };
    let sub_title = move || {
        alerts.confirmation.with(|c| {
            c.as_ref()
                .and_then(|c| c.sub_title.clone())
                .unwrap_or_default()
        })
    };
    view! {
        <RgModalBody
            id=CONFIRMATION_MODAL_ID
            modal_title=title
            footer=Box::new(move || view! {
                <RgModalFooterRow>
                    <RgButton label="No" style=RgButtonStyle::Danger custom_classes=crate::app_style::MARGIN_END click=move |()| alerts.no_confirmation()/>
                    <RgButton label="Yes" style=RgButtonStyle::Primary custom_classes=crate::app_style::MARGIN_START click=move |()| alerts.yes_confirmation()/>
                </RgModalFooterRow>
            }.into_any())
        >
            <h6>{sub_title}</h6>
        </RgModalBody>
        {move || alerts.message.get().map(|message| view! {
            <div class="rg-alert-success" role="alert">
                <svg aria-hidden="true" fill="currentColor" width="16" height="16">
                    <path d="M2.5 8a5.5 5.5 0 0 1 8.25-4.764.5.5 0 0 0 .5-.866A6.5 6.5 0 1 0 14.5 8a.5.5 0 0 0-1 0 5.5 5.5 0 1 1-11 0"></path>
                    <path d="M15.354 3.354a.5.5 0 0 0-.708-.708L8 9.293 5.354 6.646a.5.5 0 1 0-.708.708l3 3a.5.5 0 0 0 .708 0z"></path>
                </svg>
                <div>{message}</div>
            </div>
        })}
    }
}

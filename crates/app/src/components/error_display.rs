//! = `ErrorDisplay.kt`.

use crate::cb::Cb;
use crate::components::rg_button::{RgButton, RgButtonStyle};
use leptos::prelude::*;

#[component]
pub fn ErrorDisplay(#[prop(into)] message: String, #[prop(into)] handler: Cb<()>) -> impl IntoView {
    view! {
        <h1>"Something went wrong"</h1>
        <p><b>{message}</b></p>
        <RgButton label="OK" style=RgButtonStyle::Danger click=handler/>
    }
}

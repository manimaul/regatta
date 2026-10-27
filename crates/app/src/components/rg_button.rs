//! = `RgButton.kt`.

use crate::cb::Cb;
use crate::components::modal::ModalClose;
use leptos::prelude::*;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RgButtonStyle {
    Default,
    #[default]
    Primary,
    Secondary,
    Danger,
    Success,
    Light,
    Dark,
    Link,
    SuccessOutline,
    SecondaryOutline,
    PrimaryOutline,
}

impl RgButtonStyle {
    /// The full class list, spelled out so Tailwind's scanner sees it.
    pub const fn classes(self) -> &'static str {
        match self {
            RgButtonStyle::Default => "rg-btn",
            RgButtonStyle::Primary => "rg-btn rg-btn-primary",
            RgButtonStyle::Secondary => "rg-btn rg-btn-secondary",
            RgButtonStyle::Danger => "rg-btn rg-btn-danger",
            RgButtonStyle::Success => "rg-btn rg-btn-success",
            RgButtonStyle::Light => "rg-btn rg-btn-light",
            RgButtonStyle::Dark => "rg-btn rg-btn-dark",
            RgButtonStyle::Link => "rg-btn rg-btn-link",
            RgButtonStyle::SuccessOutline => "rg-btn rg-btn-outline-success",
            RgButtonStyle::SecondaryOutline => "rg-btn rg-btn-outline-secondary",
            RgButtonStyle::PrimaryOutline => "rg-btn rg-btn-outline-primary",
        }
    }
}

/// A button: `custom_classes` first, then the style's classes (the Kotlin order).
///
/// `closes_modal`: when true and this button is inside an `RgModalBody`, the modal is closed
/// after `click`.
#[component]
pub fn RgButton(
    #[prop(into)] label: TextProp,
    #[prop(optional)] style: RgButtonStyle,
    #[prop(optional, into)] disabled: Signal<bool>,
    #[prop(optional, into)] custom_classes: super::Cls,
    #[prop(optional)] closes_modal: bool,
    #[prop(into)] click: Cb<()>,
) -> impl IntoView {
    let close_modal = use_context::<ModalClose>();
    let class = super::join_classes(&[custom_classes.as_str(), style.classes()]);
    view! {
        <button
            class=class
            disabled=move || disabled.get()
            on:click=move |_| {
                click.run(());
                if closes_modal {
                    if let Some(close) = &close_modal {
                        close.run(());
                    }
                }
            }
        >
            {move || label.get()}
        </button>
    }
}

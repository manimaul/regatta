//! = `Modal.kt`: native `<dialog>` modals opened by id, closed by `RgButton(closes_modal)`, the
//! × button, Escape or a backdrop click.

use crate::cb::Cb;
use crate::components::rg_button::RgButtonStyle;
use crate::state::alerts::{use_alerts, CONFIRMATION_MODAL_ID};
use crate::utils::browser;
use leptos::prelude::*;

/// Closes the enclosing `RgModalBody` (= `LocalModalClose`), provided to the modal's content and
/// footer; `RgButton(closes_modal = true)` reads it.
#[derive(Clone, Copy)]
pub struct ModalClose(pub Cb<()>);

impl ModalClose {
    pub fn run(&self, arg: ()) {
        self.0.run(arg)
    }
}

#[component]
pub fn RgModalButton(
    #[prop(into)] id: String,
    #[prop(default = RgButtonStyle::SuccessOutline)] style: RgButtonStyle,
    #[prop(optional, into)] custom_classes: super::Cls,
    #[prop(into)] button_label: TextProp,
    #[prop(optional, into)] open_action: Option<Cb<()>>,
) -> impl IntoView {
    let class = super::join_classes(&[style.classes(), custom_classes.as_str()]);
    view! {
        <button
            class=class
            on:click=move |_| {
                if let Some(action) = &open_action {
                    action.run(());
                }
                browser::show_modal(&id);
            }
        >
            {move || button_label.get()}
        </button>
    }
}

/// A native `<dialog>` that fills the viewport and scrolls, wrapping a centered panel that doesn't
/// clip its content (so dropdown menus inside it aren't cut off). Mirrors Bootstrap's `.modal` >
/// `.modal-dialog` structure.
///
/// `large` widens the panel on large screens (Bootstrap's `.modal-lg`) for content with tables.
#[component]
pub fn RgModalBody(
    #[prop(into)] id: String,
    #[prop(into)] modal_title: TextProp,
    #[prop(optional)] large: bool,
    children: Children,
    #[prop(optional)] footer: Option<Children>,
) -> impl IntoView {
    let dialog_ref = NodeRef::<leptos::html::Dialog>::new();
    let title_id = format!("{id}-title");
    let labelled_by = title_id.clone();
    let close_id = id.clone();
    let close = ModalClose(Cb::new(move |()| browser::close_dialog(&close_id)));
    provide_context(close);
    // Backdrop click closes, like Bootstrap: only when the press both started and ended outside
    // the panel, so dragging a text selection out of the panel doesn't dismiss it.
    let pressed_on_backdrop = StoredValue::new(false);
    let is_confirmation = id == CONFIRMATION_MODAL_ID;
    let alerts = use_alerts();
    if is_confirmation {
        alerts.confirmation_modal_created();
        on_cleanup(move || alerts.confirmation_modal_destroyed());
    }
    // The close event is queued, so it can arrive after a new `confirm` reopened the dialog; that
    // one is not a dismissal of the new question.
    let on_close_event = move |_| {
        let reopened = dialog_ref
            .get_untracked()
            .map(|d| d.open())
            .unwrap_or(false);
        if is_confirmation && !reopened {
            alerts.confirmation_closed();
        }
    };
    let panel_class = if large {
        "rg-modal-panel rg-modal-panel-lg"
    } else {
        "rg-modal-panel"
    };
    view! {
        <dialog
            class="rg-modal"
            id=id
            aria-labelledby=labelled_by
            node_ref=dialog_ref
            on:pointerdown=move |ev| {
                let on_dialog = dialog_ref
                    .get_untracked()
                    .map(|d| ev.target().as_ref() == Some(d.as_ref()))
                    .unwrap_or(false);
                pressed_on_backdrop.set_value(on_dialog);
            }
            on:click=move |ev| {
                if let Some(dialog) = dialog_ref.get_untracked() {
                    if pressed_on_backdrop.get_value() && ev.target().as_ref() == Some(dialog.as_ref()) {
                        dialog.close();
                    }
                }
            }
            on:close=on_close_event
        >
            <div class=panel_class>
                <div class="rg-modal-header">
                    <h5 class="rg-modal-title" id=title_id>{move || modal_title.get()}</h5>
                    <button type="button" class="rg-modal-close" aria-label="Close" on:click=move |_| close.run(())>
                        "×"
                    </button>
                </div>
                <div class="rg-modal-body">{children()}</div>
                {footer.map(|f| view! { <div class="rg-modal-footer">{f()}</div> })}
            </div>
        </dialog>
    }
}

/// A footer row that spreads its buttons to both ends and wraps them on narrow screens.
#[component]
pub fn RgModalFooterRow(children: Children) -> impl IntoView {
    view! { <div class="rg-modal-footer-row">{children()}</div> }
}

#[component]
pub fn RgModal(
    #[prop(into)] button_label: String,
    #[prop(into)] modal_title: String,
    #[prop(optional, into)] open_action: Option<Cb<()>>,
    #[prop(optional, into)] button_classes: super::Cls,
    children: Children,
    #[prop(optional_no_strip)] footer: Option<Children>,
) -> impl IntoView {
    let modal_id = super::next_modal_id();
    let open_action = open_action.unwrap_or_else(|| Cb::new(|()| {}));
    let body = match footer {
        Some(footer) => leptos::either::Either::Left(view! {
            <RgModalBody id=modal_id.clone() modal_title=modal_title footer=footer>{children()}</RgModalBody>
        }),
        None => leptos::either::Either::Right(view! {
            <RgModalBody id=modal_id.clone() modal_title=modal_title>{children()}</RgModalBody>
        }),
    };
    view! {
        <RgModalButton id=modal_id button_label=button_label custom_classes=button_classes open_action=open_action/>
        {body}
    }
}

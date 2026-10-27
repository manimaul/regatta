//! = `AlertsViewModel.kt`: the success banner and the shared confirmation dialog
//! (`Alerts` component, id `confirmation`).

use crate::cb::Cb;
use crate::utils::browser;
use leptos::prelude::*;

pub const CONFIRMATION_MODAL_ID: &str = "confirmation";

#[derive(Clone)]
pub struct Confirmation {
    pub msg: String,
    pub sub_title: Option<String>,
    pub handler: Cb<bool>,
}

#[derive(Clone, Copy)]
pub struct AlertsState {
    pub message: RwSignal<Option<String>>,
    pub confirmation: RwSignal<Option<Confirmation>>,
    /// Set while an `Alerts` component is mounted (`confirmationModalCreated`).
    pub confirmation_modal_mounted: RwSignal<bool>,
    /// Bumped by every `show_alert`, so a later alert cancels the earlier one's timeout.
    generation: RwSignal<u64>,
}

pub fn provide_alerts() -> AlertsState {
    let state = AlertsState {
        message: RwSignal::new(None),
        confirmation: RwSignal::new(None),
        confirmation_modal_mounted: RwSignal::new(false),
        generation: RwSignal::new(0),
    };
    provide_context(state);
    state
}

pub fn use_alerts() -> AlertsState {
    use_context::<AlertsState>().unwrap_or_else(provide_alerts)
}

impl AlertsState {
    /// Shows the banner for three seconds.
    pub fn show_alert(&self, message: impl Into<String>) {
        self.message.set(Some(message.into()));
        let generation = self.generation.get_untracked() + 1;
        self.generation.set(generation);
        let (message_signal, generation_signal) = (self.message, self.generation);
        browser::set_timeout(
            move || {
                if generation_signal.try_get_untracked() == Some(generation) {
                    message_signal.try_set(None);
                }
            },
            3000,
        );
    }

    pub fn confirm(
        &self,
        msg: impl Into<String>,
        sub_title: Option<String>,
        handler: impl Into<Cb<bool>>,
    ) {
        self.confirmation.set(Some(Confirmation {
            msg: msg.into(),
            sub_title,
            handler: handler.into(),
        }));
        browser::show_modal(CONFIRMATION_MODAL_ID);
    }

    pub fn no_confirmation(&self) {
        if let Some(c) = self.confirmation.get_untracked() {
            c.handler.run(false);
        }
        browser::close_dialog(CONFIRMATION_MODAL_ID);
    }

    pub fn yes_confirmation(&self) {
        if let Some(c) = self.confirmation.get_untracked() {
            c.handler.run(true);
        }
        browser::close_dialog(CONFIRMATION_MODAL_ID);
        self.confirmation.set(None);
    }

    /// Esc / backdrop click close the dialog without answering (as with Bootstrap); drop the stale question.
    pub fn confirmation_closed(&self) {
        self.confirmation.set(None);
    }

    pub fn confirmation_modal_created(&self) {
        self.confirmation_modal_mounted.set(true);
    }

    pub fn confirmation_modal_destroyed(&self) {
        self.confirmation_modal_mounted.set(false);
    }
}

//! The browser calls the components make: `localStorage`, the `<dialog>` lookups, cookies, the clipboard.
//!
//! Real under `hydrate`; on the server they are no-ops that return nothing, so the SSR render and its tests never
//! touch a DOM.

use crate::state::theme::Theme;

#[cfg(feature = "hydrate")]
mod imp {
    use super::Theme;
    use crate::state::theme::THEME_STORAGE_KEY;
    use leptos::prelude::{document, window};
    use wasm_bindgen::JsCast;
    use web_sys::HtmlDialogElement;

    pub fn initial_theme() -> Theme {
        let applied = document()
            .document_element()
            .and_then(|html| html.get_attribute("data-theme"));
        match applied {
            Some(value) => Theme::from_value(Some(&value)),
            None => Theme::from_value(local_storage_get(THEME_STORAGE_KEY).as_deref()),
        }
    }

    pub fn set_document_theme(attr: &str) {
        if let Some(html) = document().document_element() {
            let _ = html.set_attribute("data-theme", attr);
        }
    }

    pub fn local_storage_get(key: &str) -> Option<String> {
        window()
            .local_storage()
            .ok()
            .flatten()?
            .get_item(key)
            .ok()
            .flatten()
    }

    pub fn local_storage_set(key: &str, value: &str) {
        if let Some(storage) = window().local_storage().ok().flatten() {
            let _ = storage.set_item(key, value);
        }
    }

    pub fn local_storage_remove(key: &str) {
        if let Some(storage) = window().local_storage().ok().flatten() {
            let _ = storage.remove_item(key);
        }
    }

    pub fn set_cookie(value: &str) {
        if let Some(doc) = document().dyn_ref::<web_sys::HtmlDocument>() {
            let _ = doc.set_cookie(value);
        }
    }

    pub fn cookies() -> String {
        document()
            .dyn_ref::<web_sys::HtmlDocument>()
            .and_then(|doc| doc.cookie().ok())
            .unwrap_or_default()
    }

    pub fn random_uuid() -> String {
        window()
            .crypto()
            .map(|c| c.random_uuid())
            .unwrap_or_default()
    }

    pub fn dialog(id: &str) -> Option<HtmlDialogElement> {
        document()
            .get_element_by_id(id)
            .and_then(|el| el.dyn_into::<HtmlDialogElement>().ok())
    }

    /// Opens the `<dialog>` with this id unless it is already open.
    pub fn show_modal(id: &str) {
        if let Some(dialog) = dialog(id) {
            if !dialog.open() {
                let _ = dialog.show_modal();
            }
        }
    }

    pub fn close_dialog(id: &str) {
        if let Some(dialog) = dialog(id) {
            dialog.close();
        }
    }

    pub fn set_timeout(cb: impl FnOnce() + 'static, millis: u64) {
        leptos::prelude::set_timeout(cb, std::time::Duration::from_millis(millis));
    }

    pub fn set_interval(cb: impl Fn() + 'static, millis: u64) {
        leptos::prelude::set_interval(cb, std::time::Duration::from_millis(millis));
    }

    pub fn clipboard_write(text: &str) {
        let _ = window().navigator().clipboard().write_text(text);
    }

    pub fn alert(text: &str) {
        let _ = window().alert_with_message(text);
    }

    pub fn history_back() {
        if let Ok(history) = window().history() {
            let _ = history.back();
        }
    }

    pub fn location_href() -> String {
        window().location().href().unwrap_or_default()
    }

    pub fn encode_uri_component(s: &str) -> String {
        js_sys::encode_uri_component(s).into()
    }
}

#[cfg(not(feature = "hydrate"))]
mod imp {
    use super::Theme;

    pub fn initial_theme() -> Theme {
        Theme::Light
    }
    pub fn set_document_theme(_attr: &str) {}
    pub fn local_storage_get(_key: &str) -> Option<String> {
        None
    }
    pub fn local_storage_set(_key: &str, _value: &str) {}
    pub fn local_storage_remove(_key: &str) {}
    pub fn set_cookie(_value: &str) {}
    pub fn cookies() -> String {
        String::new()
    }
    pub fn random_uuid() -> String {
        #[cfg(feature = "ssr")]
        {
            uuid::Uuid::new_v4().to_string()
        }
        #[cfg(not(feature = "ssr"))]
        {
            String::new()
        }
    }
    pub fn show_modal(_id: &str) {}
    pub fn close_dialog(_id: &str) {}
    pub fn set_timeout(_cb: impl FnOnce() + 'static, _millis: u64) {}
    pub fn set_interval(_cb: impl Fn() + 'static, _millis: u64) {}
    pub fn clipboard_write(_text: &str) {}
    pub fn alert(_text: &str) {}
    pub fn history_back() {}
    pub fn location_href() -> String {
        String::new()
    }
    pub fn encode_uri_component(s: &str) -> String {
        s.to_string()
    }
}

pub use imp::*;

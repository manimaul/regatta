//! = `ResizeObserver.kt` + the resize handler of `Main.js.kt`: the app's root div reports its
//! height to the embedding page (`window.parent.postMessage({regatta_scroll_height})`) so an
//! iframe host can size itself.

use leptos::prelude::*;

/// Observes the root element's content height and posts it to the parent window. Browser only.
pub fn observe_scroll_height(root: NodeRef<leptos::html::Div>) {
    #[cfg(feature = "hydrate")]
    {
        use wasm_bindgen::prelude::*;
        use wasm_bindgen::JsCast;
        let observer = StoredValue::new_local(
            None::<(
                web_sys::ResizeObserver,
                Closure<dyn FnMut(js_sys::Array, web_sys::ResizeObserver)>,
            )>,
        );
        Effect::new(move |_| {
            if let Some(div) = root.get() {
                let el: web_sys::Element = div.into();
                let callback = Closure::<dyn FnMut(js_sys::Array, web_sys::ResizeObserver)>::new(
                    move |entries: js_sys::Array, _observer: web_sys::ResizeObserver| {
                        let Some(entry) = entries
                            .get(0)
                            .dyn_into::<web_sys::ResizeObserverEntry>()
                            .ok()
                        else {
                            return;
                        };
                        let Some(size) = entry
                            .content_box_size()
                            .get(0)
                            .dyn_into::<web_sys::ResizeObserverSize>()
                            .ok()
                        else {
                            return;
                        };
                        let h = size.block_size() as i64;
                        let window = leptos::prelude::window();
                        let wh = window
                            .inner_height()
                            .ok()
                            .and_then(|v| v.as_f64())
                            .unwrap_or(0.0);
                        leptos::logging::log!("resize wh={wh} h = {h}");
                        if let Ok(Some(parent)) = window.parent() {
                            let message = js_sys::Object::new();
                            let _ = js_sys::Reflect::set(
                                &message,
                                &JsValue::from_str("regatta_scroll_height"),
                                &JsValue::from_f64(h as f64),
                            );
                            let _ = parent.post_message(&message, "*");
                        }
                    },
                );
                if let Ok(ro) = web_sys::ResizeObserver::new(callback.as_ref().unchecked_ref()) {
                    ro.observe(&el);
                    observer.set_value(Some((ro, callback)));
                }
            }
        });
        on_cleanup(move || {
            if let Some(Some((ro, _))) = observer.try_update_value(|o| o.take()) {
                ro.disconnect();
            }
        });
    }
    #[cfg(not(feature = "hydrate"))]
    let _ = root;
}

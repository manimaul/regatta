//! wasm-bindgen externs to the SortableJS 1.15.2 global `window.Sortable`, for drag-and-drop lists.
//!
//! The library is loaded by a CDN tag in the shell ([`crate::SORTABLE_JS`]), not bundled; the bindings are
//! hydrate only.

#[cfg(feature = "hydrate")]
pub mod imp {
    use js_sys::{Object, Reflect};
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen]
    extern "C" {
        pub type Sortable;

        #[wasm_bindgen(static_method_of = Sortable, js_name = create)]
        pub fn sortable_create(element: &web_sys::Element, args: &JsValue) -> Sortable;

        #[wasm_bindgen(method)]
        pub fn destroy(this: &Sortable);
    }

    /// Makes `element`'s children sortable with the options object `args` (`Sortable.create`).
    pub fn create(element: &web_sys::Element, args: &JsValue) -> Sortable {
        Sortable::sortable_create(element, args)
    }

    /// A drop: the item's index before and after the move.
    #[derive(Debug, Clone, Copy, PartialEq, Eq)]
    pub struct SortEvent {
        pub old_index: usize,
        pub new_index: usize,
    }

    fn index(event: &JsValue, key: &str) -> Option<usize> {
        Reflect::get(event, &JsValue::from_str(key))
            .ok()?
            .as_f64()
            .map(|f| f as usize)
    }

    /// The SortableJS options object with its `onSort` closure.
    ///
    /// The closure lives as long as this handle, so keep it for as long as the list is sortable.
    pub struct SortableArgs {
        pub object: Object,
        _on_sort: Option<Closure<dyn FnMut(JsValue)>>,
    }

    pub fn sortable_args(
        ghost_class: Option<&str>,
        on_sort: Option<Box<dyn FnMut(SortEvent)>>,
    ) -> SortableArgs {
        let object = Object::new();
        if let Some(ghost) = ghost_class {
            let _ = Reflect::set(
                &object,
                &JsValue::from_str("ghostClass"),
                &JsValue::from_str(ghost),
            );
        }
        let on_sort = on_sort.map(|mut f| {
            let closure = Closure::<dyn FnMut(JsValue)>::new(move |event: JsValue| {
                if let (Some(old_index), Some(new_index)) =
                    (index(&event, "oldIndex"), index(&event, "newIndex"))
                {
                    f(SortEvent {
                        old_index,
                        new_index,
                    });
                }
            });
            let _ = Reflect::set(&object, &JsValue::from_str("onSort"), closure.as_ref());
            closure
        });
        SortableArgs {
            object,
            _on_sort: on_sort,
        }
    }
}

#[cfg(feature = "hydrate")]
pub use imp::*;

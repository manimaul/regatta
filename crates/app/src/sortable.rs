//! = `lib/Sortable.kt` + `lib/SortableUtil.kt`: externs to the SortableJS 1.15.2 global
//! (`window.Sortable`, loaded by a CDN tag in the shell; webpack used to bundle it).

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

    /// `Sortable.create(element, options)`.
    pub fn create(element: &web_sys::Element, args: &JsValue) -> Sortable {
        Sortable::sortable_create(element, args)
    }

    /// `SortEvent.oldIndex` / `newIndex`.
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

    /// `sortableArgs(ghostClass, onSort)`: the options object; the closure is kept alive by the
    /// returned handle.
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

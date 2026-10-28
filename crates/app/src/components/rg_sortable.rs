//! A drag-to-reorder list, driven by SortableJS in the browser.

use crate::cb::Cb;
use crate::components::rg_dropdown::Namer;
use leptos::prelude::*;

/// A list of `items` named by `name` that the user reorders by dragging.
///
/// `on_sort` gets the items in their new order after each drop. The server renders the plain list; SortableJS
/// attaches after hydration.
#[component]
pub fn RgSortable<T>(
    items: Vec<T>,
    #[prop(into)] name: Namer<T>,
    #[prop(into)] on_sort: Cb<Vec<T>>,
) -> impl IntoView
where
    T: Clone + Send + Sync + 'static,
{
    let list_ref = NodeRef::<leptos::html::Ul>::new();
    let arranged = StoredValue::new(items.clone());
    #[cfg(feature = "hydrate")]
    {
        let handle = StoredValue::new_local(
            None::<(crate::sortable::Sortable, crate::sortable::SortableArgs)>,
        );
        Effect::new(move |_| {
            if let Some(ul) = list_ref.get() {
                let args = crate::sortable::sortable_args(
                    Some("rg-sortable-ghost"),
                    Some(Box::new(move |event: crate::sortable::SortEvent| {
                        let next = arranged.with_value(|a| {
                            let mut a = a.clone();
                            if event.old_index < a.len() {
                                let e = a.remove(event.old_index);
                                let at = event.new_index.min(a.len());
                                a.insert(at, e);
                            }
                            a
                        });
                        arranged.set_value(next.clone());
                        on_sort.run(next);
                    })),
                );
                let el: web_sys::Element = ul.into();
                let sortable = crate::sortable::create(&el, &args.object);
                handle.set_value(Some((sortable, args)));
            }
        });
        on_cleanup(move || {
            if let Some(Some((sortable, _))) = handle.try_update_value(|h| h.take()) {
                sortable.destroy();
            }
        });
    }
    #[cfg(not(feature = "hydrate"))]
    let _ = (arranged, on_sort);
    view! {
        <ul class="rg-list-group" node_ref=list_ref>
            {items.iter().map(|item| view! { <li class="rg-list-group-item">{name.call(item)}</li> }).collect_view()}
        </ul>
    }
}

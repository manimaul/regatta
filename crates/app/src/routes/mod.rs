//! The screens, one module per route.
//!
//! Server data comes through [`crate::utils::async_state::Loaded`] (a resource over a server function, rendered
//! inside a `<Suspense>` so the server sends the screen with its data) and mutations through
//! [`crate::utils::async_state::call`], reloading the affected data afterwards.

pub mod admin;
pub mod boats;
pub mod classes;
pub mod course;
pub mod home;
pub mod people;
pub mod race_results;
pub mod race_results_view;
pub mod races;
pub mod rc;
pub mod series;
pub mod series_standings;

pub use admin::*;
pub use boats::*;
pub use classes::*;
pub use course::*;
pub use home::*;
pub use people::*;
pub use race_results::*;
pub use race_results_view::*;
pub use races::*;
pub use rc::*;
pub use series::*;
pub use series_standings::*;

use crate::components::error_display::ErrorDisplay;
use crate::components::rg_spinner::RgSpinner;
use crate::utils::async_state::Async;
use leptos::either::EitherOf4;
use leptos::prelude::*;

/// Renders a load's state: the loaded view, an error with a reload button, a spinner, or nothing.
///
/// The error is an `ErrorDisplay` with a reload button; the spinner shows while loading and nothing shows while
/// uninitialized. The match sits in a `<Suspense>` whose fallback is the loading view, so on the server the
/// resource behind `state` is awaited and the chunk arrives rendered; in the browser a reload shows the spinner
/// again.
pub fn async_view<T, V>(
    state: Signal<Async<T>>,
    reload: impl Fn() + Clone + Send + Sync + 'static,
    complete: impl Fn(T) -> V + Clone + Send + Sync + 'static,
) -> impl IntoView
where
    T: Clone + Send + Sync + 'static,
    V: IntoView + 'static,
{
    async_view_loading(state, reload, || view! { <RgSpinner/> }, complete)
}

pub fn async_view_loading<T, V, L>(
    state: Signal<Async<T>>,
    reload: impl Fn() + Clone + Send + Sync + 'static,
    loading: impl Fn() -> L + Clone + Send + Sync + 'static,
    complete: impl Fn(T) -> V + Clone + Send + Sync + 'static,
) -> impl IntoView
where
    T: Clone + Send + Sync + 'static,
    V: IntoView + 'static,
    L: IntoView + 'static,
{
    let fallback = loading.clone();
    view! {
        <Suspense fallback=move || fallback()>
            {move || match state.get() {
                Async::Complete(value) => EitherOf4::A(complete(value)),
                Async::Error { message, .. } => {
                    let reload = reload.clone();
                    EitherOf4::B(view! { <ErrorDisplay message=message handler=move |()| reload()/> })
                }
                Async::Loading(_) => EitherOf4::C(loading()),
                Async::Uninitialized => EitherOf4::D(()),
            }}
        </Suspense>
    }
}

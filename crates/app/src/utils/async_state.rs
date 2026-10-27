//! = `Async.kt`: the load state of a screen's data, fed by [`Loaded`] (a resource over a server
//! function) and [`call`] (a mutation); the screens render a spinner for `Loading`, `ErrorDisplay`
//! for `Error`.

pub const DEFAULT_ERROR_MESSAGE: &str = "An unknown error occurred :(";

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Async<T> {
    Uninitialized,
    Loading(Option<T>),
    Complete(T),
    Error { value: Option<T>, message: String },
}

impl<T> From<Result<T, String>> for Async<T> {
    fn from(result: Result<T, String>) -> Self {
        match result {
            Ok(value) => Async::Complete(value),
            Err(message) => Async::error(message),
        }
    }
}

impl<T> Default for Async<T> {
    fn default() -> Self {
        Async::Loading(None)
    }
}

impl<T> Async<T> {
    pub fn error(message: impl Into<String>) -> Async<T> {
        Async::Error {
            value: None,
            message: message.into(),
        }
    }

    pub fn complete(&self) -> bool {
        matches!(self, Async::Complete(_) | Async::Error { .. })
    }

    pub fn value(&self) -> Option<&T> {
        match self {
            Async::Uninitialized => None,
            Async::Loading(v) | Async::Error { value: v, .. } => v.as_ref(),
            Async::Complete(v) => Some(v),
        }
    }

    pub fn message(&self) -> Option<&str> {
        match self {
            Async::Error { message, .. } => Some(message),
            _ => None,
        }
    }
}

use crate::server_fns::error_message;
use leptos::prelude::*;
use serde::de::DeserializeOwned;
use serde::Serialize;
use std::future::Future;

/// A screen's server data (= the `Async<T>` fields of the Kotlin view models, fed by `Api` calls
/// in `init`/`reload`): a leptos `Resource` over a server function, so the server renders the
/// page with the data and the hydrating browser picks it up without a second request, viewed as
/// an [`Async`] for the screens. A mutation that fails parks its message in `failure`, which the
/// view shows as `ErrorDisplay` with a reload button, as the Kotlin `Error` state did.
pub struct Loaded<T: Send + Sync + 'static> {
    resource: Option<Resource<Result<T, ServerFnError>>>,
    failure: RwSignal<Option<String>>,
}

// handles only (`Resource` and `RwSignal` are `Copy` whatever `T` is)
impl<T: Send + Sync + 'static> Clone for Loaded<T> {
    fn clone(&self) -> Self {
        *self
    }
}

impl<T: Send + Sync + 'static> Copy for Loaded<T> {}

impl<T> Loaded<T>
where
    T: Clone + Serialize + DeserializeOwned + Send + Sync + 'static,
{
    /// Loads once (and on every `reload`).
    pub fn new<Fut>(fetcher: impl Fn() -> Fut + Send + Sync + 'static) -> Loaded<T>
    where
        Fut: Future<Output = Result<T, ServerFnError>> + Send + 'static,
    {
        Loaded::with_source(|| (), move |()| fetcher())
    }

    /// Loads whenever `source` changes (the selected year, the race id).
    pub fn with_source<S, Fut>(
        source: impl Fn() -> S + Send + Sync + 'static,
        fetcher: impl Fn(S) -> Fut + Send + Sync + 'static,
    ) -> Loaded<T>
    where
        S: PartialEq + Clone + Send + Sync + 'static,
        Fut: Future<Output = Result<T, ServerFnError>> + Send + 'static,
    {
        Loaded {
            resource: Some(Resource::new(source, fetcher)),
            failure: RwSignal::new(None),
        }
    }

    /// Never loads: stays `Loading`. For contexts created outside the app (component tests).
    pub fn inert() -> Loaded<T> {
        Loaded {
            resource: None,
            failure: RwSignal::new(None),
        }
    }

    /// The load state, tracked. Read it inside a `<Suspense>` so the server waits for the data.
    pub fn get(&self) -> Async<T> {
        let loaded = match self.resource {
            Some(resource) => match resource.get() {
                None => Async::Loading(None),
                Some(Ok(value)) => Async::Complete(value),
                Some(Err(e)) => Async::error(error_message(&e)),
            },
            None => Async::Loading(None),
        };
        match self.failure.get() {
            Some(message) => Async::Error {
                value: loaded.value().cloned(),
                message,
            },
            None => loaded,
        }
    }

    pub fn signal(&self) -> Signal<Async<T>> {
        let this = *self;
        Signal::derive(move || this.get())
    }

    /// Reads the loaded value, tracked; `None` while loading or failed.
    pub fn with_value<R>(&self, f: impl FnOnce(&T) -> R) -> Option<R> {
        match self.resource {
            Some(resource) => resource.with(|r| match r {
                Some(Ok(value)) => Some(f(value)),
                _ => None,
            }),
            None => None,
        }
    }

    /// The loaded value, untracked.
    pub fn value(&self) -> Option<T> {
        self.resource
            .and_then(|r| r.get_untracked())
            .and_then(|r| r.ok())
    }

    /// Fetches again (`reload()`): the view shows the spinner meanwhile.
    pub fn reload(&self) {
        self.failure.set(None);
        if let Some(resource) = self.resource {
            resource.refetch();
        }
    }

    /// A mutation failed: show its message in place of the list.
    pub fn fail(&self, message: impl Into<String>) {
        self.failure.set(Some(message.into()));
    }

    /// `then` after a mutation: reload on success, show the failure otherwise.
    pub fn after(&self, result: Result<(), String>) {
        match result {
            Ok(()) => self.reload(),
            Err(message) => self.fail(message),
        }
    }
}

/// Runs a server function from an event handler (= `launch { Api... }` in a view model) and
/// hands the outcome to `then`, the failure as the text the UI shows.
pub fn call<T: 'static>(
    fut: impl Future<Output = Result<T, ServerFnError>> + 'static,
    then: impl FnOnce(Result<T, String>) + 'static,
) {
    leptos::task::spawn_local(async move {
        then(fut.await.map_err(|e| error_message(&e)));
    });
}

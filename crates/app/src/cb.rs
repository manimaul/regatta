//! [`Cb`], a callback prop type that converts from any `Fn(In) -> Out` closure through `#[prop(into)]`.
//!
//! leptos's own `Callback<In>` only converts from closures when `In` is a tuple (`Callback<(T,)>`), which would
//! make every single-argument handler `|(v,)|`; with this wrapper a prop is `listener: Cb<String>` and the call site
//! passes `|v| ...`.

use leptos::prelude::{Callable, Callback};

#[derive(Debug)]
pub struct Cb<In: 'static, Out: 'static = ()>(pub Callback<In, Out>);

impl<In: 'static, Out: 'static> Clone for Cb<In, Out> {
    fn clone(&self) -> Self {
        *self
    }
}

impl<In: 'static, Out: 'static> Copy for Cb<In, Out> {}

impl<In: 'static, Out: 'static> Cb<In, Out> {
    pub fn new<F>(f: F) -> Cb<In, Out>
    where
        F: Fn(In) -> Out + Send + Sync + 'static,
    {
        Cb(Callback::new(f))
    }

    pub fn run(&self, input: In) -> Out {
        self.0.run(input)
    }
}

impl<F, In, Out> From<F> for Cb<In, Out>
where
    F: Fn(In) -> Out + Send + Sync + 'static,
    In: 'static,
    Out: 'static,
{
    fn from(f: F) -> Self {
        Cb::new(f)
    }
}

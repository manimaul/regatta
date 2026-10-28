//! Layout helpers: Bootstrap-style containers and positions as `.rg-*` classes, and [`RgDiv`] with its
//! [`Customizer`] spacing helper.

use leptos::prelude::*;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RgContainerType {
    #[default]
    Container,
    ContainerSm,
    ContainerMd,
    ContainerLg,
    ContainerXl,
    ContainerXxl,
    ContainerFluid,
}

impl RgContainerType {
    pub const ENTRIES: [RgContainerType; 7] = [
        RgContainerType::Container,
        RgContainerType::ContainerSm,
        RgContainerType::ContainerMd,
        RgContainerType::ContainerLg,
        RgContainerType::ContainerXl,
        RgContainerType::ContainerXxl,
        RgContainerType::ContainerFluid,
    ];

    pub const fn cls(self) -> &'static str {
        match self {
            RgContainerType::Container => "rg-container",
            RgContainerType::ContainerSm => "rg-container-sm",
            RgContainerType::ContainerMd => "rg-container-md",
            RgContainerType::ContainerLg => "rg-container-lg",
            RgContainerType::ContainerXl => "rg-container-xl",
            RgContainerType::ContainerXxl => "rg-container-xxl",
            RgContainerType::ContainerFluid => "rg-container-fluid",
        }
    }
}

/// A fixed or sticky position, one `.rg-*` class per entry, defined in `crates/app/style/app.css`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RgPosition {
    FixedBottom,
    FixedTop,
    StickySmTop,
    StickyMdTop,
    StickyLgTop,
    StickyXlTop,
}

impl RgPosition {
    pub const ENTRIES: [RgPosition; 6] = [
        RgPosition::FixedBottom,
        RgPosition::FixedTop,
        RgPosition::StickySmTop,
        RgPosition::StickyMdTop,
        RgPosition::StickyLgTop,
        RgPosition::StickyXlTop,
    ];

    pub const fn cls(self) -> &'static str {
        match self {
            RgPosition::FixedBottom => "rg-fixed-bottom",
            RgPosition::FixedTop => "rg-fixed-top",
            RgPosition::StickySmTop => "rg-sticky-sm-top",
            RgPosition::StickyMdTop => "rg-sticky-md-top",
            RgPosition::StickyLgTop => "rg-sticky-lg-top",
            RgPosition::StickyXlTop => "rg-sticky-xl-top",
        }
    }
}

/// The full-height flex column of the page frame, with a container type and an optional position.
#[component]
pub fn RgGrid(
    #[prop(optional)] r#type: RgContainerType,
    #[prop(optional)] position: Option<RgPosition>,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    let class = super::join_classes(&[
        r#type.cls(),
        position.map(RgPosition::cls).unwrap_or(""),
        "min-h-screen flex flex-col",
    ]);
    view! { <div class=class>{children.map(|c| c())}</div> }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RgSpace {
    /// margin
    M,
    /// padding
    P,
}

impl RgSpace {
    pub const ENTRIES: [RgSpace; 2] = [RgSpace::M, RgSpace::P];
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RgSide {
    /// top
    T,
    /// bottom
    B,
    /// start
    S,
    /// end
    E,
    /// sides
    X,
    /// top, bottom
    Y,
}

impl RgSide {
    pub const ENTRIES: [RgSide; 6] = [
        RgSide::T,
        RgSide::B,
        RgSide::S,
        RgSide::E,
        RgSide::X,
        RgSide::Y,
    ];
}

/// A Bootstrap spacer step, mapped to the Tailwind step of the same size.
///
/// Bootstrap 3 = 1rem = Tailwind 4, 4 = 1.5rem = 6, 5 = 3rem = 12.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RgSz {
    S0,
    S1,
    S2,
    S3,
    S4,
    S5,
    Auto,
}

impl RgSz {
    pub const ENTRIES: [RgSz; 7] = [
        RgSz::S0,
        RgSz::S1,
        RgSz::S2,
        RgSz::S3,
        RgSz::S4,
        RgSz::S5,
        RgSz::Auto,
    ];

    pub const fn size(self) -> &'static str {
        match self {
            RgSz::S0 => "0",
            RgSz::S1 => "1",
            RgSz::S2 => "2",
            RgSz::S3 => "3",
            RgSz::S4 => "4",
            RgSz::S5 => "5",
            RgSz::Auto => "auto",
        }
    }

    pub const fn tw(self) -> &'static str {
        match self {
            RgSz::S0 => "0",
            RgSz::S1 => "1",
            RgSz::S2 => "2",
            RgSz::S3 => "4",
            RgSz::S4 => "6",
            RgSz::S5 => "12",
            RgSz::Auto => "auto",
        }
    }
}

/// Returns the spacing class for `space + side + size`, e.g. `"mb-4"`.
///
/// Every combination is spelled out as a literal so Tailwind's scanner sees it, and the `@source inline(...)`
/// safelist in `crates/app/style/app.css` covers the same set. Never build these names with `format!`.
pub const fn spacing_class(
    space: RgSpace,
    side: Option<RgSide>,
    size: Option<RgSz>,
) -> &'static str {
    use RgSide::*;
    use RgSpace::*;
    use RgSz::*;
    match (space, side, size) {
        (M, None, None) => "m",
        (M, None, Some(S0)) => "m-0",
        (M, None, Some(S1)) => "m-1",
        (M, None, Some(S2)) => "m-2",
        (M, None, Some(S3)) => "m-4",
        (M, None, Some(S4)) => "m-6",
        (M, None, Some(S5)) => "m-12",
        (M, None, Some(Auto)) => "m-auto",
        (M, Some(T), None) => "mt",
        (M, Some(T), Some(S0)) => "mt-0",
        (M, Some(T), Some(S1)) => "mt-1",
        (M, Some(T), Some(S2)) => "mt-2",
        (M, Some(T), Some(S3)) => "mt-4",
        (M, Some(T), Some(S4)) => "mt-6",
        (M, Some(T), Some(S5)) => "mt-12",
        (M, Some(T), Some(Auto)) => "mt-auto",
        (M, Some(B), None) => "mb",
        (M, Some(B), Some(S0)) => "mb-0",
        (M, Some(B), Some(S1)) => "mb-1",
        (M, Some(B), Some(S2)) => "mb-2",
        (M, Some(B), Some(S3)) => "mb-4",
        (M, Some(B), Some(S4)) => "mb-6",
        (M, Some(B), Some(S5)) => "mb-12",
        (M, Some(B), Some(Auto)) => "mb-auto",
        (M, Some(S), None) => "ms",
        (M, Some(S), Some(S0)) => "ms-0",
        (M, Some(S), Some(S1)) => "ms-1",
        (M, Some(S), Some(S2)) => "ms-2",
        (M, Some(S), Some(S3)) => "ms-4",
        (M, Some(S), Some(S4)) => "ms-6",
        (M, Some(S), Some(S5)) => "ms-12",
        (M, Some(S), Some(Auto)) => "ms-auto",
        (M, Some(E), None) => "me",
        (M, Some(E), Some(S0)) => "me-0",
        (M, Some(E), Some(S1)) => "me-1",
        (M, Some(E), Some(S2)) => "me-2",
        (M, Some(E), Some(S3)) => "me-4",
        (M, Some(E), Some(S4)) => "me-6",
        (M, Some(E), Some(S5)) => "me-12",
        (M, Some(E), Some(Auto)) => "me-auto",
        (M, Some(X), None) => "mx",
        (M, Some(X), Some(S0)) => "mx-0",
        (M, Some(X), Some(S1)) => "mx-1",
        (M, Some(X), Some(S2)) => "mx-2",
        (M, Some(X), Some(S3)) => "mx-4",
        (M, Some(X), Some(S4)) => "mx-6",
        (M, Some(X), Some(S5)) => "mx-12",
        (M, Some(X), Some(Auto)) => "mx-auto",
        (M, Some(Y), None) => "my",
        (M, Some(Y), Some(S0)) => "my-0",
        (M, Some(Y), Some(S1)) => "my-1",
        (M, Some(Y), Some(S2)) => "my-2",
        (M, Some(Y), Some(S3)) => "my-4",
        (M, Some(Y), Some(S4)) => "my-6",
        (M, Some(Y), Some(S5)) => "my-12",
        (M, Some(Y), Some(Auto)) => "my-auto",
        (P, None, None) => "p",
        (P, None, Some(S0)) => "p-0",
        (P, None, Some(S1)) => "p-1",
        (P, None, Some(S2)) => "p-2",
        (P, None, Some(S3)) => "p-4",
        (P, None, Some(S4)) => "p-6",
        (P, None, Some(S5)) => "p-12",
        (P, None, Some(Auto)) => "p-auto",
        (P, Some(T), None) => "pt",
        (P, Some(T), Some(S0)) => "pt-0",
        (P, Some(T), Some(S1)) => "pt-1",
        (P, Some(T), Some(S2)) => "pt-2",
        (P, Some(T), Some(S3)) => "pt-4",
        (P, Some(T), Some(S4)) => "pt-6",
        (P, Some(T), Some(S5)) => "pt-12",
        (P, Some(T), Some(Auto)) => "pt-auto",
        (P, Some(B), None) => "pb",
        (P, Some(B), Some(S0)) => "pb-0",
        (P, Some(B), Some(S1)) => "pb-1",
        (P, Some(B), Some(S2)) => "pb-2",
        (P, Some(B), Some(S3)) => "pb-4",
        (P, Some(B), Some(S4)) => "pb-6",
        (P, Some(B), Some(S5)) => "pb-12",
        (P, Some(B), Some(Auto)) => "pb-auto",
        (P, Some(S), None) => "ps",
        (P, Some(S), Some(S0)) => "ps-0",
        (P, Some(S), Some(S1)) => "ps-1",
        (P, Some(S), Some(S2)) => "ps-2",
        (P, Some(S), Some(S3)) => "ps-4",
        (P, Some(S), Some(S4)) => "ps-6",
        (P, Some(S), Some(S5)) => "ps-12",
        (P, Some(S), Some(Auto)) => "ps-auto",
        (P, Some(E), None) => "pe",
        (P, Some(E), Some(S0)) => "pe-0",
        (P, Some(E), Some(S1)) => "pe-1",
        (P, Some(E), Some(S2)) => "pe-2",
        (P, Some(E), Some(S3)) => "pe-4",
        (P, Some(E), Some(S4)) => "pe-6",
        (P, Some(E), Some(S5)) => "pe-12",
        (P, Some(E), Some(Auto)) => "pe-auto",
        (P, Some(X), None) => "px",
        (P, Some(X), Some(S0)) => "px-0",
        (P, Some(X), Some(S1)) => "px-1",
        (P, Some(X), Some(S2)) => "px-2",
        (P, Some(X), Some(S3)) => "px-4",
        (P, Some(X), Some(S4)) => "px-6",
        (P, Some(X), Some(S5)) => "px-12",
        (P, Some(X), Some(Auto)) => "px-auto",
        (P, Some(Y), None) => "py",
        (P, Some(Y), Some(S0)) => "py-0",
        (P, Some(Y), Some(S1)) => "py-1",
        (P, Some(Y), Some(S2)) => "py-2",
        (P, Some(Y), Some(S3)) => "py-4",
        (P, Some(Y), Some(S4)) => "py-6",
        (P, Some(Y), Some(S5)) => "py-12",
        (P, Some(Y), Some(Auto)) => "py-auto",
    }
}

/// The class list of an `RgDiv`: one spacing class (the last `set` wins) plus custom classes.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Customizer {
    classes: Vec<&'static str>,
    sss: Option<&'static str>,
}

impl Customizer {
    pub fn new() -> Customizer {
        Customizer::default()
    }

    pub fn get_classes(&self) -> Vec<&'static str> {
        self.classes.clone()
    }

    pub fn add_position(mut self, position: RgPosition) -> Customizer {
        self.push(position.cls());
        self
    }

    /// Sets the spacing class, replacing the previous one: `M + B + S3` gives `"mb-4"`.
    ///
    /// A `None` space clears the spacing class.
    pub fn set(
        mut self,
        space: Option<RgSpace>,
        side: Option<RgSide>,
        size: Option<RgSz>,
    ) -> Customizer {
        if let Some(previous) = self.sss.take() {
            self.classes.retain(|c| *c != previous);
        }
        if let Some(space) = space {
            let cls = spacing_class(space, side, size);
            self.sss = Some(cls);
            self.push(cls);
        }
        self
    }

    pub fn add_custom(mut self, style: &'static str) -> Customizer {
        self.push(style);
        self
    }

    fn push(&mut self, cls: &'static str) {
        if !self.classes.contains(&cls) {
            self.classes.push(cls);
        }
    }

    pub fn class_attr(&self) -> Option<String> {
        (!self.classes.is_empty()).then(|| self.classes.join(" "))
    }
}

/// A `<div>` with the classes of `customizer`; no `class` attribute at all when it has none.
#[component]
pub fn RgDiv(
    #[prop(optional, into)] id: Option<String>,
    #[prop(optional)] customizer: Option<Customizer>,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    let class = customizer.as_ref().and_then(Customizer::class_attr);
    // the builder: a `class=None` in `view!` would render `class=""`
    leptos::html::div()
        .attr("id", id)
        .attr("class", class)
        .child(children.map(|c| c()))
}

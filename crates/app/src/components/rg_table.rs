//! = `RgTable.kt`.

use leptos::prelude::*;

/// `cls` is the full class name, spelled out so Tailwind's scanner sees it.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum TableColor {
    Primary,
    #[default]
    Secondary,
    Success,
    Danger,
    Warning,
    Info,
    Light,
    Dark,
}

impl TableColor {
    pub const fn cls(self) -> &'static str {
        match self {
            TableColor::Primary => "rg-table-primary",
            TableColor::Secondary => "rg-table-secondary",
            TableColor::Success => "rg-table-success",
            TableColor::Danger => "rg-table-danger",
            TableColor::Warning => "rg-table-warning",
            TableColor::Info => "rg-table-info",
            TableColor::Light => "rg-table-light",
            TableColor::Dark => "rg-table-dark",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Scope {
    #[default]
    Col,
    Row,
    Colgroup,
    Rowgroup,
}

impl Scope {
    pub const fn attr(self) -> &'static str {
        match self {
            Scope::Col => "col",
            Scope::Row => "row",
            Scope::Colgroup => "colgroup",
            Scope::Rowgroup => "rowgroup",
        }
    }
}

#[component]
pub fn RgTable(
    #[prop(optional, into)] caption: Option<String>,
    #[prop(optional)] color: TableColor,
    #[prop(optional)] stripe_column: bool,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    let class = super::join_classes(&[
        "rg-table rg-table-hover",
        color.cls(),
        if stripe_column {
            "rg-table-striped-columns"
        } else {
            "rg-table-striped"
        },
    ]);
    view! {
        <div class="overflow-x-auto">
            <table class=class>
                {caption.map(|c| view! { <caption>{c}</caption> })}
                {children.map(|c| c())}
            </table>
        </div>
    }
}

#[component]
pub fn RgTfoot(#[prop(optional)] children: Option<Children>) -> impl IntoView {
    view! { <tfoot>{children.map(|c| c())}</tfoot> }
}

#[component]
pub fn RgThead(#[prop(optional)] children: Option<Children>) -> impl IntoView {
    view! { <thead>{children.map(|c| c())}</thead> }
}

#[component]
pub fn RgTbody(#[prop(optional)] children: Option<Children>) -> impl IntoView {
    view! { <tbody>{children.map(|c| c())}</tbody> }
}

#[component]
pub fn RgTrColor(
    #[prop(optional)] color: Option<TableColor>,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    leptos::html::tr()
        .attr("class", color.map(TableColor::cls))
        .child(children.map(|c| c()))
}

#[component]
pub fn RgTr(
    #[prop(optional)] classes: Option<&'static str>,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    leptos::html::tr()
        .attr("class", classes)
        .child(children.map(|c| c()))
}

#[component]
pub fn RgTh(
    #[prop(optional)] scope: Scope,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    view! { <th scope=scope.attr()>{children.map(|c| c())}</th> }
}

#[component]
pub fn RgTdColor(
    #[prop(optional)] col_span: Option<i32>,
    #[prop(optional)] color: Option<TableColor>,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    leptos::html::td()
        .attr("class", color.map(TableColor::cls))
        .attr("scope", "row")
        .attr("colspan", col_span)
        .child(children.map(|c| c()))
}

#[component]
pub fn RgTd(
    #[prop(optional)] col_span: Option<i32>,
    #[prop(optional)] classes: Option<&'static str>,
    #[prop(optional)] children: Option<Children>,
) -> impl IntoView {
    leptos::html::td()
        .attr("class", classes)
        .attr("scope", "row")
        .attr("colspan", col_span)
        .child(children.map(|c| c()))
}

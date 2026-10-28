//! Multi-line table cells for the results and standings tables.

use crate::utils::standings_report::ScoreLine;
use leptos::either::Either;
use leptos::prelude::*;

/// Plain lines of a table cell, separated by line breaks.
#[component]
pub fn CellLines(lines: Vec<String>) -> impl IntoView {
    lines
        .into_iter()
        .enumerate()
        .map(|(i, line)| view! { {(i > 0).then(|| view! { <br/> })}{line} })
        .collect_view()
}

/// Decorated lines of a standings cell: bold or plain segments with their classes, then a plain suffix.
///
/// The lines come from [`crate::utils::standings_report`].
#[component]
pub fn ScoreLines(lines: Vec<ScoreLine>) -> impl IntoView {
    lines
        .into_iter()
        .enumerate()
        .map(|(i, line)| {
            let segments = line
                .segments
                .into_iter()
                .map(|segment| {
                    let class = (!segment.classes.is_empty()).then(|| segment.classes.join(" "));
                    if segment.bold {
                        Either::Left(leptos::html::b().attr("class", class).child(segment.text))
                    } else {
                        Either::Right(
                            leptos::html::span()
                                .attr("class", class)
                                .child(segment.text),
                        )
                    }
                })
                .collect_view();
            let suffix = (!line.suffix.is_empty()).then_some(line.suffix);
            view! { {(i > 0).then(|| view! { <br/> })}{segments}{suffix} }
        })
        .collect_view()
}

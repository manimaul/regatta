//! = `web/src/jsMain/kotlin/components/*.kt`, one module per Kotlin file, same `.rg-*` classes
//! and element structure (Tailwind scans these sources, so every class is a literal string).

pub mod add_edit_boat_modal;
pub mod alerts;
pub mod cell_lines;
pub mod error_display;
pub mod finish_code_drop;
pub mod modal;
pub mod nav_bar;
pub mod race_config_form;
pub mod rated_label;
pub mod rating_selections;
pub mod rg_button;
pub mod rg_confirm;
pub mod rg_dropdown;
pub mod rg_form;
pub mod rg_grid;
pub mod rg_race_year_selector;
pub mod rg_sortable;
pub mod rg_spinner;
pub mod rg_switch;
pub mod rg_table;
pub mod theme_toggle;
pub mod time_row;

pub use add_edit_boat_modal::*;
pub use alerts::*;
pub use cell_lines::*;
pub use error_display::*;
pub use finish_code_drop::*;
pub use modal::*;
pub use nav_bar::*;
pub use race_config_form::*;
pub use rated_label::*;
pub use rating_selections::*;
pub use rg_button::*;
pub use rg_confirm::*;
pub use rg_dropdown::*;
pub use rg_form::*;
pub use rg_grid::*;
pub use rg_race_year_selector::*;
pub use rg_sortable::*;
pub use rg_spinner::*;
pub use rg_switch::*;
pub use rg_table::*;
pub use theme_toggle::*;
pub use time_row::*;

use leptos::prelude::*;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

/// Element ids the forms and modals hand out (`"${++num}_input"`, `"modal-${++num}"`). The
/// Kotlin counters were globals; here one counter per `App` (in context) so the server and the
/// hydrating browser, rendering the same tree in the same order, agree on every id.
#[derive(Clone, Default)]
pub struct ElementIds {
    inputs: Arc<AtomicUsize>,
    modals: Arc<AtomicUsize>,
}

static FALLBACK_IDS: std::sync::OnceLock<ElementIds> = std::sync::OnceLock::new();

fn ids() -> ElementIds {
    use_context::<ElementIds>()
        .unwrap_or_else(|| FALLBACK_IDS.get_or_init(ElementIds::default).clone())
}

pub fn next_input_id() -> String {
    format!("{}_input", ids().inputs.fetch_add(1, Ordering::Relaxed) + 1)
}

pub fn next_modal_id() -> String {
    format!("modal-{}", ids().modals.fetch_add(1, Ordering::Relaxed) + 1)
}

/// A class-list prop: `"me-2"`, `String`, or a `[&str]` slice convert through `#[prop(into)]`.
/// Empty by default. The values are literal class strings from the call sites, never built names.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Cls(pub String);

impl Cls {
    pub fn as_str(&self) -> &str {
        &self.0
    }
    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }
}

impl From<&str> for Cls {
    fn from(s: &str) -> Self {
        Cls(s.to_string())
    }
}

impl From<String> for Cls {
    fn from(s: String) -> Self {
        Cls(s)
    }
}

impl<const N: usize> From<[&str; N]> for Cls {
    fn from(parts: [&str; N]) -> Self {
        Cls(join_classes(&parts))
    }
}

/// Joins class lists; `None`/empty parts are dropped. The parts are literal class strings from
/// the call sites, never built names.
pub fn join_classes(parts: &[&str]) -> String {
    parts
        .iter()
        .filter(|p| !p.is_empty())
        .copied()
        .collect::<Vec<_>>()
        .join(" ")
}

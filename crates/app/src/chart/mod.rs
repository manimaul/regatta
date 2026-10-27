//! = `utils/MapLibre.kt`, `ChartController.js.kt`, `ChartViewModel.kt`, `ResizeObserver.kt`: the
//! course chart. The marks and the view-model state compile everywhere (the Course screen renders
//! the picker on the server); the MapLibre externs and the controller are browser-only.

pub mod chart_view_model;
pub mod resize_observer;

#[cfg(feature = "hydrate")]
pub mod chart_controller;
#[cfg(feature = "hydrate")]
pub mod maplibre;

use crate::state::theme::Theme;

/// OpenENC chart style for the site theme: the day palette in light mode, dusk in dark mode.
pub fn chart_style_url(theme: Theme) -> &'static str {
    match theme {
        Theme::Light => "https://openenc.com/v1/style/feet/day",
        Theme::Dark => "https://openenc.com/v1/style/feet/dusk",
    }
}

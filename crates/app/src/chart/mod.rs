//! The course chart on the Course screen.
//!
//! The marks and the chart state ([`chart_view_model`]) compile everywhere, since the Course screen renders the
//! picker on the server; the MapLibre externs and the controller are hydrate only. The map container renders
//! empty on the server.

pub mod chart_view_model;
pub mod resize_observer;

#[cfg(feature = "hydrate")]
pub mod chart_controller;
#[cfg(feature = "hydrate")]
pub mod maplibre;

use crate::state::theme::Theme;

/// Returns the OpenENC chart style for the site theme: the day palette in light mode, dusk in dark mode.
pub fn chart_style_url(theme: Theme) -> &'static str {
    match theme {
        Theme::Light => "https://openenc.com/v1/style/feet/day",
        Theme::Dark => "https://openenc.com/v1/style/feet/dusk",
    }
}

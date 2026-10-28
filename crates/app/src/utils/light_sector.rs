//! Light sector images for the OpenENC chart style, drawn the same way as njord's `SvgLightSector.kt`
//! (mxmariner.com / openenc.com).
//!
//! The style's `LIGHTS_sector` layer uses the feature's `SI` property as its icon id, and the id
//! encodes everything needed to draw the sector:
//! `sector_<sectr1>_<sectr2>_<day>_<dusk>_<night>_<dayLine>_<duskLine>_<nightLine>_<radius>`.
//! MapLibre raises `styleimagemissing` for it, and the chart controller answers with the
//! rasterized [`sector_svg`] in the palette of the current theme.

use crate::state::theme::Theme;

#[derive(Debug, Clone, PartialEq)]
pub struct SectorImage {
    pub sectr1: f64,
    pub sectr2: f64,
    pub day_color: String,
    pub dusk_color: String,
    pub night_color: String,
    pub day_line_color: String,
    pub dusk_line_color: String,
    pub night_line_color: String,
    pub radius: f64,
}

impl SectorImage {
    /// Arc and sector-line colors for the site theme: day in light mode, dusk in dark mode.
    pub fn colors(&self, theme: Theme) -> (&str, &str) {
        match theme {
            Theme::Light => (&self.day_color, &self.day_line_color),
            Theme::Dark => (&self.dusk_color, &self.dusk_line_color),
        }
    }

    pub fn svg(&self, theme: Theme) -> String {
        let (fill, line) = self.colors(theme);
        sector_svg(self.sectr1, self.sectr2, fill, line, self.radius)
    }
}

pub const SECTOR_IMAGE_PREFIX: &str = "sector_";

/// Parses a `LIGHTS_sector` icon id, or `None` when it is not one.
pub fn parse_sector_image_id(id: &str) -> Option<SectorImage> {
    let rest = id.strip_prefix(SECTOR_IMAGE_PREFIX)?;
    let parts: Vec<&str> = rest.split('_').collect();
    if parts.len() < 9 {
        return None;
    }
    Some(SectorImage {
        sectr1: parts[0].parse().ok()?,
        sectr2: parts[1].parse().ok()?,
        day_color: parts[2].to_string(),
        dusk_color: parts[3].to_string(),
        night_color: parts[4].to_string(),
        day_line_color: parts[5].to_string(),
        dusk_line_color: parts[6].to_string(),
        night_line_color: parts[7].to_string(),
        radius: parts[8].parse().unwrap_or(80.0),
    })
}

/// Draws a 200×200 SVG of the light's visible arc, with dashed sector limit lines from the light at the center.
///
/// SECTR1/SECTR2 are bearings *from seaward toward the light*, so they are reversed to
/// draw from the light outward; equal bearings mean an all-round light, drawn as a full circle.
pub fn sector_svg(
    sectr1: f64,
    sectr2: f64,
    fill_color: &str,
    line_color: &str,
    radius: f64,
) -> String {
    let sector1 = (sectr1 + 180.0) % 360.0;
    let sector2 = (sectr2 + 180.0) % 360.0;
    // whole-number coordinates are written without a fraction
    let cx = "100";
    let cy = "100";
    let (cxf, cyf) = (100.0_f64, 100.0_f64);

    let a1 = sector1 * std::f64::consts::PI / 180.0;
    let a2 = sector2 * std::f64::consts::PI / 180.0;

    let x1 = cxf + radius * a1.sin();
    let y1 = cyf - radius * a1.cos();
    let x2 = cxf + radius * a2.sin();
    let y2 = cyf - radius * a2.cos();

    let span = (sector2 - sector1 + 360.0) % 360.0;
    let arc_element = if span == 0.0 {
        format!(
            r#"<circle cx="{cx}" cy="{cy}" r="{}" fill="none" stroke="{fill_color}" stroke-width="4"/>"#,
            fmt(radius)
        )
    } else {
        let large_arc = if span > 180.0 { 1 } else { 0 };
        format!(
            "<path d=\"M {} {} A {} {} 0 {large_arc} 1 {} {}\"\n      fill=\"none\" stroke=\"{fill_color}\" stroke-width=\"4\" stroke-linecap=\"butt\"/>",
            fmt(x1),
            fmt(y1),
            fmt(radius),
            fmt(radius),
            fmt(x2),
            fmt(y2)
        )
    };

    let sector_lines_element = if span == 0.0 {
        String::new()
    } else {
        format!(
            "  <line x1=\"{cx}\" y1=\"{cy}\" x2=\"{}\" y2=\"{}\"\n        stroke=\"{line_color}\" stroke-width=\"2\"\n        stroke-dasharray=\"4 6\" stroke-linecap=\"butt\"/>\n  <line x1=\"{cx}\" y1=\"{cy}\" x2=\"{}\" y2=\"{}\"\n        stroke=\"{line_color}\" stroke-width=\"2\"\n        stroke-dasharray=\"4 6\" stroke-linecap=\"butt\"/>",
            fmt(x1),
            fmt(y1),
            fmt(x2),
            fmt(y2)
        )
    };

    format!(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 200\">\n  {arc_element}\n  {sector_lines_element}\n</svg>"
    )
}

/// Formats `x` with one decimal, rounding half to even on tenths: `32.0`, `168.0`, `-0.5`.
///
/// The SVG text `crates/app/tests/light_sector_test.rs` pins depends on the half-to-even rounding.
fn fmt(x: f64) -> String {
    let tenths = (x * 10.0).round_ties_even() as i64;
    format!("{}.{}", tenths / 10, (tenths % 10).abs())
}

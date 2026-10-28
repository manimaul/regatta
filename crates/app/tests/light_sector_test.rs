//! Tests of the light sector icon ids and SVGs, as the OpenENC style's `LIGHTS_sector` layer uses them.
#![cfg(not(target_arch = "wasm32"))]

use regatta_app::state::theme::Theme;
use regatta_app::utils::light_sector::{parse_sector_image_id, sector_svg};

const ID: &str = "sector_90.0_270.0_#FF0000_#B30000_#500000_#000000_#8CA6B3_#555555_68.0";

#[test]
fn parses_the_style_icon_id_and_picks_theme_colors() {
    let img = parse_sector_image_id(ID).unwrap();
    assert_eq!(img.sectr1, 90.0);
    assert_eq!(img.sectr2, 270.0);
    assert_eq!(img.radius, 68.0);
    assert_eq!(img.colors(Theme::Light), ("#FF0000", "#000000"));
    assert_eq!(img.colors(Theme::Dark), ("#B30000", "#8CA6B3"));
}

#[test]
fn rejects_ids_that_are_not_sectors() {
    assert!(parse_sector_image_id("LIGHTS11").is_none());
    assert!(parse_sector_image_id("sector_1_2_3").is_none());
    assert!(parse_sector_image_id("sector_x_270.0_a_b_c_d_e_f_80").is_none());
}

#[test]
fn radius_defaults_to_80_when_unparseable() {
    assert_eq!(
        parse_sector_image_id("sector_0.0_0.0_a_b_c_d_e_f_nope")
            .unwrap()
            .radius,
        80.0
    );
}

#[test]
fn equal_bearings_draw_an_all_round_light_without_sector_lines() {
    let svg = sector_svg(0.0, 0.0, "#FFD700", "#000000", 80.0);
    assert!(
        svg.contains(r#"<circle cx="100" cy="100" r="80.0""#),
        "{svg}"
    );
    assert!(svg.contains(r##"stroke="#FFD700""##), "{svg}");
    assert!(!svg.contains("<line"), "{svg}");
}

#[test]
fn bearings_are_reversed_to_draw_from_the_light_outward() {
    // SECTR1 90 (seen from the east) starts the arc west of the light, SECTR2 270 ends it east
    let svg = sector_svg(90.0, 270.0, "#FF0000", "#000000", 68.0);
    assert!(
        svg.contains(r#"<path d="M 32.0 100.0 A 68.0 68.0 0 0 1 168.0 100.0""#),
        "{svg}"
    );
    assert!(svg.contains(r#"x2="32.0" y2="100.0""#), "{svg}");
    assert!(svg.contains(r#"x2="168.0" y2="100.0""#), "{svg}");
}

#[test]
fn spans_over_180_degrees_use_the_large_arc_flag() {
    // SECTR1 0 / SECTR2 270 reverses to 180 -> 90 clockwise, a 270 degree arc
    let svg = sector_svg(0.0, 270.0, "#FF0000", "#000000", 80.0);
    assert!(svg.contains(" 0 1 1 "), "{svg}");
    assert!(!sector_svg(0.0, 90.0, "#FF0000", "#000000", 80.0).contains(" 0 1 1 "));
}

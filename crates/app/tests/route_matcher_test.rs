//! Tests of the nav bar's path matcher and route resolution.
#![cfg(not(target_arch = "wasm32"))]

use regatta_app::state::route::Route;
use regatta_app::utils::route_matcher::Routing;

#[test]
fn route_arg_match() {
    let route = Routing::from("/races/results/view/1");
    assert_eq!(route.route, Route::RaceResultView);
    assert_eq!(route.arg("id"), Some("1"));
}

#[test]
fn route_match() {
    assert_eq!(Routing::from("/people").route, Route::People);
}

#[test]
fn every_pattern_matches_itself_and_a_trailing_slash() {
    for route in Route::ENTRIES {
        let path = route
            .path_pattern()
            .replace(":id", "7")
            .replace(":year", "2026");
        assert_eq!(Routing::from(&path).route, route, "{path}");
        if path != "/" {
            assert_eq!(Routing::from(&format!("{path}/")).route, route, "{path}/");
        }
    }
    assert_eq!(
        Routing::from("/series/standings/view/3/2025").arg("year"),
        Some("2025")
    );
    assert_eq!(Routing::from("/nowhere").route, Route::NotFound);
    // "/races/results/view" is the results editor of race "view", not the viewer
    let edit = Routing::from("/races/results/view");
    assert_eq!(edit.route, Route::RaceResultEdit);
    assert_eq!(edit.arg("id"), Some("view"));
}

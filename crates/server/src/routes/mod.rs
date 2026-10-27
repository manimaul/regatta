//! One handler per row of the plan's route table (= `plugins/Routing.kt`), nested under
//! `/v1/api` by [`crate::build_router`]. Admin routes take the [`Admin`](crate::extract::Admin)
//! extractor first, so the bearer check answers before any body is read or parsed.

pub mod auth;
pub mod boats;
pub mod classes;
pub mod image;
pub mod orc;
pub mod people;
pub mod races;
pub mod results;
pub mod series;

use crate::fallback;
use crate::AppState;
use axum::routing::{get, post};
use axum::Router;

pub fn api_router() -> Router<AppState> {
    Router::new()
        // series
        .route(
            "/series",
            get(series::get_series)
                .post(series::post_series)
                .delete(series::delete_series),
        )
        .route("/allSeries", get(series::all_series))
        .route("/find/series", get(series::find_series))
        .route("/series/standings", get(series::standings))
        // classes and brackets
        .route(
            "/raceClass",
            get(classes::get_class)
                .post(classes::post_class)
                .delete(classes::delete_class),
        )
        .route("/allClasses", get(classes::all_classes))
        .route("/raceClassList", post(classes::post_class_list))
        .route(
            "/bracket",
            get(classes::get_bracket)
                .post(classes::post_bracket)
                .delete(classes::delete_bracket),
        )
        // auth
        .route("/login", post(auth::login))
        .route("/auth", post(auth::post_auth))
        // races
        .route("/years", get(races::years))
        .route("/allRaces", get(races::all_races))
        .route(
            "/race",
            get(races::get_race)
                .post(races::post_race)
                .delete(races::delete_race),
        )
        .route(
            "/raceSchedule",
            get(races::get_race_schedule).post(races::post_race_schedule),
        )
        // results
        .route(
            "/results",
            get(results::get_results)
                .post(results::post_result)
                .delete(results::delete_result),
        )
        .route("/scheduleResults", get(results::schedule_results))
        .route("/resultCount", get(results::result_count))
        .route("/report", get(results::report))
        // images: the download route is one segment with a literal prefix (`/v1/api/image<name>`)
        .route(
            "/image",
            post(image::upload).get(image::download_empty_name),
        )
        .route("/image{name}", get(image::download))
        // people
        .route("/allSkippers", get(people::all_skippers))
        .route(
            "/skipper",
            get(people::get_skipper)
                .post(people::post_skipper)
                .delete(people::delete_skipper),
        )
        .route("/find/skipper", get(people::find_skipper))
        // boats
        .route("/allBoat", get(boats::all_boats))
        .route(
            "/boat",
            get(boats::get_boat)
                .post(boats::post_boat)
                .delete(boats::delete_boat),
        )
        // ORC
        .route("/orcCert", get(orc::get_cert).post(orc::post_cert))
        .route("/orcCerts", get(orc::all_certs))
        .method_not_allowed_fallback(fallback::site)
}

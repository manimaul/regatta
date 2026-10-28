//! SSR integration tests: every screen rendered through the router over the golden dump.
//!
//! Each route answers 200 `text/html` with its data already in the markup (not just a shell), the nav bar follows the
//! auth cookie, the theme cookie sets `data-theme`, and every unknown path (including under `/v1/api`) is the shell
//! with the NotFound view and 200, as the contract cassettes record for the fallback (T7 in
//! `docs/explore_implemented/rust_port.md`).

mod support;

use axum::body::Body;
use axum::http::{Request, StatusCode};
use base64::prelude::{Engine, BASE64_STANDARD};
use http_body_util::BodyExt;
use regatta_app::app::NOT_FOUND_TEXT;
use regatta_common::{AuthRecord, Login, VersionInfo};
use regatta_server::{build_router, App, AppState};
use regatta_service::auth;
use tower::ServiceExt;

struct Page {
    status: StatusCode,
    content_type: String,
    html: String,
}

impl Page {
    fn has(&self, needle: &str) -> bool {
        self.html.contains(needle)
    }
}

async fn get(app: &App, path: &str, cookie: Option<&str>) -> Page {
    let mut request = Request::builder().uri(path);
    if let Some(cookie) = cookie {
        request = request.header("cookie", cookie);
    }
    let response = app
        .clone()
        .oneshot(request.body(Body::empty()).unwrap())
        .await
        .unwrap();
    let status = response.status();
    let content_type = response
        .headers()
        .get("content-type")
        .map(|v| v.to_str().unwrap().to_string())
        .unwrap_or_default();
    let body = response.into_body().collect().await.unwrap().to_bytes();
    Page {
        status,
        content_type,
        // without the `<!>` text-node markers that erased components put inside elements
        html: String::from_utf8(body.to_vec()).unwrap().replace("<!>", ""),
    }
}

async fn html(app: &App, path: &str, cookie: Option<&str>) -> Page {
    let page = get(app, path, cookie).await;
    assert_eq!(page.status, StatusCode::OK, "{path}");
    assert!(
        page.content_type.starts_with("text/html"),
        "{path}: {}",
        page.content_type
    );
    assert!(page.has("<!DOCTYPE html>"), "{path} is a document");
    page
}

/// Asserts the frame every page carries: the nav bar and the footer with the build hash.
fn assert_frame(page: &Page, path: &str) {
    assert!(page.has("rg-navbar"), "{path}: nav bar");
    assert!(page.has("rg-nav-link"), "{path}: nav links");
    assert!(
        page.has(VersionInfo::CURRENT.git_hash),
        "{path}: footer build hash"
    );
}

/// Returns the nav link labels rendered on the page, in order.
fn nav_links(page: &Page) -> Vec<&str> {
    page.html
        .split("class=\"rg-nav-link\"")
        .skip(1)
        .filter_map(|s| {
            let start = s.find('>')? + 1;
            let end = s[start..].find("</button>")? + start;
            Some(s[start..end].trim())
        })
        .collect()
}

#[test]
fn routes_render_with_their_data_and_follow_the_cookies() {
    support::block_on_with_render_stack(routes_render_with_their_data_and_follow_the_cookies_body)
}

async fn routes_render_with_their_data_and_follow_the_cookies_body() {
    let golden = support::start().await;
    let state = AppState::new(
        golden.db.clone(),
        Some(support::DUMP_INSTANT.parse().unwrap()),
    );
    let app = build_router(state);
    let db = &golden.db;

    // ---- logged out ----
    let home = html(&app, "/", None).await;
    assert_frame(&home, "/");
    assert_eq!(
        nav_links(&home),
        vec!["Race Results", "Course", "Admin"],
        "public links and the Admin link"
    );
    assert!(home.has("data-theme=\"light\""), "the default theme");

    let series_names: Vec<String> = db
        .all_series()
        .await
        .unwrap()
        .into_iter()
        .map(|s| s.name)
        .collect();
    assert!(!series_names.is_empty());
    let series = html(&app, "/series", None).await;
    assert_frame(&series, "/series");
    for name in &series_names {
        assert!(series.has(name), "/series lists {name}");
    }
    assert!(series.has("Total Races In Series"));

    let races = db.all_races(2026).await.unwrap();
    assert!(!races.is_empty(), "the dump has 2026 races");
    let results = html(&app, "/races/results", None).await;
    assert_frame(&results, "/races/results");
    assert!(results.has("<h1>Race Results</h1>"));
    for race in &races {
        assert!(
            results.has(&race.race.name),
            "/races/results lists {}",
            race.race.name
        );
    }
    assert!(results.has("View Results"), "races with results");
    assert!(!results.has("Edit Results"), "no Action column logged out");

    let classes = db.all_classes().await.unwrap();
    assert!(!classes.is_empty());
    let class_page = html(&app, "/class", None).await;
    assert_frame(&class_page, "/class");
    for class in &classes {
        assert!(
            class_page.has(&class.race_class.name),
            "/class lists {}",
            class.race_class.name
        );
    }

    // skippers are admin data, but the dump's auth table is empty: bootstrap mode, where every
    // caller is the admin (the REST API behaves the same), so the screen renders without a login
    let people = html(&app, "/people", None).await;
    assert_frame(&people, "/people");
    assert!(people.has("<h1>Skippers</h1>"), "/people in bootstrap mode");

    let series_1 = db.find_series(1).await.unwrap().unwrap();
    let standings = html(&app, "/series/standings/view/1/2024", None).await;
    assert_frame(&standings, "/series/standings/view/1/2024");
    assert!(
        standings.has(&format!("{} Standings 2024", series_1.name)),
        "the standings heading"
    );
    let standings_report = regatta_service::reporter::get_standings_report(
        db,
        1,
        2024,
        support::DUMP_INSTANT.parse().unwrap(),
    )
    .await
    .unwrap()
    .unwrap();
    let boat = &standings_report.standings[0].standings[0].standings[0]
        .boat_skipper
        .boat;
    assert!(
        standings.has(&boat.as_ref().unwrap().name),
        "a boat of the standings"
    );

    let race_1 = db.find_race_schedule(1).await.unwrap().unwrap();
    let report = html(&app, "/races/results/view/1", None).await;
    assert_frame(&report, "/races/results/view/1");
    assert!(
        report.has(&format!("{} - Results", race_1.race.name)),
        "the report heading"
    );
    let race_report = regatta_service::reporter::get_report(db, 1)
        .await
        .unwrap()
        .unwrap();
    let first_card = &race_report.class_reports[0].bracket_report[0].cards[0];
    assert!(
        report.has(&first_card.boat_name),
        "a boat of the report: {}",
        first_card.boat_name
    );
    assert!(!report.has(">Editor<"), "no editor button logged out");

    // the admin screen is the login form
    let admin = html(&app, "/admin", None).await;
    assert!(admin.has("<h2>Login</h2>"));
    assert!(admin.has("id=\"password\""));

    // ---- theme cookie ----
    let dark = html(&app, "/series", Some("theme=dark")).await;
    assert!(dark.has("data-theme=\"dark\""), "the theme cookie");
    let dark_too = html(&app, "/series", Some("a=b; theme=dark; c=d")).await;
    assert!(dark_too.has("data-theme=\"dark\""));

    // ---- unknown paths: the shell with the NotFound view, 200 ----
    let missing = html(&app, "/no/such/page", None).await;
    assert_frame(&missing, "/no/such/page");
    assert!(missing.has(NOT_FOUND_TEXT));
    let missing_api = html(&app, "/v1/api/noSuchRoute", None).await;
    assert!(missing_api.has(NOT_FOUND_TEXT));
    let wrong_method = app
        .clone()
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri("/v1/api/series")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(wrong_method.status(), StatusCode::OK);
    assert!(wrong_method
        .headers()
        .get("content-type")
        .unwrap()
        .to_str()
        .unwrap()
        .starts_with("text/html"));

    // ---- logged in: an admin created through the service layer, a token signed like the server's ----
    let stored_hash = auth::hash(&["password"]);
    let admin_record = db
        .save_auth(&AuthRecord {
            id: 0,
            hash: stored_hash.clone(),
            user_name: "skipper".to_string(),
        })
        .await
        .unwrap()
        .unwrap();
    let now: jiff::Timestamp = support::DUMP_INSTANT.parse().unwrap();
    let salt = "client-salt".to_string();
    let login = Login {
        user_name: admin_record.user_name.clone(),
        hash_of_hash: auth::time_stamp_hash(now, &salt, &stored_hash),
        salt,
        time: now,
    };
    let response = auth::create_login_response(db, &login, jiff::Timestamp::now())
        .await
        .unwrap()
        .expect("a login response");
    let token = BASE64_STANDARD.encode(serde_json::to_vec(&response).unwrap());
    let cookie = format!("regatta_token={token}");

    let home = html(&app, "/", Some(&cookie)).await;
    assert_frame(&home, "/ (admin)");
    assert_eq!(
        nav_links(&home),
        vec![
            "Home",
            "Skippers",
            "Boats",
            "Series",
            "Classes",
            "Races",
            "RC",
            "Race Results",
            "Course",
            "Logout"
        ],
        "the admin links and Logout"
    );
    assert!(home.has("Logged in as skipper"));
    assert!(home.has("Copy auth token"));

    // once an admin exists, the logged-out screen shows the server's refusal instead of the data
    let people_logged_out = html(&app, "/people", None).await;
    assert_frame(&people_logged_out, "/people (logged out, admin exists)");
    assert!(
        people_logged_out.has("unauthorized"),
        "/people logged out once an admin exists"
    );
    assert!(!people_logged_out.has("<h1>Skippers</h1>"));

    let people = html(&app, "/people", Some(&cookie)).await;
    assert!(people.has("<h1>Skippers</h1>"));
    let a_person = db.all_people().await.unwrap().remove(0);
    assert!(people.has(&a_person.first) && people.has(&a_person.last));

    let results = html(&app, "/races/results", Some(&cookie)).await;
    assert!(
        results.has("Edit Results"),
        "the Action column for an admin"
    );

    let report = html(&app, "/races/results/view/1", Some(&cookie)).await;
    assert!(report.has(">Editor<"), "the editor button for an admin");

    // a tampered token is logged out, not an error
    let bad_cookie = format!("regatta_token={}", BASE64_STANDARD.encode("{\"id\":1}"));
    let home = html(&app, "/", Some(&bad_cookie)).await;
    assert_eq!(nav_links(&home), vec!["Race Results", "Course", "Admin"]);
}

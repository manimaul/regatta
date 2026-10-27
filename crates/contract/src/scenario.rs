//! The contract scenario as data: every row of the plan's route table, every listed status,
//! the auth modes, the request-parsing quirks, multipart upload, gzip, the fallback. It runs on
//! a fresh load of the golden dump (`crates/scoring/tests/golden/*.sql`), whose ids are
//! referenced below (series 1, race 1 with results, class 1, bracket 16 in use, person 2, boat
//! 1, RC person 31 of race 1, certificate 04560003NFK), and whose `auth` table is empty, so the
//! run starts in bootstrap mode. Every record a step creates is read back by later steps and
//! deleted at the end.

use crate::auth;
use crate::png::tiny_png;
use crate::random::random_steps;
use crate::runner::Scenario;
use crate::step::{delete, get, post, put, AuthMode, Forge, ForgeId, Part, PartContent, Step};
use serde_json::{json, Value};

pub const ADMIN_USER: &str = "contract";
pub const ADMIN_PASSWORD: &str = "contract-password";
/// Fixed login salt and time: the `Login` body is deterministic; the server checks no time window.
pub const LOGIN_SALT: &str = "contract-login-salt";
pub const LOGIN_TIME: &str = "2026-10-14T02:10:11Z";
pub const FORGED_SALT: &str = "contract-forged-salt";
pub const FORGED_VALID_EXPIRES: &str = "2099-01-01T00:00:00Z";
pub const FORGED_EXPIRED_EXPIRES: &str = "2000-01-01T00:00:00Z";
/// The golden dump's instant: the clock to run the servers with (`-Dregatta.now` / `REGATTA_FIXED_NOW`).
pub const DEFAULT_FIXED_NOW: &str = "2026-10-14T02:10:11Z";
pub const IMAGE_NAME: &str = "contract.png";

/// Every `method path` of the plan's route table (`image/{name}` stands for any name).
pub const ROUTES: &[(&str, &str)] = &[
    ("GET", "series"),
    ("GET", "allSeries"),
    ("GET", "find/series"),
    ("GET", "series/standings"),
    ("POST", "series"),
    ("DELETE", "series"),
    ("GET", "raceClass"),
    ("GET", "allClasses"),
    ("POST", "raceClass"),
    ("POST", "raceClassList"),
    ("DELETE", "raceClass"),
    ("GET", "bracket"),
    ("POST", "bracket"),
    ("DELETE", "bracket"),
    ("POST", "login"),
    ("POST", "auth"),
    ("GET", "years"),
    ("GET", "allRaces"),
    ("GET", "race"),
    ("POST", "race"),
    ("DELETE", "race"),
    ("GET", "raceSchedule"),
    ("POST", "raceSchedule"),
    ("GET", "results"),
    ("POST", "results"),
    ("DELETE", "results"),
    ("GET", "scheduleResults"),
    ("GET", "resultCount"),
    ("GET", "report"),
    ("POST", "image"),
    ("GET", "image{name}"),
    ("GET", "allSkippers"),
    ("GET", "skipper"),
    ("GET", "find/skipper"),
    ("POST", "skipper"),
    ("DELETE", "skipper"),
    ("GET", "allBoat"),
    ("GET", "boat"),
    ("POST", "boat"),
    ("DELETE", "boat"),
    ("GET", "orcCert"),
    ("GET", "orcCerts"),
    ("POST", "orcCert"),
];

/// Routes the scenario leaves out on purpose: `GET orcCert` calls data.orc.org live.
pub const SKIPPED_ROUTES: &[(&str, &str)] = &[("GET", "orcCert")];

#[derive(Debug, Clone)]
pub struct ScenarioOptions {
    /// `LoginResponse.expires` is stable only when the server runs with a fixed clock.
    pub expires_volatile: bool,
    /// Extra proptest-generated steps appended after the scenario (`diff --random N`).
    pub random: usize,
    pub seed: u64,
}

impl Default for ScenarioOptions {
    fn default() -> Self {
        ScenarioOptions {
            expires_volatile: true,
            random: 0,
            seed: 42,
        }
    }
}

fn forged(id: ForgeId, expires: &str, correct: bool) -> AuthMode {
    AuthMode::Forged(Forge {
        id,
        salt: FORGED_SALT.to_string(),
        expires: expires.to_string(),
        correct,
    })
}

fn raw(value: &str) -> AuthMode {
    AuthMode::Raw(value.to_string())
}

fn png_part() -> Part {
    Part {
        name: "image".into(),
        filename: Some(IMAGE_NAME.into()),
        content_type: "image/png".into(),
        content: PartContent::Bytes(tiny_png()),
    }
}

fn text_part() -> Part {
    Part {
        name: "note".into(),
        filename: None,
        content_type: "text/plain".into(),
        content: PartContent::Text("not an image; the server ignores this part".into()),
    }
}

fn series_json(id: Value, name: &str, sort: i32, active: bool) -> Value {
    json!({"id": id, "name": name, "sort": sort, "raceCount": 0, "active": active})
}

fn race_class_json(id: Value, name: &str, sort: i32) -> Value {
    json!({"id": id, "name": name, "sort": sort, "ratingType": "PHRF", "numberOfRaces": 0, "active": true})
}

fn bracket_json(id: Value, name: &str, min: f64, max: f64, class_id: Value) -> Value {
    json!({"id": id, "name": name, "description": null, "active": true, "numberOfRaces": 0, "minRating": min, "maxRating": max, "classId": class_id})
}

/// `orc` = (scoring option, 3-band, 5-band).
fn race_json(
    id: Value,
    name: &str,
    series_id: Value,
    rc_id: Value,
    cf: i32,
    orc: (&str, &str, &str),
) -> Value {
    json!({"id": id, "name": name, "seriesId": series_id, "rcId": rc_id, "reportImage": null, "phrfBFactor": cf, "orcScoringOption": orc.0, "orc3Band": orc.1, "orc5Band": orc.2})
}

fn contract_race(id: Value) -> Value {
    race_json(
        id,
        "Contract Race",
        json!("{{series_id}}"),
        Value::Null,
        600,
        ("FiveBandWindwardLeeward", "Low", "LowMedium"),
    )
}

fn result_json(
    race_id: Value,
    boat_id: Value,
    finish: Value,
    phrf: Value,
    orc_ref: Value,
    rating_type: &str,
    code: &str,
) -> Value {
    json!({
        "id": 0, "raceId": race_id, "boatId": boat_id, "finish": finish, "phrfRating": phrf, "orcRef": orc_ref,
        "hocPosition": null, "bracketId": "{{bracket_a}}", "raceClassId": "{{class_id}}", "penalty": null,
        "ratingType": rating_type, "finishCode": code
    })
}

fn schedule_json(cf: i32, b5: &str) -> Value {
    json!({
        "race": race_json(json!("{{race_id}}"), "Contract Race", json!("{{series_id}}"), Value::Null, cf, ("FiveBandWindwardLeeward", "Low", b5)),
        "resultCount": 0,
        "series": null,
        "rc": null,
        "schedule": [{
            "raceClass": race_class_json(json!("{{class_id}}"), "Contract Class", 50),
            "brackets": [bracket_json(json!("{{bracket_a}}"), "A", 0.0, 100.0, json!("{{class_id}}"))],
            "startDate": "2026-06-01T17:00:00Z",
            "endDate": "2026-06-01T21:00:00Z"
        }]
    })
}

fn boat_json(id: Value, phrf: i32) -> Value {
    json!({
        "id": id, "name": "Contract Boat", "sailNumber": "C-1", "boatType": "Contract 30", "phrfRating": phrf,
        "orcCerts": [], "skipperId": "{{person_id}}", "ratingType": "PHRF", "numberOfRaces": 0, "active": true
    })
}

/// The scenario's steps, in order.
pub fn steps(options: &ScenarioOptions) -> Vec<Step> {
    let stored_hash = auth::stored_hash(ADMIN_PASSWORD);
    let login = |password: &str, user: &str| {
        auth::login_body(user, password, LOGIN_SALT, LOGIN_TIME).expect("fixed login time")
    };
    let mut login_volatile = vec!["/salt", "/hashOfHash"];
    if options.expires_volatile {
        login_volatile.push("/expires");
    }
    let big = i64::MAX;
    let mut s: Vec<Step> = Vec::new();

    // --- Bootstrap: the auth table is empty, admin routes are open to any bearer value ---
    // Ktor's bearer provider challenges when the header is absent (or the value is empty) before
    // the token is validated, so bootstrap needs a bearer value: the web client sends `Bearer none`.
    let none = || raw("Bearer none");
    s.push(get("bootstrap-missing-header", "allSkippers"));
    s.push(get("bootstrap-empty-bearer", "allSkippers").auth(raw("Bearer ")));
    s.push(get("bootstrap-malformed-bearer", "allSkippers").auth(raw("Bearer not*base64!")));
    s.push(get("bootstrap-any-bearer", "allSkippers").auth(raw("Bearer anything")));
    s.push(get("bootstrap-all-skippers", "allSkippers").auth(none()));
    s.push(
        get("bootstrap-skipper", "skipper")
            .query("id", "2")
            .auth(none()),
    );
    s.push(get("bootstrap-boat", "boat").query("id", "1").auth(none()));
    s.push(get("bootstrap-all-boats", "allBoat").auth(none()));
    s.push(
        get("bootstrap-orc-certs", "orcCerts")
            .auth(none())
            .capture_find("cert_nfk", "RefNo", "04560003NFK"),
    );
    s.push(
        post("auth-create-admin-missing-hash", "auth")
            .json(json!({"id": 0, "userName": "nobody"}))
            .auth(none()),
    );
    s.push(
        post("auth-create-admin", "auth")
            .json(json!({"id": 0, "hash": stored_hash, "userName": ADMIN_USER}))
            .auth(none())
            .capture("auth_id", "/id"),
    );
    s.push(get("bootstrap-closed-bearer-none", "allSkippers").auth(none()));
    s.push(get("bootstrap-closed-missing-header", "allSkippers"));

    // --- Login ---
    s.push(post("login-wrong-password", "login").json(login("wrong-password", ADMIN_USER)));
    s.push(post("login-unknown-user", "login").json(login(ADMIN_PASSWORD, "nobody")));
    s.push(post("login-missing-fields", "login").json(json!({"userName": ADMIN_USER})));
    s.push(post("login-malformed-json", "login").text("application/json", "{\"userName\": "));
    s.push(post("login-empty-body", "login").text("application/json", ""));
    s.push(
        post("login", "login")
            .json(login(ADMIN_PASSWORD, ADMIN_USER))
            .volatile(&login_volatile)
            .capture_token(),
    );

    // --- Auth modes on admin routes ---
    s.push(get("auth-admin-token", "allSkippers").admin());
    s.push(
        post("auth-duplicate-user-name", "auth")
            .json(json!({"id": 0, "hash": auth::stored_hash("other"), "userName": ADMIN_USER}))
            .admin(),
    );
    s.push(
        post("auth-create-admin-missing-hash-with-token", "auth")
            .json(json!({"id": 0, "userName": "nobody"}))
            .admin(),
    );
    s.push(get("auth-missing-header", "allSkippers"));
    s.push(get("auth-bearer-none", "allSkippers").auth(raw("Bearer none")));
    s.push(get("auth-malformed-base64", "allSkippers").auth(raw("Bearer not*base64!")));
    s.push(get("auth-base64-not-json", "allSkippers").auth(raw("Bearer aGVsbG8gd29ybGQ=")));
    s.push(get("auth-json-wrong-shape", "allSkippers").auth(raw("Bearer eyJmb28iOjF9")));
    s.push(get("auth-expired-token", "allSkippers").auth(forged(
        ForgeId::Captured,
        FORGED_EXPIRED_EXPIRES,
        true,
    )));
    s.push(get("auth-forged-valid-token", "allSkippers").auth(forged(
        ForgeId::Captured,
        FORGED_VALID_EXPIRES,
        true,
    )));
    s.push(get("auth-forged-bad-hash", "allSkippers").auth(forged(
        ForgeId::Captured,
        FORGED_VALID_EXPIRES,
        false,
    )));
    s.push(get("auth-forged-unknown-id", "allSkippers").auth(forged(
        ForgeId::Fixed(999_999),
        FORGED_VALID_EXPIRES,
        true,
    )));
    s.push(get("auth-basic-scheme", "allSkippers").auth(raw("Basic Zm9vOmJhcg==")));
    s.push(get("auth-empty-bearer", "allSkippers").auth(raw("Bearer ")));
    s.push(post("auth-mutation-missing-header", "skipper").json(
        json!({"id": 0, "first": "No", "last": "Token", "clubMember": false, "active": true}),
    ));
    s.push(
        delete("auth-mutation-bearer-none", "series")
            .query("id", "1")
            .auth(raw("Bearer none")),
    );
    s.push(
        delete("auth-mutation-expired-token", "series")
            .query("id", "1")
            .auth(forged(ForgeId::Captured, FORGED_EXPIRED_EXPIRES, true)),
    );
    s.push(get("auth-mutation-not-applied", "series").query("id", "1"));

    // --- Public reads ---
    s.push(get("series-by-id", "series").query("id", "1"));
    s.push(get("series-missing", "series").query("id", "99999"));
    s.push(get("series-no-id", "series"));
    s.push(get("series-non-numeric-id", "series").query("id", "abc"));
    s.push(get("series-id-overflow", "series").query("id", &format!("{big}0")));
    s.push(get("series-double-slash", "/v1/api//series").query("id", "1"));
    s.push(get("all-series", "allSeries"));
    s.push(get("find-series", "find/series").query("name", "Harbor"));
    s.push(get("find-series-no-match", "find/series").query("name", "zzz-no-such-series"));
    s.push(get("find-series-no-name", "find/series"));
    s.push(
        get("standings", "series/standings")
            .query("id", "1")
            .query("year", "2026"),
    );
    s.push(
        get("standings-2024", "series/standings")
            .query("id", "1")
            .query("year", "2024"),
    );
    s.push(get("standings-missing-year", "series/standings").query("id", "1"));
    s.push(
        get("standings-unknown-series", "series/standings")
            .query("id", "99999")
            .query("year", "2026"),
    );
    s.push(
        get("standings-non-numeric-year", "series/standings")
            .query("id", "1")
            .query("year", "abc"),
    );
    s.push(
        get("standings-non-numeric-id", "series/standings")
            .query("id", "abc")
            .query("year", "2026"),
    );
    s.push(get("race-class", "raceClass").query("id", "1"));
    s.push(get("race-class-missing", "raceClass").query("id", "99999"));
    s.push(get("race-class-no-id", "raceClass"));
    s.push(get("race-class-non-numeric", "raceClass").query("id", "abc"));
    s.push(get("all-classes", "allClasses"));
    s.push(get("bracket", "bracket").query("id", "16"));
    s.push(get("bracket-missing", "bracket").query("id", "99999"));
    s.push(get("bracket-no-id", "bracket"));
    s.push(get("bracket-non-numeric", "bracket").query("id", "abc"));
    s.push(get("years", "years"));
    s.push(get("all-races", "allRaces").query("year", "2026"));
    s.push(get("all-races-empty-year", "allRaces").query("year", "1990"));
    s.push(get("all-races-non-numeric", "allRaces").query("year", "abc"));
    s.push(get("all-races-no-year", "allRaces"));
    s.push(get("race", "race").query("id", "1"));
    s.push(get("race-missing", "race").query("id", "99999"));
    s.push(get("race-no-id", "race"));
    s.push(get("race-non-numeric", "race").query("id", "abc"));
    s.push(get("race-schedule", "raceSchedule").query("id", "1"));
    s.push(get("race-schedule-missing", "raceSchedule").query("id", "99999"));
    s.push(get("race-schedule-no-id", "raceSchedule"));
    s.push(get("race-schedule-non-numeric", "raceSchedule").query("id", "abc"));
    s.push(get("results", "results").query("raceId", "1"));
    s.push(get("results-unknown-race", "results").query("raceId", "99999"));
    s.push(get("results-no-race-id", "results"));
    s.push(get("results-non-numeric", "results").query("raceId", "abc"));
    s.push(get("schedule-results", "scheduleResults").query("raceId", "1"));
    s.push(get("schedule-results-missing", "scheduleResults").query("raceId", "99999"));
    s.push(get("schedule-results-no-race-id", "scheduleResults"));
    s.push(get("schedule-results-non-numeric", "scheduleResults").query("raceId", "abc"));
    s.push(get("result-count", "resultCount").query("raceId", "1"));
    s.push(get("result-count-unknown-race", "resultCount").query("raceId", "99999"));
    s.push(get("result-count-no-race-id", "resultCount"));
    s.push(get("result-count-non-numeric", "resultCount").query("raceId", "abc"));
    s.push(get("report", "report").query("raceId", "1"));
    s.push(
        get("report-gzip", "report")
            .query("raceId", "1")
            .header("Accept-Encoding", "gzip"),
    );
    s.push(get("report-missing", "report").query("raceId", "99999"));
    s.push(get("report-no-race-id", "report"));
    s.push(get("report-non-numeric", "report").query("raceId", "abc"));

    // --- Admin reads ---
    s.push(get("all-skippers", "allSkippers").admin());
    s.push(get("skipper", "skipper").query("id", "2").admin());
    s.push(
        get("skipper-missing", "skipper")
            .query("id", "99999")
            .admin(),
    );
    s.push(get("skipper-no-id", "skipper").admin());
    s.push(
        get("skipper-non-numeric", "skipper")
            .query("id", "abc")
            .admin(),
    );
    s.push(
        get("find-skipper", "find/skipper")
            .query("name", "John")
            .admin(),
    );
    s.push(
        get("find-skipper-no-match", "find/skipper")
            .query("name", "zzz-no-such-person")
            .admin(),
    );
    s.push(get("find-skipper-no-name", "find/skipper").admin());
    s.push(get("all-boats", "allBoat").admin());
    s.push(get("boat", "boat").query("id", "1").admin());
    s.push(get("boat-missing-id", "boat").query("id", "99999").admin());
    s.push(
        get("boat-by-person", "boat")
            .query("person_id", "23")
            .admin(),
    );
    s.push(
        get("boat-by-person-missing", "boat")
            .query("person_id", "99999")
            .admin(),
    );
    s.push(get("boat-no-params", "boat").admin());
    s.push(get("boat-non-numeric", "boat").query("id", "abc").admin());
    s.push(
        get("boat-non-numeric-person", "boat")
            .query("person_id", "abc")
            .admin(),
    );
    s.push(get("orc-certs", "orcCerts").admin());

    // --- Series ---
    s.push(
        post("series-post-empty-list", "series")
            .json(json!([]))
            .admin(),
    );
    s.push(
        post("series-create", "series")
            .json(json!([series_json(json!(0), "Contract Series", 99, true)]))
            .admin()
            .capture("series_id", "/0/id"),
    );
    s.push(post("series-create-unknown-key", "series").json(json!([{"id": 0, "name": "Contract Unknown Key", "sort": 1, "raceCount": 0, "active": true, "bogus": 1}])).admin());
    s.push(
        post("series-create-empty-body", "series")
            .text("application/json", "")
            .admin(),
    );
    s.push(
        post("series-create-wrong-content-type", "series")
            .text(
                "text/plain",
                r#"[{"id":0,"name":"Contract Text","sort":1}]"#,
            )
            .admin(),
    );
    s.push(
        post("series-create-not-a-list", "series")
            .json(series_json(json!(0), "Contract Object", 1, true))
            .admin(),
    );
    s.push(
        post("series-create-defaults-only", "series")
            .json(json!([{"name": "Contract Defaults"}]))
            .admin()
            .capture("series_defaults_id", "/0/id"),
    );
    s.push(get("series-read-created", "series").query("id", "{{series_id}}"));
    s.push(get("find-series-created", "find/series").query("name", "Contract"));
    s.push(
        post("series-update", "series")
            .json(json!([series_json(
                json!("{{series_id}}"),
                "  Contract Series Renamed  ",
                98,
                false
            )]))
            .admin(),
    );
    s.push(get("all-series-with-created", "allSeries"));

    // --- Classes and brackets ---
    s.push(
        post("class-create", "raceClass")
            .json(json!({
                "raceClass": race_class_json(json!(0), "Contract Class", 50),
                "brackets": [
                    bracket_json(json!(0), "A", 0.0, 100.0, json!(0)),
                    bracket_json(json!(0), "B", 101.0, 200.0, json!(0)),
                ]
            }))
            .admin()
            .capture("class_id", "/raceClass/id")
            .capture("bracket_a", "/brackets/0/id")
            .capture("bracket_b", "/brackets/1/id"),
    );
    s.push(post("class-create-bad-enum", "raceClass").json(json!({"raceClass": {"id": 0, "name": "Bad Enum", "sort": 1, "ratingType": "NOPE", "numberOfRaces": 0, "active": true}, "brackets": []})).admin());
    s.push(post("class-create-unknown-key", "raceClass").json(json!({"raceClass": race_class_json(json!(0), "Unknown Key", 1), "brackets": [], "bogus": true})).admin());
    s.push(
        post("class-create-empty-body", "raceClass")
            .text("application/json", "")
            .admin(),
    );
    s.push(
        post("class-list-update", "raceClassList")
            .json(json!([race_class_json(
                json!("{{class_id}}"),
                "Contract Class Listed",
                51
            )]))
            .admin(),
    );
    s.push(
        post("class-list-empty", "raceClassList")
            .json(json!([]))
            .admin(),
    );
    s.push(
        post("class-list-bad-enum", "raceClassList")
            .json(json!([{"id": 0, "name": "Bad", "sort": 1, "ratingType": "NOPE"}]))
            .admin(),
    );
    s.push(get("class-read-created", "raceClass").query("id", "{{class_id}}"));
    s.push(get("bracket-read-created", "bracket").query("id", "{{bracket_a}}"));
    s.push(get("all-classes-with-created", "allClasses"));
    s.push(
        post("bracket-create", "bracket")
            .json(bracket_json(
                json!(0),
                "C",
                201.0,
                300.0,
                json!("{{class_id}}"),
            ))
            .admin()
            .capture("bracket_c", "/id"),
    );
    s.push(
        post("bracket-create-unknown-class", "bracket")
            .json(bracket_json(json!(0), "Orphan", 0.0, 1.0, json!(99999)))
            .admin(),
    );
    s.push(
        post("bracket-create-unknown-key", "bracket")
            .json(json!({"id": 0, "name": "K", "classId": "{{class_id}}", "bogus": 1}))
            .admin(),
    );
    s.push(
        post("bracket-create-empty-body", "bracket")
            .text("application/json", "")
            .admin(),
    );
    s.push(
        post("class-update-drops-bracket", "raceClass")
            .json(json!({
                "raceClass": race_class_json(json!("{{class_id}}"), "Contract Class", 50),
                "brackets": [
                    bracket_json(json!("{{bracket_a}}"), "A", 0.0, 100.0, json!("{{class_id}}")),
                    bracket_json(json!("{{bracket_b}}"), "B", 101.0, 200.0, json!("{{class_id}}")),
                ]
            }))
            .admin(),
    );
    s.push(get("bracket-dropped", "bracket").query("id", "{{bracket_c}}"));
    s.push(
        delete("class-delete-in-use", "raceClass")
            .query("id", "1")
            .admin(),
    );
    s.push(
        delete("bracket-delete-in-use", "bracket")
            .query("id", "16")
            .admin(),
    );

    // --- People and boats ---
    s.push(
        post("skipper-create", "skipper")
            .json(json!({"id": 0, "first": "Contract", "last": "Skipper", "clubMember": true, "active": true}))
            .admin()
            .capture("person_id", "/id"),
    );
    s.push(post("skipper-create-duplicate-name", "skipper").json(json!({"id": 0, "first": "Contract", "last": "Skipper", "clubMember": false, "active": true})).admin());
    s.push(
        post("skipper-create-unicode", "skipper")
            .json(json!({"id": 0, "first": "Zoë", "last": "Ünïcode ☃ 日本", "clubMember": false, "active": true}))
            .admin()
            .capture("person2_id", "/id"),
    );
    s.push(
        post("skipper-create-unknown-key", "skipper")
            .json(json!({"id": 0, "first": "Unknown", "last": "Key", "bogus": 1}))
            .admin(),
    );
    s.push(
        post("skipper-create-empty-body", "skipper")
            .text("application/json", "")
            .admin(),
    );
    s.push(
        get("skipper-read-created", "skipper")
            .query("id", "{{person_id}}")
            .admin(),
    );
    s.push(
        get("skipper-read-unicode", "skipper")
            .query("id", "{{person2_id}}")
            .admin(),
    );
    s.push(
        get("find-skipper-created", "find/skipper")
            .query("name", "Contract")
            .admin(),
    );
    s.push(
        get("find-skipper-unicode", "find/skipper")
            .query("name", "Ünï")
            .admin(),
    );
    s.push(post("skipper-update", "skipper").json(json!({"id": "{{person_id}}", "first": " Contract ", "last": "Skipper", "clubMember": false, "active": true})).admin());
    s.push(
        post("boat-create", "boat")
            .json(boat_json(json!(0), 100))
            .admin()
            .capture("boat_id", "/id"),
    );
    s.push(post("boat-create-unknown-skipper", "boat").json(json!({"id": 0, "name": "Orphan Boat", "sailNumber": "", "boatType": "", "phrfRating": null, "orcCerts": [], "skipperId": 99999, "ratingType": "PHRF", "numberOfRaces": 0, "active": true})).admin());
    s.push(
        post("boat-create-bad-enum", "boat")
            .json(json!({"id": 0, "name": "Bad Enum Boat", "ratingType": "NOPE"}))
            .admin(),
    );
    s.push(
        post("boat-create-unknown-key", "boat")
            .json(json!({"id": 0, "name": "Unknown Key Boat", "bogus": 1}))
            .admin(),
    );
    s.push(
        post("boat-create-invalid-cert", "boat")
            .json(json!({"id": 0, "name": "Zero Cert Boat", "orcCerts": [{"RefNo": "CONTRACT0"}]}))
            .admin(),
    );
    s.push(
        get("boat-read-created", "boat")
            .query("id", "{{boat_id}}")
            .admin(),
    );
    s.push(
        get("boat-by-created-person", "boat")
            .query("person_id", "{{person_id}}")
            .admin(),
    );
    s.push(
        post("boat-update", "boat")
            .json(boat_json(json!("{{boat_id}}"), 99))
            .admin(),
    );
    s.push(get("all-boats-with-created", "allBoat").admin());
    s.push(get("all-skippers-with-created", "allSkippers").admin());
    s.push(
        delete("skipper-delete-in-use", "skipper")
            .query("id", "31")
            .admin(),
    );
    s.push(
        delete("boat-delete-in-use", "boat")
            .query("id", "1")
            .admin(),
    );

    // --- Races, schedules, results ---
    s.push(
        post("race-create", "race")
            .json(contract_race(json!(0)))
            .admin()
            .capture("race_id", "/id"),
    );
    s.push(
        post("race-create-band-mismatch-5", "race")
            .json(race_json(
                json!(0),
                "Band Mismatch",
                Value::Null,
                Value::Null,
                600,
                ("FiveBandWindwardLeeward", "Low", "High"),
            ))
            .admin(),
    );
    s.push(
        post("race-create-band-mismatch-3", "race")
            .json(race_json(
                json!(0),
                "Band Mismatch",
                Value::Null,
                Value::Null,
                480,
                ("TripleNumberAllPurpose", "Low", "High"),
            ))
            .admin(),
    );
    s.push(
        post("race-create-single-number-any-band", "race")
            .json(race_json(
                json!(0),
                "Contract Single Number",
                json!("{{series_id}}"),
                Value::Null,
                480,
                ("SingleNumberAllPurpose", "Low", "Low"),
            ))
            .admin()
            .capture("race2_id", "/id"),
    );
    s.push(
        post("race-create-bad-enum", "race")
            .json(race_json(
                json!(0),
                "Bad Enum",
                Value::Null,
                Value::Null,
                600,
                ("Bogus", "Low", "LowMedium"),
            ))
            .admin(),
    );
    s.push(
        post("race-create-unknown-series", "race")
            .json(race_json(
                json!(0),
                "Orphan Race",
                json!(99999),
                Value::Null,
                600,
                ("FiveBandWindwardLeeward", "Low", "LowMedium"),
            ))
            .admin(),
    );
    s.push(
        post("race-create-empty-body", "race")
            .text("application/json", "")
            .admin(),
    );
    s.push(
        post("race-create-wrong-content-type", "race")
            .text("text/plain", r#"{"id":0,"name":"Text Race"}"#)
            .admin(),
    );
    s.push(
        post("race-create-unknown-key", "race")
            .json(json!({"id": 0, "name": "Unknown Key Race", "bogus": 1}))
            .admin(),
    );
    s.push(
        post("race-create-defaults-only", "race")
            .json(json!({"name": "Contract Defaults Race"}))
            .admin()
            .capture("race3_id", "/id"),
    );
    s.push(get("race-read-created", "race").query("id", "{{race_id}}"));
    s.push(
        post("race-update", "race")
            .json(race_json(
                json!("{{race_id}}"),
                " Contract Race ",
                json!("{{series_id}}"),
                json!("{{person_id}}"),
                600,
                ("FiveBandWindwardLeeward", "Low", "LowMedium"),
            ))
            .admin(),
    );
    s.push(
        post("schedule-create", "raceSchedule")
            .json(schedule_json(600, "LowMedium"))
            .admin(),
    );
    s.push(
        post("schedule-create-band-mismatch", "raceSchedule")
            .json(schedule_json(600, "High"))
            .admin(),
    );
    s.push(
        post("schedule-create-unknown-key", "raceSchedule")
            .json(json!({"race": contract_race(json!("{{race_id}}")), "bogus": 1}))
            .admin(),
    );
    s.push(
        post("schedule-create-empty-body", "raceSchedule")
            .text("application/json", "")
            .admin(),
    );
    s.push(get("schedule-read-created", "raceSchedule").query("id", "{{race_id}}"));
    s.push(get("all-races-with-created", "allRaces").query("year", "2026"));
    s.push(get("years-after-schedule", "years"));
    s.push(
        post("result-create", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!("{{boat_id}}"),
                json!("2026-06-01T19:30:15Z"),
                json!(100),
                Value::Null,
                "PHRF",
                "TIME",
            ))
            .admin()
            .capture("result_id", "/id"),
    );
    s.push(
        post("result-create-phrf-without-rating", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(1),
                json!("2026-06-01T19:40:00Z"),
                Value::Null,
                Value::Null,
                "PHRF",
                "TIME",
            ))
            .admin(),
    );
    s.push(
        post("result-create-orc-without-cert", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(1),
                json!("2026-06-01T19:40:00Z"),
                Value::Null,
                Value::Null,
                "ORC",
                "TIME",
            ))
            .admin(),
    );
    s.push(
        post("result-create-orc-phrf-without-rating", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(1),
                json!("2026-06-01T19:40:00Z"),
                Value::Null,
                json!("04560003NFK"),
                "ORC_PHRF",
                "TIME",
            ))
            .admin(),
    );
    s.push(
        post("result-create-unknown-boat", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(99999),
                json!("2026-06-01T19:40:00Z"),
                json!(100),
                Value::Null,
                "PHRF",
                "TIME",
            ))
            .admin(),
    );
    s.push(
        post("result-create-unknown-cert", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(1),
                json!("2026-06-01T19:40:00Z"),
                json!(100),
                json!("NOPE"),
                "ORC_PHRF",
                "TIME",
            ))
            .admin(),
    );
    s.push(
        post("result-create-bad-enum", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(1),
                json!("2026-06-01T19:40:00Z"),
                json!(100),
                Value::Null,
                "PHRF",
                "NOPE",
            ))
            .admin(),
    );
    s.push(
        post("result-create-bad-instant", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(1),
                json!("yesterday"),
                json!(100),
                Value::Null,
                "PHRF",
                "TIME",
            ))
            .admin(),
    );
    s.push(
        post("result-create-unknown-key", "results")
            .json(json!({"id": 0, "raceId": "{{race_id}}", "boatId": 1, "bogus": 1}))
            .admin(),
    );
    s.push(
        post("result-create-empty-body", "results")
            .text("application/json", "")
            .admin(),
    );
    s.push(
        post("result-create-orc", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(62),
                json!("2026-06-01T19:45:00Z"),
                json!(90),
                json!("04560003NFK"),
                "ORC_PHRF",
                "TIME",
            ))
            .admin()
            .capture("result2_id", "/id"),
    );
    s.push(
        post("result-create-dnf-cruising", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!(6),
                Value::Null,
                Value::Null,
                Value::Null,
                "CruisingNonFlyingSails",
                "DNF",
            ))
            .admin()
            .capture("result3_id", "/id"),
    );
    s.push(post("result-create-defaults-only", "results").json(json!({"raceId": "{{race_id}}", "boatId": 47, "bracketId": "{{bracket_a}}", "raceClassId": "{{class_id}}"})).admin().capture("result4_id", "/id"));
    s.push(
        post("result-update", "results")
            .json(result_json(
                json!("{{race_id}}"),
                json!("{{boat_id}}"),
                Value::Null,
                json!(100),
                Value::Null,
                "PHRF",
                "DNF",
            ))
            .admin(),
    );
    s.push(get("results-created", "results").query("raceId", "{{race_id}}"));
    s.push(get("schedule-results-created", "scheduleResults").query("raceId", "{{race_id}}"));
    s.push(get("result-count-created", "resultCount").query("raceId", "{{race_id}}"));
    s.push(get("report-created", "report").query("raceId", "{{race_id}}"));
    s.push(get("report-unscheduled-race", "report").query("raceId", "{{race2_id}}"));
    s.push(
        get("standings-created", "series/standings")
            .query("id", "{{series_id}}")
            .query("year", "2026"),
    );
    s.push(
        delete("race-delete-with-results", "race")
            .query("id", "{{race_id}}")
            .admin(),
    );

    // --- Image upload and download (race 1 keeps the image: there is no delete route) ---
    // The download route is `ApiPaths.image.versionedApi() + "{name}"` = `/v1/api//image{name}`: one
    // segment with a literal prefix, so the image is at `/v1/api/imagecontract.png` and
    // `/v1/api/image/contract.png` is the SPA fallback.
    s.push(
        post("image-upload", "image")
            .query("raceId", "1")
            .multipart(vec![png_part(), text_part()])
            .admin(),
    );
    s.push(
        post("image-upload-no-image-part", "image")
            .query("raceId", "1")
            .multipart(vec![text_part()])
            .admin(),
    );
    s.push(
        post("image-upload-no-parts", "image")
            .query("raceId", "1")
            .multipart(vec![])
            .admin(),
    );
    s.push(
        post("image-upload-no-race-id", "image")
            .multipart(vec![png_part()])
            .admin(),
    );
    s.push(
        post("image-upload-non-numeric-race-id", "image")
            .query("raceId", "abc")
            .multipart(vec![png_part()])
            .admin(),
    );
    s.push(
        post("image-upload-unknown-race", "image")
            .query("raceId", "99999")
            .multipart(vec![Part {
                filename: Some("orphan.png".into()),
                ..png_part()
            }])
            .admin(),
    );
    s.push(
        post("image-upload-not-multipart", "image")
            .query("raceId", "1")
            .text("text/plain", "not multipart")
            .admin(),
    );
    s.push(
        post("image-upload-missing-header", "image")
            .query("raceId", "1")
            .multipart(vec![png_part()]),
    );
    s.push(get("image-get", &format!("image{IMAGE_NAME}")).admin());
    s.push(get("image-get-missing", "imagenope.png").admin());
    s.push(get("image-get-empty-name", "image").admin());
    s.push(get(
        "image-get-missing-header",
        &format!("image{IMAGE_NAME}"),
    ));
    s.push(get("image-get-bearer-none", &format!("image{IMAGE_NAME}")).auth(raw("Bearer none")));
    s.push(
        get(
            "image-get-slash-path",
            &format!("/v1/api/image/{IMAGE_NAME}"),
        )
        .admin()
        .fallback(),
    );
    s.push(get("race-schedule-with-image", "raceSchedule").query("id", "1"));
    s.push(get("report-with-image", "report").query("raceId", "1"));

    // --- ORC certificates ---
    s.push(
        post("orc-cert-replace-same", "orcCert")
            .captured_body("cert_nfk", vec![])
            .admin(),
    );
    s.push(
        post("orc-cert-unknown-ref", "orcCert")
            .captured_body("cert_nfk", vec![("/RefNo", json!("CONTRACT-NOPE"))])
            .admin(),
    );
    s.push(
        post("orc-cert-invalid", "orcCert")
            .captured_body("cert_nfk", vec![("/APHD", json!(0.0))])
            .admin(),
    );
    s.push(
        post("orc-cert-unknown-key", "orcCert")
            .captured_body("cert_nfk", vec![("/bogus", json!(1))])
            .admin(),
    );
    s.push(
        post("orc-cert-empty-body", "orcCert")
            .text("application/json", "")
            .admin(),
    );
    s.push(post("orc-cert-missing-header", "orcCert").captured_body("cert_nfk", vec![]));
    s.push(get("orc-certs-after", "orcCerts").admin());

    // --- Deletes, in dependency order ---
    s.push(
        delete("result-delete", "results")
            .query("id", "{{result_id}}")
            .admin(),
    );
    s.push(
        delete("result-delete-orc", "results")
            .query("id", "{{result2_id}}")
            .admin(),
    );
    s.push(
        delete("result-delete-dnf", "results")
            .query("id", "{{result3_id}}")
            .admin(),
    );
    s.push(
        delete("result-delete-defaults", "results")
            .query("id", "{{result4_id}}")
            .admin(),
    );
    s.push(
        delete("result-delete-missing", "results")
            .query("id", "99999")
            .admin(),
    );
    s.push(delete("result-delete-no-id", "results").admin());
    s.push(
        delete("result-delete-non-numeric", "results")
            .query("id", "abc")
            .admin(),
    );
    s.push(get("results-after-delete", "results").query("raceId", "{{race_id}}"));
    s.push(
        delete("race-delete", "race")
            .query("id", "{{race_id}}")
            .admin(),
    );
    s.push(
        delete("race-delete-unscheduled", "race")
            .query("id", "{{race2_id}}")
            .admin(),
    );
    s.push(
        delete("race-delete-defaults", "race")
            .query("id", "{{race3_id}}")
            .admin(),
    );
    s.push(
        delete("race-delete-non-numeric", "race")
            .query("id", "abc")
            .admin(),
    );
    s.push(delete("race-delete-no-id", "race").admin());
    s.push(
        delete("race-delete-missing", "race")
            .query("id", "99999")
            .admin(),
    );
    s.push(
        delete("race-delete-race-1-with-results", "race")
            .query("id", "1")
            .admin(),
    );
    s.push(get("race-deleted", "race").query("id", "{{race_id}}"));
    s.push(
        delete("bracket-delete-a", "bracket")
            .query("id", "{{bracket_a}}")
            .admin(),
    );
    s.push(
        delete("bracket-delete-b", "bracket")
            .query("id", "{{bracket_b}}")
            .admin(),
    );
    s.push(
        delete("bracket-delete-c-already-gone", "bracket")
            .query("id", "{{bracket_c}}")
            .admin(),
    );
    s.push(delete("bracket-delete-no-id", "bracket").admin());
    s.push(
        delete("bracket-delete-non-numeric", "bracket")
            .query("id", "abc")
            .admin(),
    );
    s.push(
        delete("class-delete", "raceClass")
            .query("id", "{{class_id}}")
            .admin(),
    );
    s.push(delete("class-delete-no-id", "raceClass").admin());
    s.push(
        delete("class-delete-non-numeric", "raceClass")
            .query("id", "abc")
            .admin(),
    );
    s.push(
        delete("class-delete-missing", "raceClass")
            .query("id", "99999")
            .admin(),
    );
    s.push(get("class-deleted", "raceClass").query("id", "{{class_id}}"));
    s.push(
        delete("boat-delete", "boat")
            .query("id", "{{boat_id}}")
            .admin(),
    );
    s.push(delete("boat-delete-no-id", "boat").admin());
    s.push(
        delete("boat-delete-non-numeric", "boat")
            .query("id", "abc")
            .admin(),
    );
    s.push(
        delete("boat-delete-missing", "boat")
            .query("id", "99999")
            .admin(),
    );
    s.push(
        get("boat-deleted", "boat")
            .query("id", "{{boat_id}}")
            .admin(),
    );
    s.push(
        delete("skipper-delete", "skipper")
            .query("id", "{{person_id}}")
            .admin(),
    );
    s.push(
        delete("skipper-delete-unicode", "skipper")
            .query("id", "{{person2_id}}")
            .admin(),
    );
    s.push(delete("skipper-delete-no-id", "skipper").admin());
    s.push(
        delete("skipper-delete-non-numeric", "skipper")
            .query("id", "abc")
            .admin(),
    );
    s.push(
        delete("skipper-delete-missing", "skipper")
            .query("id", "99999")
            .admin(),
    );
    s.push(
        get("skipper-deleted", "skipper")
            .query("id", "{{person_id}}")
            .admin(),
    );
    s.push(
        delete("series-delete", "series")
            .query("id", "{{series_id}}")
            .admin(),
    );
    s.push(
        delete("series-delete-defaults", "series")
            .query("id", "{{series_defaults_id}}")
            .admin(),
    );
    s.push(
        delete("series-delete-in-use", "series")
            .query("id", "1")
            .admin(),
    );
    s.push(delete("series-delete-no-id", "series").admin());
    s.push(
        delete("series-delete-non-numeric", "series")
            .query("id", "abc")
            .admin(),
    );
    s.push(
        delete("series-delete-missing", "series")
            .query("id", "99999")
            .admin(),
    );
    s.push(get("series-deleted", "series").query("id", "{{series_id}}"));

    // --- Fallback: unmatched paths, unmatched methods ---
    s.push(get("fallback-root", "/").fallback());
    s.push(get("fallback-unknown-page", "/no/such/page").fallback());
    s.push(get("fallback-unknown-api", "/v1/api/noSuchRoute").fallback());
    s.push(put("fallback-wrong-method", "/v1/api/series").fallback());
    s.push(delete("fallback-wrong-method-public", "/v1/api/allSeries").fallback());
    s.push(get("fallback-image-no-name", "/v1/api/image/").fallback());
    s.push(get("fallback-api-root", "/v1/api").fallback());

    if options.random > 0 {
        s.extend(random_steps(options.random, options.seed));
    }
    s
}

/// The scenario with its admin password.
pub fn scenario(options: &ScenarioOptions) -> Scenario {
    Scenario {
        steps: steps(options),
        admin_password: ADMIN_PASSWORD.to_string(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::step::{Body, Compare, Method};
    use std::collections::{BTreeSet, HashSet};

    #[test]
    fn covers_every_route_and_method_in_the_table() {
        let steps = steps(&ScenarioOptions::default());
        let covered: HashSet<(String, String)> = steps
            .iter()
            .filter_map(|s| s.route().map(|r| (s.method.to_string(), r)))
            .collect();
        let mut missing = Vec::new();
        for (method, path) in ROUTES {
            if SKIPPED_ROUTES.contains(&(method, path)) {
                continue;
            }
            if !covered.contains(&(method.to_string(), path.to_string())) {
                missing.push(format!("{method} {path}"));
            }
        }
        assert!(missing.is_empty(), "routes without a step: {missing:?}");
        for (method, path) in SKIPPED_ROUTES {
            assert!(
                !covered.contains(&(method.to_string(), path.to_string())),
                "{method} {path} must be skipped"
            );
        }
        // No step names a route outside the table (a typo would silently hit the fallback).
        let table: HashSet<(String, String)> = ROUTES
            .iter()
            .map(|(m, p)| (m.to_string(), p.to_string()))
            .collect();
        for (method, route) in &covered {
            assert!(
                table.contains(&(method.clone(), route.clone())),
                "{method} {route} is not in the route table"
            );
        }
    }

    #[test]
    fn step_names_are_unique_and_file_safe() {
        let steps = steps(&ScenarioOptions::default());
        let mut seen = BTreeSet::new();
        for s in &steps {
            assert!(
                seen.insert(s.name.clone()),
                "duplicate step name {}",
                s.name
            );
            assert!(
                s.name
                    .chars()
                    .all(|c| c.is_ascii_alphanumeric() || c == '-'),
                "step name {} is not a file name",
                s.name
            );
        }
        assert!(steps.len() >= 200, "{} steps", steps.len());
    }

    #[test]
    fn placeholders_resolve_in_order() {
        // Every placeholder a step uses was captured by an earlier step.
        let steps = steps(&ScenarioOptions {
            random: 10,
            ..ScenarioOptions::default()
        });
        let mut captured: HashSet<String> = HashSet::new();
        for s in &steps {
            let mut text = s.path.clone();
            for (_, v) in &s.query {
                text.push_str(v);
            }
            match &s.body {
                Body::Json(v) => text.push_str(&v.to_string()),
                Body::Captured { capture, .. } => assert!(
                    captured.contains(capture),
                    "{}: {capture} not captured yet",
                    s.name
                ),
                _ => {}
            }
            if s.auth == AuthMode::Admin {
                assert!(captured.contains("token"), "{}: admin before login", s.name);
            }
            if let AuthMode::Forged(Forge {
                id: ForgeId::Captured,
                ..
            }) = &s.auth
            {
                assert!(
                    captured.contains("auth_id"),
                    "{}: forged before the admin exists",
                    s.name
                );
            }
            let mut rest = text.as_str();
            while let Some(start) = rest.find("{{") {
                let end = rest[start..].find("}}").unwrap() + start;
                let name = &rest[start + 2..end];
                assert!(
                    captured.contains(name),
                    "{}: {{{{{name}}}}} not captured yet",
                    s.name
                );
                rest = &rest[end + 2..];
            }
            for c in &s.captures {
                captured.insert(c.name.clone());
            }
        }
    }

    #[test]
    fn bootstrap_steps_come_before_the_admin_exists() {
        let steps = steps(&ScenarioOptions::default());
        let admin_at = steps
            .iter()
            .position(|s| s.name == "auth-create-admin")
            .unwrap();
        for s in &steps[..admin_at] {
            assert!(
                matches!(s.auth, AuthMode::None | AuthMode::Raw(_)),
                "{} sends a real token during bootstrap",
                s.name
            );
        }
        let login_at = steps.iter().position(|s| s.name == "login").unwrap();
        assert!(login_at > admin_at);
        let login = &steps[login_at];
        assert_eq!(login.compare, Compare::JsonWithVolatile);
        assert_eq!(login.volatile, vec!["/salt", "/hashOfHash", "/expires"]);
        let fixed = self::steps(&ScenarioOptions {
            expires_volatile: false,
            ..ScenarioOptions::default()
        });
        assert_eq!(fixed[login_at].volatile, vec!["/salt", "/hashOfHash"]);
    }

    #[test]
    fn mutations_are_undone_before_the_end() {
        let steps = steps(&ScenarioOptions::default());
        let position = |name: &str| {
            steps
                .iter()
                .position(|s| s.name == name)
                .unwrap_or_else(|| panic!("no step {name}"))
        };
        assert!(position("result-delete") > position("result-create"));
        assert!(position("race-delete") > position("result-delete"));
        assert!(position("bracket-delete-a") > position("race-delete"));
        assert!(position("class-delete") > position("bracket-delete-b"));
        assert!(position("boat-delete") > position("result-delete"));
        assert!(position("skipper-delete") > position("boat-delete"));
        assert!(position("series-delete") > position("race-delete"));
        let last_mutation = steps
            .iter()
            .rposition(|s| s.method != Method::Get && s.compare == Compare::Full)
            .unwrap();
        assert_eq!(steps[last_mutation].name, "series-delete-missing");
    }

    #[test]
    fn fallback_steps_use_absolute_paths_and_media_type_compare() {
        let steps = steps(&ScenarioOptions::default());
        let fallbacks: Vec<&Step> = steps
            .iter()
            .filter(|s| s.compare == Compare::StatusAndMediaType)
            .collect();
        assert_eq!(fallbacks.len(), 8);
        for s in fallbacks {
            assert!(s.path.starts_with('/'), "{}", s.name);
            assert_eq!(s.route(), None);
        }
    }

    #[test]
    fn random_steps_are_appended() {
        let base = steps(&ScenarioOptions::default()).len();
        let with = steps(&ScenarioOptions {
            random: 7,
            ..ScenarioOptions::default()
        });
        assert_eq!(with.len(), base + 7);
        assert!(with[base].name.starts_with("random-001"));
    }
}

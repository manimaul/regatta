//! End-to-end test of record, then replay, then diff against the in-process stub server (`tests/support`).

mod support;

use regatta_contract::cassette::{Cassette, Meta};
use regatta_contract::compare::Mismatch;
use regatta_contract::{diff, record, replay, scenario, Options, ScenarioOptions};
use std::path::PathBuf;
use support::{spawn, StubConfig};

fn temp_dir(tag: &str) -> PathBuf {
    let dir =
        std::env::temp_dir().join(format!("regatta-contract-e2e-{tag}-{}", std::process::id()));
    let _ = std::fs::remove_dir_all(&dir);
    dir
}

fn quiet() -> Options {
    Options {
        quiet: true,
        fixed_now: None,
    }
}

#[tokio::test]
async fn record_then_replay_is_clean_and_detects_changes() {
    let scenario = scenario(&ScenarioOptions::default());
    let dir = temp_dir("cassettes");

    // Record from one stub.
    let recorder = spawn(StubConfig::default()).await;
    let report = record(&recorder, &dir, &scenario, &quiet()).await.unwrap();
    assert!(report.is_clean(), "{report}");
    assert_eq!(report.outcomes.len(), scenario.steps.len());
    let files: Vec<String> = std::fs::read_dir(&dir)
        .unwrap()
        .map(|e| e.unwrap().file_name().to_string_lossy().to_string())
        .collect();
    assert_eq!(
        files.len(),
        scenario.steps.len() + 1,
        "one cassette per step plus _meta.json"
    );
    assert!(files.contains(&"001-bootstrap-missing-header.json".to_string()));
    assert!(files.contains(&"005-bootstrap-all-skippers.json".to_string()));
    let meta = Meta::read(&dir).unwrap().unwrap();
    assert_eq!(meta.steps, scenario.steps.len());

    // The live token is replaced by the placeholder; forged and literal tokens are kept.
    let login_index = scenario
        .steps
        .iter()
        .position(|s| s.name == "login")
        .unwrap()
        + 1;
    let login = Cassette::read_step(&dir, login_index, "login").unwrap();
    assert_eq!(login.response.status, 200);
    assert_eq!(login.volatile, vec!["/salt", "/hashOfHash", "/expires"]);
    let admin = Cassette::read_step(&dir, login_index + 1, "auth-admin-token").unwrap();
    assert_eq!(admin.request.headers["authorization"], "Bearer {{token}}");
    assert_eq!(admin.response.status, 200);
    let none_index = scenario
        .steps
        .iter()
        .position(|s| s.name == "auth-bearer-none")
        .unwrap()
        + 1;
    let none = Cassette::read_step(&dir, none_index, "auth-bearer-none").unwrap();
    assert_eq!(none.request.headers["authorization"], "Bearer none");
    let expired_index = scenario
        .steps
        .iter()
        .position(|s| s.name == "auth-expired-token")
        .unwrap()
        + 1;
    let expired = Cassette::read_step(&dir, expired_index, "auth-expired-token").unwrap();
    assert_eq!(expired.response.status, 401);
    let forged_index = scenario
        .steps
        .iter()
        .position(|s| s.name == "auth-forged-valid-token")
        .unwrap()
        + 1;
    assert_eq!(
        Cassette::read_step(&dir, forged_index, "auth-forged-valid-token")
            .unwrap()
            .response
            .status,
        200
    );
    let image_index = scenario
        .steps
        .iter()
        .position(|s| s.name == "image-get")
        .unwrap()
        + 1;
    let image = Cassette::read_step(&dir, image_index, "image-get").unwrap();
    assert_eq!(image.response.status, 200);
    assert_eq!(image.response.header("content-type"), Some("image/png"));
    assert!(matches!(
        image.response.body,
        Some(regatta_contract::cassette::RecordedBody::Base64(_))
    ));

    // Replay against a fresh stub: different login salt, same everything else.
    let replayer = spawn(StubConfig::default()).await;
    let report = replay(&replayer, &dir, &scenario, &quiet()).await.unwrap();
    assert!(report.is_clean(), "{report}");
    assert_eq!(report.outcomes.len(), scenario.steps.len());

    // A stub that renames series: value mismatches on the series echoes only.
    let changed = spawn(StubConfig {
        series_suffix: " (changed)".into(),
        ..StubConfig::default()
    })
    .await;
    let report = replay(&changed, &dir, &scenario, &quiet()).await.unwrap();
    assert!(!report.is_clean());
    let failed: Vec<&str> = report.failed().map(|o| o.name.as_str()).collect();
    assert!(failed.contains(&"series-create"), "{failed:?}");
    assert!(!failed.contains(&"series-post-empty-list"), "{failed:?}");
    assert!(!failed.contains(&"class-create"), "{failed:?}");
    let series_create = report.failed().find(|o| o.name == "series-create").unwrap();
    assert!(
        matches!(&series_create.mismatches[0], Mismatch::JsonValue(diffs) if diffs[0].pointer == "/0/name")
    );
    let text = report.to_string();
    assert!(
        text.contains("### ") && text.contains("series-create") && text.contains("/0/name"),
        "{text}"
    );

    // A stub that reverses series keys: order mismatches, reported apart from values.
    let reordered = spawn(StubConfig {
        reorse_series_keys: true,
        ..StubConfig::default()
    })
    .await;
    let report = replay(&reordered, &dir, &scenario, &quiet()).await.unwrap();
    assert!(!report.is_clean());
    let series_create = report.failed().find(|o| o.name == "series-create").unwrap();
    assert!(
        matches!(&series_create.mismatches[0], Mismatch::JsonOrder(diffs) if diffs[0].pointer == "/0"),
        "{:?}",
        series_create.mismatches
    );

    std::fs::remove_dir_all(&dir).unwrap();
}

#[tokio::test]
async fn replay_without_cassettes_reports_and_stops() {
    let scenario = scenario(&ScenarioOptions::default());
    let stub = spawn(StubConfig::default()).await;
    let report = replay(&stub, &temp_dir("missing"), &scenario, &quiet())
        .await
        .unwrap();
    assert!(!report.is_clean());
    assert!(report.aborted.is_some());
    assert_eq!(report.outcomes.len(), 1);
}

#[tokio::test]
async fn diff_two_stubs_with_random_steps() {
    let scenario = scenario(&ScenarioOptions {
        random: 12,
        seed: 9,
        ..ScenarioOptions::default()
    });
    let left = spawn(StubConfig::default()).await;
    let right = spawn(StubConfig::default()).await;
    let report = diff(&left, &right, &scenario, &quiet()).await.unwrap();
    assert!(report.is_clean(), "{report}");
    assert_eq!(report.outcomes.len(), scenario.steps.len());
    assert!(report
        .outcomes
        .last()
        .unwrap()
        .name
        .starts_with("random-012"));

    let left = spawn(StubConfig::default()).await;
    let changed = spawn(StubConfig {
        series_suffix: "!".into(),
        ..StubConfig::default()
    })
    .await;
    let report = diff(&left, &changed, &scenario, &quiet()).await.unwrap();
    assert!(!report.is_clean());
    assert!(report.failed().any(|o| o.name == "series-create"));
}

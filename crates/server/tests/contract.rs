//! T2: the contract cassettes recorded from the Kotlin server replay cleanly against the Rust
//! server, run in-process on a fresh load of the golden dump with the same fixed clock.

mod support;

use regatta_contract::{replay, scenario, Options, ScenarioOptions};
use regatta_server::{build_router, AppState};
use std::path::Path;

const CASSETTES: &str = "../contract/cassettes";

#[tokio::test(flavor = "multi_thread")]
async fn contract_cassettes_replay_cleanly() {
    let golden = support::start().await;
    let state = AppState::new(
        golden.db.clone(),
        Some(support::DUMP_INSTANT.parse().unwrap()),
    );
    let app = build_router(state);
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let base_url = format!("http://{}", listener.local_addr().unwrap());
    tokio::spawn(async move {
        axum::serve(
            listener,
            axum::ServiceExt::<axum::extract::Request>::into_make_service(app),
        )
        .await
        .unwrap();
    });

    let cassettes = Path::new(env!("CARGO_MANIFEST_DIR")).join(CASSETTES);
    let report = replay(
        &base_url,
        &cassettes,
        &scenario(&ScenarioOptions::default()),
        &Options {
            quiet: true,
            fixed_now: None,
        },
    )
    .await
    .expect("replay");
    println!("{report}");
    assert!(
        report.is_clean(),
        "contract replay has mismatches:\n{report}"
    );
    assert_eq!(report.outcomes.len(), 277, "every recorded step ran");
}

//! `regatta`: the server binary. Configuration from the environment or flags (see
//! `regatta_service::Config`), the startup schema check (D6), then axum on `LEPTOS_SITE_ADDR`
//! when cargo-leptos (or the image) set it, else `REGATTA_BIND`.

use anyhow::{Context, Result};
use clap::Parser;
use regatta_server::{build_router, leptos_env_configured, AppState, RENDER_THREAD_STACK};
use regatta_service::{startup, Config, Db};
use std::process::ExitCode;
use tracing_subscriber::EnvFilter;

fn main() -> ExitCode {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();
    // Pages render on the worker threads; see `RENDER_THREAD_STACK`.
    let runtime = match tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .thread_stack_size(RENDER_THREAD_STACK)
        .build()
    {
        Ok(runtime) => runtime,
        Err(e) => {
            tracing::error!("tokio runtime: {e}");
            return ExitCode::FAILURE;
        }
    };
    match runtime.block_on(run()) {
        Ok(()) => ExitCode::SUCCESS,
        Err(e) => {
            tracing::error!("{e:#}");
            ExitCode::FAILURE
        }
    }
}

async fn run() -> Result<()> {
    let config = Config::parse();
    let pool = startup::connect(&config.db_url).await?;
    startup::prepare(&pool).await?;
    if let Some(now) = config.fixed_now {
        tracing::warn!("fixed clock: now = {now} (REGATTA_FIXED_NOW is for tests only)");
    }
    let state = AppState::new(Db::new(pool), config.fixed_now);
    let bind = if leptos_env_configured() {
        state.leptos_options.site_addr.to_string()
    } else {
        config.bind.clone()
    };
    tracing::info!(
        "site root {} (output {})",
        state.leptos_options.site_root,
        state.leptos_options.output_name
    );
    let app = build_router(state);
    let listener = tokio::net::TcpListener::bind(&bind)
        .await
        .with_context(|| format!("binding {bind}"))?;
    tracing::info!("listening on {}", listener.local_addr()?);
    axum::serve(
        listener,
        axum::ServiceExt::<axum::extract::Request>::into_make_service(app),
    )
    .with_graceful_shutdown(async {
        let _ = tokio::signal::ctrl_c().await;
        tracing::info!("shutting down");
    })
    .await
    .context("serving")?;
    Ok(())
}

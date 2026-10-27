//! A throwaway Postgres loaded with the scrubbed production snapshot, for the contract and
//! golden tests. Needs a container runtime: `DOCKER_HOST` is taken from
//! `podman machine inspect` when unset and there is no `/var/run/docker.sock`, and Ryuk is
//! disabled.

#![allow(dead_code)]

use regatta_service::{startup, Db};
use sqlx::PgPool;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::Once;
use std::time::Duration;
use testcontainers::core::{ExecCommand, IntoContainerPort, WaitFor};
use testcontainers::runners::AsyncRunner;
use testcontainers::{ContainerAsync, GenericImage, ImageExt};

/// The scrubbed dump at schema version 5.
pub const DUMP: &str = "../scoring/tests/golden/regatta-2026-10-13-v5.sql";
/// The approved golden outputs.
pub const APPROVED: &str = "../scoring/tests/golden/approved";
/// The instant the snapshot was taken, also the contract's fixed clock.
pub const DUMP_INSTANT: &str = "2026-10-14T02:10:11Z";

const USER: &str = "regatta_admin";
const PASSWORD: &str = "mysecretpassword";
const DATABASE: &str = "regatta";

pub fn repo_path(relative: &str) -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR")).join(relative)
}

static RUNTIME: Once = Once::new();

/// Points testcontainers at the container runtime, or panics with what to do.
fn configure_runtime() {
    RUNTIME.call_once(|| {
        if std::env::var_os("DOCKER_HOST").is_none() && !Path::new("/var/run/docker.sock").exists()
        {
            let output = Command::new("podman")
                .args([
                    "machine",
                    "inspect",
                    "--format",
                    "{{.ConnectionInfo.PodmanSocket.Path}}",
                ])
                .output();
            let path = output
                .ok()
                .filter(|o| o.status.success())
                .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
                .filter(|p| !p.is_empty());
            match path {
                Some(path) => std::env::set_var("DOCKER_HOST", format!("unix://{path}")),
                None => panic!(
                    "this test needs a container runtime: DOCKER_HOST is not set, there is no \
                     /var/run/docker.sock, and `podman machine inspect` failed. Start the podman \
                     machine (`podman machine start`) or set DOCKER_HOST."
                ),
            }
        }
        if std::env::var_os("TESTCONTAINERS_RYUK_DISABLED").is_none() {
            std::env::set_var("TESTCONTAINERS_RYUK_DISABLED", "true");
        }
    });
}

/// The database under test: the container (dropped with it), the pool and the service layer.
pub struct GoldenDb {
    pub container: ContainerAsync<GenericImage>,
    pub pool: PgPool,
    pub db: Db,
    pub url: String,
}

/// Starts `postgres:16-bookworm`, loads the dump with `psql` inside the container (the dump uses
/// `COPY ... FROM stdin`, which a driver cannot run), then runs the server's own startup path.
pub async fn start() -> GoldenDb {
    configure_runtime();
    let dump = std::fs::read(repo_path(DUMP))
        .unwrap_or_else(|e| panic!("reading {}: {e}", repo_path(DUMP).display()));
    let container = GenericImage::new("postgres", "16-bookworm")
        .with_exposed_port(5432.tcp())
        .with_wait_for(WaitFor::message_on_stderr(
            "database system is ready to accept connections",
        ))
        .with_wait_for(WaitFor::message_on_stdout(
            "database system is ready to accept connections",
        ))
        .with_env_var("POSTGRES_USER", USER)
        .with_env_var("POSTGRES_PASSWORD", PASSWORD)
        .with_env_var("POSTGRES_DB", DATABASE)
        .with_copy_to("/dump.sql", dump)
        .start()
        .await
        .expect("starting the postgres container (is the container runtime up?)");
    wait_until_ready(&container).await;
    let mut load = container
        .exec(ExecCommand::new([
            "psql",
            "-v",
            "ON_ERROR_STOP=1",
            "-U",
            USER,
            "-d",
            DATABASE,
            "-f",
            "/dump.sql",
        ]))
        .await
        .expect("psql");
    let code = load.exit_code().await.expect("psql exit code");
    if code != Some(0) {
        let stderr =
            String::from_utf8_lossy(&load.stderr_to_vec().await.unwrap_or_default()).to_string();
        panic!("loading {DUMP} failed (exit {code:?}):\n{stderr}");
    }
    let port = container
        .get_host_port_ipv4(5432.tcp())
        .await
        .expect("host port");
    let url = format!("postgres://{USER}:{PASSWORD}@127.0.0.1:{port}/{DATABASE}");
    let pool = startup::connect(&url)
        .await
        .expect("connecting to the container");
    // the production startup path: the schema version check
    startup::prepare(&pool).await.expect("startup");
    GoldenDb {
        db: Db::new(pool.clone()),
        container,
        pool,
        url,
    }
}

/// The entrypoint restarts the server once after init; wait for the final one.
async fn wait_until_ready(container: &ContainerAsync<GenericImage>) {
    for _ in 0..60 {
        let ready = container
            .exec(ExecCommand::new(["pg_isready", "-U", USER, "-d", DATABASE]))
            .await
            .expect("pg_isready");
        if ready.exit_code().await.expect("exit code") == Some(0) {
            return;
        }
        tokio::time::sleep(Duration::from_millis(500)).await;
    }
    panic!("postgres did not become ready");
}

/// Runs `make()`'s future to completion on a multi-thread runtime whose threads (and the calling
/// thread) have `RENDER_THREAD_STACK`: SSR of the larger screens overflows the default test
/// thread stack in debug builds, exactly as it would overflow a default tokio worker.
pub fn block_on_with_render_stack<F, Fut>(make: F) -> Fut::Output
where
    F: FnOnce() -> Fut + Send + 'static,
    Fut: std::future::Future,
    Fut::Output: Send + 'static,
{
    std::thread::Builder::new()
        .stack_size(regatta_server::RENDER_THREAD_STACK)
        .spawn(move || {
            tokio::runtime::Builder::new_multi_thread()
                .enable_all()
                .thread_stack_size(regatta_server::RENDER_THREAD_STACK)
                .build()
                .expect("tokio runtime")
                .block_on(make())
        })
        .expect("render thread")
        .join()
        .expect("render thread panicked")
}

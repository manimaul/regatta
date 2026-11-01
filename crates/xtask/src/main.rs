//! The `cargo xtask` tasks: running the server, building the container image and the Kubernetes deploy.
//!
//! The `xtask` alias is in `.cargo/config.toml`, and every command runs from the repository root.
//!
//! `k8s_deploy/secrets.yaml` carries a `{REGATTA_DB_URL}` placeholder, filled from the
//! `REGATTA_PG_HOST` / `REGATTA_PG_PORT` / `REGATTA_PG_USER` / `REGATTA_PG_PASS` environment
//! variables and piped to `kubectl apply -f -`, so a filled manifest never touches the disk and no
//! environment-specific endpoint is committed. `server.yaml` has no placeholders and needs neither.

use anyhow::{bail, Context, Result};
use clap::{Parser, Subcommand};
use std::path::{Path, PathBuf};
use xshell::{cmd, Shell};

const IMAGE: &str = "ghcr.io/manimaul/regatta:latest";
const PLATFORM: &str = "linux/amd64";
const NAMESPACE: &str = "mxmariner";
const DEPLOYMENT: &str = "deployment/regatta-deployment";
const DB_NAME: &str = "regatta";

#[derive(Parser)]
#[command(
    name = "cargo xtask",
    bin_name = "cargo xtask",
    about = "regatta build, image and deploy tasks"
)]
struct Cli {
    #[command(subcommand)]
    task: Task,
}

#[derive(Subcommand)]
enum Task {
    /// Build and serve the release site on :8888 (needs the database: `cd database && podman-compose up`).
    Run,
    /// Build the linux/amd64 container image ghcr.io/manimaul/regatta:latest.
    MakeImg,
    /// Build the image, then push it to ghcr.io (needs `podman login ghcr.io`).
    PubImg,
    /// Apply the database URL secret (first deploy and credential changes).
    K8sApplySecrets(KubectlArgs),
    /// Apply the server manifest (autoscaler, service, deployment, certificate, ingress).
    K8sApplyServer(KubectlArgs),
    /// Delete everything the server manifest creates.
    K8sDeleteServer(KubectlArgs),
    /// Restart the deployment and wait for the rollout.
    RolloutRestart,
    /// The full deploy: pub-img, k8s-apply-server, rollout-restart.
    DeployServer,
    /// The golden regression: race reports and standings over the snapshot, byte for byte (needs podman).
    Golden,
    /// Replay the recorded REST contract against an in-process server (needs podman).
    Contract,
}

#[derive(clap::Args)]
struct KubectlArgs {
    /// Pass `--dry-run=client` to kubectl: validate the filled manifest without applying it.
    #[arg(long)]
    dry_run: bool,
}

fn main() -> Result<()> {
    let cli = Cli::parse();
    let sh = Shell::new()?;
    sh.change_dir(repo_root());
    match cli.task {
        Task::Run => cmd!(sh, "cargo leptos serve --release").run()?,
        Task::MakeImg => make_img(&sh)?,
        Task::PubImg => pub_img(&sh)?,
        Task::K8sApplySecrets(args) => kubectl(&sh, "apply", "secrets.yaml", args.dry_run)?,
        Task::K8sApplyServer(args) => kubectl(&sh, "apply", "server.yaml", args.dry_run)?,
        Task::K8sDeleteServer(args) => kubectl(&sh, "delete", "server.yaml", args.dry_run)?,
        Task::RolloutRestart => rollout_restart(&sh)?,
        Task::DeployServer => {
            pub_img(&sh)?;
            kubectl(&sh, "apply", "server.yaml", false)?;
            rollout_restart(&sh)?;
        }
        Task::Golden => cmd!(sh, "cargo test -p regatta-server --test golden").run()?,
        Task::Contract => cmd!(sh, "cargo test -p regatta-server --test contract").run()?,
    }
    Ok(())
}

fn repo_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .ancestors()
        .nth(2)
        .expect("crates/xtask sits two levels below the repository root")
        .to_path_buf()
}

/// Builds the linux/amd64 image and checks that it really is amd64.
///
/// The build stage has no `.git`, so the git hash, branch and dirty flag go in as the `REGATTA_GIT_*` build
/// arguments (`unknown` when git cannot tell).
fn make_img(sh: &Shell) -> Result<()> {
    let git = |args: &[&str]| -> String {
        cmd!(sh, "git {args...}")
            .quiet()
            .ignore_stderr()
            .read()
            .ok()
            .filter(|s| !s.trim().is_empty())
            .unwrap_or_else(|| "unknown".into())
    };
    let hash = git(&["rev-parse", "--verify", "--short", "HEAD"]);
    let branch = git(&["symbolic-ref", "--short", "-q", "HEAD"]);
    let dirty = !cmd!(sh, "git diff-index --quiet HEAD --")
        .quiet()
        .ignore_status()
        .output()
        .map(|o| o.status.success())
        .unwrap_or(true);
    let build_args = [
        "--build-arg".to_string(),
        format!("REGATTA_GIT_HASH={hash}"),
        "--build-arg".to_string(),
        format!("REGATTA_GIT_BRANCH={branch}"),
        "--build-arg".to_string(),
        format!("REGATTA_GIT_DIRTY={dirty}"),
    ];
    cmd!(
        sh,
        "podman build --platform {PLATFORM} {build_args...} -f Containerfile -t {IMAGE} ."
    )
    .run()?;
    let format = "{{.Architecture}}";
    let arch = cmd!(sh, "podman image inspect --format {format} {IMAGE}").read()?;
    if arch.trim() != "amd64" {
        bail!("{IMAGE} was built for {arch}, expected amd64");
    }
    Ok(())
}

fn pub_img(sh: &Shell) -> Result<()> {
    make_img(sh)?;
    cmd!(sh, "podman push {IMAGE}").run()?;
    Ok(())
}

fn rollout_restart(sh: &Shell) -> Result<()> {
    cmd!(sh, "kubectl -n {NAMESPACE} rollout restart {DEPLOYMENT}").run()?;
    cmd!(
        sh,
        "kubectl -n {NAMESPACE} rollout status {DEPLOYMENT} --timeout=180s"
    )
    .run()?;
    Ok(())
}

/// Runs `kubectl <verb> -f -` with `k8s_deploy/<manifest>` on stdin, its placeholders (if any) filled from the
/// environment.
fn kubectl(sh: &Shell, verb: &str, manifest: &str, dry_run: bool) -> Result<()> {
    let path = Path::new("k8s_deploy").join(manifest);
    let template = sh
        .read_file(&path)
        .with_context(|| format!("reading {}", path.display()))?;
    let yaml = if template.contains(DB_URL_PLACEHOLDER) {
        fill(&template)?
    } else {
        template
    };
    let dry = dry_run.then_some("--dry-run=client");
    cmd!(sh, "kubectl {verb} {dry...} -f -").stdin(yaml).run()?;
    Ok(())
}

/// The placeholder in `k8s_deploy/secrets.yaml` that [`fill`] replaces with the database URL.
const DB_URL_PLACEHOLDER: &str = "{REGATTA_DB_URL}";

/// Fills the `{REGATTA_DB_URL}` placeholder of a manifest from `REGATTA_PG_HOST` / `REGATTA_PG_PORT` /
/// `REGATTA_PG_USER` / `REGATTA_PG_PASS`.
///
/// Any of them missing or empty is an error.
fn fill(template: &str) -> Result<String> {
    let host = require_env("REGATTA_PG_HOST")?;
    let port = require_env("REGATTA_PG_PORT")?;
    let user = require_env("REGATTA_PG_USER")?;
    let pass = require_env("REGATTA_PG_PASS")?;
    Ok(template.replace(DB_URL_PLACEHOLDER, &db_url(&host, &port, &user, &pass)))
}

/// The server's database URL: credentials percent-encoded, TLS required end to end.
fn db_url(host: &str, port: &str, user: &str, pass: &str) -> String {
    format!(
        "postgres://{}:{}@{host}:{port}/{DB_NAME}?sslmode=require",
        percent_encode(user),
        percent_encode(pass)
    )
}

fn require_env(name: &str) -> Result<String> {
    match std::env::var(name) {
        Ok(v) if !v.is_empty() => Ok(v),
        _ => bail!("{name} is not set"),
    }
}

/// Percent-encodes a URL userinfo part per RFC 3986: everything but the unreserved characters.
fn percent_encode(value: &str) -> String {
    let mut out = String::with_capacity(value.len());
    for b in value.bytes() {
        if b.is_ascii_alphanumeric() || matches!(b, b'-' | b'.' | b'_' | b'~') {
            out.push(b as char);
        } else {
            out.push_str(&format!("%{b:02X}"));
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::{db_url, percent_encode};

    #[test]
    fn userinfo_is_percent_encoded() {
        assert_eq!(percent_encode("regatta_admin"), "regatta_admin");
        // not real password
        assert_eq!(percent_encode("p@ss:w/rd é"), "p%40ss%3Aw%2Frd%20%C3%A9");
    }

    #[test]
    fn db_url_uses_host_port_and_tls() {
        assert_eq!(
            db_url("db.example.internal", "6432", "regatta", "p@ss"),
            // not real password
            "postgres://regatta:p%40ss@db.example.internal:6432/regatta?sslmode=require"
        );
    }
}

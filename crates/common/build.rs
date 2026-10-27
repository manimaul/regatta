//! `VersionInfo` (= the Kotlin `generateVersionInfo` task in `common/build.gradle.kts`): the
//! short git hash, the branch, the dirty flag and the build date are captured here and handed to
//! the crate as `cargo:rustc-env` values, so nothing is written under `src/`.
//!
//! Git is called with the same commands as the Gradle task (`rev-parse --verify --short HEAD`,
//! `symbolic-ref --short -q HEAD`, `diff-index --quiet HEAD --`); when git is missing or the
//! build runs outside a checkout every value falls back to `unknown` / not dirty.

use std::path::{Path, PathBuf};
use std::process::Command;
use std::time::{SystemTime, UNIX_EPOCH};

fn git(args: &[&str]) -> Option<std::process::Output> {
    Command::new("git").args(args).output().ok()
}

fn git_stdout(args: &[&str]) -> Option<String> {
    git(args)
        .filter(|o| o.status.success())
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
        .filter(|s| !s.is_empty())
}

/// Civil date and time of a Unix timestamp, in UTC (Howard Hinnant's `civil_from_days`).
fn utc_iso(secs: u64) -> String {
    let days = (secs / 86_400) as i64;
    let rem = secs % 86_400;
    let (h, m, s) = (rem / 3600, rem % 3600 / 60, rem % 60);
    let z = days + 719_468;
    let era = z.div_euclid(146_097);
    let doe = z.rem_euclid(146_097);
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let y = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = doy - (153 * mp + 2) / 5 + 1;
    let mo = if mp < 10 { mp + 3 } else { mp - 9 };
    let y = if mo <= 2 { y + 1 } else { y };
    format!("{y:04}-{mo:02}-{d:02}T{h:02}:{m:02}:{s:02}Z")
}

fn rerun_on_git_changes() {
    let Some(git_dir) = git_stdout(&["rev-parse", "--git-dir"]) else {
        return;
    };
    let git_dir: PathBuf = if Path::new(&git_dir).is_absolute() {
        PathBuf::from(git_dir)
    } else {
        std::env::current_dir()
            .map(|d| d.join(git_dir))
            .unwrap_or_default()
    };
    let head = git_dir.join("HEAD");
    println!("cargo:rerun-if-changed={}", head.display());
    if let Ok(contents) = std::fs::read_to_string(&head) {
        if let Some(reference) = contents.trim().strip_prefix("ref: ") {
            println!(
                "cargo:rerun-if-changed={}",
                git_dir.join(reference).display()
            );
        }
    }
    println!(
        "cargo:rerun-if-changed={}",
        git_dir.join("packed-refs").display()
    );
    // The dirty flag depends on the working tree; the index file changes with every staging.
    println!("cargo:rerun-if-changed={}", git_dir.join("index").display());
}

fn main() {
    let hash = git_stdout(&["rev-parse", "--verify", "--short", "HEAD"])
        .unwrap_or_else(|| "unknown".into());
    let branch =
        git_stdout(&["symbolic-ref", "--short", "-q", "HEAD"]).unwrap_or_else(|| "unknown".into());
    let dirty = git(&["diff-index", "--quiet", "HEAD", "--"])
        .map(|o| !o.status.success())
        .unwrap_or(false);
    let build_date = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| utc_iso(d.as_secs()))
        .unwrap_or_else(|_| "unknown".into());

    println!("cargo:rustc-env=REGATTA_GIT_HASH={hash}");
    println!("cargo:rustc-env=REGATTA_GIT_BRANCH={branch}");
    println!("cargo:rustc-env=REGATTA_GIT_DIRTY={dirty}");
    println!("cargo:rustc-env=REGATTA_BUILD_DATE={build_date}");
    rerun_on_git_changes();
}

//! The git and build metadata of this build, captured at compile time by `crates/common/build.rs`.

/// Git hash, branch, dirty flag and build date of this build.
///
/// Values are `unknown` (and `dirty` false) when the build ran without git or outside a checkout, unless the
/// `REGATTA_GIT_*` build arguments supplied them.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct VersionInfo {
    /// `git rev-parse --verify --short HEAD`.
    pub git_hash: &'static str,
    /// `git symbolic-ref --short -q HEAD`.
    pub branch: &'static str,
    /// `git diff-index --quiet HEAD --` failed: the working tree had uncommitted changes.
    pub dirty: bool,
    /// UTC, ISO-8601 (`2026-10-26T12:00:00Z`).
    pub build_date: &'static str,
    /// `CARGO_PKG_VERSION`.
    pub version: &'static str,
}

impl VersionInfo {
    /// The values of the running binary.
    pub const CURRENT: VersionInfo = VersionInfo {
        git_hash: env!("REGATTA_GIT_HASH"),
        branch: env!("REGATTA_GIT_BRANCH"),
        dirty: matches!(env!("REGATTA_GIT_DIRTY").as_bytes(), b"true"),
        build_date: env!("REGATTA_BUILD_DATE"),
        version: env!("CARGO_PKG_VERSION"),
    };
}

pub const GIT_HASH: &str = VersionInfo::CURRENT.git_hash;
pub const GIT_BRANCH: &str = VersionInfo::CURRENT.branch;
pub const DIRTY: bool = VersionInfo::CURRENT.dirty;
pub const BUILD_DATE: &str = VersionInfo::CURRENT.build_date;
pub const VERSION: &str = VersionInfo::CURRENT.version;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn values_are_captured() {
        assert!(!GIT_HASH.is_empty());
        assert!(!GIT_BRANCH.is_empty());
        assert!(BUILD_DATE == "unknown" || BUILD_DATE.ends_with('Z'));
        assert_eq!(VERSION, env!("CARGO_PKG_VERSION"));
    }
}

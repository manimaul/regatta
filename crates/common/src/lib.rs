//! The domain models, REST paths and label helpers shared by every other crate of the workspace.
//!
//! `regatta-scoring` scores these types, `regatta-service` stores and loads them, and the server and the UI send
//! them over `/v1/api` and the server functions. It has no I/O.
//!
//! - [`data`]: the serde models (races, series, boats, results, ORC certificates, reports and standings).
//! - [`api_paths`] and [`versioned_api`]: the REST paths.
//! - [`utils`]: labels ([`score_text`], [`display`]) and the pretty JSON writer [`to_kotlin_pretty_json`].
//! - [`serde_kotlin`]: the instant and duration string formats.
//! - [`version_info`]: the git and build metadata captured by `build.rs`.
//!
//! The JSON of these types is pinned byte for byte by the golden files in `crates/scoring/tests/golden/approved/`
//! and the contract cassettes: field order is declaration order, defaults and `None`s (as `null`) are written,
//! enums serialize by the names their `name()` methods return, and instants and durations use the [`serde_kotlin`]
//! formats. `tests/golden_round_trip.rs` checks that every approved file still round-trips.

pub mod api_paths;
pub mod data;
pub mod serde_kotlin;
pub mod utils;
pub mod version_info;

pub use data::*;
pub use utils::{
    display, move_item, score_text, to_kotlin_pretty_json, versioned_api, versioned_api_v,
    CORRECTION_FACTOR_DEFAULT, RATING_DEFAULT,
};
pub use version_info::VersionInfo;

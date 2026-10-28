//! regatta-common: the shared models, API paths and label helpers of the Rust port
//! (= the Kotlin `common` module; see `docs/explore_implemented/rust_port.md`, Phase 1).
//!
//! The JSON produced by these types is byte-for-byte the Kotlin server's (kotlinx.serialization
//! with `encodeDefaults = true`): field order is the Kotlin declaration order, defaults and nulls
//! are written, enums serialize by their Kotlin names, and `Instant` / `Duration` use the kotlinx
//! formats through [`serde_kotlin`]. The approved golden files prove it (`tests/golden_round_trip.rs`).

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

//! Per-feature UI state (= the Kotlin `viewmodel/` package, the parts that hold no server data).
//! Loads are resources over the server functions (`utils::async_state::Loaded`), mutations
//! `call`s into them.

pub mod alerts;
pub mod login;
pub mod route;
pub mod theme;

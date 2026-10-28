//! regatta-service: everything the regatta server does besides HTTP (= the Kotlin `server`
//! module without `plugins/`): configuration, the startup schema check (D6), the database
//! service layer over sqlx (one module per Kotlin `*Table.kt`, the `RegattaDatabase` functions
//! in [`db`]), the report and standings wrappers over `regatta-scoring` ([`reporter`]), the
//! auth tokens ([`auth`]) and the ORC client ([`orc`]). The axum REST handlers and, in Phase 4,
//! the Leptos server functions are thin adapters over this crate.
//! See `docs/explore_implemented/rust_port.md`, Phase 3.

pub mod auth;
pub mod config;
pub mod db;
pub mod orc;
pub mod reporter;
pub mod startup;

pub use config::Config;
pub use db::Db;

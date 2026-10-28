//! Everything the regatta server does besides HTTP, which the REST handlers and server functions adapt.
//!
//! - [`config`]: the server [`Config`] from the environment or flags.
//! - [`startup`]: the startup schema check (D6 in `docs/explore_implemented/rust_port.md`).
//! - [`db`]: the data layer over sqlx, [`Db`], one module per table.
//! - [`reporter`]: race reports and series standings, loaded from the database and scored by `regatta-scoring`.
//! - [`auth`]: the admin bearer tokens.
//! - [`orc`]: the client of ORC's data site.
//!
//! The axum REST handlers (`crates/server/src/routes/`) and the Leptos server functions
//! (`crates/app/src/server_fns.rs`) are thin adapters over this crate. See `docs/explore_implemented/rust_port.md`.

pub mod auth;
pub mod config;
pub mod db;
pub mod orc;
pub mod reporter;
pub mod startup;

pub use config::Config;
pub use db::Db;

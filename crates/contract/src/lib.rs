//! The REST API contract of the regatta server: a scripted scenario, recorded into cassettes and replayed.
//!
//! - [`scenario()`] builds the steps ([`step`]), every `/v1/api` route in [`ROUTES`] with its statuses and error paths.
//! - [`record`] runs them against a server and writes one cassette per step ([`cassette`]).
//! - [`replay`] runs them against a server and compares every response with its cassette ([`compare`]).
//! - [`diff`] runs them against two servers at once and compares the responses with each other.
//!
//! Every run needs the server on a fresh load of the golden dump. See `crates/contract/README.md` and "The API
//! contract" (T2, T3) in `docs/explore_implemented/rust_port.md`.

pub mod auth;
pub mod cassette;
pub mod compare;
pub mod http;
pub mod png;
pub mod random;
pub mod runner;
pub mod scenario;
pub mod step;

pub use runner::{diff, record, replay, Options, Report, Scenario, StepOutcome};
pub use scenario::{scenario, ScenarioOptions, ROUTES};

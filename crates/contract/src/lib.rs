//! regatta-contract: the REST API contract of the regatta server as a scripted scenario
//! (`scenario`), recorded into cassettes from one server (`runner::record`) and replayed against
//! another (`runner::replay`), or run against two servers at once (`runner::diff`). See
//! `docs/explore_implemented/rust_port.md` ("The API contract", T2, T3) and this crate's README.

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

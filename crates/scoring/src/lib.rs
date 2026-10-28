//! Pure race and series scoring, and the save-time validation of races, results and boats.
//!
//! The crate is separate from the database so the rules can be tested on their own (D8 in
//! `docs/explore_implemented/rust_port.md`); `regatta-service` loads the data and calls it
//! (`crates/service/src/reporter.rs`). Everything here is a function of its arguments, with no I/O:
//!
//! - [`card`] builds a result's report card from its record, class start, race configuration and ORC certificate.
//! - [`report`] places the cards of a race in their classes and brackets.
//! - [`standings`] scores the race reports of a series.
//! - [`validate_race`], [`validate_result`] and [`validate_boat`] check an entity before it is saved.
//!
//! The doc comments cite the CYCT General Sailing Instructions (GSI), the scoring decisions D1 to D9 of
//! `docs/explore_implemented/test_audit_and_plan.md` ("Decisions") and R1 to R4 of
//! `docs/explore_implemented/orc_series_standings.md`; they are the specification.
//!
//! The arithmetic is fixed by the golden files: corrected time is `(elapsed whole milliseconds as f64 * tcf)`
//! turned into nanoseconds rounded to nearest ([`boat_corrected_time`]), and the series scores are `f64` sums in
//! race order. `tests/golden_test.rs` reproduces the approved files (`tests/golden/approved`) byte for byte from
//! their own inputs.
//!
//! The test fixture DSL (the `fixture` module, `crates/scoring/src/fixture.rs`) is compiled for this crate's
//! tests and behind the `fixture` feature for other crates' tests.

#[cfg(any(test, feature = "fixture"))]
pub mod fixture;
pub mod place;
pub mod reporter;
pub mod sort;
pub mod validation;

pub use place::{
    card_compare_orc, card_compare_phrf, compare, is_finisher, is_starter, place, PlaceAmong,
};
pub use reporter::{
    assign_places, boat_corrected_time, card, class_card, effective_race_class, group_scores,
    orc_tcf, phrf_tcf, report, round_to_tenth, standings, tie_breaker, GroupScores, OrcTcfError,
};
pub use validation::{validate_boat, validate_race, validate_result};

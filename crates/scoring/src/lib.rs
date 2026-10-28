//! regatta-scoring: the pure race and series scoring of the Rust port (= the Kotlin
//! `RaceResultReporter`, `RaceValidation` and `ResultValidation`; D8 in
//! `docs/explore_unimplemented/rust_port.md`).
//!
//! Everything here is a function of its arguments: [`card`] builds a result's report card from
//! its record, class start, race configuration and ORC certificate; [`report`] places the cards
//! of a race; [`standings`] scores the race reports of a series. The database wrappers
//! (`getReport`, `getStandingsReport`) belong to the server crate. The doc comments cite the
//! CYCT General Sailing Instructions (GSI) and the decisions D1 to D9 of
//! `docs/explore_implemented/test_audit_and_plan.md` ("Decisions") and R1 to R4 of
//! `docs/explore_implemented/orc_series_standings.md`; they are the specification, carried over
//! from the Kotlin sources.
//!
//! Numbers follow the Kotlin arithmetic step for step: corrected time is
//! `(elapsed whole milliseconds as f64 * tcf)` turned into nanoseconds rounded to nearest
//! ([`boat_corrected_time`]), and the series scores are `f64` sums in race order. The approved
//! golden files (`tests/golden/approved`) are reproduced byte for byte
//! from their own inputs by `tests/golden_test.rs`.
//!
//! The test fixture DSL ([`fixture`]) is compiled for this crate's tests and behind the `fixture`
//! feature for other crates' tests.

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

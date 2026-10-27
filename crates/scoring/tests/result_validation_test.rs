//! = `ResultValidationTest.kt`: validateResult guards the POST results route (D8).

use pretty_assertions::assert_eq;
use regatta_common::{RaceResult, RatingType};
use regatta_scoring::validate_result;

fn result(rating_type: RatingType, phrf_rating: Option<i32>, orc_ref: Option<&str>) -> RaceResult {
    RaceResult {
        rating_type,
        phrf_rating,
        orc_ref: orc_ref.map(str::to_string),
        ..Default::default()
    }
}

#[test]
fn accepts_a_phrf_result_with_a_rating() {
    assert_eq!(
        None,
        validate_result(&result(RatingType::Phrf, Some(48), None))
    );
    assert_eq!(
        None,
        validate_result(&result(RatingType::OrcPhrf, Some(48), Some("C1")))
    );
}

#[test]
fn accepts_an_orc_result_with_a_certificate_and_no_phrf_rating() {
    assert_eq!(
        None,
        validate_result(&result(RatingType::Orc, None, Some("C1")))
    );
}

/// R2 (orc_series_standings.md): an ORC or ORC_PHRF result needs a certificate, or it would score with an ORC TCF of 1.
#[test]
fn r2_rejects_an_orc_result_without_a_certificate() {
    assert_eq!(
        Some("An ORC result needs an ORC certificate".to_string()),
        validate_result(&result(RatingType::Orc, None, None))
    );
    assert_eq!(
        Some("An ORC, PHRF result needs an ORC certificate".to_string()),
        validate_result(&result(RatingType::OrcPhrf, Some(48), None))
    );
}

#[test]
fn accepts_cruising_and_orc_results_without_a_phrf_rating() {
    assert_eq!(
        None,
        validate_result(&result(RatingType::CruisingFlyingSails, None, None))
    );
    assert_eq!(
        None,
        validate_result(&result(RatingType::CruisingNonFlyingSails, None, None))
    );
    assert_eq!(
        None,
        validate_result(&result(RatingType::Orc, None, Some("C1")))
    );
}

/// D8: a PHRF or ORC_PHRF result needs a PHRF rating; the message is what the UI shows.
#[test]
fn d8_rejects_a_phrf_result_without_a_rating() {
    assert!(validate_result(&result(RatingType::Phrf, None, None)).is_some());
    assert!(validate_result(&result(RatingType::OrcPhrf, None, Some("C1"))).is_some());
    assert_eq!(
        Some("A PHRF result needs a PHRF rating".to_string()),
        validate_result(&result(RatingType::Phrf, None, None))
    );
}

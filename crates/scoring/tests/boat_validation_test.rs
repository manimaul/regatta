//! validate_boat guards the POST boat route: a boat must carry what its rating type scores with.

use pretty_assertions::assert_eq;
use regatta_common::{Boat, OrcCertificate, RatingType};
use regatta_scoring::validate_boat;

fn boat(rating_type: RatingType, phrf_rating: Option<i32>, certificate: bool) -> Boat {
    Boat {
        rating_type,
        phrf_rating,
        orc_certs: if certificate {
            vec![OrcCertificate::default()]
        } else {
            Vec::new()
        },
        ..Default::default()
    }
}

#[test]
fn accepts_rated_boats_with_what_their_rating_needs() {
    assert_eq!(
        None,
        validate_boat(&boat(RatingType::Phrf, Some(48), false))
    );
    assert_eq!(None, validate_boat(&boat(RatingType::Orc, None, true)));
    assert_eq!(
        None,
        validate_boat(&boat(RatingType::OrcPhrf, Some(48), true))
    );
}

#[test]
fn accepts_cruising_boats_without_a_rating_or_certificate() {
    assert_eq!(
        None,
        validate_boat(&boat(RatingType::CruisingFlyingSails, None, false))
    );
    assert_eq!(
        None,
        validate_boat(&boat(RatingType::CruisingNonFlyingSails, None, false))
    );
}

#[test]
fn rejects_a_phrf_boat_without_a_rating() {
    assert_eq!(
        Some("A PHRF boat needs a PHRF rating".to_string()),
        validate_boat(&boat(RatingType::Phrf, None, false))
    );
    assert_eq!(
        Some("A ORC, PHRF boat needs a PHRF rating".to_string()),
        validate_boat(&boat(RatingType::OrcPhrf, None, true))
    );
}

#[test]
fn rejects_an_orc_boat_without_a_certificate() {
    assert_eq!(
        Some("An ORC boat needs an ORC certificate".to_string()),
        validate_boat(&boat(RatingType::Orc, None, false))
    );
    assert_eq!(
        Some("An ORC, PHRF boat needs an ORC certificate".to_string()),
        validate_boat(&boat(RatingType::OrcPhrf, Some(48), false))
    );
}

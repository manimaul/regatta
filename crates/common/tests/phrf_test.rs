//! = `PhrfTest.kt`.

use regatta_common::PhrfBFactor;

#[test]
fn test_phrf_b_factor() {
    for cf in 0..=515 {
        assert_eq!(PhrfBFactor::Hvy, PhrfBFactor::from_b_factor(cf));
    }
    for cf in 516..=575 {
        assert_eq!(PhrfBFactor::Avg, PhrfBFactor::from_b_factor(cf));
    }
    for cf in 576..=625 {
        assert_eq!(PhrfBFactor::Light, PhrfBFactor::from_b_factor(cf));
    }
    for cf in 626..=650 {
        assert_eq!(
            PhrfBFactor::Calm,
            PhrfBFactor::from_b_factor(cf),
            "cf = {cf}"
        );
    }
}

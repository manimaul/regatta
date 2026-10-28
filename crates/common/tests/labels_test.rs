//! The shared label helpers: `Bracket::label`, `RatingType::rated_label`, `score_text` and `display`.

use jiff::SignedDuration;
use regatta_common::{display, score_text, Bracket, RatingType};

fn bracket(name: &str, min_rating: Option<f32>, max_rating: Option<f32>) -> Bracket {
    let d = Bracket::default();
    Bracket {
        name: name.into(),
        min_rating: min_rating.unwrap_or(d.min_rating),
        max_rating: max_rating.unwrap_or(d.max_rating),
        ..d
    }
}

fn hms(h: i64, m: i64, s: i64, ms: i64) -> SignedDuration {
    SignedDuration::from_millis(((h * 3600 + m * 60 + s) * 1000) + ms)
}

#[test]
fn bracket_label_with_both_ends_open_is_just_the_name() {
    assert_eq!("I", bracket("I", None, None).label());
    assert_eq!("I", bracket("I", Some(-1000.0), Some(1000.0)).label());
    assert_eq!("I", bracket("I", Some(-2000.0), Some(2000.0)).label());
}

#[test]
fn bracket_label_with_open_minimum() {
    assert_eq!("I 100.0 -", bracket("I", None, Some(100.0)).label());
}

#[test]
fn bracket_label_with_open_maximum() {
    assert_eq!("II 101.0 +", bracket("II", Some(101.0), None).label());
}

#[test]
fn bracket_label_with_both_ends() {
    assert_eq!(
        "II 101.0 to 160.5",
        bracket("II", Some(101.0), Some(160.5)).label()
    );
}

/// The rated labels of every rating type; the cruising ones read `Not Rated (FS)` / `Not Rated`.
#[test]
fn rated_label_per_rating_type() {
    assert_eq!("PHRF (48)", RatingType::Phrf.rated_label(Some(48), None));
    assert_eq!("PHRF (null)", RatingType::Phrf.rated_label(None, None));
    assert_eq!("ORC", RatingType::Orc.rated_label(None, None));
    assert_eq!("ORC(~75)", RatingType::Orc.rated_label(None, Some("~75")));
    assert_eq!(
        "PHRF (48), ORC",
        RatingType::OrcPhrf.rated_label(Some(48), None)
    );
    assert_eq!(
        "PHRF (48), ORC (~75)",
        RatingType::OrcPhrf.rated_label(Some(48), Some("~75"))
    );
    assert_eq!(
        "Not Rated (FS)",
        RatingType::CruisingFlyingSails.rated_label(Some(48), Some("~75"))
    );
    assert_eq!(
        "Not Rated",
        RatingType::CruisingNonFlyingSails.rated_label(None, None)
    );
}

/// D1 (`docs/explore_implemented/test_audit_and_plan.md`): whole scores have no decimal, fractions one or two.
#[test]
fn score_text_formats_fractions() {
    assert_eq!("4", score_text(4.0));
    assert_eq!("0", score_text(0.0));
    assert_eq!("3.5", score_text(3.5));
    assert_eq!("3.33", score_text(10.0 / 3.0));
    assert_eq!("10.67", score_text(32.0 / 3.0));
    assert_eq!("2.1", score_text(2.1));
    assert_eq!("3", score_text(2.999));
}

#[test]
fn display_zero() {
    assert_eq!("0h:0m:0s", display(SignedDuration::ZERO));
}

#[test]
fn display_hours_minutes_seconds() {
    assert_eq!("1h:2m:3s", display(hms(1, 2, 3, 0)));
    assert_eq!("0h:54m:12s", display(hms(0, 54, 12, 0)));
}

#[test]
fn display_over_24_hours() {
    assert_eq!("25h:0m:0s", display(hms(25, 0, 0, 0)));
    assert_eq!("49h:59m:59s", display(hms(49, 59, 59, 0)));
}

#[test]
fn display_whole_seconds_are_unchanged_by_sub_second_zero() {
    assert_eq!("0h:0m:12s", display(hms(0, 0, 12, 0)));
}

/// D7 (`docs/explore_implemented/test_audit_and_plan.md`): labels round to the nearest second instead of truncating.
#[test]
fn d7_display_rounds_to_nearest_second() {
    assert_eq!("0h:0m:13s", display(hms(0, 0, 12, 900)));
    assert_eq!("0h:0m:12s", display(hms(0, 0, 12, 400)));
    assert_eq!("0h:1m:0s", display(hms(0, 0, 59, 600)));
    assert_eq!("1h:0m:0s", display(hms(0, 59, 59, 500)));
}

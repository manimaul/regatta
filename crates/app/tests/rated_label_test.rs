//! = `RatedLabelTest.kt`: the rating cell links the ORC reference number to the certificate's public page in a new tab when its id is known.
#![cfg(all(not(target_arch = "wasm32"), feature = "ssr"))]

mod common;

use common::*;
use leptos::prelude::*;
use regatta_app::components::RatedLabel;
use regatta_common::RatingType;

const URL: &str = "https://data.orc.org/public/WPub.dll/CC/264182";

fn anchor(html: &str) -> Option<&str> {
    tags(html).into_iter().find(|t| t.starts_with("<a"))
}

#[test]
fn orc_reference_links_to_the_certificate() {
    let html = render(
        || view! { <RatedLabel rating_type=RatingType::Orc phrf_rating=None orc_ref=Some("0456000518S".to_string()) orc_url=Some(URL.to_string())/> },
    );
    assert_eq!(
        text(&html),
        RatingType::Orc.rated_label(None, Some("0456000518S")),
        "the text is the plain label"
    );
    let a = anchor(&html).expect("a");
    assert!(html.contains(">0456000518S</a>"));
    assert_eq!(attr(a, "href"), Some(URL));
    assert_eq!(attr(a, "target"), Some("_blank"));
    assert_eq!(attr(a, "rel"), Some("noopener noreferrer"));
}

#[test]
fn orc_phrf_keeps_the_phrf_part_as_text() {
    let html = render(
        || view! { <RatedLabel rating_type=RatingType::OrcPhrf phrf_rating=Some(48) orc_ref=Some("0456000518S".to_string()) orc_url=Some(URL.to_string())/> },
    );
    assert_eq!(text(&html), "PHRF (48), ORC (0456000518S)");
    assert!(html.contains(">0456000518S</a>"));
}

#[test]
fn no_url_means_plain_text() {
    let html = render(
        || view! { <RatedLabel rating_type=RatingType::OrcPhrf phrf_rating=Some(48) orc_ref=Some("04560003WR9".to_string()) orc_url=None/> },
    );
    assert_eq!(text(&html), "PHRF (48), ORC (04560003WR9)");
    assert!(
        anchor(&html).is_none(),
        "an unresolved (e.g. expired) certificate is not linked"
    );
}

#[test]
fn phrf_and_cruising_are_unchanged() {
    let html = render(
        || view! { <RatedLabel rating_type=RatingType::Phrf phrf_rating=Some(48) orc_ref=None orc_url=None/> },
    );
    assert_eq!(text(&html), "PHRF (48)");
    assert!(anchor(&html).is_none());
}

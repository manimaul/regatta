//! = `RatedLabel.kt`: the rating cell of a results table, `RatingType.ratedLabel` with the ORC
//! reference number linked to the certificate's public page when `orc_url` is known (a new tab).
//! Without a URL, or when the label does not contain the reference, the plain label is rendered.

use leptos::either::Either;
use leptos::prelude::*;
use regatta_common::RatingType;

#[component]
pub fn RatedLabel(
    rating_type: RatingType,
    phrf_rating: Option<i32>,
    #[prop(optional_no_strip)] orc_ref: Option<String>,
    #[prop(optional_no_strip)] orc_url: Option<String>,
) -> impl IntoView {
    let label = rating_type.rated_label(phrf_rating, orc_ref.as_deref());
    let at = match (&orc_url, &orc_ref) {
        (Some(_), Some(r)) if !r.trim().is_empty() => label.find(r.as_str()),
        _ => None,
    };
    match (at, orc_ref, orc_url) {
        (Some(at), Some(orc_ref), Some(orc_url)) => {
            let before = label[..at].to_string();
            let after = label[at + orc_ref.len()..].to_string();
            Either::Left(view! {
                {before}
                <a target="_blank" rel="noopener noreferrer" href=orc_url>{orc_ref}</a>
                {after}
            })
        }
        _ => Either::Right(label),
    }
}

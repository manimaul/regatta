//! Race placing: the card comparators and `place` (= the top-level functions after
//! `RaceResultReporter` in `RaceResultReporter.kt`).

use jiff::SignedDuration;
use regatta_common::{FinishCode, RaceReportCard};
use std::cmp::{min, Ordering};

/// `Duration.inWholeMilliseconds`: whole milliseconds, truncated toward zero.
pub(crate) fn whole_millis(d: SignedDuration) -> i64 {
    d.as_millis() as i64
}

/// `cardComparePhrf`: the cards by PHRF corrected time (see [`compare`]).
pub fn card_compare_phrf(lhs: &RaceReportCard, rhs: &RaceReportCard) -> Ordering {
    compare(lhs, rhs, |card| card.corrected_phrf_time)
}

/// `cardCompareOrc`: the cards by ORC corrected time (see [`compare`]).
pub fn card_compare_orc(lhs: &RaceReportCard, rhs: &RaceReportCard) -> Ordering {
    compare(lhs, rhs, |card| card.corrected_orc_time)
}

/// Corrected time (to the millisecond), a card with one before a card without; then HOC position,
/// a card with one before a card without; then the finish code's weight (D6). A total preorder,
/// so the standard stable sort is safe on it.
pub fn compare(
    lhs: &RaceReportCard,
    rhs: &RaceReportCard,
    c_time: impl Fn(&RaceReportCard) -> Option<SignedDuration>,
) -> Ordering {
    // compare corrected time
    let lhs_corrected_time = c_time(lhs);
    let rhs_corrected_time = c_time(rhs);
    match (lhs_corrected_time, rhs_corrected_time) {
        (Some(l), Some(r)) => whole_millis(l).cmp(&whole_millis(r)),
        (Some(_), None) => Ordering::Less,
        (None, Some(_)) => Ordering::Greater,
        (None, None) => {
            // compare HOC
            match (lhs.hoc_position, rhs.hoc_position) {
                (Some(l), Some(r)) => l.cmp(&r),
                (Some(_), None) => Ordering::Less,
                (None, Some(_)) => Ordering::Greater,
                (None, None) => lhs
                    .result_record
                    .result
                    .finish_code
                    .weight()
                    .cmp(&rhs.result_record.result.finish_code.weight()),
            }
        }
    }
}

/// Which entries [`place`] ranks: every card, the PHRF-rated ones, or the ORC-rated ones (R1).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PlaceAmong {
    All,
    Phrf,
    Orc,
}

impl PlaceAmong {
    pub fn includes(self, card: &RaceReportCard) -> bool {
        match self {
            PlaceAmong::All => true,
            PlaceAmong::Phrf => card.rating_type().is_phrf(),
            PlaceAmong::Orc => card.rating_type().is_orc(),
        }
    }
}

/// Ranks the cards that `among` includes and hands each its place through `place_handler`.
/// Returns the ranked cards in place order, followed by the cards `among` left out, untouched
/// and in their input order. The ORC ranking uses the ORC corrected times.
///
/// The Kotlin extension works on shared card objects; here `cards` is the one card store and
/// `subset` the indices into it that stand for the Kotlin receiver list. The returned indices are
/// the Kotlin return list, and `place_handler` mutates the card in place as the Kotlin lambda did.
pub fn place(
    cards: &mut [RaceReportCard],
    subset: &[usize],
    among: PlaceAmong,
    mut place_handler: impl FnMut(i32, &mut RaceReportCard),
) -> Vec<usize> {
    let orc = among == PlaceAmong::Orc;
    let card_list: Vec<usize> = subset
        .iter()
        .copied()
        .filter(|&i| among.includes(&cards[i]))
        .collect();
    let left: Vec<usize> = subset
        .iter()
        .copied()
        .filter(|&i| !among.includes(&cards[i]))
        .collect();
    // GSI 14.1 with D3 (test_audit_and_plan.md): starters are every entry except RC volunteers and DNS;
    // finishers are timed finishers and HOC, so DNF = RET = finishers + 1 and NSC = starters + 1.
    // A boat's own score counts the *other* starters, as a DNF boat is not among the finishers it
    // is scored against: an NSC or DSQ boat is a starter for everyone else, not for itself.
    let starters = card_list.iter().filter(|&&i| is_starter(&cards[i])).count() as i32;
    let finishers = card_list
        .iter()
        .filter(|&&i| is_finisher(&cards[i]))
        .count() as i32;
    let other_starters = |card: &RaceReportCard| {
        if is_starter(card) {
            starters - 1
        } else {
            starters
        }
    };

    let card_compare: fn(&RaceReportCard, &RaceReportCard) -> Ordering = if orc {
        card_compare_orc
    } else {
        card_compare_phrf
    };
    //sorted by corrected time then HOC
    let mut sorted = card_list;
    sorted.sort_by(|&a, &b| card_compare(&cards[a], &cards[b]));
    // (place, card index), the Kotlin TempPlace
    let mut list: Vec<(i32, usize)> = Vec::with_capacity(sorted.len());
    let mut last: Option<usize> = None;
    let mut position = 1;
    let mut finished = 0;
    for &i in &sorted {
        let ea = &cards[i];
        let place = match ea.result_record.result.finish_code {
            FinishCode::Time | FinishCode::Hoc => {
                // D7: equal corrected times share the place, and the next place skips (1, 1, 3)
                finished += 1;
                if last.is_none_or(|l| card_compare(ea, &cards[l]) != Ordering::Equal) {
                    position = finished;
                }
                last = Some(i);
                position
            }
            FinishCode::DnsRc | FinishCode::Ret | FinishCode::Dnf => finishers + 1,
            FinishCode::Nsc | FinishCode::Dns => other_starters(ea) + 1,
            FinishCode::Dsq => other_starters(ea) + 2,
        };
        list.push((place, i));
    }

    // D4 / GSI 9.2 / RRS 44.3(c): a penalty adds to the boat's own finishing place, capped at the
    // DNF score, and changes nobody else. Only timed finishers and HOC can carry one; a penalty
    // left on another code is ignored.
    for tp in &mut list {
        let card = &cards[tp.1];
        let Some(penalty) = card.penalty else {
            continue;
        };
        if is_finisher(card) {
            tp.0 = min(tp.0 + penalty, finishers + 1);
        }
    }

    list.sort_by_key(|tp| tp.0);
    let mut out = Vec::with_capacity(subset.len());
    for (place, i) in list {
        place_handler(place, &mut cards[i]);
        out.push(i);
    }
    out.extend(left);
    out
}

/// GSI 14.1 / D3: every entry except RC volunteers (and DNS) started the race.
pub fn is_starter(card: &RaceReportCard) -> bool {
    card.result_record.result.finish_code.is_starter()
}

/// GSI 14.1 / D3: timed finishers and HOC boats are finishers.
pub fn is_finisher(card: &RaceReportCard) -> bool {
    card.result_record.result.finish_code.is_finisher()
}

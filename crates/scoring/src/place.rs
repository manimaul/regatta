//! Race placing: the report card comparators and [`place`], which ranks the cards of a class or bracket.
//!
//! The rules are GSI 14.1 and the scoring decisions D3, D4, D6 and D7 of
//! `docs/explore_implemented/test_audit_and_plan.md`, plus R1 of `docs/explore_implemented/orc_series_standings.md`.

use jiff::SignedDuration;
use regatta_common::{FinishCode, RaceReportCard};
use std::cmp::{min, Ordering};

/// Returns the whole milliseconds of `d`, truncated toward zero.
///
/// Places compare and corrected times multiply at this precision, which the golden files fix.
pub(crate) fn whole_millis(d: SignedDuration) -> i64 {
    d.as_millis() as i64
}

/// Orders two cards by PHRF corrected time (see [`compare`]).
pub fn card_compare_phrf(lhs: &RaceReportCard, rhs: &RaceReportCard) -> Ordering {
    compare(lhs, rhs, |card| card.corrected_phrf_time)
}

/// Orders two cards by ORC corrected time (see [`compare`]).
pub fn card_compare_orc(lhs: &RaceReportCard, rhs: &RaceReportCard) -> Ordering {
    compare(lhs, rhs, |card| card.corrected_orc_time)
}

/// Orders two cards by the corrected time `c_time` picks, then HOC position, then finish code weight.
///
/// Corrected time compares to the millisecond, a card with one before a card without; then the HOC position, a
/// card with one before a card without; then the finish code's weight (D6, `test_audit_and_plan.md`). A total
/// preorder, so the standard stable sort is safe on it.
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

/// Which entries [`place`] ranks: every card, the PHRF-rated ones, or the ORC-rated ones.
///
/// R1 (`orc_series_standings.md`): in a rated class each rating is placed among its own entries.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PlaceAmong {
    All,
    Phrf,
    Orc,
}

impl PlaceAmong {
    /// Tells whether `card` is ranked: always for `All`, by the card's rating type otherwise.
    pub fn includes(self, card: &RaceReportCard) -> bool {
        match self {
            PlaceAmong::All => true,
            PlaceAmong::Phrf => card.rating_type().is_phrf(),
            PlaceAmong::Orc => card.rating_type().is_orc(),
        }
    }
}

/// Ranks the cards in `subset` that `among` includes and hands each its place.
///
/// `subset` holds indices into `cards`; the result holds the same indices, the ranked ones in place order followed
/// by the ones `among` left out, untouched and in their input order. `place_handler` gets each ranked card with
/// its place: 1-based for finishers, equal corrected times sharing one (D7), and the finish code's score otherwise
/// (GSI 14.1 / D3), plus any penalty capped at the DNF score (D4); the decisions are in `test_audit_and_plan.md`.
/// The ORC ranking orders by ORC corrected time, the others by PHRF corrected time (see [`compare`]).
/// R1 (`orc_series_standings.md`): each rating is placed among its own entries only.
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
    // (place, card index)
    let mut list: Vec<(i32, usize)> = Vec::with_capacity(sorted.len());
    let mut last: Option<usize> = None;
    let mut position = 1;
    let mut finished = 0;
    for &i in &sorted {
        let ea = &cards[i];
        let place = match ea.result_record.result.finish_code {
            FinishCode::Time | FinishCode::Hoc => {
                // D7 (test_audit_and_plan.md): equal corrected times share the place, and the next place skips
                // (1, 1, 3)
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

    // D4 (test_audit_and_plan.md) / GSI 9.2 / RRS 44.3(c): a penalty adds to the boat's own finishing place,
    // capped at the DNF score, and changes nobody else. Only timed finishers and HOC can carry one; a penalty
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

/// Tells whether the card's boat started the race: every entry except RC volunteers and DNS.
///
/// GSI 14.1, D3 (`test_audit_and_plan.md`).
pub fn is_starter(card: &RaceReportCard) -> bool {
    card.result_record.result.finish_code.is_starter()
}

/// Tells whether the card's boat finished the race: a timed finisher or HOC.
///
/// GSI 14.1, D3 (`test_audit_and_plan.md`).
pub fn is_finisher(card: &RaceReportCard) -> bool {
    card.result_record.result.finish_code.is_finisher()
}

//! = the `RaceResultReporter` object, minus its two database wrappers (`getStandingsReport`,
//! `getReport`), which the server crate provides over these pure seams.

use crate::place::{is_starter, place, whole_millis, PlaceAmong};
use crate::sort::java_sort;
use jiff::{SignedDuration, Timestamp};
use regatta_common::utils::round_to_long;
use regatta_common::{
    Bracket, BracketReportCards, ClassReportCards, ClassSchedule, FinishCode, Orc3Band, Orc5Band,
    OrcCertificate, OrcScoringOption, Race, RaceClass, RaceReport, RaceReportCard,
    RaceResultBoatBracket, RaceSchedule, RaceStatus, RatingType, Series, StandingsBoatSkipper,
    StandingsBracket, StandingsClass, StandingsRace, StandingsSeries,
};
use std::cmp::Ordering;
use std::collections::HashMap;

/// The time zone of the club: the season (`effective_race_class`) is the class start's year here.
pub const PACIFIC_ZONE: &str = "America/Los_Angeles";

/// Kotlin `sumOf` over `Double`s: a left fold from `0.0` in iteration order (never `-0.0`, which
/// `f64::sum` starts from and serde_json would print).
fn sum_of(values: impl IntoIterator<Item = f64>) -> f64 {
    values.into_iter().fold(0.0, |acc, v| acc + v)
}

/// Pure series standings from the race reports. `now` tells a canceled race from one not yet
/// sailed (D2) when a race has no results at all.
///
/// The reports are sorted by their race's earliest class start (stable, so reports with equal
/// starts keep their input order). Kotlin's `startTime` throws on a report whose schedule is
/// empty; here such a report sorts first.
pub fn standings(
    series: Series,
    year: i32,
    reports: &[RaceReport],
    now: Timestamp,
) -> StandingsSeries {
    let mut sorted: Vec<&RaceReport> = reports.iter().collect();
    sorted.sort_by_key(|report| report.race_schedule.start_time());
    let races: Vec<Race> = sorted
        .iter()
        .map(|report| report.race_schedule.race.clone())
        .collect();
    let statuses: HashMap<i64, RaceStatus> = sorted
        .iter()
        .map(|report| {
            let end = report
                .race_schedule
                .schedule
                .iter()
                .map(|s| s.end_date)
                .max();
            let status = if !report.class_reports.is_empty() {
                RaceStatus::Sailed
            } else if end.is_some_and(|end| end > now) {
                RaceStatus::NotYetSailed
            } else {
                RaceStatus::Cancelled
            };
            (report.race_schedule.race.id, status)
        })
        .collect();

    let mut standings = StandingsSeries {
        year,
        series,
        standings: get_standings_class(&races, &sorted, &statuses),
        races,
    };

    //calculate place in class, per rating (D9)
    for standings_class in &mut standings.standings {
        let orc_class = standings_class.race_class.rating_type.is_orc();
        let mut rows: Vec<&mut StandingsBoatSkipper> = standings_class
            .standings
            .iter_mut()
            .flat_map(|bracket| bracket.standings.iter_mut())
            .collect();
        let phrf = phrf_rows(&rows, orc_class);
        assign_places(
            &mut rows,
            &phrf,
            |row| row.total_score_class,
            |cell| cell.place_in_class,
            |cell| cell.class_sailed,
            |row, place| row.place_in_class = place,
        );
        let orc = orc_rows(&rows, orc_class);
        assign_places(
            &mut rows,
            &orc,
            |row| row.total_score_class_orc,
            |cell| cell.place_in_class_orc,
            |cell| cell.class_sailed_orc,
            |row, place| row.place_in_class_orc = place,
        );
    }
    standings
}

/// Sorts the rows by total, then by the tiebreak, and writes 1-based places. Boats the
/// tiebreak cannot separate share the place, and the next place skips (D5). The sort is
/// stable and the comparator only looks at scores, so the input order never shows.
///
/// `subset` names the rows (indices into `rows`) that take part, the Kotlin receiver list; the
/// others are left alone. The comparator is not a total order (the tiebreak skips the RC races
/// of either boat of a pair), so the sort is Java's ([`java_sort`]), never `slice::sort_by`.
pub fn assign_places(
    rows: &mut [&mut StandingsBoatSkipper],
    subset: &[usize],
    total: impl Fn(&StandingsBoatSkipper) -> f64,
    score: impl Fn(&StandingsRace) -> f64,
    sailed: impl Fn(&StandingsRace) -> bool,
    set: impl Fn(&mut StandingsBoatSkipper, i32),
) {
    let comparator = |lhs: &StandingsBoatSkipper, rhs: &StandingsBoatSkipper| -> Ordering {
        match total(lhs).total_cmp(&total(rhs)) {
            Ordering::Equal => tie_breaker(lhs, rhs, &score, &sailed),
            other => other,
        }
    };
    let mut sorted: Vec<usize> = subset.to_vec();
    java_sort(&mut sorted, |&a, &b| comparator(rows[a], rows[b]));
    // the place boundaries first, since the comparator reads the rows the loop then writes
    let new_place: Vec<bool> = (0..sorted.len())
        .map(|i| i == 0 || comparator(rows[sorted[i - 1]], rows[sorted[i]]) != Ordering::Equal)
        .collect();
    let mut place = 0;
    for (i, &idx) in sorted.iter().enumerate() {
        if new_place[i] {
            place = i as i32 + 1;
        }
        set(rows[idx], place);
    }
}

/// GSI 14.5: "If two or more boats are tied in a series, the boat with the highest finishing
/// place in the last race of the series will be scored the series winner." D5 continues to
/// the next-to-last race and so on when a race is equal. Races not sailed for this group
/// (D2) and races where either boat was the RC volunteer (GSI 1.4.1.2) are skipped. Raw
/// scores are compared, thrown-out races included. 0 means the boats cannot be separated.
pub fn tie_breaker(
    lhs: &StandingsBoatSkipper,
    rhs: &StandingsBoatSkipper,
    score: impl Fn(&StandingsRace) -> f64,
    sailed: impl Fn(&StandingsRace) -> bool,
) -> Ordering {
    for i in (0..lhs.race_standings.len()).rev() {
        let left = &lhs.race_standings[i];
        let Some(right) = rhs.race_standings.get(i) else {
            continue;
        };
        if !sailed(left) || !sailed(right) {
            continue;
        }
        if left.finish_code == Some(FinishCode::DnsRc)
            || right.finish_code == Some(FinishCode::DnsRc)
        {
            continue;
        }
        let c = score(left).total_cmp(&score(right));
        if c != Ordering::Equal {
            return c;
        }
    }
    Ordering::Equal
}

fn get_standings_class(
    races: &[Race],
    reports: &[&RaceReport],
    statuses: &HashMap<i64, RaceStatus>,
) -> Vec<StandingsClass> {
    // distinctBy id, first occurrence kept, then a stable sort by `sort`
    let mut classes: Vec<RaceClass> = Vec::new();
    for class_report in reports
        .iter()
        .flat_map(|report| report.class_reports.iter())
    {
        if !classes.iter().any(|c| c.id == class_report.race_class.id) {
            classes.push(class_report.race_class.clone());
        }
    }
    classes.sort_by_key(|c| c.sort);

    classes
        .into_iter()
        .map(|race_class| StandingsClass {
            standings: get_standings_bracket(&race_class, races, reports, statuses),
            race_class,
        })
        .collect()
}

fn get_standings_bracket(
    race_class: &RaceClass,
    races: &[Race],
    race_reports: &[&RaceReport],
    statuses: &HashMap<i64, RaceStatus>,
) -> Vec<StandingsBracket> {
    let mut brackets: Vec<Bracket> = Vec::new();
    for bracket in race_reports
        .iter()
        .flat_map(|report| report.class_reports.iter())
        .filter(|class_report| class_report.race_class.id == race_class.id)
        .flat_map(|class_report| class_report.bracket_report.iter())
        .map(|bracket_report| &bracket_report.bracket)
    {
        if !brackets.iter().any(|b| b.id == bracket.id) {
            brackets.push(bracket.clone());
        }
    }
    brackets
        .into_iter()
        .map(|bracket| StandingsBracket {
            standings: get_standings_boat_bracket(
                race_class,
                &bracket,
                races,
                race_reports,
                statuses,
            ),
            bracket,
        })
        .collect()
}

/// D9 / R1: which entries a rating's groups are scored among. In an ORC class each rating is
/// placed among its own entries; elsewhere every entry is PHRF (or cruising) and none is ORC.
fn phrf_card(card: &RaceReportCard, orc_class: bool) -> bool {
    !orc_class || card.rating_type().is_phrf()
}

fn orc_card(card: &RaceReportCard, orc_class: bool) -> bool {
    orc_class && card.rating_type().is_orc()
}

/// D9: the rows a rating's series places are assigned among; the others keep place 0.
fn phrf_rows(rows: &[&mut StandingsBoatSkipper], orc_class: bool) -> Vec<usize> {
    (0..rows.len())
        .filter(|&i| !orc_class || rows[i].rating_type.is_phrf())
        .collect()
}

fn orc_rows(rows: &[&mut StandingsBoatSkipper], orc_class: bool) -> Vec<usize> {
    if orc_class {
        (0..rows.len())
            .filter(|&i| rows[i].rating_type.is_orc())
            .collect()
    } else {
        Vec::new()
    }
}

/// GSI 14.1.2: no entry in the group scores starters + 1, per group (D2 / D9)
fn score(eligible: bool, sailed: bool, placed: bool, place: i32, starters: i32) -> f64 {
    if !eligible || !sailed {
        0.0
    } else if placed {
        place as f64
    } else {
        starters as f64 + 1.0
    }
}

/// The rows of one bracket. Each row is scored under up to two ratings (D9): the PHRF groups
/// (bracket, class) and, in an ORC class, the ORC groups, each with its own starters, sailed
/// races, DNS scores, throw-out, RC average, total and tiebreak. A row is scored under a
/// rating when any of its results in the series carries it; the other rating's fields stay 0.
fn get_standings_boat_bracket(
    race_class: &RaceClass,
    bracket: &Bracket,
    races: &[Race],
    race_reports: &[&RaceReport],
    statuses: &HashMap<i64, RaceStatus>,
) -> Vec<StandingsBoatSkipper> {
    let class_records: Vec<&RaceReportCard> = race_reports
        .iter()
        .flat_map(|report| {
            report
                .class_reports
                .iter()
                .filter(|class_report| class_report.race_class.id == race_class.id)
        })
        .flat_map(|class_report| class_report.bracket_report.iter())
        .flat_map(|bracket_report| bracket_report.cards.iter())
        .collect();

    let bracket_records: Vec<&RaceReportCard> = class_records
        .iter()
        .copied()
        .filter(|card| card.result_record.bracket.id == bracket.id)
        .collect();

    let orc_class = race_class.rating_type.is_orc();
    // D2 / D3: the starters of each group per race, which decide "sailed" and the DNS score
    let starters =
        |records: &[&RaceReportCard], include: &dyn Fn(&RaceReportCard) -> bool| -> Vec<i32> {
            races
                .iter()
                .map(|race| {
                    records
                        .iter()
                        .filter(|card| {
                            card.result_record.result.race_id == race.id
                                && is_starter(card)
                                && include(card)
                        })
                        .count() as i32
                })
                .collect()
        };
    let bracket_starters = starters(&bracket_records, &|card| phrf_card(card, orc_class));
    let class_starters = starters(&class_records, &|card| phrf_card(card, orc_class));
    let bracket_starters_orc = starters(&bracket_records, &|card| orc_card(card, orc_class));
    let class_starters_orc = starters(&class_records, &|card| orc_card(card, orc_class));
    let unscored = || GroupScores {
        scores: vec![None; races.len()],
        throw_out: None,
        total: 0.0,
    };

    let race_order: HashMap<i64, usize> = races
        .iter()
        .enumerate()
        .map(|(i, race)| (race.id, i))
        .collect();
    // groupBy boat id (0 without a boat): one group per key, in order of first appearance
    let mut groups: Vec<(i64, Vec<&RaceReportCard>)> = Vec::new();
    for card in &bracket_records {
        let key = card
            .result_record
            .boat_skipper
            .boat
            .as_ref()
            .map_or(0, |boat| boat.id);
        match groups.iter_mut().find(|(k, _)| *k == key) {
            Some((_, cards)) => cards.push(card),
            None => groups.push((key, vec![card])),
        }
    }
    let mut result: Vec<StandingsBoatSkipper> = groups
        .into_iter()
        .map(|(_, race_report_cards)| {
            // D8: one row per boat, shown with the skipper of its most recent race in the series
            let order = |card: &RaceReportCard| -> i64 {
                race_order
                    .get(&card.result_record.result.race_id)
                    .map_or(-1, |&i| i as i64)
            };
            // maxBy keeps the first of equal maxima
            let latest = race_report_cards
                .iter()
                .copied()
                .fold(None::<&RaceReportCard>, |best, card| match best {
                    Some(b) if order(b) >= order(card) => Some(b),
                    _ => Some(card),
                })
                .expect("a group has at least one card");
            let boat_skipper = latest.result_record.boat_skipper.clone();
            let phrf_eligible = race_report_cards
                .iter()
                .any(|card| phrf_card(card, orc_class));
            let orc_eligible = race_report_cards
                .iter()
                .any(|card| orc_card(card, orc_class));
            let rating_type = if !orc_class {
                latest.rating_type()
            } else if phrf_eligible && orc_eligible {
                RatingType::OrcPhrf
            } else if orc_eligible {
                RatingType::Orc
            } else {
                RatingType::Phrf
            };
            let raw: Vec<StandingsRace> = races
                .iter()
                .enumerate()
                .map(|(i, race)| {
                    let status = statuses
                        .get(&race.id)
                        .copied()
                        .unwrap_or(RaceStatus::Sailed);
                    // D2: a race is sailed for a group when the group had a starter
                    let bracket_sailed = bracket_starters[i] > 0;
                    let class_sailed = class_starters[i] > 0;
                    let bracket_sailed_orc = bracket_starters_orc[i] > 0;
                    let class_sailed_orc = class_starters_orc[i] > 0;
                    let card = race_report_cards
                        .iter()
                        .copied()
                        .find(|card| card.result_record.result.race_id == race.id);
                    let phrf_placed = card.is_some_and(|card| phrf_card(card, orc_class));
                    let orc_placed = card.is_some_and(|card| orc_card(card, orc_class));
                    let mut cell = StandingsRace::new(
                        card.is_some_and(|card| card.finish_time.is_some()),
                        card.map(|card| card.result_record.result.finish_code),
                    );
                    cell.non_starter = card.is_none();
                    cell.place_in_bracket = score(
                        phrf_eligible,
                        bracket_sailed,
                        phrf_placed,
                        card.map_or(0, |card| card.place_in_bracket),
                        bracket_starters[i],
                    );
                    cell.place_in_class = score(
                        phrf_eligible,
                        class_sailed,
                        phrf_placed,
                        card.map_or(0, |card| card.place_in_class),
                        class_starters[i],
                    );
                    cell.hoc_position = card.and_then(|card| card.hoc_position);
                    cell.bracket_sailed = bracket_sailed;
                    cell.class_sailed = class_sailed;
                    cell.status = status;
                    cell.place_in_bracket_orc = score(
                        orc_eligible,
                        bracket_sailed_orc,
                        orc_placed,
                        card.map_or(0, |card| card.place_in_bracket_orc),
                        bracket_starters_orc[i],
                    );
                    cell.place_in_class_orc = score(
                        orc_eligible,
                        class_sailed_orc,
                        orc_placed,
                        card.map_or(0, |card| card.place_in_class_orc),
                        class_starters_orc[i],
                    );
                    cell.bracket_sailed_orc = bracket_sailed_orc;
                    cell.class_sailed_orc = class_sailed_orc;
                    cell.non_starter_orc =
                        orc_eligible && !orc_placed && (bracket_sailed_orc || class_sailed_orc);
                    cell
                })
                .collect();

            let bracket = if phrf_eligible {
                group_scores(
                    &raw,
                    |c| c.bracket_sailed,
                    |c| c.place_in_bracket,
                    |c| c.place_in_class,
                    |i| bracket_starters[i] as f64 + 1.0,
                )
            } else {
                unscored()
            };
            let klass = if phrf_eligible {
                group_scores(
                    &raw,
                    |c| c.class_sailed,
                    |c| c.place_in_class,
                    |c| c.place_in_bracket,
                    |i| class_starters[i] as f64 + 1.0,
                )
            } else {
                unscored()
            };
            let bracket_orc = if orc_eligible {
                group_scores(
                    &raw,
                    |c| c.bracket_sailed_orc,
                    |c| c.place_in_bracket_orc,
                    |c| c.place_in_class_orc,
                    |i| bracket_starters_orc[i] as f64 + 1.0,
                )
            } else {
                unscored()
            };
            let class_orc = if orc_eligible {
                group_scores(
                    &raw,
                    |c| c.class_sailed_orc,
                    |c| c.place_in_class_orc,
                    |c| c.place_in_bracket_orc,
                    |i| class_starters_orc[i] as f64 + 1.0,
                )
            } else {
                unscored()
            };
            let standings: Vec<StandingsRace> = raw
                .into_iter()
                .enumerate()
                .map(|(i, mut cell)| {
                    cell.place_in_bracket = bracket.scores[i].unwrap_or(0.0);
                    cell.place_in_class = klass.scores[i].unwrap_or(0.0);
                    cell.throw_out = Some(i) == bracket.throw_out;
                    cell.throw_out_class = Some(i) == klass.throw_out;
                    cell.place_in_bracket_orc = bracket_orc.scores[i].unwrap_or(0.0);
                    cell.place_in_class_orc = class_orc.scores[i].unwrap_or(0.0);
                    cell.throw_out_orc = Some(i) == bracket_orc.throw_out;
                    cell.throw_out_class_orc = Some(i) == class_orc.throw_out;
                    cell
                })
                .collect();

            StandingsBoatSkipper {
                boat_skipper,
                race_standings: standings,
                total_score_bracket: bracket.total,
                total_score_class: klass.total,
                place_in_bracket: 0,
                place_in_class: 0,
                rating_type,
                total_score_bracket_orc: bracket_orc.total,
                total_score_class_orc: class_orc.total,
                place_in_bracket_orc: 0,
                place_in_class_orc: 0,
            }
        })
        .collect();

    {
        let mut rows: Vec<&mut StandingsBoatSkipper> = result.iter_mut().collect();
        let phrf = phrf_rows(&rows, orc_class);
        assign_places(
            &mut rows,
            &phrf,
            |row| row.total_score_bracket,
            |cell| cell.place_in_bracket,
            |cell| cell.bracket_sailed,
            |row, place| row.place_in_bracket = place,
        );
        let orc = orc_rows(&rows, orc_class);
        assign_places(
            &mut rows,
            &orc,
            |row| row.total_score_bracket_orc,
            |cell| cell.place_in_bracket_orc,
            |cell| cell.bracket_sailed_orc,
            |row, place| row.place_in_bracket_orc = place,
        );
    }
    // rows with a PHRF place by it, then the ORC-only rows by their ORC place
    result.sort_by_key(|row| {
        (
            row.place_in_bracket == 0,
            if row.place_in_bracket == 0 {
                row.place_in_bracket_orc
            } else {
                row.place_in_bracket
            },
        )
    });
    result
}

/// The scores of one group of a boat's series row (see [`group_scores`]).
#[derive(Debug, Clone, PartialEq)]
pub struct GroupScores {
    /// By race index; `None` for a race not sailed for the group.
    pub scores: Vec<Option<f64>>,
    /// The race index thrown out, if any.
    pub throw_out: Option<usize>,
    pub total: f64,
}

/// RRS A10(a): to the nearest tenth of a point, 0.05 rounded upward. The epsilon absorbs binary representation error.
pub fn round_to_tenth(score: f64) -> f64 {
    (score * 10.0 + 0.5 + 1e-9).floor() / 10.0
}

/// Scores one group (bracket or class) of a boat's series row, by race index.
///
/// - D2: a race not sailed for the group scores null and is left out of everything.
/// - GSI 14.4 / D2: with 5 or more sailed races, the worst scored race is thrown out. Among
///   equal worst scores the one with the worse `tie_score` goes, the first on a full tie.
/// - GSI 1.4.1.2 / D1: the boat's first RC volunteer race scores the average of its other
///   sailed races, the throw-out left out, rounded to the nearest tenth of a point with 0.05
///   rounded up (RRS A10(a), [round_to_tenth]); later RC races score DNS (`dns`) and count in
///   that average; with no other race to average, the RC race scores DNS too. The RC race is
///   never the throw-out, since the average cannot be worse than the worst of the races it is
///   taken from.
/// - The total is the sum of the sailed races that were not thrown out.
pub fn group_scores(
    cells: &[StandingsRace],
    sailed: impl Fn(&StandingsRace) -> bool,
    score: impl Fn(&StandingsRace) -> f64,
    tie_score: impl Fn(&StandingsRace) -> f64,
    dns: impl Fn(usize) -> f64,
) -> GroupScores {
    let sailed_idx: Vec<usize> = (0..cells.len()).filter(|&i| sailed(&cells[i])).collect();
    let rc_idx: Vec<usize> = sailed_idx
        .iter()
        .copied()
        .filter(|&i| cells[i].finish_code == Some(FinishCode::DnsRc))
        .collect();
    let mut scores: Vec<Option<f64>> = vec![None; cells.len()];
    for &i in &sailed_idx {
        scores[i] = Some(score(&cells[i]));
    }
    for &i in rc_idx.iter().skip(1) {
        scores[i] = Some(dns(i));
    }
    let first_rc = rc_idx.first().copied();
    let scored_idx: Vec<usize> = sailed_idx
        .iter()
        .copied()
        .filter(|&i| Some(i) != first_rc)
        .collect();
    let scored = |i: usize| scores[i].expect("a scored race has a score");

    let throw_out = if sailed_idx.len() >= 5 && !scored_idx.is_empty() {
        let worst = scored_idx
            .iter()
            .map(|&i| scored(i))
            .fold(f64::NEG_INFINITY, f64::max);
        // maxByOrNull keeps the first of equal maxima
        scored_idx
            .iter()
            .copied()
            .filter(|&i| scored(i) == worst)
            .fold(None::<usize>, |best, i| match best {
                Some(b) if tie_score(&cells[b]) >= tie_score(&cells[i]) => Some(b),
                _ => Some(i),
            })
    } else {
        None
    };

    if let Some(rc) = first_rc {
        let others: Vec<usize> = scored_idx
            .iter()
            .copied()
            .filter(|&i| Some(i) != throw_out)
            .collect();
        scores[rc] = Some(if others.is_empty() {
            dns(rc)
        } else {
            round_to_tenth(sum_of(others.iter().map(|&i| scored(i))) / others.len() as f64)
        });
    }

    let total = sum_of(
        sailed_idx
            .iter()
            .copied()
            .filter(|&i| Some(i) != throw_out)
            .map(|i| scores[i].expect("a sailed race has a score")),
    );
    GroupScores {
        scores,
        throw_out,
        total,
    }
}

/// Places the cards of one class (`boat_cards`, indices into `cards`) in the class and in each
/// of the class's brackets, writing the places into `cards`, and returns the class report with
/// copies of the placed cards; `None` when no bracket has a card.
pub fn class_card(
    race_schedule: &RaceSchedule,
    class_schedule: &ClassSchedule,
    cards: &mut [RaceReportCard],
    boat_cards: &[usize],
    race_class: &RaceClass,
) -> Option<ClassReportCards> {
    //Class Places
    let mut has_orc_results = false;
    // R1 (orc_series_standings.md): in a rated class the PHRF places are among the PHRF-rated
    // entries, as the ORC places are among the ORC-rated ones; a cruising class places every entry.
    let among = if race_class.rating_type.is_orc_or_phrf() {
        PlaceAmong::Phrf
    } else {
        PlaceAmong::All
    };
    let in_class: Vec<usize> = boat_cards
        .iter()
        .copied()
        .filter(|&i| cards[i].result_record.bracket.class_id == race_class.id)
        .collect();
    let class_cards = place(cards, &in_class, among, |p, card| card.place_in_class = p);

    place(cards, &class_cards, PlaceAmong::Orc, |p, card| {
        has_orc_results = true;
        card.place_in_class_orc = p;
    });

    //Bracket Places
    let bracket_cards: Vec<BracketReportCards> = class_schedule
        .brackets
        .iter()
        .filter_map(|bracket| {
            let in_bracket: Vec<usize> = class_cards
                .iter()
                .copied()
                .filter(|&i| cards[i].result_record.bracket.id == bracket.id)
                .collect();
            let mut bracket_cards = place(cards, &in_bracket, among, |p, card| {
                card.place_in_bracket = p
            });

            place(cards, &bracket_cards, PlaceAmong::Orc, |p, card| {
                card.place_in_bracket_orc = p;
            });

            if !bracket_cards.is_empty() {
                // entries with a PHRF place by that place, then ORC-only entries by their ORC place
                bracket_cards.sort_by_key(|&i| {
                    let card = &cards[i];
                    (
                        card.place_in_bracket == 0,
                        if card.place_in_bracket == 0 {
                            card.place_in_bracket_orc
                        } else {
                            card.place_in_bracket
                        },
                    )
                });
                Some(BracketReportCards {
                    bracket: bracket.clone(),
                    cards: bracket_cards.iter().map(|&i| cards[i].clone()).collect(),
                })
            } else {
                None
            }
        })
        .collect();

    let class_report = ClassReportCards {
        race_class: race_class.clone(),
        bracket_report: bracket_cards,
        phrf_b_factor: race_schedule.race.phrf_b_factor,
        orc_scoring_option: race_schedule.race.orc_scoring_option,
        orc3_band: race_schedule.race.orc3_band,
        orc5_band: race_schedule.race.orc5_band,
        has_orc_results,
    };
    (!class_report.bracket_report.is_empty()).then_some(class_report)
}

/// The 2025→2026 switch: ORC scoring only applies from the 2026 season on. Before that a rated
/// class scores as PHRF only, whatever its rating type says. The season is the class start's
/// year in America/Los_Angeles. Cruising classes are returned unchanged.
pub fn effective_race_class(class_schedule: &ClassSchedule) -> RaceClass {
    let race_class = &class_schedule.race_class;
    if !race_class.rating_type.is_orc_or_phrf() {
        return race_class.clone();
    }
    let year = class_schedule
        .race_start()
        .and_then(|start| start.in_tz(PACIFIC_ZONE).ok())
        .map_or(0, |zoned| i32::from(zoned.year()));
    if year >= 2026 {
        race_class.clone()
    } else {
        RaceClass {
            rating_type: RatingType::Phrf,
            ..race_class.clone()
        }
    }
}

/// Pure race report: places every card in its class and bracket. Each card belongs to at most
/// one class (by its bracket's class id), so the cards can be shared across the class loop.
pub fn report(race_schedule: RaceSchedule, mut cards: Vec<RaceReportCard>) -> RaceReport {
    let mut class_report_list: Vec<ClassReportCards> = Vec::new();

    //Places
    for class_schedule in &race_schedule.schedule {
        let rt = class_schedule.race_class.rating_type;
        let class_cards: Vec<usize> = (0..cards.len())
            .filter(|&i| {
                if rt.is_orc_or_phrf() {
                    cards[i].rating_type().is_orc_or_phrf()
                } else {
                    cards[i].rating_type() == rt
                }
            })
            .collect();
        if let Some(class_report) = class_card(
            &race_schedule,
            class_schedule,
            &mut cards,
            &class_cards,
            &effective_race_class(class_schedule),
        ) {
            class_report_list.push(class_report);
        }
    }

    //Orphan results
    let orphans: Vec<usize> = (0..cards.len())
        .filter(|&i| cards[i].result_record.bracket.id == 0)
        .collect();
    place(&mut cards, &orphans, PlaceAmong::All, |i, card| {
        card.place_in_bracket = i;
        card.place_in_class = i;
    });
    if !orphans.is_empty() {
        let orphan_cards = BracketReportCards {
            bracket: Bracket {
                name: "Invalid result records".to_string(),
                ..Bracket::default()
            },
            // the orphans in their input order, not in place order (Kotlin passes the receiver list)
            cards: orphans.iter().map(|&i| cards[i].clone()).collect(),
        };
        class_report_list.push(ClassReportCards {
            race_class: RaceClass {
                name: "Incorrectly classed".to_string(),
                ..RaceClass::default()
            },
            bracket_report: vec![orphan_cards],
            phrf_b_factor: race_schedule.race.phrf_b_factor,
            orc_scoring_option: race_schedule.race.orc_scoring_option,
            orc3_band: race_schedule.race.orc3_band,
            orc5_band: race_schedule.race.orc5_band,
            has_orc_results: false,
        });
    }

    RaceReport {
        race_schedule,
        class_reports: class_report_list,
    }
}

/// Pure card: elapsed time from the class start, PHRF and ORC time correction factors, and
/// corrected times. Places are left at 0 for [class_card] to fill in.
///
/// (= `card`; the Kotlin `reduceToCard` that picks the certificate by `orcRef` and the class
/// start by the bracket's class id is the server crate's.)
pub fn card(
    record: RaceResultBoatBracket,
    class_start: Option<Timestamp>,
    race: &Race,
    orc_cert: Option<&OrcCertificate>,
) -> RaceReportCard {
    let result = &record.result;
    let boat = record.boat_skipper.boat.as_ref();
    let skipper = record.boat_skipper.skipper.as_ref();
    let time = result
        .finish
        .and_then(|finish| class_start.map(|start| finish.duration_since(start)));

    let phrf_tcf = if result.rating_type.is_cruising() {
        1.0
    } else {
        phrf_tcf(Some(race.phrf_b_factor), result.phrf_rating)
    };

    let orc_tcf = orc_cert.map_or(1.0, |cert| {
        orc_tcf(
            Some(race.orc3_band),
            Some(race.orc5_band),
            race.orc_scoring_option,
            cert,
        )
        .expect("a race carries both ORC wind bands")
    });

    let corrected_phrf_time = boat_corrected_time(phrf_tcf, class_start, result.finish)
        .filter(|_| result.finish.is_some());
    let corrected_orc_time = boat_corrected_time(orc_tcf, class_start, result.finish)
        .filter(|_| result.finish.is_some());

    RaceReportCard {
        boat_name: boat.map_or_else(String::new, |b| b.name.clone()),
        sail: boat.map_or_else(String::new, |b| b.sail_number.clone()),
        skipper: skipper.map_or_else(String::new, |s| s.full_name()),
        boat_type: boat.map_or_else(String::new, |b| b.boat_type.clone()),
        phrf_rating: result.phrf_rating,
        start_time: class_start,
        finish_time: result.finish,
        elapsed_time: time,
        phrf_tcf,
        orc_tcf,
        corrected_phrf_time,
        corrected_orc_time,
        place_in_bracket: 0,
        place_in_class_orc: 0,
        place_in_class: 0,
        place_in_bracket_orc: 0,
        hoc_position: result.hoc_position,
        penalty: result.penalty,
        orc_dxt_id: orc_cert.and_then(|cert| cert.dxt_id),
        result_record: record,
    }
}

/// GSI 14.3 time-on-time: `650 / (B + rating)`; 1.0 without a B factor or a rating (D8 legacy rows).
pub fn phrf_tcf(factor: Option<i32>, phrf_rating: Option<i32>) -> f64 {
    match (factor, phrf_rating) {
        (Some(cf), Some(rating)) => 650.0 / (f64::from(cf) + f64::from(rating)),
        _ => 1.0,
    }
}

/// `orc_tcf` was asked for a banded option without its band (Kotlin `requireNotNull`).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OrcTcfError {
    /// A triple-number option needs the 3-band wind band.
    Missing3Band(OrcScoringOption),
    /// A five-band option needs the 5-band wind band.
    Missing5Band(OrcScoringOption),
}

impl std::fmt::Display for OrcTcfError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            OrcTcfError::Missing3Band(option) => {
                write!(f, "{} needs the ORC 3-band wind band", option.name())
            }
            OrcTcfError::Missing5Band(option) => {
                write!(f, "{} needs the ORC 5-band wind band", option.name())
            }
        }
    }
}

impl std::error::Error for OrcTcfError {}

/// The certificate's time-on-time factor for the race's scoring option and wind band: one
/// certificate field per option × band; the single-number options ignore the bands.
pub fn orc_tcf(
    orc3_band: Option<Orc3Band>,
    orc5_band: Option<Orc5Band>,
    orc_scoring_option: OrcScoringOption,
    orc_certificate: &OrcCertificate,
) -> Result<f64, OrcTcfError> {
    let c = orc_certificate;
    let band3 = || orc3_band.ok_or(OrcTcfError::Missing3Band(orc_scoring_option));
    let band5 = || orc5_band.ok_or(OrcTcfError::Missing5Band(orc_scoring_option));
    Ok(match orc_scoring_option {
        OrcScoringOption::SingleNumberAllPurpose => c.all_purpose_tot,
        OrcScoringOption::SingleNumberWindwardLeeward => c.wl_single_number_tot,
        OrcScoringOption::TripleNumberAllPurpose => match band3()? {
            Orc3Band::Low => c.triple_number_all_purpose_low_tot,
            Orc3Band::Medium => c.triple_number_all_purpose_med_tot,
            Orc3Band::High => c.triple_number_all_purpose_hi_tot,
        },

        OrcScoringOption::TripleNumberWindwardLeeward => match band3()? {
            Orc3Band::Low => c.triple_number_wl_low_tot,
            Orc3Band::Medium => c.triple_number_wl_med_tot,
            Orc3Band::High => c.triple_number_wl_hi_tot,
        },

        OrcScoringOption::SingleNumberPredominantUpwind => c.single_number_predominant_upwind_tot,
        OrcScoringOption::SingleNumberPredominantReaching => {
            c.single_number_predominant_reaching_tot
        }
        OrcScoringOption::SingleNumberPredominantDownwind => {
            c.single_number_predominant_downwind_tot
        }
        OrcScoringOption::PredominantUpwind => match band5()? {
            Orc5Band::Low => c.predominant_upwind_low_tot,
            Orc5Band::LowMedium => c.predominant_upwind_low_med_tot,
            Orc5Band::Medium => c.predominant_upwind_med_tot,
            Orc5Band::MediumHigh => c.predominant_upwind_med_hi_tot,
            Orc5Band::High => c.predominant_upwind_hi_tot,
        },

        OrcScoringOption::PredominantDownwind => match band5()? {
            Orc5Band::Low => c.predominant_downwind_low_tot,
            Orc5Band::LowMedium => c.predominant_downwind_low_med_tot,
            Orc5Band::Medium => c.predominant_downwind_med_tot,
            Orc5Band::MediumHigh => c.predominant_downwind_med_hi_tot,
            Orc5Band::High => c.predominant_downwind_hi_tot,
        },

        OrcScoringOption::PredominantReaching => match band5()? {
            Orc5Band::Low => c.predominant_reaching_low_tot,
            Orc5Band::LowMedium => c.predominant_reaching_low_med_tot,
            Orc5Band::Medium => c.predominant_reaching_med_tot,
            Orc5Band::MediumHigh => c.predominant_reaching_med_hi_tot,
            Orc5Band::High => c.predominant_reaching_hi_tot,
        },

        OrcScoringOption::FiveBandWindwardLeeward => match band5()? {
            Orc5Band::Low => c.five_band_wl_low_tot,
            Orc5Band::LowMedium => c.five_band_wl_low_med_tot,
            Orc5Band::Medium => c.five_band_wl_med_tot,
            Orc5Band::MediumHigh => c.five_band_wl_med_hi_tot,
            Orc5Band::High => c.five_band_wl_hi_tot,
        },

        OrcScoringOption::WindwardLeeward6040 => match band5()? {
            Orc5Band::Low => c.us_wl6040_l_tot,
            Orc5Band::LowMedium => c.us_wl6040_lm_tot,
            Orc5Band::Medium => c.us_wl6040_m_tot,
            Orc5Band::MediumHigh => c.us_wl6040_mh_tot,
            Orc5Band::High => c.us_wl6040_h_tot,
        },

        OrcScoringOption::FiveBandAllPurpose => match band5()? {
            Orc5Band::Low => c.five_band_all_purpose_low_tot,
            Orc5Band::LowMedium => c.five_band_all_purpose_low_med_tot,
            Orc5Band::Medium => c.five_band_all_purpose_med_tot,
            Orc5Band::MediumHigh => c.five_band_all_purpose_med_hi_tot,
            Orc5Band::High => c.five_band_all_purpose_hi_tot,
        },
    })
}

/// Kotlin `Duration.MAX_NANOS`: the largest magnitude a `Duration` stores in nanoseconds.
const MAX_NANOS: i64 = i64::MAX / 2 / 1_000_000 * 1_000_000 - 1;
/// Kotlin `Duration.MAX_MILLIS`.
const MAX_MILLIS: i64 = i64::MAX / 2;

/// The corrected time: `(finish - start).inWholeMilliseconds * tcf` as a `Double`, then Kotlin's
/// `Double.toDuration(MILLISECONDS)`, which stores the value in nanoseconds rounded to nearest
/// (`roundToLong`, ties toward positive infinity) and falls back to whole milliseconds only for
/// magnitudes over ~146 years. The ISO strings of the golden files carry exactly those
/// nanoseconds (`PT1H0M10.173160173S`).
pub fn boat_corrected_time(
    tcf: f64,
    start: Option<Timestamp>,
    finish: Option<Timestamp>,
) -> Option<SignedDuration> {
    let (start, finish) = (start?, finish?);
    let ms = whole_millis(finish.duration_since(start)) as f64 * tcf;
    Some(double_millis_to_duration(ms))
}

/// Kotlin `Double.toDuration(DurationUnit.MILLISECONDS)` (NaN, which cannot occur here, becomes
/// zero where Kotlin throws).
fn double_millis_to_duration(ms: f64) -> SignedDuration {
    let value_in_ns = ms * 1_000_000.0;
    let nanos = round_to_long(value_in_ns);
    if (-MAX_NANOS..=MAX_NANOS).contains(&nanos) {
        SignedDuration::from_nanos(nanos)
    } else {
        let millis = round_to_long(ms).clamp(-MAX_MILLIS, MAX_MILLIS);
        SignedDuration::from_millis(millis)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn corrected_time_rounds_to_the_nearest_nanosecond() {
        // 3_600_000 ms * 650 / 648 = 3611111.1111... ms -> 3611111111111.1 ns -> ...111 ns
        let start: Timestamp = "2026-06-18T01:00:00Z".parse().unwrap();
        let finish: Timestamp = "2026-06-18T02:00:00Z".parse().unwrap();
        let d = boat_corrected_time(650.0 / 648.0, Some(start), Some(finish)).unwrap();
        assert_eq!(d.as_nanos(), 3_611_111_111_111);
        assert_eq!(
            regatta_common::serde_kotlin::format_duration(d),
            "PT1H0M11.111111111S"
        );
        assert_eq!(boat_corrected_time(1.0, None, Some(finish)), None);
        assert_eq!(boat_corrected_time(1.0, Some(start), None), None);
    }

    #[test]
    fn round_to_tenth_rounds_half_up() {
        assert_eq!(round_to_tenth(2.75), 2.8);
        assert_eq!(round_to_tenth(2.65), 2.7);
        assert_eq!(round_to_tenth(8.0 / 3.0), 2.7);
        assert_eq!(round_to_tenth(2.0), 2.0);
        assert_eq!(round_to_tenth(2.5), 2.5);
    }

    #[test]
    fn sum_of_an_empty_group_is_positive_zero() {
        let total = sum_of(std::iter::empty());
        assert_eq!(total.to_bits(), 0.0f64.to_bits());
        assert_eq!(serde_json::to_string(&total).unwrap(), "0.0");
    }
}

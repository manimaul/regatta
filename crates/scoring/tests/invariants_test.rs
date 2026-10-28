//! Property-style checks over random fleets (`docs/explore_implemented/test_audit_and_plan.md` §4).
//!
//! Each test builds many random races or series with the fixture DSL and checks a rule that must hold whatever
//! the fleet looks like. proptest strategies generate the fleets (and shrink a failing one); the entry shuffles
//! come from a small seeded PRNG. Every bare decision ID (`Dn`) in this file is a scoring decision of
//! `test_audit_and_plan.md`.

use proptest::prelude::*;
use proptest::test_runner::{Config, TestRunner};
use regatta_common::FinishCode::*;
use regatta_common::{
    FinishCode, OrcCertificate, RaceReport, RaceReportCard, RatingType, StandingsSeries,
};
use regatta_scoring::fixture::*;
use std::cell::Cell;
use std::collections::{HashMap, HashSet};

// ---- random fleet specs, buildable in any entry order

#[derive(Clone, Debug, PartialEq)]
enum Outcome {
    Finish(i64),
    Hoc(i32),
    Code(FinishCode),
}

#[derive(Clone, Debug)]
struct BoatSpec {
    name: String,
    outcome: Outcome,
    penalty: Option<i32>,
    rating_type: RatingType,
    orc_ref: Option<String>,
}

/// bracket name -> entries (in order of first appearance); [orc_class] enters them in an ORC/PHRF class instead of a
/// PHRF one
#[derive(Clone, Debug)]
struct RaceSpec {
    brackets: Vec<(String, Vec<BoatSpec>)>,
    orc_class: bool,
}

impl RaceSpec {
    fn map(&self, f: impl Fn(&str, &BoatSpec) -> Option<BoatSpec>) -> RaceSpec {
        RaceSpec {
            brackets: self
                .brackets
                .iter()
                .map(|(b, boats)| {
                    (
                        b.clone(),
                        boats.iter().filter_map(|boat| f(b, boat)).collect(),
                    )
                })
                .collect(),
            orc_class: self.orc_class,
        }
    }

    fn boats(&self) -> impl Iterator<Item = &BoatSpec> {
        self.brackets.iter().flat_map(|(_, boats)| boats.iter())
    }
}

#[derive(Clone, Debug)]
struct SeriesSpec {
    races: Vec<RaceSpec>,
    certs: HashMap<String, OrcCertificate>,
}

/// Groups `items` by `key`, the groups in order of first appearance.
fn group_by<T: Clone>(items: &[T], key: impl Fn(&T) -> String) -> Vec<(String, Vec<T>)> {
    let mut groups: Vec<(String, Vec<T>)> = Vec::new();
    for item in items {
        let k = key(item);
        match groups.iter_mut().find(|(g, _)| *g == k) {
            Some((_, v)) => v.push(item.clone()),
            None => groups.push((k, vec![item.clone()])),
        }
    }
    groups
}

/// A seeded xorshift PRNG for the entry shuffles, so a failing shuffle can be replayed.
struct Rng(u64);

impl Rng {
    fn new(seed: u64) -> Self {
        Rng(seed.wrapping_mul(0x9E37_79B9_7F4A_7C15) | 1)
    }

    fn next(&mut self) -> u64 {
        let mut x = self.0;
        x ^= x << 13;
        x ^= x >> 7;
        x ^= x << 17;
        self.0 = x;
        x
    }

    fn shuffled<T: Clone>(&mut self, items: &[T]) -> Vec<T> {
        let mut v = items.to_vec();
        for i in (1..v.len()).rev() {
            let j = (self.next() % (i as u64 + 1)) as usize;
            v.swap(i, j);
        }
        v
    }
}

fn clock(ms: i64) -> String {
    let h = 18 + ms / 3_600_000;
    let m = ms / 60_000 % 60;
    let s = ms / 1000 % 60;
    let millis = ms % 1000;
    format!("{h:02}:{m:02}:{s:02}.{millis:03}")
}

fn enter(b: &mut BracketBuilder<'_>, spec: &BoatSpec) {
    let rating = (spec.rating_type != RatingType::Orc).then_some(50);
    let entry = b.boat_with(
        &spec.name,
        rating,
        BoatOpts {
            rating_type: Some(spec.rating_type),
            ..Default::default()
        },
    );
    if let Some(orc_ref) = &spec.orc_ref {
        entry.orc(orc_ref);
    }
    match &spec.outcome {
        Outcome::Finish(ms) => {
            entry.finishes(&clock(*ms));
        }
        Outcome::Hoc(position) => {
            entry.hoc(*position);
        }
        Outcome::Code(code) => {
            entry.code(*code);
        }
    }
    if let Some(penalty) = spec.penalty {
        entry.penalty(penalty);
    }
}

fn enter_race(r: &mut RaceBuilder<'_>, spec: &RaceSpec, shuffle: &mut Option<Rng>) {
    let block = |c: &mut ClassBuilder<'_>| {
        for (name, boats) in &spec.brackets {
            let order = match shuffle {
                Some(rng) => rng.shuffled(boats),
                None => boats.clone(),
            };
            c.bracket(name, |b| {
                for boat in &order {
                    enter(b, boat);
                }
            });
        }
    };
    if spec.orc_class {
        r.orc_phrf_class("Fast", block)
    } else {
        r.phrf_class("Fast", block)
    }
}

fn build_race(spec: &RaceSpec, shuffle: Option<u64>) -> RaceReport {
    let mut rng = shuffle.map(Rng::new);
    race_on("2026-06-17", "18:00", |r| enter_race(r, spec, &mut rng)).report(&HashMap::new())
}

fn build_series(spec: &SeriesSpec, shuffle: Option<u64>) -> StandingsSeries {
    let mut rng = shuffle.map(Rng::new);
    series(|s| {
        for r in &spec.races {
            s.race(|rb| enter_race(rb, r, &mut rng));
        }
    })
    .standings_with(None, &spec.certs)
}

fn boat_names() -> Vec<String> {
    ('A'..='L').map(|c| c.to_string()).collect()
}

/// A random race over [boats]: 1 or 2 brackets, mixed outcomes, no exact ties, no penalties unless asked.
fn race_spec(
    boats: Vec<String>,
    codes: Vec<FinishCode>,
    with_penalties: bool,
) -> BoxedStrategy<RaceSpec> {
    let n = boats.len();
    let bracket_count = if n >= 4 {
        prop_oneof![Just(1usize), Just(2usize)].boxed()
    } else {
        Just(1usize).boxed()
    };
    let kinds = prop::collection::vec(0..10u8, n);
    let finishes = prop::collection::btree_set(0..3 * 3_600_000i64, n)
        .prop_map(|set| set.into_iter().collect::<Vec<_>>())
        .prop_shuffle();
    // HOC positions are numbered across the whole race (GSI 14.2), so they are unique per race, not per bracket
    let hocs = Just((1..=n as i32).collect::<Vec<_>>()).prop_shuffle();
    let code_idx = prop::collection::vec(0..codes.len(), n);
    let penalties = prop::collection::vec((0..4u8, 1..4i32), n);
    (bracket_count, kinds, finishes, hocs, code_idx, penalties)
        .prop_map(
            move |(bracket_count, kinds, finishes, hocs, code_idx, penalties)| {
                let mut hoc_positions = hocs.into_iter();
                let specs: Vec<(String, BoatSpec)> = boats
                    .iter()
                    .enumerate()
                    .map(|(i, name)| {
                        let outcome = match kinds[i] {
                            0..=5 => Outcome::Finish(finishes[i]),
                            6..=7 => {
                                Outcome::Hoc(hoc_positions.next().expect("a position per boat"))
                            }
                            _ => Outcome::Code(codes[code_idx[i]]),
                        };
                        let penalty = (with_penalties
                            && !matches!(outcome, Outcome::Code(_))
                            && penalties[i].0 == 0)
                            .then_some(penalties[i].1);
                        (
                            "I".repeat(1 + i % bracket_count),
                            BoatSpec {
                                name: name.clone(),
                                outcome,
                                penalty,
                                rating_type: RatingType::Phrf,
                                orc_ref: None,
                            },
                        )
                    })
                    .collect();
                RaceSpec {
                    brackets: group_by(&specs, |(b, _)| b.clone())
                        .into_iter()
                        .map(|(b, entries)| {
                            (b, entries.into_iter().map(|(_, boat)| boat).collect())
                        })
                        .collect(),
                    orc_class: false,
                }
            },
        )
        .boxed()
}

/// A random race: 2..7 boats from the pool.
fn random_race(codes: Vec<FinishCode>, with_penalties: bool) -> BoxedStrategy<RaceSpec> {
    (Just(boat_names()).prop_shuffle(), 2..8usize)
        .prop_flat_map(move |(names, n)| {
            race_spec(names[..n].to_vec(), codes.clone(), with_penalties)
        })
        .boxed()
}

/// A random series: 1..7 races over a pool of 3..6 boats that keep their bracket; absent boats
/// are DNS. With [with_orc] the class is ORC/PHRF and each boat is ORC/PHRF, PHRF-only or
/// ORC-only for the whole series, with a random certificate when ORC-rated.
fn random_series(with_rc: bool, with_orc: bool) -> BoxedStrategy<SeriesSpec> {
    let codes = if with_rc {
        vec![Dnf, Ret, Nsc, DnsRc]
    } else {
        vec![Dnf, Ret, Nsc]
    };
    (
        Just(boat_names()).prop_shuffle(),
        3..7usize,
        prop::collection::vec(any::<bool>(), 12),
        1..8usize,
    )
        .prop_flat_map(move |(names, pool_n, bracket_bools, race_n)| {
            let pool: Vec<String> = names[..pool_n].to_vec();
            let bracket_of: HashMap<String, String> = pool
                .iter()
                .enumerate()
                .map(|(i, b)| {
                    (
                        b.clone(),
                        if pool_n >= 4 && bracket_bools[i] {
                            "II"
                        } else {
                            "I"
                        }
                        .to_string(),
                    )
                })
                .collect();
            let codes = codes.clone();
            let race = {
                let pool = pool.clone();
                prop::collection::vec(0..5u8, pool_n).prop_flat_map(move |present_draw| {
                    let present: Vec<String> = pool
                        .iter()
                        .zip(&present_draw)
                        .filter(|(_, d)| **d != 0)
                        .map(|(p, _)| p.clone())
                        .collect();
                    let present = if present.is_empty() {
                        vec![pool[0].clone()]
                    } else {
                        present
                    };
                    race_spec(present, codes.clone(), false)
                })
            };
            let races = prop::collection::vec(race, race_n);
            let ratings = prop::collection::vec((0..10u8, 0.0..1.0f64), pool_n);
            (races, ratings).prop_map(move |(races, ratings)| {
                // regroup into the boats' fixed brackets
                let races: Vec<RaceSpec> = races
                    .iter()
                    .map(|r| {
                        let all: Vec<BoatSpec> = r.boats().cloned().collect();
                        RaceSpec {
                            brackets: group_by(&all, |b| bracket_of[&b.name].clone()),
                            orc_class: false,
                        }
                    })
                    .collect();
                if !with_orc {
                    return SeriesSpec {
                        races,
                        certs: HashMap::new(),
                    };
                }
                let rating_of: HashMap<String, RatingType> = pool
                    .iter()
                    .zip(&ratings)
                    .map(|(b, (k, _))| {
                        (
                            b.clone(),
                            match k {
                                0..=5 => RatingType::OrcPhrf,
                                6..=7 => RatingType::Phrf,
                                _ => RatingType::Orc,
                            },
                        )
                    })
                    .collect();
                let certs: HashMap<String, OrcCertificate> = pool
                    .iter()
                    .zip(&ratings)
                    .filter(|(b, _)| rating_of[*b].is_orc())
                    .map(|(b, (_, r))| {
                        (
                            b.clone(),
                            OrcCertificate {
                                ref_no: b.clone(),
                                five_band_wl_low_med_tot: 0.5 + r * 0.5,
                                ..Default::default()
                            },
                        )
                    })
                    .collect();
                let orc_races = races
                    .iter()
                    .map(|r| RaceSpec {
                        brackets: r
                            .brackets
                            .iter()
                            .map(|(name, boats)| {
                                (
                                    name.clone(),
                                    boats
                                        .iter()
                                        .map(|b| BoatSpec {
                                            rating_type: rating_of[&b.name],
                                            orc_ref: certs.get(&b.name).map(|c| c.ref_no.clone()),
                                            ..b.clone()
                                        })
                                        .collect(),
                                )
                            })
                            .collect(),
                        orc_class: true,
                    })
                    .collect();
                SeriesSpec {
                    races: orc_races,
                    certs,
                }
            })
        })
        .boxed()
}

fn code(card: &RaceReportCard) -> FinishCode {
    card.result_record.result.finish_code
}

fn ms(card: &RaceReportCard) -> Option<i64> {
    card.corrected_phrf_time.map(|d| d.as_millis() as i64)
}

#[derive(Debug, Clone, PartialEq)]
struct RowSnapshot {
    bracket: Vec<f64>,
    class: Vec<f64>,
    total_bracket: f64,
    total_class: f64,
    place_bracket: i32,
    place_class: i32,
    bracket_orc: Vec<f64>,
    class_orc: Vec<f64>,
    total_bracket_orc: f64,
    total_class_orc: f64,
    place_bracket_orc: i32,
    place_class_orc: i32,
    rating_type: RatingType,
}

fn snapshot(standings: &StandingsSeries) -> HashMap<String, RowSnapshot> {
    standings
        .rows()
        .into_iter()
        .map(|row| {
            let boat = row
                .boat_skipper
                .boat
                .as_ref()
                .map_or(String::new(), |b| b.name.clone());
            (
                boat,
                RowSnapshot {
                    bracket: row
                        .race_standings
                        .iter()
                        .map(|r| r.place_in_bracket)
                        .collect(),
                    class: row
                        .race_standings
                        .iter()
                        .map(|r| r.place_in_class)
                        .collect(),
                    total_bracket: row.total_score_bracket,
                    total_class: row.total_score_class,
                    place_bracket: row.place_in_bracket,
                    place_class: row.place_in_class,
                    bracket_orc: row
                        .race_standings
                        .iter()
                        .map(|r| r.place_in_bracket_orc)
                        .collect(),
                    class_orc: row
                        .race_standings
                        .iter()
                        .map(|r| r.place_in_class_orc)
                        .collect(),
                    total_bracket_orc: row.total_score_bracket_orc,
                    total_class_orc: row.total_score_class_orc,
                    place_bracket_orc: row.place_in_bracket_orc,
                    place_class_orc: row.place_in_class_orc,
                    rating_type: row.rating_type,
                },
            )
        })
        .collect()
}

fn config(cases: u32) -> ProptestConfig {
    ProptestConfig {
        cases,
        ..ProptestConfig::default()
    }
}

/// The three-digit seed of a shuffled rebuild.
fn seeds() -> impl Strategy<Value = [u64; 3]> {
    prop::array::uniform3(any::<u64>())
}

fn sum_f64(values: impl Iterator<Item = f64>) -> f64 {
    values.fold(0.0, |a, b| a + b)
}

// ---- race invariants

proptest! {
    #![proptest_config(config(300))]

    #[test]
    fn shuffling_race_entries_never_changes_a_score(spec in random_race(vec![Dnf, Ret, Nsc], false), seeds in seeds()) {
        let base = build_race(&spec, None);
        for seed in seeds {
            let shuffled = build_race(&spec, Some(seed));
            prop_assert_eq!(base.bracket_scores(), shuffled.bracket_scores(), "bracket");
            prop_assert_eq!(base.class_scores(), shuffled.class_scores(), "class");
        }
    }

    /// Timed finishers take places 1..k in corrected-time order, HOC boats k+1..k+h by position, and no score is below
    /// 1.
    #[test]
    fn places_are_contiguous_and_follow_corrected_time_then_hoc(spec in random_race(vec![Dnf, Ret, Nsc], false)) {
        let report = build_race(&spec, None);
        for cr in &report.class_reports {
            let mut groups: Vec<Vec<&RaceReportCard>> = cr.bracket_report.iter().map(|b| b.cards.iter().collect()).collect();
            groups.push(cr.bracket_report.iter().flat_map(|b| b.cards.iter()).collect());
            for (g, cards) in groups.iter().enumerate() {
                let score = |c: &RaceReportCard| if g < cr.bracket_report.len() { c.place_in_bracket } else { c.place_in_class };
                let mut timed: Vec<&RaceReportCard> = cards.iter().copied().filter(|c| code(c) == Time).collect();
                timed.sort_by_key(|c| ms(c));
                let mut hoc: Vec<&RaceReportCard> = cards.iter().copied().filter(|c| code(c) == Hoc).collect();
                hoc.sort_by_key(|c| c.hoc_position);
                let k = timed.len() as i32;
                let h = hoc.len() as i32;
                prop_assert_eq!((1..=k).collect::<Vec<_>>(), timed.iter().map(|c| score(c)).collect::<Vec<_>>(), "timed places");
                prop_assert_eq!((k + 1..=k + h).collect::<Vec<_>>(), hoc.iter().map(|c| score(c)).collect::<Vec<_>>(), "hoc places");
                prop_assert!(cards.iter().all(|c| score(c) >= 1), "a score below 1");
            }
        }
    }

    /// GSI 14.1.1 / 14.1.3: DNF and RET score finishers + 1, where finishers are TIME and HOC.
    #[test]
    fn dnf_and_ret_score_finishers_plus_one(spec in random_race(vec![Dnf, Ret, Nsc], false)) {
        let report = build_race(&spec, None);
        for cr in &report.class_reports {
            for br in &cr.bracket_report {
                let finishers = br.cards.iter().filter(|c| code(c) == Time || code(c) == Hoc).count() as i32;
                for c in br.cards.iter().filter(|c| code(c) == Dnf || code(c) == Ret) {
                    prop_assert_eq!(finishers + 1, c.place_in_bracket, "{}", c.boat_name);
                }
            }
            let all: Vec<&RaceReportCard> = cr.bracket_report.iter().flat_map(|b| b.cards.iter()).collect();
            let class_finishers = all.iter().filter(|c| code(c) == Time || code(c) == Hoc).count() as i32;
            for c in all.iter().filter(|c| code(c) == Dnf || code(c) == Ret) {
                prop_assert_eq!(class_finishers + 1, c.place_in_class, "{} class", c.boat_name);
            }
        }
    }

    /// NSC scores at least the DNF score.
    #[test]
    fn nsc_is_never_better_than_dnf(spec in random_race(vec![Dnf, Ret, Nsc], false)) {
        let report = build_race(&spec, None);
        for br in report.class_reports.iter().flat_map(|c| c.bracket_report.iter()) {
            let dnf = br.cards.iter().filter(|c| code(c) == Time || code(c) == Hoc).count() as i32 + 1;
            for c in br.cards.iter().filter(|c| code(c) == Nsc) {
                prop_assert!(c.place_in_bracket >= dnf, "{}: NSC {} < DNF {}", c.boat_name, c.place_in_bracket, dnf);
            }
        }
    }

    /// Shuffling with penalties present must not change a score either (D4 applies each penalty independently).
    #[test]
    fn shuffling_race_entries_with_penalties_never_changes_a_score(spec in random_race(vec![Dnf, Ret, Nsc], true), seeds in seeds()) {
        let base = build_race(&spec, None);
        for seed in seeds {
            let shuffled = build_race(&spec, Some(seed));
            prop_assert_eq!(base.bracket_scores(), shuffled.bracket_scores());
        }
    }
}

/// D4: a penalty adds to the penalised boat's own place, capped at DNF, and changes nobody else.
#[test]
fn a_penalty_never_changes_another_boats_score() {
    let mut runner = TestRunner::new(Config {
        cases: 300,
        ..Config::default()
    });
    let penalised = Cell::new(0);
    runner
        .run(&random_race(vec![Dnf, Ret, Nsc], true), |spec| {
            let with = build_race(&spec, None);
            let without = build_race(
                &spec.map(|_, b| {
                    Some(BoatSpec {
                        penalty: None,
                        ..b.clone()
                    })
                }),
                None,
            );
            for br in with
                .class_reports
                .iter()
                .flat_map(|c| c.bracket_report.iter())
            {
                let dnf = br
                    .cards
                    .iter()
                    .filter(|c| code(c) == Time || code(c) == Hoc)
                    .count() as i32;
                let dnf = f64::from(dnf + 1);
                for card in &br.cards {
                    let clean = without.bracket_scores()[&card.boat_name];
                    let expected = match card.penalty {
                        Some(p) => {
                            penalised.set(penalised.get() + 1);
                            (clean + f64::from(p)).min(dnf)
                        }
                        None => clean,
                    };
                    prop_assert_eq!(
                        expected,
                        f64::from(card.place_in_bracket),
                        "{}",
                        card.boat_name
                    );
                }
            }
            let unpenalised = |scores: HashMap<String, f64>| -> HashMap<String, f64> {
                scores
                    .into_iter()
                    .filter(|(name, _)| {
                        spec.boats()
                            .find(|b| &b.name == name)
                            .unwrap()
                            .penalty
                            .is_none()
                    })
                    .collect()
            };
            prop_assert_eq!(
                unpenalised(with.class_scores()),
                unpenalised(without.class_scores()),
                "class scores of unpenalised boats"
            );
            Ok(())
        })
        .unwrap();
    let penalised = penalised.get();
    assert!(
        penalised > 50,
        "the generator produced too few penalties: {penalised}"
    );
}

// ---- series invariants

proptest! {
    #![proptest_config(config(200))]

    /// A series total is the sum of the counted race scores, with at most one score excluded, and only when 5 or more
    /// races were started.
    #[test]
    fn series_total_is_the_sum_of_counted_scores_with_at_most_one_excluded(spec in random_series(false, false)) {
        let standings = build_series(&spec, None);
        for row in standings.rows() {
            let boat = row.boat_skipper.boat.as_ref().map(|b| b.name.clone()).unwrap_or_default();
            // D2: each group has its own throw-out, chosen among the races sailed for that group
            let bracket_sailed: Vec<_> = row.race_standings.iter().filter(|r| r.bracket_sailed).collect();
            let class_sailed: Vec<_> = row.race_standings.iter().filter(|r| r.class_sailed).collect();
            let excluded_bracket = row.race_standings.iter().filter(|r| r.throw_out).count();
            let excluded_class = row.race_standings.iter().filter(|r| r.throw_out_class).count();
            prop_assert!(excluded_bracket <= 1 && excluded_class <= 1, "{boat}: {excluded_bracket} / {excluded_class} throw-outs");
            if bracket_sailed.len() < 5 {
                prop_assert_eq!(0, excluded_bracket, "{}: bracket throw-out with {} sailed races", boat, bracket_sailed.len());
            }
            if class_sailed.len() < 5 {
                prop_assert_eq!(0, excluded_class, "{}: class throw-out with {} sailed races", boat, class_sailed.len());
            }
            prop_assert!(!row.race_standings.iter().any(|r| r.throw_out && !r.bracket_sailed), "{boat}: an unsailed race thrown out");
            prop_assert_eq!(sum_f64(bracket_sailed.iter().filter(|r| !r.throw_out).map(|r| r.place_in_bracket)), row.total_score_bracket, "bracket total");
            prop_assert_eq!(sum_f64(class_sailed.iter().filter(|r| !r.throw_out_class).map(|r| r.place_in_class)), row.total_score_class, "class total");
        }
    }

    /// Series places follow totals: a better place never has a worse total, and every boat has one row.
    #[test]
    fn series_places_follow_totals(spec in random_series(false, false)) {
        let standings = build_series(&spec, None);
        for sc in &standings.standings {
            for sb in &sc.standings {
                let mut rows: Vec<_> = sb.standings.iter().collect();
                rows.sort_by_key(|r| r.place_in_bracket);
                for pair in rows.windows(2) {
                    prop_assert!(pair[0].total_score_bracket <= pair[1].total_score_bracket, "bracket {}: place order disagrees with totals", sb.bracket.name);
                }
                let names: HashSet<_> = rows.iter().map(|r| r.boat_skipper.boat.as_ref().map(|b| b.name.clone())).collect();
                prop_assert_eq!(rows.len(), names.len(), "a boat has two rows");
            }
            let mut rows: Vec<_> = sc.standings.iter().flat_map(|b| b.standings.iter()).collect();
            rows.sort_by_key(|r| r.place_in_class);
            for pair in rows.windows(2) {
                prop_assert!(pair[0].total_score_class <= pair[1].total_score_class, "class: place order disagrees with totals");
            }
        }
    }

    /// D5: shuffling the entries of every race never changes a standings score or place.
    #[test]
    fn shuffling_series_entries_never_changes_standings(spec in random_series(false, false), seeds in seeds()) {
        let base = snapshot(&build_series(&spec, None));
        for seed in seeds {
            prop_assert_eq!(&base, &snapshot(&build_series(&spec, Some(seed))));
        }
    }

    // ---- ORC series invariants (D9)

    /// D9: an ORC-rated row's ORC total follows the same rules as the PHRF one; a row not rated for a side has that
    /// side at zero.
    #[test]
    fn d9_orc_series_total_is_the_sum_of_counted_orc_scores_with_at_most_one_excluded(spec in random_series(false, true)) {
        let standings = build_series(&spec, None);
        for row in standings.rows() {
            let boat = row.boat_skipper.boat.as_ref().map(|b| b.name.clone()).unwrap_or_default();
            if row.rating_type.is_orc() {
                let bracket_sailed: Vec<_> = row.race_standings.iter().filter(|r| r.bracket_sailed_orc).collect();
                let class_sailed: Vec<_> = row.race_standings.iter().filter(|r| r.class_sailed_orc).collect();
                let excluded_bracket = row.race_standings.iter().filter(|r| r.throw_out_orc).count();
                let excluded_class = row.race_standings.iter().filter(|r| r.throw_out_class_orc).count();
                prop_assert!(excluded_bracket <= 1 && excluded_class <= 1, "{boat}: {excluded_bracket} / {excluded_class} ORC throw-outs");
                if bracket_sailed.len() < 5 {
                    prop_assert_eq!(0, excluded_bracket, "{}: ORC bracket throw-out with {} sailed races", boat, bracket_sailed.len());
                }
                if class_sailed.len() < 5 {
                    prop_assert_eq!(0, excluded_class, "{}: ORC class throw-out with {} sailed races", boat, class_sailed.len());
                }
                prop_assert!(!row.race_standings.iter().any(|r| r.throw_out_orc && !r.bracket_sailed_orc), "{boat}: an ORC-unsailed race thrown out");
                prop_assert!(!row.race_standings.iter().any(|r| !r.bracket_sailed_orc && r.place_in_bracket_orc != 0.0), "{boat}: an ORC-unsailed race scored");
                prop_assert_eq!(sum_f64(bracket_sailed.iter().filter(|r| !r.throw_out_orc).map(|r| r.place_in_bracket_orc)), row.total_score_bracket_orc, "{} ORC bracket total", boat);
                prop_assert_eq!(sum_f64(class_sailed.iter().filter(|r| !r.throw_out_class_orc).map(|r| r.place_in_class_orc)), row.total_score_class_orc, "{} ORC class total", boat);
            } else {
                prop_assert!(row.total_score_bracket_orc == 0.0 && row.total_score_class_orc == 0.0 && row.place_in_bracket_orc == 0 && row.place_in_class_orc == 0, "{boat}: ORC fields on a PHRF row");
                prop_assert!(row.race_standings.iter().all(|r| r.place_in_bracket_orc == 0.0 && r.place_in_class_orc == 0.0 && !r.throw_out_orc && !r.throw_out_class_orc && !r.non_starter_orc), "{boat}: ORC cells on a PHRF row");
            }
            if !row.rating_type.is_phrf() {
                prop_assert!(row.total_score_bracket == 0.0 && row.total_score_class == 0.0 && row.place_in_bracket == 0 && row.place_in_class == 0, "{boat}: PHRF fields on an ORC-only row");
                prop_assert!(row.race_standings.iter().all(|r| r.place_in_bracket == 0.0 && r.place_in_class == 0.0 && !r.throw_out && !r.throw_out_class), "{boat}: PHRF cells on an ORC-only row");
            }
        }
    }

    /// D9: ORC places follow ORC totals among the ORC-rated rows, and PHRF places follow PHRF totals among the
    /// PHRF-rated rows.
    #[test]
    fn d9_series_places_follow_totals_per_rating(spec in random_series(false, true)) {
        let standings = build_series(&spec, None);
        for sc in &standings.standings {
            for sb in &sc.standings {
                let mut orc: Vec<_> = sb.standings.iter().filter(|r| r.rating_type.is_orc()).collect();
                orc.sort_by_key(|r| r.place_in_bracket_orc);
                prop_assert!(orc.iter().all(|r| r.place_in_bracket_orc >= 1), "an ORC row without an ORC place");
                for pair in orc.windows(2) {
                    prop_assert!(pair[0].total_score_bracket_orc <= pair[1].total_score_bracket_orc, "ORC bracket: place order disagrees with totals");
                }
                let mut phrf: Vec<_> = sb.standings.iter().filter(|r| r.rating_type.is_phrf()).collect();
                phrf.sort_by_key(|r| r.place_in_bracket);
                prop_assert!(phrf.iter().all(|r| r.place_in_bracket >= 1), "a PHRF row without a PHRF place");
                for pair in phrf.windows(2) {
                    prop_assert!(pair[0].total_score_bracket <= pair[1].total_score_bracket, "PHRF bracket: place order disagrees with totals");
                }
                // row order: PHRF-placed rows by place, then the ORC-only rows by ORC place
                let order: Vec<(i32, i32)> = sb.standings.iter().map(|r| if r.place_in_bracket == 0 { (1, r.place_in_bracket_orc) } else { (0, r.place_in_bracket) }).collect();
                let mut sorted = order.clone();
                sorted.sort();
                prop_assert_eq!(sorted, order, "row order");
            }
            let mut orc: Vec<_> = sc.standings.iter().flat_map(|b| b.standings.iter()).filter(|r| r.rating_type.is_orc()).collect();
            orc.sort_by_key(|r| r.place_in_class_orc);
            for pair in orc.windows(2) {
                prop_assert!(pair[0].total_score_class_orc <= pair[1].total_score_class_orc, "ORC class: place order disagrees with totals");
            }
        }
    }

    /// D5 / D9: shuffling the entries never changes an ORC score or place either.
    #[test]
    fn d9_shuffling_orc_series_entries_never_changes_standings(spec in random_series(false, true), seeds in seeds()) {
        let base = snapshot(&build_series(&spec, None));
        for seed in seeds {
            prop_assert_eq!(&base, &snapshot(&build_series(&spec, Some(seed))));
        }
    }

    /// D9: outside an ORC class every ORC field is zero and every row is scored under PHRF.
    #[test]
    fn d9_orc_fields_are_zero_outside_an_orc_class(spec in random_series(false, false)) {
        let standings = build_series(&spec, None);
        for row in standings.rows() {
            prop_assert!(row.rating_type.is_phrf(), "a non-PHRF row in a PHRF class");
            prop_assert!(row.total_score_bracket_orc == 0.0 && row.total_score_class_orc == 0.0 && row.place_in_bracket_orc == 0 && row.place_in_class_orc == 0, "ORC fields set");
            prop_assert!(row.race_standings.iter().all(|r| r.place_in_bracket_orc == 0.0 && r.place_in_class_orc == 0.0 && !r.bracket_sailed_orc && !r.class_sailed_orc && !r.non_starter_orc), "ORC cells set");
        }
    }
}

/// GSI 1.4.1.2 / D3: an RC volunteer entry never changes another boat's scores or totals, nor
/// the order of the other boats among themselves. (The volunteer's own total, and so its place
/// between them, is what the rule is about, so it is left out of the comparison.)
#[test]
fn an_rc_volunteer_never_changes_another_boat() {
    let mut runner = TestRunner::new(Config {
        cases: 200,
        ..Config::default()
    });
    let volunteers = Cell::new(0);
    runner
        .run(&random_series(true, false), |spec| {
            let rc_boats: HashSet<String> = spec
                .races
                .iter()
                .flat_map(|r| {
                    r.boats()
                        .filter(|b| b.outcome == Outcome::Code(DnsRc))
                        .map(|b| b.name.clone())
                })
                .collect();
            if rc_boats.is_empty() {
                return Ok(());
            }
            volunteers.set(volunteers.get() + rc_boats.len());
            let with = build_series(&spec, None);
            // the same series with the RC entries removed, so those boats are plain DNS in those races
            let without = build_series(
                &SeriesSpec {
                    races: spec
                        .races
                        .iter()
                        .map(|r| {
                            r.map(|_, b| (b.outcome != Outcome::Code(DnsRc)).then(|| b.clone()))
                        })
                        .collect(),
                    certs: HashMap::new(),
                },
                None,
            );
            let others: HashSet<String> = snapshot(&with)
                .keys()
                .filter(|k| !rc_boats.contains(*k))
                .cloned()
                .collect();
            for boat in &others {
                prop_assert_eq!(
                    without.bracket_race_scores(boat),
                    with.bracket_race_scores(boat),
                    "{} bracket scores",
                    boat
                );
                prop_assert_eq!(
                    without.class_race_scores(boat),
                    with.class_race_scores(boat),
                    "{} class scores",
                    boat
                );
                let (without_bracket, with_bracket) =
                    (without.bracket_totals(), with.bracket_totals());
                prop_assert_eq!(
                    without_bracket.get(boat),
                    with_bracket.get(boat),
                    "{} bracket total",
                    boat
                );
                let (without_class, with_class) = (without.class_totals(), with.class_totals());
                prop_assert_eq!(
                    without_class.get(boat),
                    with_class.get(boat),
                    "{} class total",
                    boat
                );
            }
            // the other boats keep their order within each bracket and within each class
            let name_of = |r: &regatta_common::StandingsBoatSkipper| {
                r.boat_skipper
                    .boat
                    .as_ref()
                    .map(|b| b.name.clone())
                    .unwrap_or_default()
            };
            // ... unless they are tied on total with the volunteer: the D5 tiebreak skips the RC
            // race for every pair that contains the volunteer, so three tied boats can form a
            // cycle (G beats A on race 1, A beats H on race 1, H beats G on the RC race), and the
            // volunteer's row between them can reverse their mutual order. `java_sort` does exactly that,
            // as the golden standings were recorded; proptest can draw such a fleet. Those ties are left out.
            let rc_bracket_totals: HashMap<String, Vec<f64>> = with
                .standings
                .iter()
                .flat_map(|c| c.standings.iter())
                .map(|b| {
                    (
                        b.bracket.name.clone(),
                        b.standings
                            .iter()
                            .filter(|r| rc_boats.contains(&name_of(r)))
                            .map(|r| r.total_score_bracket)
                            .collect(),
                    )
                })
                .collect();
            let rc_class_totals: HashMap<String, Vec<f64>> = with
                .standings
                .iter()
                .map(|c| {
                    (
                        c.race_class.name.clone(),
                        c.standings
                            .iter()
                            .flat_map(|b| b.standings.iter())
                            .filter(|r| rc_boats.contains(&name_of(r)))
                            .map(|r| r.total_score_class)
                            .collect(),
                    )
                })
                .collect();
            let bracket_orders = |s: &StandingsSeries| -> HashMap<String, Vec<String>> {
                s.standings
                    .iter()
                    .flat_map(|c| {
                        c.standings.iter().map(|b| {
                            let tied = rc_bracket_totals.get(&b.bracket.name);
                            let mut rows: Vec<_> = b
                                .standings
                                .iter()
                                .filter(|r| others.contains(&name_of(r)))
                                .filter(|r| {
                                    !tied.is_some_and(|t| t.contains(&r.total_score_bracket))
                                })
                                .collect();
                            rows.sort_by_key(|r| r.place_in_bracket);
                            (
                                b.bracket.name.clone(),
                                rows.iter().map(|r| name_of(r)).collect::<Vec<_>>(),
                            )
                        })
                    })
                    // a bracket whose only entries were RC volunteers has no other boat to
                    // order, and is absent from the series without them
                    .filter(|(_, names)| !names.is_empty())
                    .collect()
            };
            let class_orders = |s: &StandingsSeries| -> HashMap<String, Vec<String>> {
                s.standings
                    .iter()
                    .map(|c| {
                        let tied = rc_class_totals.get(&c.race_class.name);
                        let mut rows: Vec<_> = c
                            .standings
                            .iter()
                            .flat_map(|b| b.standings.iter())
                            .filter(|r| others.contains(&name_of(r)))
                            .filter(|r| !tied.is_some_and(|t| t.contains(&r.total_score_class)))
                            .collect();
                        rows.sort_by_key(|r| r.place_in_class);
                        (
                            c.race_class.name.clone(),
                            rows.iter().map(|r| name_of(r)).collect::<Vec<_>>(),
                        )
                    })
                    .filter(|(_, names)| !names.is_empty())
                    .collect()
            };
            prop_assert_eq!(
                bracket_orders(&without),
                bracket_orders(&with),
                "bracket order of the other boats"
            );
            prop_assert_eq!(
                class_orders(&without),
                class_orders(&with),
                "class order of the other boats"
            );
            Ok(())
        })
        .unwrap();
    let volunteers = volunteers.get();
    assert!(
        volunteers > 30,
        "the generator produced too few RC volunteers: {volunteers}"
    );
}

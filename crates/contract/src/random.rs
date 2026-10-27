//! Proptest-generated extra steps for `diff --random N`: valid and invalid payloads (results,
//! races with every scoring option and band, series lists, unicode names), deterministic from a
//! seed, all sent with the admin token and compared in full between the two servers.

use crate::step::{post, Step};
use proptest::prelude::*;
use proptest::strategy::ValueTree;
use proptest::test_runner::{Config, RngAlgorithm, TestRng, TestRunner};
use serde_json::{json, Map, Value};

const RATING_TYPES: &[&str] = &[
    "ORC",
    "ORC_PHRF",
    "PHRF",
    "CruisingFlyingSails",
    "CruisingNonFlyingSails",
];
const FINISH_CODES: &[&str] = &["TIME", "HOC", "RET", "DNF", "NSC", "DNS", "DSQ", "DNS_RC"];
const ORC_OPTIONS: &[&str] = &[
    "SingleNumberAllPurpose",
    "SingleNumberWindwardLeeward",
    "TripleNumberAllPurpose",
    "TripleNumberWindwardLeeward",
    "SingleNumberPredominantUpwind",
    "SingleNumberPredominantReaching",
    "SingleNumberPredominantDownwind",
    "PredominantUpwind",
    "PredominantDownwind",
    "PredominantReaching",
    "FiveBandWindwardLeeward",
    "WindwardLeeward60_40",
    "FiveBandAllPurpose",
];
const ORC_3_BANDS: &[&str] = &["Low", "Medium", "High"];
const ORC_5_BANDS: &[&str] = &["Low", "LowMedium", "Medium", "MediumHigh", "High"];
/// Certificates in the golden dump (`orc` table).
const ORC_REFS: &[&str] = &[
    "04560003NFK",
    "04560003L6C",
    "04560004KJP",
    "04560003WR7",
    "04560003WR8",
    "04560003WR9",
    "04560004KI4",
];

/// A field that is omitted, explicitly null, or set.
#[derive(Debug, Clone)]
enum Field {
    Omit,
    Null,
    Set(Value),
}

fn field(value: impl Strategy<Value = Value> + 'static) -> BoxedStrategy<Field> {
    prop_oneof![
        1 => Just(Field::Omit),
        1 => Just(Field::Null),
        6 => value.prop_map(Field::Set),
    ]
    .boxed()
}

fn always(value: impl Strategy<Value = Value> + 'static) -> BoxedStrategy<Field> {
    value.prop_map(Field::Set).boxed()
}

fn object(fields: Vec<(&'static str, Field)>) -> Value {
    let mut map = Map::new();
    for (key, f) in fields {
        match f {
            Field::Omit => {}
            Field::Null => {
                map.insert(key.to_string(), Value::Null);
            }
            Field::Set(v) => {
                map.insert(key.to_string(), v);
            }
        }
    }
    Value::Object(map)
}

fn name(choices: &'static [&'static str]) -> impl Strategy<Value = Value> {
    prop_oneof![
        9 => proptest::sample::select(choices).prop_map(|s| Value::String(s.to_string())),
        1 => Just(Value::String("NotAValue".to_string())),
    ]
}

fn id(known: &'static [i64]) -> impl Strategy<Value = Value> {
    prop_oneof![
        8 => proptest::sample::select(known).prop_map(Value::from),
        1 => Just(Value::from(99999)),
        1 => Just(Value::from(0)),
    ]
}

fn text() -> impl Strategy<Value = Value> {
    prop_oneof![
        3 => "[A-Za-z][A-Za-z0-9 ]{0,20}",
        1 => "[\\p{L}\\p{N} ☃éü]{1,12}",
        1 => Just(String::new()),
        1 => Just("  padded  ".to_string()),
    ]
    .prop_map(Value::String)
}

fn instant_2026() -> impl Strategy<Value = Value> {
    (1u32..=12, 1u32..=28, 0u32..24, 0u32..60, 0u32..60).prop_map(|(mo, d, h, mi, s)| {
        Value::String(format!("2026-{mo:02}-{d:02}T{h:02}:{mi:02}:{s:02}Z"))
    })
}

fn result_body() -> impl Strategy<Value = Value> {
    (
        always(id(&[0])),
        always(id(&[1, 2, 3])),
        always(id(&[1, 6, 11, 12, 16, 47, 50])),
        field(instant_2026()),
        field((-50i32..300).prop_map(Value::from)),
        field(name(ORC_REFS)),
        field((1i32..10).prop_map(Value::from)),
        always(id(&[1, 16, 17, 18])),
        always(id(&[1, 2, 3])),
        field((0i32..10).prop_map(Value::from)),
        field(name(RATING_TYPES)),
        field(name(FINISH_CODES)),
    )
        .prop_map(
            |(id, race, boat, finish, phrf, orc, hoc, bracket, class, penalty, rating, code)| {
                object(vec![
                    ("id", id),
                    ("raceId", race),
                    ("boatId", boat),
                    ("finish", finish),
                    ("phrfRating", phrf),
                    ("orcRef", orc),
                    ("hocPosition", hoc),
                    ("bracketId", bracket),
                    ("raceClassId", class),
                    ("penalty", penalty),
                    ("ratingType", rating),
                    ("finishCode", code),
                ])
            },
        )
}

fn race_body() -> impl Strategy<Value = Value> {
    (
        always(id(&[0])),
        field(text()),
        field(id(&[1, 13, 18])),
        field(id(&[2, 31])),
        field(prop_oneof![
            4 => proptest::sample::select(&[480i32, 510, 550, 600, 650]).prop_map(Value::from),
            1 => (400i32..700).prop_map(Value::from),
        ]),
        field(name(ORC_OPTIONS)),
        field(name(ORC_3_BANDS)),
        field(name(ORC_5_BANDS)),
    )
        .prop_map(|(id, name, series, rc, cf, option, b3, b5)| {
            object(vec![
                ("id", id),
                ("name", name),
                ("seriesId", series),
                ("rcId", rc),
                ("phrfBFactor", cf),
                ("orcScoringOption", option),
                ("orc3Band", b3),
                ("orc5Band", b5),
            ])
        })
}

fn series_list_body() -> impl Strategy<Value = Value> {
    proptest::collection::vec(
        (
            field(text()),
            field((0i32..100).prop_map(Value::from)),
            field(any::<bool>().prop_map(Value::from)),
        )
            .prop_map(|(name, sort, active)| {
                object(vec![
                    ("id", Field::Set(Value::from(0))),
                    ("name", name),
                    ("sort", sort),
                    ("active", active),
                ])
            }),
        0..3,
    )
    .prop_map(Value::Array)
}

fn person_body() -> impl Strategy<Value = Value> {
    (
        field(text()),
        field(text()),
        field(any::<bool>().prop_map(Value::from)),
    )
        .prop_map(|(first, last, member)| {
            object(vec![
                ("id", Field::Set(Value::from(0))),
                ("first", first),
                ("last", last),
                ("clubMember", member),
            ])
        })
}

#[derive(Debug, Clone)]
enum Kind {
    Result(Value),
    Race(Value),
    Series(Value),
    Person(Value),
}

fn kind() -> impl Strategy<Value = Kind> {
    prop_oneof![
        4 => result_body().prop_map(Kind::Result),
        3 => race_body().prop_map(Kind::Race),
        1 => series_list_body().prop_map(Kind::Series),
        1 => person_body().prop_map(Kind::Person),
    ]
}

/// `count` generated steps, the same for the same `seed`.
pub fn random_steps(count: usize, seed: u64) -> Vec<Step> {
    let mut seed_bytes = [0u8; 32];
    seed_bytes[..8].copy_from_slice(&seed.to_le_bytes());
    let mut runner = TestRunner::new_with_rng(
        Config::default(),
        TestRng::from_seed(RngAlgorithm::ChaCha, &seed_bytes),
    );
    let strategy = kind();
    (0..count)
        .map(|i| {
            let value = strategy.new_tree(&mut runner).expect("strategy").current();
            let index = i + 1;
            match value {
                Kind::Result(body) => {
                    post(&format!("random-{index:03}-result"), "results").json(body)
                }
                Kind::Race(body) => post(&format!("random-{index:03}-race"), "race").json(body),
                Kind::Series(body) => {
                    post(&format!("random-{index:03}-series"), "series").json(body)
                }
                Kind::Person(body) => {
                    post(&format!("random-{index:03}-skipper"), "skipper").json(body)
                }
            }
            .admin()
        })
        .collect()
}

/// The JSON of a series list: used by the scenario test to pin the generator's shape.
pub fn example_series_list() -> Value {
    json!([{"id": 0, "name": "x", "sort": 1, "active": true}])
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::step::{AuthMode, Body, Method};

    #[test]
    fn same_seed_same_steps() {
        let a = random_steps(25, 7);
        let b = random_steps(25, 7);
        assert_eq!(a, b);
        assert_eq!(a.len(), 25);
        let c = random_steps(25, 8);
        assert_ne!(a, c);
    }

    #[test]
    fn steps_are_admin_posts_with_json_bodies() {
        for step in random_steps(60, 1) {
            assert_eq!(step.method, Method::Post);
            assert_eq!(step.auth, AuthMode::Admin);
            assert!(matches!(step.body, Body::Json(_)));
            assert!(
                ["results", "race", "series", "skipper"].contains(&step.path.as_str()),
                "{}",
                step.path
            );
            assert!(step.name.starts_with("random-"));
        }
    }

    #[test]
    fn generator_reaches_every_kind() {
        let steps = random_steps(80, 3);
        for path in ["results", "race", "series", "skipper"] {
            assert!(steps.iter().any(|s| s.path == path), "no {path} step in 80");
        }
        assert!(example_series_list().is_array());
    }
}

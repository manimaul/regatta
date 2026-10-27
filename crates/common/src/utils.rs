//! Shared helpers (= `Utils.kt`), plus the Kotlin number-to-string rules the labels depend on.

use jiff::SignedDuration;
use serde::Serialize;

/// Default PHRF B factor (`correctionFactorDefault`).
pub const CORRECTION_FACTOR_DEFAULT: i32 = 600;
/// Default PHRF rating (`ratingDefault`).
pub const RATING_DEFAULT: i32 = 200;

/// `"/v1/api/<path>"` with the query string built from `params` in order, no encoding
/// (`String.versionedApi(params)`); an empty slice is Kotlin's `null`.
pub fn versioned_api(path: &str, params: &[(&str, &str)]) -> String {
    versioned_api_v(path, 1, params)
}

/// `String.versionedApi(version, params)`.
pub fn versioned_api_v(path: &str, version: i32, params: &[(&str, &str)]) -> String {
    let mut out = format!("/v{version}/api/{path}");
    for (index, (key, value)) in params.iter().enumerate() {
        out.push(if index == 0 { '?' } else { '&' });
        out.push_str(key);
        out.push('=');
        out.push_str(value);
    }
    out
}

/// `Double.roundToLong()` on the JVM = `Math.round`: nearest, ties toward positive infinity,
/// NaN → 0, saturating.
pub fn round_to_long(x: f64) -> i64 {
    if x.is_nan() {
        return 0;
    }
    let r = x.round();
    let r = if x < 0.0 && (x - r).abs() == 0.5 {
        r + 1.0
    } else {
        r
    };
    r as i64
}

/// `Double.toString()` of the JVM: plain decimal for `1e-3 <= |x| < 1e7` (and 0) with at least
/// one fraction digit, otherwise computerized scientific notation (`1.0E7`, `1.0E-5`).
pub fn kotlin_f64_to_string(x: f64) -> String {
    if x.is_nan() {
        return "NaN".into();
    }
    if x.is_infinite() {
        return if x > 0.0 {
            "Infinity".into()
        } else {
            "-Infinity".into()
        };
    }
    let abs = x.abs();
    if x == 0.0 || (1e-3..1e7).contains(&abs) {
        with_fraction_digit(format!("{x}"))
    } else {
        scientific(format!("{x:e}"))
    }
}

/// `Float.toString()` of the JVM, same rules as [`kotlin_f64_to_string`] on the `f32` shortest
/// representation (`101.0`, `160.5`).
pub fn kotlin_f32_to_string(x: f32) -> String {
    if x.is_nan() {
        return "NaN".into();
    }
    if x.is_infinite() {
        return if x > 0.0 {
            "Infinity".into()
        } else {
            "-Infinity".into()
        };
    }
    let abs = x.abs();
    if x == 0.0 || (1e-3..1e7).contains(&abs) {
        with_fraction_digit(format!("{x}"))
    } else {
        scientific(format!("{x:e}"))
    }
}

fn with_fraction_digit(mut s: String) -> String {
    if !s.contains('.') {
        s.push_str(".0");
    }
    s
}

fn scientific(s: String) -> String {
    let (mantissa, exponent) = s.split_once('e').unwrap_or((&s, "0"));
    format!("{}E{}", with_fraction_digit(mantissa.to_string()), exponent)
}

/// A series score as a label: whole numbers without a decimal ("4"), others with one or two ("3.5", "3.33").
pub fn score_text(score: f64) -> String {
    let rounded = round_to_long(score * 100.0) as f64 / 100.0;
    let whole = round_to_long(rounded);
    if rounded == whole as f64 {
        return whole.to_string();
    }
    kotlin_f64_to_string(rounded)
        .trim_end_matches('0')
        .trim_end_matches('.')
        .to_string()
}

/// "1h:2m:3s", rounded to the nearest second (D7). Places still use millisecond precision.
pub fn display(duration: SignedDuration) -> String {
    let whole_millis = duration.as_millis() as i64;
    let total_seconds = round_to_long(whole_millis as f64 / 1000.0);
    let hrs = total_seconds / 3600;
    let min = total_seconds % 3600 / 60;
    let sec = total_seconds % 60;
    format!("{hrs}h:{min}m:{sec}s")
}

/// `List.moveItem(up, predicate)`: a copy of the list with the first matching item swapped one
/// place up or down; unchanged when there is no match or it is already at the end.
pub fn move_item<T: Clone>(list: &[T], up: bool, predicate: impl Fn(&T) -> bool) -> Vec<T> {
    let mut lst = list.to_vec();
    let Some(i) = list.iter().position(predicate) else {
        return lst;
    };
    if up {
        if i > 0 {
            lst.swap(i - 1, i);
        }
    } else if i + 1 < list.len() {
        lst.swap(i, i + 1);
    }
    lst
}

/// The JSON of the golden files and of the Kotlin server's pretty printer: 4-space indent,
/// `": "` after keys, `[]` for empty arrays, no trailing newline.
pub fn to_kotlin_pretty_json<T: Serialize + ?Sized>(value: &T) -> serde_json::Result<String> {
    let mut buf = Vec::new();
    let formatter = serde_json::ser::PrettyFormatter::with_indent(b"    ");
    let mut ser = serde_json::Serializer::with_formatter(&mut buf, formatter);
    value.serialize(&mut ser)?;
    Ok(String::from_utf8(buf).expect("serde_json writes UTF-8"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn versioned_api_builds_query_in_order() {
        assert_eq!(versioned_api("/race", &[]), "/v1/api//race");
        assert_eq!(
            versioned_api("series/standings", &[]),
            "/v1/api/series/standings"
        );
        assert_eq!(
            versioned_api("series/standings", &[("id", "1"), ("year", "2024")]),
            "/v1/api/series/standings?id=1&year=2024"
        );
        assert_eq!(
            versioned_api_v("/race", 2, &[("id", "7")]),
            "/v2/api//race?id=7"
        );
    }

    #[test]
    fn round_to_long_is_java_math_round() {
        assert_eq!(round_to_long(2.5), 3);
        assert_eq!(round_to_long(-2.5), -2);
        assert_eq!(round_to_long(-3.5), -3);
        assert_eq!(round_to_long(2.4), 2);
        assert_eq!(round_to_long(-2.6), -3);
        assert_eq!(round_to_long(f64::NAN), 0);
    }

    #[test]
    fn kotlin_number_strings() {
        assert_eq!(kotlin_f32_to_string(101.0), "101.0");
        assert_eq!(kotlin_f32_to_string(160.5), "160.5");
        assert_eq!(kotlin_f32_to_string(-1000.0), "-1000.0");
        assert_eq!(kotlin_f32_to_string(0.0), "0.0");
        assert_eq!(kotlin_f32_to_string(1e7), "1.0E7");
        assert_eq!(kotlin_f32_to_string(1.5e-4), "1.5E-4");
        assert_eq!(kotlin_f64_to_string(3.33), "3.33");
        assert_eq!(kotlin_f64_to_string(4.0), "4.0");
        assert_eq!(
            kotlin_f64_to_string(1.0030864197530864),
            "1.0030864197530864"
        );
        assert_eq!(kotlin_f64_to_string(12345678.5), "1.23456785E7");
        assert_eq!(kotlin_f64_to_string(0.0001), "1.0E-4");
    }

    #[test]
    fn move_item_swaps_neighbours() {
        let v = vec![1, 2, 3];
        assert_eq!(move_item(&v, false, |x| *x == 1), vec![2, 1, 3]);
        assert_eq!(move_item(&v, true, |x| *x == 1), vec![1, 2, 3]);
        assert_eq!(move_item(&v, true, |x| *x == 3), vec![1, 3, 2]);
        assert_eq!(move_item(&v, false, |x| *x == 3), vec![1, 2, 3]);
        assert_eq!(move_item(&v, false, |x| *x == 9), vec![1, 2, 3]);
    }

    #[test]
    fn pretty_json_matches_kotlin_shape() {
        #[derive(Serialize)]
        struct S {
            a: Vec<i32>,
            b: Option<i32>,
            c: f32,
        }
        assert_eq!(
            to_kotlin_pretty_json(&S {
                a: vec![],
                b: None,
                c: 10.0
            })
            .unwrap(),
            "{\n    \"a\": [],\n    \"b\": null,\n    \"c\": 10.0\n}"
        );
    }
}

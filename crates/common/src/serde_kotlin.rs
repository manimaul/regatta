//! The JSON string formats of instants and durations that the golden files and contract cassettes pin.
//!
//! The race reports, series standings and REST bodies carry [`Timestamp`] and [`SignedDuration`] values in these
//! exact formats, so changing either one changes the approved files in `crates/scoring/tests/golden/approved/` and
//! the cassettes in `crates/contract/cassettes/`. The models in [`crate::data`] use them through serde attributes:
//!
//! - [`kotlin_instant`] (with [`format_instant`] / [`parse_instant`]): UTC with `Z`, fractional seconds only when
//!   non-zero and then as 3, 6 or 9 digits (`.233`, `.233100`, `.000000001`; never `.233000`).
//! - [`kotlin_duration`] (with [`format_duration`] / [`parse_duration`]): ISO-8601 durations in hours, minutes and
//!   seconds: `PT4H5M29S`, `PT1H0M10.173160173S`, `PT0S`, negative as `-PT…`.
//! - [`non_null`]: an optional field that may be left out but must not be an explicit JSON `null`.
//! - [`KotlinInstant`]: a [`Timestamp`] newtype in the instant format, for values rather than fields.
//!
//! [`DISTANT_PAST`] / [`DISTANT_FUTURE`] are the open ends of a date range (a series without a start or end date).
//! They are `Timestamp::MIN` / `Timestamp::MAX` and are written and read as the fixed strings
//! `-100001-12-31T23:59:59.999999999Z` / `+100000-01-01T00:00:00Z`, which lie outside the range [`Timestamp`] can
//! hold.

use jiff::tz::TimeZone;
use jiff::{SignedDuration, Timestamp};
use serde::{Deserialize, Deserializer, Serialize, Serializer};

/// The open start of a date range, earlier than any real instant; serialized as `-100001-12-31T23:59:59.999999999Z`.
pub const DISTANT_PAST: Timestamp = Timestamp::MIN;
/// The open end of a date range, later than any real instant; serialized as `+100000-01-01T00:00:00Z`.
pub const DISTANT_FUTURE: Timestamp = Timestamp::MAX;

const DISTANT_PAST_STR: &str = "-100001-12-31T23:59:59.999999999Z";
const DISTANT_FUTURE_STR: &str = "+100000-01-01T00:00:00Z";

/// Formats an instant as the UTC string the golden files pin.
///
/// `YYYY-MM-DDTHH:MM:SS` then `Z`, with the fraction only when it is non-zero, as 3, 6 or 9 digits (the fewest
/// that hold it exactly): `2025-04-08T19:32:22.815Z`, `2026-07-09T10:45:56.233001Z`. Years before 0 get a `-` and
/// years after 9999 a `+`. [`DISTANT_PAST`] and [`DISTANT_FUTURE`] give their fixed sentinel strings.
pub fn format_instant(ts: Timestamp) -> String {
    if ts == DISTANT_PAST {
        return DISTANT_PAST_STR.to_string();
    }
    if ts == DISTANT_FUTURE {
        return DISTANT_FUTURE_STR.to_string();
    }
    let dt = TimeZone::UTC.to_datetime(ts);
    let year = i32::from(dt.year());
    let mut out = String::with_capacity(30);
    if year < 0 {
        out.push('-');
        out.push_str(&format!("{:04}", -year));
    } else if year > 9999 {
        out.push('+');
        out.push_str(&year.to_string());
    } else {
        out.push_str(&format!("{year:04}"));
    }
    out.push_str(&format!(
        "-{:02}-{:02}T{:02}:{:02}:{:02}",
        dt.month(),
        dt.day(),
        dt.hour(),
        dt.minute(),
        dt.second()
    ));
    let nanos = ts.subsec_nanosecond();
    if nanos != 0 {
        if nanos % 1_000_000 == 0 {
            out.push_str(&format!(".{:03}", nanos / 1_000_000));
        } else if nanos % 1_000 == 0 {
            out.push_str(&format!(".{:06}", nanos / 1_000));
        } else {
            out.push_str(&format!(".{nanos:09}"));
        }
    }
    out.push('Z');
    out
}

/// Parses an ISO-8601 instant, the inverse of [`format_instant`].
///
/// Any offset is accepted and normalized to UTC (`2024-07-03T02:46:40-07:00`). The two sentinel strings map to
/// [`DISTANT_PAST`] / [`DISTANT_FUTURE`].
///
/// # Errors
///
/// Returns `Err` if `s` is not an ISO-8601 instant with an offset or `Z`, or is outside [`Timestamp`]'s range.
pub fn parse_instant(s: &str) -> Result<Timestamp, jiff::Error> {
    match s {
        DISTANT_PAST_STR => Ok(DISTANT_PAST),
        DISTANT_FUTURE_STR => Ok(DISTANT_FUTURE),
        _ => s.parse(),
    }
}

/// Formats a duration as the ISO-8601 string the golden files pin (`PT1H0M10.173160173S`).
///
/// `-` for a negative duration, then `PT`. Hours are written when non-zero and are not folded into days (`PT25H`);
/// minutes when non-zero or when both hours and seconds are present (`PT1H0M5S`); seconds when non-zero, including
/// a fraction, or when nothing else was written (`PT0S`). The fraction is the 9-digit nanoseconds with trailing
/// zeros trimmed to a multiple of 3 digits (`PT45M12.500S`).
pub fn format_duration(d: SignedDuration) -> String {
    let mut out = String::with_capacity(24);
    if d.is_negative() {
        out.push('-');
    }
    out.push_str("PT");
    let abs = d.unsigned_abs();
    let secs = abs.as_secs();
    let nanos = abs.subsec_nanos();
    let hours = secs / 3600;
    let minutes = secs % 3600 / 60;
    let seconds = secs % 60;
    let has_hours = hours != 0;
    let has_seconds = seconds != 0 || nanos != 0;
    let has_minutes = minutes != 0 || (has_seconds && has_hours);
    if has_hours {
        out.push_str(&hours.to_string());
        out.push('H');
    }
    if has_minutes {
        out.push_str(&minutes.to_string());
        out.push('M');
    }
    if has_seconds || (!has_hours && !has_minutes) {
        out.push_str(&seconds.to_string());
        if nanos != 0 {
            let frac = format!("{nanos:09}");
            let non_zero_digits = frac.trim_end_matches('0').len();
            let keep = non_zero_digits.div_ceil(3) * 3;
            out.push('.');
            out.push_str(&frac[..keep]);
        }
        out.push('S');
    }
    out
}

/// Parses an ISO-8601 duration of the form `[+-]P[nD][T[nH][nM][n[.f]S]]`, the inverse of [`format_duration`].
///
/// A day is 24 hours. Only seconds may have a fraction; digits past the ninth round the nanoseconds half up.
///
/// # Errors
///
/// Returns `Err` with a message naming the input if `s` does not start with `P` (after an optional sign), has no
/// component, has components out of order or repeated, a unit other than `D` before `T` or `H`/`M`/`S` after it, a
/// fraction on anything but seconds, an empty `T` part, or a value that overflows `i64` seconds.
pub fn parse_duration(s: &str) -> Result<SignedDuration, String> {
    let err = |msg: &str| format!("invalid ISO duration {s:?}: {msg}");
    let mut rest = s;
    let negative = if let Some(r) = rest.strip_prefix('-') {
        rest = r;
        true
    } else if let Some(r) = rest.strip_prefix('+') {
        rest = r;
        false
    } else {
        false
    };
    rest = rest.strip_prefix('P').ok_or_else(|| err("expected 'P'"))?;
    if rest.is_empty() {
        return Err(err("no components"));
    }
    let mut secs: i64 = 0;
    let mut nanos: i64 = 0;
    let mut in_time = false;
    let mut last_unit = 0u8;
    while !rest.is_empty() {
        if let Some(r) = rest.strip_prefix('T') {
            if in_time {
                return Err(err("duplicate 'T'"));
            }
            in_time = true;
            rest = r;
            if rest.is_empty() {
                return Err(err("no time components after 'T'"));
            }
            continue;
        }
        let num_len = rest
            .find(|c: char| !(c.is_ascii_digit() || c == '.'))
            .ok_or_else(|| err("missing unit"))?;
        let (number, after) = rest.split_at(num_len);
        let unit = after.chars().next().ok_or_else(|| err("missing unit"))?;
        rest = &after[unit.len_utf8()..];
        if number.is_empty() {
            return Err(err("missing number"));
        }
        let (whole, frac) = match number.split_once('.') {
            Some((w, f)) => (w, Some(f)),
            None => (number, None),
        };
        let whole: i64 = whole.parse().map_err(|_| err("bad number"))?;
        let rank = match (unit, in_time) {
            ('D', false) => 1,
            ('H', true) => 2,
            ('M', true) => 3,
            ('S', true) => 4,
            _ => return Err(err("unexpected unit")),
        };
        if rank <= last_unit {
            return Err(err("components out of order"));
        }
        last_unit = rank;
        let per_unit: i64 = match unit {
            'D' => 86_400,
            'H' => 3600,
            'M' => 60,
            _ => 1,
        };
        secs = secs
            .checked_add(whole.checked_mul(per_unit).ok_or_else(|| err("overflow"))?)
            .ok_or_else(|| err("overflow"))?;
        if let Some(frac) = frac {
            if unit != 'S' || frac.is_empty() || !frac.bytes().all(|b| b.is_ascii_digit()) {
                return Err(err("fraction only allowed on seconds"));
            }
            // Nanosecond precision: pad to 9 digits, round half up on the tenth.
            let mut digits: Vec<u8> = frac.bytes().map(|b| b - b'0').collect();
            let round_up = digits.len() > 9 && digits[9] >= 5;
            digits.truncate(9);
            while digits.len() < 9 {
                digits.push(0);
            }
            let mut n = digits.iter().fold(0i64, |acc, &d| acc * 10 + i64::from(d));
            if round_up {
                n += 1;
            }
            nanos += n;
        }
    }
    if in_time && last_unit < 2 {
        return Err(err("no time components after 'T'"));
    }
    secs = secs
        .checked_add(nanos / 1_000_000_000)
        .ok_or_else(|| err("overflow"))?;
    nanos %= 1_000_000_000;
    let d = SignedDuration::new(secs, nanos as i32);
    Ok(if negative { -d } else { d })
}

/// Serde adapter that writes a [`Timestamp`] field in the [`format_instant`] format.
///
/// Use `#[serde(with = "kotlin_instant")]` on a `Timestamp` field and `#[serde(with = "kotlin_instant::option",
/// default)]` on an `Option<Timestamp>` one.
pub mod kotlin_instant {
    use super::*;

    /// Writes the timestamp as a [`format_instant`] string.
    ///
    /// # Errors
    ///
    /// Returns the serializer's error if it fails to write the string.
    pub fn serialize<S: Serializer>(ts: &Timestamp, s: S) -> Result<S::Ok, S::Error> {
        s.serialize_str(&format_instant(*ts))
    }

    /// Reads a timestamp from a string through [`parse_instant`].
    ///
    /// # Errors
    ///
    /// Returns `Err` if the value is not a string or [`parse_instant`] rejects it.
    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Timestamp, D::Error> {
        let s = String::deserialize(d)?;
        parse_instant(&s).map_err(serde::de::Error::custom)
    }

    /// The same format for an `Option<Timestamp>`, with `None` as JSON `null`.
    pub mod option {
        use super::*;

        /// Writes `Some` as a [`format_instant`] string and `None` as JSON `null`.
        ///
        /// # Errors
        ///
        /// Returns the serializer's error if it fails to write the value.
        pub fn serialize<S: Serializer>(ts: &Option<Timestamp>, s: S) -> Result<S::Ok, S::Error> {
            match ts {
                Some(ts) => s.serialize_some(&format_instant(*ts)),
                None => s.serialize_none(),
            }
        }

        /// Reads JSON `null` as `None` and a string through [`parse_instant`].
        ///
        /// # Errors
        ///
        /// Returns `Err` if the value is neither `null` nor a string, or [`parse_instant`] rejects it.
        pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Option<Timestamp>, D::Error> {
            match Option::<String>::deserialize(d)? {
                Some(s) => parse_instant(&s)
                    .map(Some)
                    .map_err(serde::de::Error::custom),
                None => Ok(None),
            }
        }
    }
}

/// Serde adapter that writes a [`SignedDuration`] field in the [`format_duration`] format.
///
/// Use `#[serde(with = "kotlin_duration")]` on a `SignedDuration` field and `#[serde(with =
/// "kotlin_duration::option", default)]` on an `Option<SignedDuration>` one.
pub mod kotlin_duration {
    use super::*;

    /// Writes the duration as a [`format_duration`] string.
    ///
    /// # Errors
    ///
    /// Returns the serializer's error if it fails to write the string.
    pub fn serialize<S: Serializer>(d: &SignedDuration, s: S) -> Result<S::Ok, S::Error> {
        s.serialize_str(&format_duration(*d))
    }

    /// Reads a duration from a string through [`parse_duration`].
    ///
    /// # Errors
    ///
    /// Returns `Err` if the value is not a string or [`parse_duration`] rejects it.
    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<SignedDuration, D::Error> {
        let s = String::deserialize(d)?;
        parse_duration(&s).map_err(serde::de::Error::custom)
    }

    /// The same format for an `Option<SignedDuration>`, with `None` as JSON `null`.
    pub mod option {
        use super::*;

        /// Writes `Some` as a [`format_duration`] string and `None` as JSON `null`.
        ///
        /// # Errors
        ///
        /// Returns the serializer's error if it fails to write the value.
        pub fn serialize<S: Serializer>(
            d: &Option<SignedDuration>,
            s: S,
        ) -> Result<S::Ok, S::Error> {
            match d {
                Some(d) => s.serialize_some(&format_duration(*d)),
                None => s.serialize_none(),
            }
        }

        /// Reads JSON `null` as `None` and a string through [`parse_duration`].
        ///
        /// # Errors
        ///
        /// Returns `Err` if the value is neither `null` nor a string, or [`parse_duration`] rejects it.
        pub fn deserialize<'de, D: Deserializer<'de>>(
            d: D,
        ) -> Result<Option<SignedDuration>, D::Error> {
            match Option::<String>::deserialize(d)? {
                Some(s) => parse_duration(&s)
                    .map(Some)
                    .map_err(serde::de::Error::custom),
                None => Ok(None),
            }
        }
    }
}

/// Serde adapter for an optional input field whose `None` means "compute the default", not "no value".
///
/// Use `#[serde(default, deserialize_with = "non_null::deserialize")]` on an `Option<T>`. A missing key leaves the
/// field `None`, and the model fills in its computed default; an explicit JSON `null` is rejected, because the API
/// treats the field as required to hold a value whenever it is present.
pub mod non_null {
    use super::*;

    /// Reads a present value as `Some`.
    ///
    /// # Errors
    ///
    /// Returns `Err` if the value is JSON `null` or does not deserialize as `T`.
    pub fn deserialize<'de, D: Deserializer<'de>, T: Deserialize<'de>>(
        d: D,
    ) -> Result<Option<T>, D::Error> {
        T::deserialize(d).map(Some)
    }
}

/// A [`Timestamp`] that serializes in the [`format_instant`] format, for values rather than struct fields.
///
/// Use it where a `Serialize`/`Deserialize` value is needed and a `#[serde(with = "kotlin_instant")]` attribute
/// can't be attached, such as a collection element.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct KotlinInstant(pub Timestamp);

impl Serialize for KotlinInstant {
    fn serialize<S: Serializer>(&self, s: S) -> Result<S::Ok, S::Error> {
        kotlin_instant::serialize(&self.0, s)
    }
}

impl<'de> Deserialize<'de> for KotlinInstant {
    fn deserialize<D: Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
        kotlin_instant::deserialize(d).map(KotlinInstant)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ts(s: &str) -> Timestamp {
        s.parse().unwrap()
    }

    /// Vectors from the approved golden files (whole seconds, milliseconds) plus the micro- and
    /// nanosecond groups.
    #[test]
    fn instant_vectors() {
        let cases = [
            "2024-01-21T02:00:00Z",
            "2025-04-08T19:32:22.815Z",
            "2026-04-07T16:51:21.001Z",
            "2026-07-09T10:45:56.233Z",
            "2025-08-18T12:19:45.802Z",
            "2026-07-09T10:45:56.233001Z",
            "2026-07-09T10:45:56.000001Z",
            "2026-07-09T10:45:56.233000001Z",
            "2026-07-09T10:45:56.100Z",
            "1970-01-01T00:00:00Z",
            "0001-01-01T00:00:00Z",
        ];
        for c in cases {
            assert_eq!(format_instant(ts(c)), c);
            assert_eq!(parse_instant(c).unwrap(), ts(c));
        }
    }

    #[test]
    fn instant_fraction_groups_of_three() {
        let base = Timestamp::constant(1_720_000_000, 0);
        assert_eq!(
            format_instant(Timestamp::constant(1_720_000_000, 233_000_000)).len(),
            base.to_string().len() + 4
        );
        assert_eq!(
            format_instant(Timestamp::constant(1_720_000_000, 200_000_000)),
            "2024-07-03T09:46:40.200Z"
        );
        assert_eq!(
            format_instant(Timestamp::constant(1_720_000_000, 233_100_000)),
            "2024-07-03T09:46:40.233100Z"
        );
        assert_eq!(
            format_instant(Timestamp::constant(1_720_000_000, 1)),
            "2024-07-03T09:46:40.000000001Z"
        );
    }

    #[test]
    fn instant_offset_input_is_normalised_to_utc() {
        assert_eq!(
            format_instant(parse_instant("2024-07-03T02:46:40-07:00").unwrap()),
            "2024-07-03T09:46:40Z"
        );
    }

    #[test]
    fn distant_past_and_future_sentinels() {
        assert_eq!(
            format_instant(DISTANT_PAST),
            "-100001-12-31T23:59:59.999999999Z"
        );
        assert_eq!(format_instant(DISTANT_FUTURE), "+100000-01-01T00:00:00Z");
        assert_eq!(
            parse_instant("-100001-12-31T23:59:59.999999999Z").unwrap(),
            DISTANT_PAST
        );
        assert_eq!(
            parse_instant("+100000-01-01T00:00:00Z").unwrap(),
            DISTANT_FUTURE
        );
        assert!(DISTANT_PAST < ts("1970-01-01T00:00:00Z"));
        assert!(DISTANT_FUTURE > ts("2999-01-01T00:00:00Z"));
        let json = serde_json::to_string(&KotlinInstant(DISTANT_FUTURE)).unwrap();
        assert_eq!(json, "\"+100000-01-01T00:00:00Z\"");
        let back: KotlinInstant = serde_json::from_str(&json).unwrap();
        assert_eq!(back.0, DISTANT_FUTURE);
    }

    #[test]
    fn duration_vectors_from_golden_files() {
        let cases = [
            ("PT4H5M29S", SignedDuration::new(4 * 3600 + 5 * 60 + 29, 0)),
            (
                "PT1H0M10.173160173S",
                SignedDuration::new(3610, 173_160_173),
            ),
            ("PT1H0M2.083333333S", SignedDuration::new(3602, 83_333_333)),
            (
                "PT1H0M30.385852090S",
                SignedDuration::new(3630, 385_852_090),
            ),
            ("PT1H0M16S", SignedDuration::new(3616, 0)),
            ("PT1H10M", SignedDuration::new(4200, 0)),
            ("PT1H15M", SignedDuration::new(4500, 0)),
            ("PT1M", SignedDuration::new(60, 0)),
            ("PT3M", SignedDuration::new(180, 0)),
            (
                "PT45M12.500S",
                SignedDuration::new(45 * 60 + 12, 500_000_000),
            ),
        ];
        for (text, d) in cases {
            assert_eq!(format_duration(d), text);
            assert_eq!(parse_duration(text).unwrap(), d);
        }
    }

    #[test]
    fn duration_edge_cases() {
        assert_eq!(format_duration(SignedDuration::ZERO), "PT0S");
        assert_eq!(format_duration(SignedDuration::new(45, 0)), "PT45S");
        assert_eq!(format_duration(SignedDuration::new(300, 0)), "PT5M");
        assert_eq!(format_duration(SignedDuration::new(3600, 0)), "PT1H");
        assert_eq!(format_duration(SignedDuration::new(3605, 0)), "PT1H0M5S");
        assert_eq!(format_duration(SignedDuration::new(3900, 0)), "PT1H5M");
        assert_eq!(format_duration(SignedDuration::new(125, 0)), "PT2M5S");
        assert_eq!(
            format_duration(SignedDuration::new(0, 500_000_000)),
            "PT0.500S"
        );
        assert_eq!(
            format_duration(SignedDuration::new(0, 1_000)),
            "PT0.000001S"
        );
        assert_eq!(format_duration(SignedDuration::new(0, 1)), "PT0.000000001S");
        assert_eq!(
            format_duration(SignedDuration::new(12, 120_000_000)),
            "PT12.120S"
        );
        assert_eq!(format_duration(SignedDuration::new(-3605, 0)), "-PT1H0M5S");
        assert_eq!(
            format_duration(SignedDuration::new(-1, -500_000_000)),
            "-PT1.500S"
        );
        assert_eq!(format_duration(SignedDuration::new(90_000, 0)), "PT25H");
        assert_eq!(
            format_duration(SignedDuration::new(360_000 * 24, 0)),
            "PT2400H"
        );
    }

    #[test]
    fn duration_parse() {
        assert_eq!(parse_duration("PT0S").unwrap(), SignedDuration::ZERO);
        assert_eq!(
            parse_duration("-PT1H0M5S").unwrap(),
            SignedDuration::new(-3605, 0)
        );
        assert_eq!(
            parse_duration("PT1H").unwrap(),
            SignedDuration::new(3600, 0)
        );
        assert_eq!(
            parse_duration("P1DT2H").unwrap(),
            SignedDuration::new(26 * 3600, 0)
        );
        assert_eq!(
            parse_duration("P1D").unwrap(),
            SignedDuration::new(86_400, 0)
        );
        assert_eq!(
            parse_duration("PT1.5S").unwrap(),
            SignedDuration::new(1, 500_000_000)
        );
        assert_eq!(
            parse_duration("+PT2M").unwrap(),
            SignedDuration::new(120, 0)
        );
        for bad in [
            "", "P", "PT", "T1S", "PT1X", "PT1M1H", "PT1.5M", "1S", "PT1S1S",
        ] {
            assert!(parse_duration(bad).is_err(), "{bad:?} should not parse");
        }
    }

    #[test]
    fn duration_serde_round_trip() {
        #[derive(Serialize, Deserialize, PartialEq, Debug)]
        struct W {
            #[serde(with = "kotlin_duration")]
            d: SignedDuration,
            #[serde(with = "kotlin_duration::option", default)]
            o: Option<SignedDuration>,
        }
        let w = W {
            d: SignedDuration::new(3610, 173_160_173),
            o: None,
        };
        let json = serde_json::to_string(&w).unwrap();
        assert_eq!(json, r#"{"d":"PT1H0M10.173160173S","o":null}"#);
        assert_eq!(serde_json::from_str::<W>(&json).unwrap(), w);
        assert_eq!(
            serde_json::from_str::<W>(r#"{"d":"PT5M"}"#).unwrap().o,
            None
        );
    }
}

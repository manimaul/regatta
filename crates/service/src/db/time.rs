//! Conversions between instants and the civil times of the `timestamp without time zone` columns.
//!
//! Production reads and writes those columns as UTC (D7 in `docs/explore_implemented/rust_port.md`). The golden
//! test reads them in Pacific time, the zone the approved golden files were recorded in, so the zone is a
//! parameter ([`Db::naive_time_zone`](super::Db::naive_time_zone)), UTC unless a test says otherwise. Values travel
//! as text: `col::text` on the way out (`2024-04-11 02:00:00[.ffffff]`) and `$n::text::timestamp` on the way in.
//! Season years follow the Pacific calendar (D8 in `docs/explore_implemented/test_audit_and_plan.md`).

use anyhow::{Context, Result};
use jiff::civil::{date, DateTime};
use jiff::tz::TimeZone;
use jiff::Timestamp;

/// The zone of the season calendar.
pub const PACIFIC: &str = "America/Los_Angeles";

fn pacific() -> Result<TimeZone> {
    TimeZone::get(PACIFIC).context("tz database")
}

/// Parses a Postgres `timestamp::text` value as an instant, reading the civil time in `tz`.
///
/// # Errors
///
/// Returns `Err` if the text is not a civil date-time or the time is out of range in `tz`.
pub fn from_pg(text: &str, tz: &TimeZone) -> Result<Timestamp> {
    let civil: DateTime = text
        .trim()
        .parse()
        .with_context(|| format!("timestamp {text:?}"))?;
    Ok(civil.to_zoned(tz.clone())?.timestamp())
}

/// Formats an instant as the civil time in `tz`, which Postgres reads into a `timestamp` column.
pub fn to_pg(ts: Timestamp, tz: &TimeZone) -> String {
    ts.to_zoned(tz.clone()).datetime().to_string()
}

/// Returns the season year's bounds: midnight Pacific on January 1st of `year` and of `year + 1`.
///
/// D8 (`test_audit_and_plan.md`): seasons follow the Pacific calendar, not UTC.
///
/// # Errors
///
/// Returns `Err` if the time zone database has no Pacific zone or the year is out of range.
pub fn year_bounds(year: i32) -> Result<(Timestamp, Timestamp)> {
    let tz = pacific()?;
    let start = date(year as i16, 1, 1).to_zoned(tz.clone())?.timestamp();
    let end = date((year + 1) as i16, 1, 1).to_zoned(tz)?.timestamp();
    Ok((start, end))
}

/// Returns the Pacific calendar year of an instant.
///
/// # Errors
///
/// Returns `Err` if the time zone database has no Pacific zone.
pub fn pacific_year(ts: Timestamp) -> Result<i32> {
    Ok(ts.to_zoned(pacific()?).year() as i32)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trip_pg_text() {
        let utc = TimeZone::UTC;
        let ts = from_pg("2024-04-11 02:00:00", &utc).unwrap();
        assert_eq!(ts.to_string(), "2024-04-11T02:00:00Z");
        assert_eq!(to_pg(ts, &utc), "2024-04-11T02:00:00");
        let frac = from_pg("2026-07-09 10:45:56.233", &utc).unwrap();
        assert_eq!(frac.to_string(), "2026-07-09T10:45:56.233Z");
        assert_eq!(to_pg(frac, &utc), "2026-07-09T10:45:56.233");
        assert!(from_pg("yesterday", &utc).is_err());
        // the golden test reads the dump in Pacific time: race-1.json's 02:00Z start
        let pacific = pacific().unwrap();
        let local = from_pg("2024-01-20 18:00:00", &pacific).unwrap();
        assert_eq!(local.to_string(), "2024-01-21T02:00:00Z");
        assert_eq!(to_pg(local, &pacific), "2024-01-20T18:00:00");
    }

    #[test]
    fn pacific_year_bounds() {
        let (start, end) = year_bounds(2024).unwrap();
        assert_eq!(start.to_string(), "2024-01-01T08:00:00Z");
        assert_eq!(end.to_string(), "2025-01-01T08:00:00Z");
        // 2024-12-31 20:00 Pacific is 2025-01-01 04:00 UTC: still 2024 by the season calendar
        let late: Timestamp = "2025-01-01T04:00:00Z".parse().unwrap();
        assert_eq!(pacific_year(late).unwrap(), 2024);
    }
}

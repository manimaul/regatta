//! `timestamp without time zone` columns hold civil times that the Kotlin server read in its
//! JVM's default zone: UTC in production (`main` forces it, D7), but the host's zone in the Gradle
//! golden test, which is how the approved golden files came to carry Pacific-interpreted
//! instants. The zone is therefore a parameter ([`Db::naive_time_zone`](super::Db)), UTC unless
//! a test says otherwise. Values travel as text: `col::text` on the way out
//! (`2024-04-11 02:00:00[.ffffff]`) and `$n::text::timestamp` on the way in. Season years
//! follow the Pacific calendar (D8), like `RegattaDatabase.yearBounds` and `RaceTimeTable.allYears`.

use anyhow::{Context, Result};
use jiff::civil::{date, DateTime};
use jiff::tz::TimeZone;
use jiff::Timestamp;

pub const PACIFIC: &str = "America/Los_Angeles";

fn pacific() -> Result<TimeZone> {
    TimeZone::get(PACIFIC).context("tz database")
}

/// A Postgres `timestamp::text` value as an instant, the civil time read in `tz`.
pub fn from_pg(text: &str, tz: &TimeZone) -> Result<Timestamp> {
    let civil: DateTime = text
        .trim()
        .parse()
        .with_context(|| format!("timestamp {text:?}"))?;
    Ok(civil.to_zoned(tz.clone())?.timestamp())
}

/// An instant as the civil text in `tz` that Postgres reads into a `timestamp` column.
pub fn to_pg(ts: Timestamp, tz: &TimeZone) -> String {
    ts.to_zoned(tz.clone()).datetime().to_string()
}

/// D8: the season year's bounds, midnight Pacific on January 1st of `year` and of `year + 1`.
pub fn year_bounds(year: i32) -> Result<(Timestamp, Timestamp)> {
    let tz = pacific()?;
    let start = date(year as i16, 1, 1).to_zoned(tz.clone())?.timestamp();
    let end = date((year + 1) as i16, 1, 1).to_zoned(tz)?.timestamp();
    Ok((start, end))
}

/// The Pacific calendar year of an instant.
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
        // the Gradle golden test's JVM ran in the host's Pacific zone: race-1.json's 02:00Z start
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

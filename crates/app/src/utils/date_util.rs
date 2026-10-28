//! Date and time formatting for the UI, always in the club's Pacific time zone ([`TIME_ZONE`]).
//!
//! The server and the browser both render in Pacific time, not the viewer's zone, so the server's HTML and the
//! hydrated page agree; both builds bundle jiff's tz database for that (see `crates/app/Cargo.toml`).

use jiff::civil::DateTime;
use jiff::tz::TimeZone;
use jiff::Timestamp;

/// The club's zone (CYCT, Tacoma).
pub const TIME_ZONE: &str = "America/Los_Angeles";

pub fn time_zone() -> TimeZone {
    TimeZone::get(TIME_ZONE).expect("the tz database is bundled (jiff tzdb-bundle-always)")
}

pub fn double_digit(n: i32) -> String {
    format!("{n:02}")
}

pub fn quad_digit(n: i32) -> String {
    format!("{n:04}")
}

/// Formats the time of day as `HH:MM:SS`.
pub fn time_str(dt: &DateTime) -> String {
    format!(
        "{}:{}:{}",
        double_digit(i32::from(dt.hour())),
        double_digit(i32::from(dt.minute())),
        double_digit(i32::from(dt.second()))
    )
}

/// Formats the date as `M/D/YYYY`.
pub fn date_str(dt: &DateTime) -> String {
    format!("{}/{}/{}", dt.month(), dt.day(), dt.year())
}

/// Formats the date as `YYYY-MM-DD`, the value of an `<input type="date">`.
pub fn input_str(dt: &DateTime) -> String {
    format!(
        "{}-{}-{}",
        quad_digit(i32::from(dt.year())),
        double_digit(i32::from(dt.month())),
        double_digit(i32::from(dt.day()))
    )
}

/// Converts a Pacific local date-time back to an instant, truncated to whole seconds.
pub fn instant(dt: &DateTime) -> Timestamp {
    let ts = time_zone()
        .to_timestamp(*dt)
        .unwrap_or_else(|_| Timestamp::default());
    Timestamp::from_second(ts.as_second()).unwrap_or(ts)
}

pub fn local_date_time(ts: Timestamp) -> DateTime {
    time_zone().to_datetime(ts)
}

pub fn now() -> Timestamp {
    Timestamp::now()
}

pub fn get_clock_value() -> String {
    time_str(&local_date_time(now()))
}

pub fn current_year() -> String {
    year(now())
}

pub fn ts_date_str(ts: Timestamp) -> String {
    date_str(&local_date_time(ts))
}

pub fn ts_time_str(ts: Timestamp) -> String {
    time_str(&local_date_time(ts))
}

/// Formats an instant as `M/D/YYYY HH:MM:SS ` (with a trailing space) in Pacific time.
pub fn display(ts: Timestamp) -> String {
    let dt = local_date_time(ts);
    format!("{} {} ", date_str(&dt), time_str(&dt))
}

pub fn year(ts: Timestamp) -> String {
    local_date_time(ts).year().to_string()
}

/// Formats an optional start for the schedule tables like [`display`]; `"-"` for `None` (a race with no classes).
pub fn display_opt(ts: Option<Timestamp>) -> String {
    ts.map(display).unwrap_or_else(|| "-".to_string())
}

pub fn year_opt(ts: Option<Timestamp>) -> String {
    ts.map(year).unwrap_or_default()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pacific_display() {
        // 2026-06-17T17:00:00Z is 10:00 PDT
        let ts: Timestamp = "2026-06-17T17:00:00Z".parse().unwrap();
        assert_eq!(display(ts), "6/17/2026 10:00:00 ");
        assert_eq!(ts_time_str(ts), "10:00:00");
        assert_eq!(ts_date_str(ts), "6/17/2026");
        assert_eq!(input_str(&local_date_time(ts)), "2026-06-17");
        assert_eq!(year(ts), "2026");
        assert_eq!(instant(&local_date_time(ts)), ts);
    }

    #[test]
    fn digits_pad() {
        assert_eq!(double_digit(5), "05");
        assert_eq!(quad_digit(26), "0026");
    }
}

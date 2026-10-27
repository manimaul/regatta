//! Server configuration from the environment or flags (= `DbConfig` and the `-Djdbcurl`,
//! `-Duser`, `-Dpassword`, `-Dregatta.now` system properties of the Kotlin server).
//!
//! | Kotlin | Rust |
//! |---|---|
//! | `-Djdbcurl=jdbc:postgresql://host:5432/regatta -Duser=u -Dpassword=p` | `REGATTA_DB_URL=postgres://u:p@host:5432/regatta` |
//! | port 8888 (hard-coded) | `REGATTA_BIND=0.0.0.0:8888` |
//! | `-Dregatta.now=<instant>` (test only) | `REGATTA_FIXED_NOW=<instant>` |
//! | logback | `RUST_LOG` |

use clap::Parser;
use jiff::Timestamp;

/// The defaults match `database/docker-compose.yaml`.
pub const DEFAULT_DB_URL: &str = "postgres://regatta_admin:mysecretpassword@localhost:5432/regatta";
pub const DEFAULT_BIND: &str = "0.0.0.0:8888";

#[derive(Parser, Debug, Clone)]
#[command(name = "regatta", about = "The regatta scoring server", version)]
pub struct Config {
    /// PostgreSQL connection URL.
    #[arg(long, env = "REGATTA_DB_URL", default_value = DEFAULT_DB_URL)]
    pub db_url: String,
    /// Address to listen on.
    #[arg(long, env = "REGATTA_BIND", default_value = DEFAULT_BIND)]
    pub bind: String,
    /// Test only: a fixed ISO-8601 instant used as "now" for series standings and
    /// `LoginResponse.expires` (= `-Dregatta.now`). Absent or blank, the wall clock is used.
    #[arg(long, env = "REGATTA_FIXED_NOW", value_parser = parse_fixed_now)]
    pub fixed_now: Option<Timestamp>,
}

fn parse_fixed_now(value: &str) -> Result<Timestamp, String> {
    value
        .trim()
        .parse::<Timestamp>()
        .map_err(|e| format!("REGATTA_FIXED_NOW is not an ISO-8601 instant: {e}"))
}

impl Config {
    /// `FixedClock.now()`: the fixed instant when configured, else the wall clock.
    pub fn now(&self) -> Timestamp {
        now(self.fixed_now)
    }
}

/// `FixedClock.now()` for a parsed `REGATTA_FIXED_NOW`.
pub fn now(fixed_now: Option<Timestamp>) -> Timestamp {
    fixed_now.unwrap_or_else(Timestamp::now)
}

/// `FixedClock.parse`: the instant a `REGATTA_FIXED_NOW` value names, or `None` when absent or blank.
pub fn parse_fixed_now_value(value: Option<&str>) -> Result<Option<Timestamp>, jiff::Error> {
    match value.map(str::trim) {
        None | Some("") => Ok(None),
        Some(v) => v.parse::<Timestamp>().map(Some),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fixed_now_parses_or_is_absent() {
        assert_eq!(parse_fixed_now_value(None).unwrap(), None);
        assert_eq!(parse_fixed_now_value(Some("  ")).unwrap(), None);
        let ts = parse_fixed_now_value(Some(" 2026-10-14T02:10:11Z "))
            .unwrap()
            .unwrap();
        assert_eq!(ts.to_string(), "2026-10-14T02:10:11Z");
        assert!(parse_fixed_now_value(Some("yesterday")).is_err());
    }
}

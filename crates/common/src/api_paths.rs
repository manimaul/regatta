//! The REST path constants, appended to `/v1/api/` by [`crate::utils::versioned_api`].
//!
//! Most carry a leading slash and so produce `/v1/api//race`, which the server treats like `/v1/api/race`;
//! [`STANDINGS`] has none.

pub const SERIES: &str = "/series";
pub const ALL_SERIES: &str = "/allSeries";
pub const FIND_SERIES: &str = "/find/series";
pub const STANDINGS: &str = "series/standings";

pub const RACE_CLASS: &str = "/raceClass";
pub const RACE_CLASS_LIST: &str = "/raceClassList";
pub const ALL_CLASSES: &str = "/allClasses";
pub const BRACKET: &str = "/bracket";

pub const LOGIN: &str = "/login";
pub const AUTH: &str = "/auth";

pub const YEARS: &str = "/years";

pub const ALL_RACES: &str = "/allRaces";
pub const RACE: &str = "/race";
pub const RACE_SCHEDULE: &str = "/raceSchedule";

pub const SKIPPER: &str = "/skipper";
pub const FIND_SKIPPER: &str = "/find/skipper";
pub const ALL_SKIPPERS: &str = "/allSkippers";

pub const REPORT: &str = "/report";
pub const RESULTS: &str = "/results";
pub const SCHEDULE_RESULTS: &str = "/scheduleResults";
/// The number of results recorded for a race.
pub const RESULT_COUNT: &str = "/resultCount";

pub const BOAT: &str = "/boat";
/// Every boat; the path is singular (`allBoat`) on the wire.
pub const ALL_BOATS: &str = "/allBoat";

/// One ORC certificate fetched from data.orc.org by `refNo`.
pub const ORC_CERT: &str = "/orcCert";
pub const ORC_CERTS: &str = "/orcCerts";

pub const IMAGE: &str = "/image";

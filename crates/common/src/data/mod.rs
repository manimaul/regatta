//! The serde domain models: the REST bodies, the stored records and the race reports and series standings.
//!
//! Field order is declaration order and is pinned by the golden files; every field with a default is optional on
//! input and always written on output (`None` as `null`); enums serialize by their `name()`.
//!
//! - [`race_data`]: races, series, classes, brackets, boats, people, results, reports and standings.
//! - [`orc_certificate`]: ORC certificates, scoring options and wind bands, and the named PHRF B factors.
//! - [`finish_code`]: how a boat's race ended.
//! - [`login_data`]: login and auth records.
//! - [`about_info`]: the about record.

pub mod about_info;
pub mod finish_code;
pub mod login_data;
pub mod orc_certificate;
pub mod race_data;

pub use about_info::*;
pub use finish_code::*;
pub use login_data::*;
pub use orc_certificate::*;
pub use race_data::*;

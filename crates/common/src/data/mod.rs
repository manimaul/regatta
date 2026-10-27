//! The `@Serializable` domain models (= `com.mxmariner.regatta.data`), one module per Kotlin file.
//!
//! Field order is the Kotlin declaration order; every field with a Kotlin default is optional on
//! input and always written on output (nulls included); enums serialize by their Kotlin names.

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

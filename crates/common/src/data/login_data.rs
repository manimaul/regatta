//! The login request, the stored auth record and the login response that is the bearer token.
//!
//! These have no defaults (except [`AuthRecord::id`]), so every field is required on input. See
//! `crates/service/src/auth.rs` for how the hashes and tokens are made and checked.

use crate::serde_kotlin::kotlin_instant;
use jiff::Timestamp;
use serde::{Deserialize, Serialize};

/// A login request: the user name and a signature of the request time, made from the stored password hash.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Login {
    pub user_name: String,
    /// The signature of `time`: salted SHA-512 of `salt`, `time` and the stored password hash
    /// (`time_stamp_hash` in `crates/service/src/auth.rs`).
    pub hash_of_hash: String,
    pub salt: String,
    /// When the client made the request.
    #[serde(with = "kotlin_instant")]
    pub time: Timestamp,
}

/// An admin account as stored in the `auth` table. While the table is empty, every request is admin.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthRecord {
    #[serde(default)]
    pub id: i64,
    pub hash: String,
    pub user_name: String,
}

/// A successful login; `base64(json(LoginResponse))` is the bearer token and the `regatta_token` cookie.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LoginResponse {
    /// The [`AuthRecord::id`] of the account.
    pub id: i64,
    /// The signature of `expires`, made from `salt` and the account's stored password hash; the server checks it to
    /// accept the token.
    pub hash_of_hash: String,
    pub salt: String,
    /// When the token stops being accepted, 30 days after login (`REGATTA_FIXED_NOW` fixes the clock in tests).
    #[serde(with = "kotlin_instant")]
    pub expires: Timestamp,
}

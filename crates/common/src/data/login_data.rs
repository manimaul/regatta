//! = `LoginData.kt`. These have no Kotlin defaults (except `AuthRecord.id`), so the fields are
//! required on input.

use crate::serde_kotlin::kotlin_instant;
use jiff::Timestamp;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Login {
    pub user_name: String,
    pub hash_of_hash: String,
    pub salt: String,
    #[serde(with = "kotlin_instant")]
    pub time: Timestamp,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthRecord {
    #[serde(default)]
    pub id: i64,
    pub hash: String,
    pub user_name: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LoginResponse {
    pub id: i64,
    pub hash_of_hash: String,
    pub salt: String,
    #[serde(with = "kotlin_instant")]
    pub expires: Timestamp,
}

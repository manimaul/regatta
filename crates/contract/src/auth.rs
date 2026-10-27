//! The regatta auth scheme, byte for byte (`server/.../auth/Token.kt` and `web/.../utils/Auth.kt`).
//!
//! `hash(a, b, c)` = `b64(sha512(c + b64(sha512(b + b64(sha512(a))))))`, standard base64 with
//! padding. Admin creation stores `hash(password)`; a login sends
//! `hash(salt, epochSeconds, hash(password))`; the server answers a `LoginResponse` whose
//! `hashOfHash` is `hash(serverSalt, expiresEpochSeconds, storedHash)`; the bearer token is
//! `base64(json(LoginResponse))`, validated by recomputing that hash. Knowing the stored hash (the
//! scenario created the admin), the harness can forge a correctly signed token with any `expires`.

use anyhow::{anyhow, Context, Result};
use base64::engine::general_purpose::STANDARD;
use base64::Engine;
use serde_json::{json, Value};
use sha2::{Digest, Sha512};
use std::str::FromStr;

/// `Token.hash(vararg data)`: each part is prefixed to the running result and hashed.
pub fn hash(parts: &[&str]) -> String {
    let mut result = String::new();
    for part in parts {
        let mut input = String::with_capacity(part.len() + result.len());
        input.push_str(part);
        input.push_str(&result);
        result = STANDARD.encode(Sha512::digest(input.as_bytes()));
    }
    result
}

/// `Token.timeStampHash(instant, salt, hash)` = `hash(salt, "${instant.epochSeconds}", hash)`.
pub fn time_stamp_hash(epoch_seconds: i64, salt: &str, stored_hash: &str) -> String {
    hash(&[salt, &epoch_seconds.to_string(), stored_hash])
}

/// The epoch seconds of an ISO-8601 instant, as kotlin `Instant.epochSeconds` gives them.
pub fn epoch_seconds(instant: &str) -> Result<i64> {
    let ts =
        jiff::Timestamp::from_str(instant).with_context(|| format!("not an instant: {instant}"))?;
    Ok(ts.as_second())
}

/// The stored admin hash: `hash(password)`, computed in the browser on admin creation.
pub fn stored_hash(password: &str) -> String {
    hash(&[password])
}

/// The `Login` body the web client sends, in Kotlin declaration order, with a fixed salt and time.
pub fn login_body(user_name: &str, password: &str, salt: &str, time: &str) -> Result<Value> {
    let secs = epoch_seconds(time)?;
    Ok(json!({
        "userName": user_name,
        "hashOfHash": time_stamp_hash(secs, salt, &stored_hash(password)),
        "salt": salt,
        "time": time,
    }))
}

/// The bearer token the web client stores: base64 of the `LoginResponse` JSON exactly as received.
pub fn encode_token(login_response_json: &[u8]) -> String {
    STANDARD.encode(login_response_json)
}

/// A `LoginResponse` JSON document in Kotlin field order.
pub fn login_response_json(id: i64, hash_of_hash: &str, salt: &str, expires: &str) -> String {
    serde_json::to_string(&json!({
        "id": id,
        "hashOfHash": hash_of_hash,
        "salt": salt,
        "expires": expires,
    }))
    .expect("json")
}

/// A bearer token signed as the server would (`correct`) or with a wrong hash, for auth record
/// `id` whose stored hash is `stored_hash`, with a client-chosen `salt` and `expires`.
pub fn forge_token(
    id: i64,
    stored_hash: &str,
    salt: &str,
    expires: &str,
    correct: bool,
) -> Result<String> {
    let secs = epoch_seconds(expires)?;
    let mut hash_of_hash = time_stamp_hash(secs, salt, stored_hash);
    if !correct {
        hash_of_hash = hash(&["not-the-right-hash"]);
    }
    Ok(encode_token(
        login_response_json(id, &hash_of_hash, salt, expires).as_bytes(),
    ))
}

/// Parses a bearer token the way the server does: base64, then `LoginResponse` JSON.
pub fn decode_token(token: &str) -> Result<Value> {
    let bytes = STANDARD.decode(token).context("token is not base64")?;
    serde_json::from_slice(&bytes).context("token is not LoginResponse json")
}

/// Validates a token against a stored hash as `Token.validateHash` does (no expiry check here);
/// returns the auth id the token names.
pub fn validate_token(token: &str, stored_hash: &str) -> Result<i64> {
    let value = decode_token(token)?;
    let id = value
        .get("id")
        .and_then(Value::as_i64)
        .ok_or_else(|| anyhow!("token has no id"))?;
    let salt = value
        .get("salt")
        .and_then(Value::as_str)
        .ok_or_else(|| anyhow!("token has no salt"))?;
    let expires = value
        .get("expires")
        .and_then(Value::as_str)
        .ok_or_else(|| anyhow!("token has no expires"))?;
    let hash_of_hash = value
        .get("hashOfHash")
        .and_then(Value::as_str)
        .ok_or_else(|| anyhow!("no hashOfHash"))?;
    let expected = time_stamp_hash(epoch_seconds(expires)?, salt, stored_hash);
    if expected == hash_of_hash {
        Ok(id)
    } else {
        Err(anyhow!("token hash mismatch"))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The `TokenTest` vectors (`server/src/jvmTest/.../auth/TokenTest.kt`).
    #[test]
    fn hash_single_vector() {
        assert_eq!(
            hash(&["foo"]),
            "9/u6bgY2+JDlb7vzKD5STG+jIErimDgtYkdB0NxmODJuKCxBvl5CVNiCB3LFUYosWowMf37aGVlKfrU5RT4e1w=="
        );
    }

    #[test]
    fn hash_chained_vector() {
        assert_eq!(
            hash(&["foo", "bar", "baz"]),
            "+UgXfdhOoVIqZNkC04ECxn5wEV+Oaij6/7UCPjgx4WENCMm3tq1wqv4Eslte9C5OHtRnoqkHDFhyGb8UWVQI5w=="
        );
    }

    #[test]
    fn epoch_seconds_matches_kotlin() {
        assert_eq!(epoch_seconds("1970-01-01T00:00:10Z").unwrap(), 10);
        assert_eq!(epoch_seconds("2026-10-14T02:10:11Z").unwrap(), 1791943811);
        assert!(epoch_seconds("yesterday").is_err());
    }

    #[test]
    fn login_body_is_deterministic_and_ordered() {
        let a = login_body("contract", "pw", "salt", "2026-10-14T02:10:11Z").unwrap();
        let b = login_body("contract", "pw", "salt", "2026-10-14T02:10:11Z").unwrap();
        assert_eq!(a, b);
        let text = serde_json::to_string(&a).unwrap();
        assert!(
            text.starts_with(r#"{"userName":"contract","hashOfHash":""#),
            "{text}"
        );
        assert!(
            text.ends_with(r#""salt":"salt","time":"2026-10-14T02:10:11Z"}"#),
            "{text}"
        );
        assert_eq!(
            a["hashOfHash"].as_str().unwrap(),
            time_stamp_hash(1791943811, "salt", &hash(&["pw"]))
        );
    }

    #[test]
    fn forged_token_round_trips_and_validates() {
        let stored = stored_hash("contract-password");
        let token = forge_token(4, &stored, "my-salt", "2099-01-01T00:00:00Z", true).unwrap();
        assert_eq!(validate_token(&token, &stored).unwrap(), 4);
        let decoded = decode_token(&token).unwrap();
        assert_eq!(decoded["expires"], "2099-01-01T00:00:00Z");
        assert_eq!(decoded["salt"], "my-salt");
        let wrong = forge_token(4, &stored, "my-salt", "2099-01-01T00:00:00Z", false).unwrap();
        assert!(validate_token(&wrong, &stored).is_err());
        assert!(validate_token("bm9uZQ==", &stored).is_err());
    }
}

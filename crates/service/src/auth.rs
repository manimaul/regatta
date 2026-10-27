//! Auth tokens (= `auth/Token.kt`) and the bearer header rules of Ktor's bearer provider that
//! the REST side reproduces (see "Bearer auth and bootstrap" in the plan's quirks).
//!
//! `hash(a, b, c)` = `b64(sha512(c + b64(sha512(b + b64(sha512(a))))))`, standard base64 with
//! padding; the `TokenTest` vectors pin it. The bearer token is `base64(json(LoginResponse))`.

use crate::db::Db;
use anyhow::{Context, Result};
use base64::engine::general_purpose::{GeneralPurpose, GeneralPurposeConfig, STANDARD};
use base64::engine::DecodePaddingMode;
use base64::{alphabet, Engine};
use jiff::{SignedDuration, Timestamp};
use rand::Rng;
use regatta_common::serde_kotlin::kotlin_instant;
use regatta_common::{AuthRecord, Login, LoginResponse};
use serde::Deserialize;
use sha2::{Digest, Sha512};

/// `Token.expiresIn`: 30 days.
pub const EXPIRES_IN: SignedDuration = SignedDuration::from_secs(60 * 60 * 24 * 30);

/// `Token.Admin.realm`.
pub const REALM: &str = "admin access";
/// The `WWW-Authenticate` value of every 401 on an admin route.
pub const CHALLENGE: &str = "Bearer realm=\"admin access\"";

fn hash_internal(value: &str) -> String {
    STANDARD.encode(Sha512::digest(value.as_bytes()))
}

/// `Token.hash(vararg data)`: each value is hashed with the previous result appended.
pub fn hash(data: &[&str]) -> String {
    let mut result = String::new();
    for value in data {
        result = hash_internal(&format!("{value}{result}"));
    }
    result
}

/// `Token.timeStampHash`.
pub fn time_stamp_hash(instant: Timestamp, salt: &str, hash_value: &str) -> String {
    hash(&[salt, &instant.as_second().to_string(), hash_value])
}

/// `Token.salt()`: 16 random bytes, base64.
pub fn salt() -> String {
    let mut bytes = [0u8; 16];
    rand::rng().fill_bytes(&mut bytes);
    STANDARD.encode(bytes)
}

/// `Token.createLoginResponse`: `None` when the user is unknown or the password hash does not match.
/// `now` is `FixedClock.now()` (the fixed clock when configured).
pub async fn create_login_response(
    db: &Db,
    login: &Login,
    now: Timestamp,
) -> Result<Option<LoginResponse>> {
    let Some(record) = db.get_auth_by_name(&login.user_name).await? else {
        return Ok(None);
    };
    let expected = time_stamp_hash(login.time, &login.salt, &record.hash);
    if expected != login.hash_of_hash {
        return Ok(None);
    }
    let expires = now
        .checked_add(EXPIRES_IN)
        .context("login expiry out of range")?;
    let salt = salt();
    let hash_of_hash = time_stamp_hash(expires, &salt, &record.hash);
    Ok(Some(LoginResponse {
        id: record.id,
        hash_of_hash,
        salt,
        expires,
    }))
}

/// `Token.validateHash`: not expired (against the wall clock, like Kotlin's `Clock.System.now()`)
/// and signed with the stored hash.
pub fn validate_hash(response: &LoginResponse, record: &AuthRecord) -> bool {
    if response.expires.duration_since(Timestamp::now()) <= SignedDuration::ZERO {
        return false;
    }
    time_stamp_hash(response.expires, &response.salt, &record.hash) == response.hash_of_hash
}

/// What `Token.validateAdminToken` answers.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TokenCheck {
    /// The admin principal: a valid token, or any token while the `auth` table is empty (bootstrap).
    Admin,
    /// Not an admin: a wrong signature, an expired token or an unknown auth id (401 on the REST side).
    Rejected,
}

/// Java's `Base64.getDecoder()`: the standard alphabet, padding accepted but not required.
const JAVA_DECODER: GeneralPurpose = GeneralPurpose::new(
    &alphabet::STANDARD,
    GeneralPurposeConfig::new().with_decode_padding_mode(DecodePaddingMode::Indifferent),
);

/// `LoginResponse` as kotlinx decodes it: every field required, no unknown keys.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct StrictLoginResponse {
    id: i64,
    hash_of_hash: String,
    salt: String,
    #[serde(with = "kotlin_instant")]
    expires: Timestamp,
}

/// `Token.parseToken`: base64 → JSON `LoginResponse`. Kotlin catches only `IOException`, so a
/// token that is not base64 or not that JSON is an error (a 500 on the REST side), not a rejection.
pub fn parse_token(token: &str) -> Result<LoginResponse> {
    let bytes = JAVA_DECODER
        .decode(token)
        .context("bearer token is not base64")?;
    let strict: StrictLoginResponse =
        serde_json::from_slice(&bytes).context("bearer token is not a LoginResponse")?;
    Ok(LoginResponse {
        id: strict.id,
        hash_of_hash: strict.hash_of_hash,
        salt: strict.salt,
        expires: strict.expires,
    })
}

/// `Token.validateAdminToken`: while no admin exists every token is accepted (bootstrap);
/// afterwards the token must parse (else `Err`), name a stored record and verify.
pub async fn validate_admin_token(db: &Db, token: &str) -> Result<TokenCheck> {
    if !db.admin_exists().await? {
        return Ok(TokenCheck::Admin);
    }
    let login = parse_token(token)?;
    match db.get_auth(login.id).await? {
        Some(record) if validate_hash(&login, &record) => Ok(TokenCheck::Admin),
        _ => Ok(TokenCheck::Rejected),
    }
}

/// `io.ktor.http.auth.parseAuthorizationHeader` reduced to what the bearer provider does with it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AuthHeader {
    /// `HttpAuthHeader.Single`: a scheme and a token68 blob.
    Single { scheme: String, blob: String },
    /// `HttpAuthHeader.Parameterized` (or a blank scheme): never bearer credentials.
    Other,
}

/// Ktor's `ParseException` → `BadRequestException("Invalid auth header")` → 500 on the REST side.
#[derive(Debug, thiserror::Error)]
#[error("Invalid auth header")]
pub struct InvalidAuthHeader;

const TOKEN_EXTRA: &[char] = &[
    '!', '#', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|', '~',
];
const TOKEN68_EXTRA: &[char] = &['-', '.', '_', '~', '+', '/'];

fn is_token(c: char) -> bool {
    c.is_ascii_alphanumeric() || TOKEN_EXTRA.contains(&c)
}

fn is_token68(c: char) -> bool {
    c.is_ascii_alphanumeric() || TOKEN68_EXTRA.contains(&c)
}

fn skip_spaces(s: &[char], mut i: usize) -> usize {
    while i < s.len() && s[i] == ' ' {
        i += 1;
    }
    i
}

fn match_token68(s: &[char], start: usize) -> usize {
    let mut i = skip_spaces(s, start);
    while i < s.len() && is_token68(s[i]) {
        i += 1;
    }
    while i < s.len() && s[i] == '=' {
        i += 1;
    }
    skip_spaces(s, i)
}

/// `matchParameter`: the index after one `key=value` (or `start` when there is none there).
fn match_parameter(s: &[char], start: usize) -> Result<usize, InvalidAuthHeader> {
    let key_start = skip_spaces(s, start);
    let mut i = key_start;
    while i < s.len() && is_token(s[i]) {
        i += 1;
    }
    i = skip_spaces(s, i);
    if i == s.len() || s[i] != '=' {
        return Ok(start);
    }
    i += 1;
    i = skip_spaces(s, i);
    if i < s.len() && s[i] == '"' {
        i += 1;
        let mut escaped = false;
        while i < s.len() {
            if s[i] == '"' && !escaped {
                break;
            }
            escaped = !escaped && s[i] == '\\';
            i += 1;
        }
        if i == s.len() {
            return Err(InvalidAuthHeader);
        }
        i += 1;
    } else {
        while i < s.len() && s[i] != ' ' && s[i] != ',' {
            i += 1;
        }
    }
    Ok(i)
}

/// `skipDelimiter`: spaces, one `,`, spaces; a `ParseException` when the delimiter is missing.
fn skip_delimiter(s: &[char], start: usize) -> Result<usize, InvalidAuthHeader> {
    let mut i = skip_spaces(s, start);
    if i == s.len() {
        return Ok(usize::MAX);
    }
    if s[i] != ',' {
        return Err(InvalidAuthHeader);
    }
    i += 1;
    Ok(skip_spaces(s, i))
}

/// `matchParameters`: `-1` (here `None`) when the whole remainder was parameters.
fn match_parameters(s: &[char], start: usize) -> Result<Option<usize>, InvalidAuthHeader> {
    let mut i = start;
    while i > 0 && i < s.len() {
        let next = match_parameter(s, i)?;
        if next == i {
            return Ok(Some(i));
        }
        i = skip_delimiter(s, next)?;
        if i == usize::MAX {
            return Ok(None);
        }
    }
    Ok(Some(i))
}

/// `parseAuthorizationHeader(headerValue)`: `Ok(None)` for a blank scheme, `Err` where Ktor throws.
pub fn parse_authorization_header(value: &str) -> Result<Option<AuthHeader>, InvalidAuthHeader> {
    let s: Vec<char> = value.chars().collect();
    let mut i = skip_spaces(&s, 0);
    let scheme_start = i;
    while i < s.len() && is_token(s[i]) {
        i += 1;
    }
    let scheme: String = s[scheme_start..i].iter().collect();
    i = skip_spaces(&s, i);
    if scheme.trim().is_empty() {
        return Ok(None);
    }
    if i == s.len() {
        return Ok(Some(AuthHeader::Other));
    }
    let token68_end = match_token68(&s, i);
    let blob: String = s[i..token68_end]
        .iter()
        .collect::<String>()
        .trim()
        .to_string();
    if !blob.is_empty() && token68_end == s.len() {
        return Ok(Some(AuthHeader::Single { scheme, blob }));
    }
    match match_parameters(&s, i)? {
        None => Ok(Some(AuthHeader::Other)),
        Some(_) => Err(InvalidAuthHeader),
    }
}

/// The bearer blob the provider would validate, if the header carries one
/// (`HttpAuthHeader.Single` with the `Bearer` scheme, case-insensitive).
pub fn bearer_token(value: &str) -> Result<Option<String>, InvalidAuthHeader> {
    Ok(match parse_authorization_header(value)? {
        Some(AuthHeader::Single { scheme, blob }) if scheme.eq_ignore_ascii_case("Bearer") => {
            Some(blob)
        }
        _ => None,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    /// `TokenTest.hashTest`.
    #[test]
    fn hash_test() {
        assert_eq!(
            hash(&["foo"]),
            "9/u6bgY2+JDlb7vzKD5STG+jIErimDgtYkdB0NxmODJuKCxBvl5CVNiCB3LFUYosWowMf37aGVlKfrU5RT4e1w=="
        );
    }

    /// `TokenTest.hashManyTest`.
    #[test]
    fn hash_many_test() {
        assert_eq!(
            hash(&["foo", "bar", "baz"]),
            "+UgXfdhOoVIqZNkC04ECxn5wEV+Oaij6/7UCPjgx4WENCMm3tq1wqv4Eslte9C5OHtRnoqkHDFhyGb8UWVQI5w=="
        );
    }

    #[test]
    fn salt_is_sixteen_random_bytes() {
        let a = salt();
        let b = salt();
        assert_eq!(STANDARD.decode(&a).unwrap().len(), 16);
        assert_ne!(a, b);
    }

    #[test]
    fn token_round_trip_and_strictness() {
        let response = LoginResponse {
            id: 4,
            hash_of_hash: "h".into(),
            salt: "s".into(),
            expires: "2026-11-13T02:10:11Z".parse().unwrap(),
        };
        let token = STANDARD.encode(serde_json::to_string(&response).unwrap());
        assert_eq!(parse_token(&token).unwrap(), response);
        // padding optional, like java.util.Base64
        assert_eq!(parse_token(token.trim_end_matches('=')).unwrap(), response);
        assert!(parse_token("none").is_err(), "valid base64, not JSON");
        assert!(parse_token("aGVsbG8gd29ybGQ=").is_err());
        assert!(parse_token(&STANDARD.encode(r#"{"foo":1}"#)).is_err());
        let extra = STANDARD.encode(
            r#"{"id":4,"hashOfHash":"h","salt":"s","expires":"2026-11-13T02:10:11Z","x":1}"#,
        );
        assert!(parse_token(&extra).is_err(), "unknown keys are rejected");
        assert!(parse_token("not*base64!").is_err());
    }

    #[test]
    fn validate_hash_checks_expiry_and_signature() {
        let record = AuthRecord {
            id: 1,
            hash: hash(&["pw"]),
            user_name: "u".into(),
        };
        let expires = Timestamp::now().checked_add(EXPIRES_IN).unwrap();
        let good = LoginResponse {
            id: 1,
            hash_of_hash: time_stamp_hash(expires, "salt", &record.hash),
            salt: "salt".into(),
            expires,
        };
        assert!(validate_hash(&good, &record));
        let wrong = LoginResponse {
            hash_of_hash: "x".into(),
            ..good.clone()
        };
        assert!(!validate_hash(&wrong, &record));
        let expired_at: Timestamp = "2020-01-01T00:00:00Z".parse().unwrap();
        let expired = LoginResponse {
            hash_of_hash: time_stamp_hash(expired_at, "salt", &record.hash),
            expires: expired_at,
            ..good
        };
        assert!(!validate_hash(&expired, &record));
    }

    #[test]
    fn authorization_header_like_ktor() {
        assert_eq!(
            bearer_token("Bearer none").unwrap(),
            Some("none".to_string())
        );
        assert_eq!(
            bearer_token("bearer aGVsbG8gd29ybGQ=").unwrap(),
            Some("aGVsbG8gd29ybGQ=".to_string())
        );
        assert_eq!(
            bearer_token("Bearer ").unwrap(),
            None,
            "empty bearer is a challenge"
        );
        assert_eq!(bearer_token("Bearer").unwrap(), None);
        assert_eq!(
            bearer_token("Basic dXNlcjpwdw==").unwrap(),
            None,
            "other scheme"
        );
        assert!(
            bearer_token("Bearer not*base64!").is_err(),
            "unparseable header"
        );
        assert_eq!(
            bearer_token("Bearer realm=x").unwrap(),
            None,
            "parameterized"
        );
        assert_eq!(bearer_token("   ").unwrap(), None);
        assert!(bearer_token("Bearer a=\"unterminated").is_err());
    }
}

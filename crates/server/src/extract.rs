//! Request extractors with Ktor's semantics: the first value of a query parameter and
//! `toLong()` (a 500 on anything that is not a number), bodies decoded strictly like kotlinx
//! (unknown keys, missing required fields and bad enum names are a 500), and the admin bearer
//! check of `Token.validateAdminToken` behind Ktor's bearer provider (missing or non-bearer
//! header: 401 with the challenge; unparseable header: 500; bootstrap: any bearer value).

use crate::error::{AppError, AppResult};
use crate::respond::empty;
use crate::AppState;
use anyhow::{anyhow, Context};
use axum::body::Bytes;
use axum::extract::{FromRequest, FromRequestParts, Request};
use axum::http::request::Parts;
use axum::http::{header, HeaderValue, StatusCode};
use axum::response::{IntoResponse, Response};
use regatta_service::auth::{self, TokenCheck, CHALLENGE};
use serde::de::DeserializeOwned;
use serde::Serialize;
use serde_json::Value;
use std::str::FromStr;

/// The query string, decoded; `get` answers the first value like Ktor's `queryParameters[name]`.
#[derive(Debug, Clone, Default)]
pub struct Params(pub Vec<(String, String)>);

impl Params {
    pub fn parse(query: Option<&str>) -> Params {
        let Some(query) = query else {
            return Params::default();
        };
        Params(
            query
                .split('&')
                .filter(|pair| !pair.is_empty())
                .map(|pair| match pair.find('=') {
                    Some(i) => (decode(&pair[..i]), decode(&pair[i + 1..])),
                    None => (decode(pair), String::new()),
                })
                .collect(),
        )
    }

    pub fn get(&self, name: &str) -> Option<&str> {
        self.0
            .iter()
            .find(|(k, _)| k == name)
            .map(|(_, v)| v.as_str())
    }

    /// `queryParameters[name]?.toLong()`: `None` when absent, an error (500) when not a number.
    pub fn long(&self, name: &str) -> AppResult<Option<i64>> {
        self.number::<i64>(name)
    }

    /// `queryParameters[name]?.toInt()`.
    pub fn int(&self, name: &str) -> AppResult<Option<i32>> {
        self.number::<i32>(name)
    }

    fn number<T: FromStr>(&self, name: &str) -> AppResult<Option<T>> {
        match self.get(name) {
            None => Ok(None),
            Some(v) => v
                .parse::<T>()
                .map(Some)
                .map_err(|_| AppError(anyhow!("query parameter {name}={v:?} is not a number"))),
        }
    }

    /// `queryParameters[name]?.toLongOrNull()` / `toIntOrNull()`: `None` for anything not a number.
    pub fn number_or_none<T: FromStr>(&self, name: &str) -> Option<T> {
        self.get(name).and_then(|v| v.parse::<T>().ok())
    }
}

/// Percent-decoding with `+` as space (Ktor's `decodeURLQueryComponent(plusIsSpace = true)`).
fn decode(s: &str) -> String {
    let bytes = s.as_bytes();
    let mut out = Vec::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        match bytes[i] {
            b'+' => out.push(b' '),
            b'%' if i + 2 < bytes.len() => match u8::from_str_radix(&s[i + 1..i + 3], 16) {
                Ok(b) => {
                    out.push(b);
                    i += 2;
                }
                Err(_) => out.push(b'%'),
            },
            b => out.push(b),
        }
        i += 1;
    }
    String::from_utf8_lossy(&out).into_owned()
}

impl<S: Send + Sync> FromRequestParts<S> for Params {
    type Rejection = AppError;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        Ok(Params::parse(parts.uri.query()))
    }
}

/// The admin principal: present only when the request passed the bearer check.
pub struct Admin;

/// `UnauthorizedResponse(HttpAuthHeader.bearerAuthChallenge(...))`: 401, the challenge, no body.
pub fn challenge() -> Response {
    let mut response = empty(StatusCode::UNAUTHORIZED);
    response.headers_mut().insert(
        header::WWW_AUTHENTICATE,
        HeaderValue::from_static(CHALLENGE),
    );
    response
}

impl FromRequestParts<AppState> for Admin {
    type Rejection = Response;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let Some(value) = parts.headers.get(header::AUTHORIZATION) else {
            return Err(challenge());
        };
        let value = value
            .to_str()
            .context("Authorization header is not UTF-8")
            .map_err(|e| AppError(e).into_response())?;
        let token = auth::bearer_token(value).map_err(|e| AppError(e.into()).into_response())?;
        let Some(token) = token else {
            return Err(challenge());
        };
        match auth::validate_admin_token(&state.db, &token).await {
            Ok(TokenCheck::Admin) => Ok(Admin),
            Ok(TokenCheck::Rejected) => Err(challenge()),
            Err(e) => Err(AppError(e).into_response()),
        }
    }
}

/// A JSON body received like `call.receive<T>()`: the content type must be `application/json`,
/// and the document must decode strictly (kotlinx: no unknown keys). Any failure is a 500.
pub struct Json<T>(pub T);

impl<T, S> FromRequest<S> for Json<T>
where
    T: DeserializeOwned + Serialize,
    S: Send + Sync,
{
    type Rejection = AppError;

    async fn from_request(req: Request, state: &S) -> Result<Self, Self::Rejection> {
        if !is_json(req.headers().get(header::CONTENT_TYPE)) {
            return Err(AppError(anyhow!(
                "no content converter for {:?}",
                req.headers().get(header::CONTENT_TYPE)
            )));
        }
        let bytes = Bytes::from_request(req, state)
            .await
            .map_err(|e| AppError(anyhow!("reading the request body: {e}")))?;
        Ok(Json(decode_strict(&bytes)?))
    }
}

fn is_json(content_type: Option<&HeaderValue>) -> bool {
    content_type
        .and_then(|v| v.to_str().ok())
        .map(|v| {
            v.split(';')
                .next()
                .unwrap_or("")
                .trim()
                .eq_ignore_ascii_case("application/json")
        })
        .unwrap_or(false)
}

/// kotlinx-strict decoding: parse, then re-serialize and require every input key to be a known
/// one at the same place (the common types write every field, so a key missing from the output
/// was unknown). `dxtId` / `orcDxtId` are skipped when null, the one case a known key is absent.
pub fn decode_strict<T: DeserializeOwned + Serialize>(bytes: &[u8]) -> AppResult<T> {
    let input: Value = serde_json::from_slice(bytes).context("request body is not JSON")?;
    let value: T = serde_json::from_value(input.clone()).context("request body does not decode")?;
    let output = serde_json::to_value(&value).context("re-serializing the request body")?;
    check_known_keys(&input, &output, "")?;
    Ok(value)
}

const SKIPPED_WHEN_NULL: &[&str] = &["dxtId", "orcDxtId"];

fn check_known_keys(input: &Value, output: &Value, path: &str) -> AppResult<()> {
    match (input, output) {
        (Value::Object(i), Value::Object(o)) => {
            for (key, value) in i {
                match o.get(key) {
                    Some(out) => check_known_keys(value, out, &format!("{path}/{key}"))?,
                    None if value.is_null() && SKIPPED_WHEN_NULL.contains(&key.as_str()) => {}
                    None => {
                        return Err(AppError(anyhow!(
                            "unknown key {path}/{key} in the request body"
                        )))
                    }
                }
            }
            Ok(())
        }
        (Value::Array(i), Value::Array(o)) => {
            for (n, (a, b)) in i.iter().zip(o.iter()).enumerate() {
                check_known_keys(a, b, &format!("{path}/{n}"))?;
            }
            Ok(())
        }
        _ => Ok(()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use regatta_common::{Race, RaceResult, Series};

    #[test]
    fn params_take_the_first_value_and_decode() {
        let p = Params::parse(Some("id=1&id=2&name=Ann+L%C3%B8e&flag"));
        assert_eq!(p.get("id"), Some("1"));
        assert_eq!(p.get("name"), Some("Ann Løe"));
        assert_eq!(p.get("flag"), Some(""));
        assert_eq!(p.get("missing"), None);
        assert_eq!(p.long("id").unwrap(), Some(1));
        assert!(p.long("name").is_err());
        assert_eq!(p.long("missing").unwrap(), None);
        assert_eq!(p.number_or_none::<i64>("name"), None);
        assert!(Params::parse(Some("id=99999999999999999999"))
            .long("id")
            .is_err());
        assert_eq!(Params::parse(Some("id=%2B7")).long("id").unwrap(), Some(7));
    }

    #[test]
    fn strict_decoding_matches_kotlinx() {
        let series: Vec<Series> = decode_strict(br#"[{"name": "Contract Defaults"}]"#).unwrap();
        assert_eq!(series[0].name, "Contract Defaults");
        assert!(decode_strict::<Vec<Series>>(br#"[{"name": "x", "bogus": 1}]"#).is_err());
        assert!(decode_strict::<Vec<Series>>(br#"{"name": "x"}"#).is_err());
        assert!(decode_strict::<Vec<Series>>(b"").is_err());
        let race: Race = decode_strict(br#"{"name": "Contract Defaults Race"}"#).unwrap();
        assert_eq!(race.phrf_b_factor, 600);
        assert!(decode_strict::<Race>(br#"{"orc3Band": "Gale"}"#).is_err());
        let result: RaceResult =
            decode_strict(br#"{"raceId": 138, "boatId": 47, "bracketId": 73, "raceClassId": 41}"#)
                .unwrap();
        assert_eq!(result.finish_code, regatta_common::FinishCode::Ret);
        assert!(decode_strict::<RaceResult>(br#"{"finish": "not an instant"}"#).is_err());
    }

    #[test]
    fn json_content_type_match() {
        assert!(is_json(Some(&HeaderValue::from_static("application/json"))));
        assert!(is_json(Some(&HeaderValue::from_static(
            "Application/JSON; charset=utf-8"
        ))));
        assert!(!is_json(Some(&HeaderValue::from_static("text/plain"))));
        assert!(!is_json(None));
    }
}

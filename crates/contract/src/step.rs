//! The scenario's building block: a [`Step`], one request described as data, and its resolution.
//!
//! A step holds the method, path, query, headers, body, auth mode, compare mode, volatile pointers and captures.
//! Placeholders `{{name}}` in paths, query values and JSON bodies are filled from the [`State`] the earlier steps
//! recorded ([`resolve`]), and [`capture`] records what a response gives later steps.

use crate::auth;
use crate::cassette::{Headers, RecordedBody, RecordedPart, RecordedRequest};
use anyhow::{anyhow, bail, Context, Result};
use base64::engine::general_purpose::STANDARD;
use base64::Engine;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::BTreeMap;
use std::fmt;

/// Every route lives under this prefix; relative step paths are joined to it.
pub const BASE_PATH: &str = "/v1/api";
/// The placeholder written to cassettes in place of the live admin token.
pub const TOKEN_PLACEHOLDER: &str = "{{token}}";
/// The fixed boundary of every multipart body the harness sends.
pub const MULTIPART_BOUNDARY: &str = "----regatta-contract-boundary";
/// The capture name the login step stores the bearer token under.
pub const TOKEN_CAPTURE: &str = "token";

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "UPPERCASE")]
pub enum Method {
    Get,
    Post,
    Put,
    Delete,
}

impl Method {
    pub fn as_str(self) -> &'static str {
        match self {
            Method::Get => "GET",
            Method::Post => "POST",
            Method::Put => "PUT",
            Method::Delete => "DELETE",
        }
    }
}

impl fmt::Display for Method {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.as_str())
    }
}

/// One part of a multipart body.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Part {
    pub name: String,
    pub filename: Option<String>,
    pub content_type: String,
    pub content: PartContent,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum PartContent {
    Text(String),
    Bytes(Vec<u8>),
}

/// What a step sends.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Body {
    None,
    /// `application/json`; strings `{{name}}` are replaced by the captured value (keeping its type).
    Json(Value),
    /// Any content type with a text body (e.g. JSON under `text/plain`, or an empty JSON body).
    Text {
        content_type: String,
        text: String,
    },
    /// Any content type with raw bytes.
    Raw {
        content_type: String,
        bytes: Vec<u8>,
    },
    /// `multipart/form-data` with the fixed boundary.
    Multipart(Vec<Part>),
    /// A value captured from an earlier response, sent back as JSON with pointer overrides applied.
    Captured {
        capture: String,
        overrides: Vec<(String, Value)>,
    },
}

/// How the `Authorization` header is built.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AuthMode {
    /// No `Authorization` header (public routes, and "missing header" on admin routes).
    None,
    /// `Bearer <live token>` from the login step.
    Admin,
    /// A literal header value (`Bearer none`, malformed base64, ...).
    Raw(String),
    /// A token built by the harness from the stored admin hash.
    Forged(Forge),
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Forge {
    /// The auth id: the captured one, or a fixed number (an unknown id).
    pub id: ForgeId,
    pub salt: String,
    pub expires: String,
    /// Signed as the server would (`true`) or with a wrong hash.
    pub correct: bool,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ForgeId {
    Captured,
    Fixed(i64),
}

/// How a response is compared.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Compare {
    /// Status, compared headers, body (JSON parsed and canonical bytes; text/binary exact).
    Full,
    /// Fallback routes: status and media type only.
    StatusAndMediaType,
    /// Like `Full`, with the step's volatile JSON pointers compared by type only.
    JsonWithVolatile,
}

/// A value taken from a response for later steps.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Capture {
    pub name: String,
    pub from: CaptureFrom,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum CaptureFrom {
    /// The JSON value at this pointer.
    Pointer(String),
    /// The bearer token: base64 of the raw response body (a `LoginResponse`).
    TokenFromBody,
    /// The element of a top-level array whose `key` equals `equals`.
    Find { key: String, equals: String },
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Step {
    pub name: String,
    pub method: Method,
    /// Relative to `/v1/api` (`series`, `image/x.png`) or absolute when it starts with `/`.
    pub path: String,
    pub query: Vec<(String, String)>,
    pub headers: Vec<(String, String)>,
    pub body: Body,
    pub auth: AuthMode,
    pub compare: Compare,
    pub volatile: Vec<String>,
    pub captures: Vec<Capture>,
}

impl Step {
    pub fn new(name: &str, method: Method, path: &str) -> Step {
        Step {
            name: name.to_string(),
            method,
            path: path.to_string(),
            query: Vec::new(),
            headers: Vec::new(),
            body: Body::None,
            auth: AuthMode::None,
            compare: Compare::Full,
            volatile: Vec::new(),
            captures: Vec::new(),
        }
    }

    pub fn query(mut self, key: &str, value: &str) -> Step {
        self.query.push((key.to_string(), value.to_string()));
        self
    }

    pub fn header(mut self, key: &str, value: &str) -> Step {
        self.headers.push((key.to_string(), value.to_string()));
        self
    }

    pub fn json(mut self, value: Value) -> Step {
        self.body = Body::Json(value);
        self
    }

    pub fn text(mut self, content_type: &str, text: &str) -> Step {
        self.body = Body::Text {
            content_type: content_type.to_string(),
            text: text.to_string(),
        };
        self
    }

    pub fn multipart(mut self, parts: Vec<Part>) -> Step {
        self.body = Body::Multipart(parts);
        self
    }

    pub fn captured_body(mut self, capture: &str, overrides: Vec<(&str, Value)>) -> Step {
        self.body = Body::Captured {
            capture: capture.to_string(),
            overrides: overrides
                .into_iter()
                .map(|(p, v)| (p.to_string(), v))
                .collect(),
        };
        self
    }

    pub fn admin(mut self) -> Step {
        self.auth = AuthMode::Admin;
        self
    }

    pub fn auth(mut self, auth: AuthMode) -> Step {
        self.auth = auth;
        self
    }

    pub fn compare(mut self, compare: Compare) -> Step {
        self.compare = compare;
        self
    }

    /// Compared by status and media type only (the SPA fallback).
    pub fn fallback(self) -> Step {
        self.compare(Compare::StatusAndMediaType)
    }

    pub fn volatile(mut self, pointers: &[&str]) -> Step {
        self.volatile.extend(pointers.iter().map(|p| p.to_string()));
        self.compare = Compare::JsonWithVolatile;
        self
    }

    pub fn capture(mut self, name: &str, pointer: &str) -> Step {
        self.captures.push(Capture {
            name: name.to_string(),
            from: CaptureFrom::Pointer(pointer.to_string()),
        });
        self
    }

    pub fn capture_token(mut self) -> Step {
        self.captures.push(Capture {
            name: TOKEN_CAPTURE.to_string(),
            from: CaptureFrom::TokenFromBody,
        });
        self
    }

    pub fn capture_find(mut self, name: &str, key: &str, equals: &str) -> Step {
        self.captures.push(Capture {
            name: name.to_string(),
            from: CaptureFrom::Find {
                key: key.to_string(),
                equals: equals.to_string(),
            },
        });
        self
    }

    /// Returns the [`ROUTES`](crate::scenario::ROUTES) entry this step exercises, `None` for absolute (fallback) paths.
    pub fn route(&self) -> Option<String> {
        if self.path.starts_with('/') {
            None
        } else if self.path.starts_with("image") && self.method == Method::Get {
            // The download route is one segment, `image{name}`: `/v1/api/imagecontract.png`
            // (`GET image` matches it with an empty name; `POST image` is the upload route).
            Some("image{name}".to_string())
        } else {
            Some(self.path.clone())
        }
    }
}

/// Starts a `GET` step named `name` (the cassette name) for `path`, relative to `/v1/api` unless it starts with `/`.
pub fn get(name: &str, path: &str) -> Step {
    Step::new(name, Method::Get, path)
}
/// Starts a `POST` step; see [`get`].
pub fn post(name: &str, path: &str) -> Step {
    Step::new(name, Method::Post, path)
}
/// Starts a `PUT` step; see [`get`].
pub fn put(name: &str, path: &str) -> Step {
    Step::new(name, Method::Put, path)
}
/// Starts a `DELETE` step; see [`get`].
pub fn delete(name: &str, path: &str) -> Step {
    Step::new(name, Method::Delete, path)
}

/// What earlier steps recorded: captured values and the admin's stored hash.
#[derive(Debug, Clone, Default)]
pub struct State {
    pub captures: BTreeMap<String, Value>,
    /// `hash(password)` of the admin the scenario creates; forged tokens are signed with it.
    pub stored_hash: String,
}

impl State {
    pub fn new(stored_hash: &str) -> State {
        State {
            captures: BTreeMap::new(),
            stored_hash: stored_hash.to_string(),
        }
    }

    pub fn get(&self, name: &str) -> Result<&Value> {
        self.captures.get(name).ok_or_else(|| {
            anyhow!("no captured value named {name:?}; an earlier step did not record it")
        })
    }

    pub fn token(&self) -> Result<&str> {
        self.get(TOKEN_CAPTURE)?
            .as_str()
            .ok_or_else(|| anyhow!("the captured token is not a string"))
    }

    /// Replaces every `{{name}}` in `text` with the captured value's text form.
    ///
    /// # Errors
    ///
    /// Returns `Err` for an unterminated placeholder or a name no earlier step captured.
    pub fn fill_text(&self, text: &str) -> Result<String> {
        let mut out = String::new();
        let mut rest = text;
        while let Some(start) = rest.find("{{") {
            out.push_str(&rest[..start]);
            let after = &rest[start + 2..];
            let end = after
                .find("}}")
                .ok_or_else(|| anyhow!("unterminated placeholder in {text:?}"))?;
            let name = &after[..end];
            out.push_str(&value_text(self.get(name)?));
            rest = &after[end + 2..];
        }
        out.push_str(rest);
        Ok(out)
    }

    /// Replaces placeholders in a JSON value, keeping a captured number a number.
    ///
    /// A string that is exactly `{{name}}` becomes the captured value itself; other strings are filled textually
    /// ([`State::fill_text`]).
    ///
    /// # Errors
    ///
    /// Returns `Err` for an unterminated placeholder or a name no earlier step captured.
    pub fn fill_json(&self, value: &Value) -> Result<Value> {
        Ok(match value {
            Value::String(s) => {
                if let Some(name) = s.strip_prefix("{{").and_then(|s| s.strip_suffix("}}")) {
                    if !name.contains("{{") {
                        return Ok(self.get(name)?.clone());
                    }
                }
                Value::String(self.fill_text(s)?)
            }
            Value::Array(items) => Value::Array(
                items
                    .iter()
                    .map(|v| self.fill_json(v))
                    .collect::<Result<_>>()?,
            ),
            Value::Object(map) => {
                let mut out = serde_json::Map::new();
                for (k, v) in map {
                    out.insert(k.clone(), self.fill_json(v)?);
                }
                Value::Object(out)
            }
            other => other.clone(),
        })
    }
}

fn value_text(value: &Value) -> String {
    match value {
        Value::String(s) => s.clone(),
        other => other.to_string(),
    }
}

/// A step with its placeholders filled: what goes on the wire, and its cassette form.
#[derive(Debug, Clone)]
pub struct ResolvedRequest {
    pub method: Method,
    /// Path and query, e.g. `/v1/api/series?id=1`.
    pub url: String,
    pub headers: Vec<(String, String)>,
    pub body: Option<Vec<u8>>,
    /// The request as written to the cassette (token replaced by the placeholder).
    pub recorded: RecordedRequest,
}

/// Percent-encodes a query value (RFC 3986 unreserved characters pass through).
pub fn encode_query(value: &str) -> String {
    let mut out = String::new();
    for byte in value.bytes() {
        match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(byte as char)
            }
            _ => out.push_str(&format!("%{byte:02X}")),
        }
    }
    out
}

/// Fills the step's placeholders and builds the wire request.
///
/// # Errors
///
/// Returns `Err` when a placeholder names a value no earlier step captured, or the body cannot be built.
pub fn resolve(step: &Step, state: &State) -> Result<ResolvedRequest> {
    let path = state.fill_text(&step.path)?;
    let mut url = if path.starts_with('/') {
        path
    } else {
        format!("{BASE_PATH}/{path}")
    };
    for (i, (key, value)) in step.query.iter().enumerate() {
        url.push(if i == 0 { '?' } else { '&' });
        url.push_str(&encode_query(key));
        url.push('=');
        url.push_str(&encode_query(&state.fill_text(value)?));
    }

    let mut headers: Vec<(String, String)> = Vec::new();
    let mut recorded_headers = Headers::new();
    let (bytes, recorded_body, content_type) = encode_body(&step.body, state)?;
    if let Some(ct) = content_type {
        headers.push(("content-type".to_string(), ct.clone()));
        recorded_headers.insert("content-type".to_string(), Value::String(ct));
    }
    let auth_header = match &step.auth {
        AuthMode::None => None,
        AuthMode::Admin => Some((
            format!("Bearer {}", state.token()?),
            format!("Bearer {TOKEN_PLACEHOLDER}"),
        )),
        AuthMode::Raw(value) => Some((value.clone(), value.clone())),
        AuthMode::Forged(forge) => {
            let id = match forge.id {
                ForgeId::Captured => state
                    .get("auth_id")?
                    .as_i64()
                    .ok_or_else(|| anyhow!("auth_id is not a number"))?,
                ForgeId::Fixed(id) => id,
            };
            let token = auth::forge_token(
                id,
                &state.stored_hash,
                &forge.salt,
                &forge.expires,
                forge.correct,
            )?;
            let value = format!("Bearer {token}");
            Some((value.clone(), value))
        }
    };
    if let Some((live, recorded)) = auth_header {
        headers.push(("authorization".to_string(), live));
        recorded_headers.insert("authorization".to_string(), Value::String(recorded));
    }
    for (key, value) in &step.headers {
        let value = state.fill_text(value)?;
        headers.push((key.to_lowercase(), value.clone()));
        recorded_headers.insert(key.to_lowercase(), Value::String(value));
    }

    Ok(ResolvedRequest {
        method: step.method,
        url: url.clone(),
        headers,
        body: bytes,
        recorded: RecordedRequest {
            method: step.method.to_string(),
            url,
            headers: recorded_headers,
            body: recorded_body,
        },
    })
}

type EncodedBody = (Option<Vec<u8>>, Option<RecordedBody>, Option<String>);

fn encode_body(body: &Body, state: &State) -> Result<EncodedBody> {
    Ok(match body {
        Body::None => (None, None, None),
        Body::Json(value) => {
            let filled = state.fill_json(value)?;
            let bytes = serde_json::to_vec(&filled)?;
            (
                Some(bytes),
                Some(RecordedBody::Json(filled)),
                Some("application/json".to_string()),
            )
        }
        Body::Captured { capture, overrides } => {
            let mut value = state.get(capture)?.clone();
            for (pointer, replacement) in overrides {
                match value.pointer_mut(pointer) {
                    Some(slot) => *slot = replacement.clone(),
                    None => {
                        // A new key: the pointer's parent must be an object.
                        let (parent, key) = pointer
                            .rsplit_once('/')
                            .ok_or_else(|| anyhow!("bad override pointer {pointer}"))?;
                        let parent_value = value
                            .pointer_mut(parent)
                            .and_then(Value::as_object_mut)
                            .ok_or_else(|| {
                                anyhow!("override pointer {pointer} has no object parent")
                            })?;
                        parent_value.insert(key.to_string(), replacement.clone());
                    }
                }
            }
            let bytes = serde_json::to_vec(&value)?;
            (
                Some(bytes),
                Some(RecordedBody::Json(value)),
                Some("application/json".to_string()),
            )
        }
        Body::Text { content_type, text } => {
            let text = state.fill_text(text)?;
            (
                Some(text.clone().into_bytes()),
                Some(RecordedBody::Text(text)),
                Some(content_type.clone()),
            )
        }
        Body::Raw {
            content_type,
            bytes,
        } => (
            Some(bytes.clone()),
            Some(RecordedBody::Base64(STANDARD.encode(bytes))),
            Some(content_type.clone()),
        ),
        Body::Multipart(parts) => {
            let mut bytes = Vec::new();
            let mut recorded = Vec::new();
            for part in parts {
                bytes.extend_from_slice(format!("--{MULTIPART_BOUNDARY}\r\n").as_bytes());
                let mut disposition =
                    format!("Content-Disposition: form-data; name=\"{}\"", part.name);
                if let Some(filename) = &part.filename {
                    disposition.push_str(&format!("; filename=\"{filename}\""));
                }
                bytes.extend_from_slice(
                    format!(
                        "{disposition}\r\nContent-Type: {}\r\n\r\n",
                        part.content_type
                    )
                    .as_bytes(),
                );
                let (content, recorded_part) = match &part.content {
                    PartContent::Text(text) => (
                        text.as_bytes().to_vec(),
                        RecordedPart {
                            name: part.name.clone(),
                            filename: part.filename.clone(),
                            content_type: part.content_type.clone(),
                            text: Some(text.clone()),
                            base64: None,
                        },
                    ),
                    PartContent::Bytes(data) => (
                        data.clone(),
                        RecordedPart {
                            name: part.name.clone(),
                            filename: part.filename.clone(),
                            content_type: part.content_type.clone(),
                            text: None,
                            base64: Some(STANDARD.encode(data)),
                        },
                    ),
                };
                bytes.extend_from_slice(&content);
                bytes.extend_from_slice(b"\r\n");
                recorded.push(recorded_part);
            }
            bytes.extend_from_slice(format!("--{MULTIPART_BOUNDARY}--\r\n").as_bytes());
            (
                Some(bytes),
                Some(RecordedBody::Multipart(recorded)),
                Some(format!(
                    "multipart/form-data; boundary={MULTIPART_BOUNDARY}"
                )),
            )
        }
    })
}

/// Applies a step's captures to the response it got.
///
/// # Errors
///
/// Returns `Err` when a capture cannot be made from the response (a later step would need it).
pub fn capture(
    step: &Step,
    status: u16,
    raw_body: &[u8],
    json: Option<&Value>,
    state: &mut State,
) -> Result<()> {
    for cap in &step.captures {
        let value = match &cap.from {
            CaptureFrom::TokenFromBody => {
                if raw_body.is_empty() {
                    bail!("step {:?} answered {status} with an empty body; no token to capture", step.name);
                }
                Value::String(auth::encode_token(raw_body))
            }
            CaptureFrom::Pointer(pointer) => json
                .and_then(|j| j.pointer(pointer))
                .cloned()
                .with_context(|| format!("step {:?} answered {status}; nothing at {pointer} to capture as {}", step.name, cap.name))?,
            CaptureFrom::Find { key, equals } => json
                .and_then(Value::as_array)
                .and_then(|items| items.iter().find(|item| item.get(key).and_then(Value::as_str) == Some(equals)))
                .cloned()
                .with_context(|| format!("step {:?} answered {status}; no element with {key}={equals:?} to capture as {}", step.name, cap.name))?,
        };
        state.captures.insert(cap.name.clone(), value);
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn state() -> State {
        let mut state = State::new(&auth::stored_hash("pw"));
        state.captures.insert("series_id".into(), json!(42));
        state.captures.insert("name".into(), json!("Harbor"));
        state.captures.insert(TOKEN_CAPTURE.into(), json!("TOKEN"));
        state.captures.insert("auth_id".into(), json!(4));
        state
    }

    #[test]
    fn placeholders_fill_text_and_keep_json_types() {
        let s = state();
        assert_eq!(
            s.fill_text("series?id={{series_id}}&n={{name}}").unwrap(),
            "series?id=42&n=Harbor"
        );
        let filled = s
            .fill_json(
                &json!({"id": "{{series_id}}", "label": "x-{{name}}", "list": ["{{series_id}}"]}),
            )
            .unwrap();
        assert_eq!(filled, json!({"id": 42, "label": "x-Harbor", "list": [42]}));
        assert!(s.fill_text("{{missing}}").is_err());
    }

    #[test]
    fn resolve_builds_url_headers_and_placeholder() {
        let step = get("x", "find/series")
            .query("name", "Harbor Series")
            .admin();
        let r = resolve(&step, &state()).unwrap();
        assert_eq!(r.url, "/v1/api/find/series?name=Harbor%20Series");
        assert_eq!(
            r.headers,
            vec![("authorization".to_string(), "Bearer TOKEN".to_string())]
        );
        assert_eq!(r.recorded.headers["authorization"], "Bearer {{token}}");
        assert!(r.body.is_none());

        let abs = put("y", "/v1/api/series");
        assert_eq!(resolve(&abs, &state()).unwrap().url, "/v1/api/series");
    }

    #[test]
    fn resolve_json_body_sets_content_type() {
        let step = post("x", "series").json(json!([{"id": "{{series_id}}"}]));
        let r = resolve(&step, &state()).unwrap();
        assert_eq!(
            r.headers[0],
            ("content-type".to_string(), "application/json".to_string())
        );
        assert_eq!(r.body.unwrap(), br#"[{"id":42}]"#.to_vec());
        assert_eq!(
            r.recorded.body,
            Some(RecordedBody::Json(json!([{"id": 42}])))
        );
    }

    #[test]
    fn resolve_forged_token_uses_captured_id() {
        let step = get("x", "allSkippers").auth(AuthMode::Forged(Forge {
            id: ForgeId::Captured,
            salt: "s".into(),
            expires: "2099-01-01T00:00:00Z".into(),
            correct: true,
        }));
        let r = resolve(&step, &state()).unwrap();
        let token = r.headers[0].1.strip_prefix("Bearer ").unwrap().to_string();
        assert_eq!(
            auth::validate_token(&token, &auth::stored_hash("pw")).unwrap(),
            4
        );
        assert_eq!(
            r.recorded.headers["authorization"],
            format!("Bearer {token}")
        );
    }

    #[test]
    fn multipart_body_is_deterministic() {
        let step = post("x", "image").multipart(vec![
            Part {
                name: "image".into(),
                filename: Some("a.png".into()),
                content_type: "image/png".into(),
                content: PartContent::Bytes(vec![1, 2, 3]),
            },
            Part {
                name: "note".into(),
                filename: None,
                content_type: "text/plain".into(),
                content: PartContent::Text("ignored".into()),
            },
        ]);
        let r = resolve(&step, &state()).unwrap();
        let body = String::from_utf8_lossy(r.body.as_ref().unwrap()).to_string();
        assert_eq!(
            body,
            "------regatta-contract-boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"a.png\"\r\nContent-Type: image/png\r\n\r\n\u{1}\u{2}\u{3}\r\n------regatta-contract-boundary\r\nContent-Disposition: form-data; name=\"note\"\r\nContent-Type: text/plain\r\n\r\nignored\r\n------regatta-contract-boundary--\r\n"
        );
        assert_eq!(
            r.headers[0].1,
            "multipart/form-data; boundary=----regatta-contract-boundary"
        );
        let empty = post("y", "image").multipart(vec![]);
        let r = resolve(&empty, &state()).unwrap();
        assert_eq!(
            r.body.unwrap(),
            b"------regatta-contract-boundary--\r\n".to_vec()
        );
    }

    #[test]
    fn captured_body_applies_overrides() {
        let mut s = state();
        s.captures
            .insert("cert".into(), json!({"RefNo": "A", "APHD": 1.5}));
        let step = post("x", "orcCert")
            .captured_body("cert", vec![("/RefNo", json!("B")), ("/bogus", json!(1))]);
        let r = resolve(&step, &s).unwrap();
        assert_eq!(
            r.recorded.body,
            Some(RecordedBody::Json(
                json!({"RefNo": "B", "APHD": 1.5, "bogus": 1})
            ))
        );
    }

    #[test]
    fn captures_pointer_find_and_token() {
        let mut s = state();
        let step = post("x", "series")
            .capture("id", "/0/id")
            .capture_find("cert", "RefNo", "B")
            .capture_token();
        let body = br#"[{"id":7,"RefNo":"A"},{"id":8,"RefNo":"B"}]"#;
        let json: Value = serde_json::from_slice(body).unwrap();
        capture(&step, 200, body, Some(&json), &mut s).unwrap();
        assert_eq!(s.captures["id"], json!(7));
        assert_eq!(s.captures["cert"], json!({"id":8,"RefNo":"B"}));
        assert_eq!(s.token().unwrap(), STANDARD.encode(body));
        let missing = get("y", "series").capture("nope", "/nope");
        assert!(capture(&missing, 204, b"", None, &mut s).is_err());
    }

    #[test]
    fn route_of_step() {
        assert_eq!(get("a", "series").route().as_deref(), Some("series"));
        assert_eq!(
            get("a", "imagex.png").route().as_deref(),
            Some("image{name}")
        );
        assert_eq!(get("a", "image").route().as_deref(), Some("image{name}"));
        assert_eq!(post("a", "image").route().as_deref(), Some("image"));
        assert_eq!(get("a", "/no/such").route(), None);
    }
}

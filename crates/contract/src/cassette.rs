//! Cassettes: one pretty-printed JSON file per step, `NNN-<name>.json`, holding the request as
//! sent (the live admin token replaced by `{{token}}`) and the response as received, plus the
//! step's compare mode and volatile pointers.

use crate::step::{AuthMode, Compare};
use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::fs;
use std::path::{Path, PathBuf};

/// Header name (lower case) to value; repeated headers are joined with `, `.
pub type Headers = serde_json::Map<String, Value>;

/// Case-insensitive header lookup.
pub fn header<'a>(headers: &'a Headers, name: &str) -> Option<&'a str> {
    let name = name.to_ascii_lowercase();
    headers.get(&name).and_then(Value::as_str)
}

/// One multipart part as recorded.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RecordedPart {
    pub name: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub filename: Option<String>,
    pub content_type: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub text: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub base64: Option<String>,
}

/// A body as written to a cassette: `{"json": ...}`, `{"text": "..."}`, `{"base64": "..."}` or
/// `{"multipart": [...]}`; `null` when there was none.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum RecordedBody {
    Json(Value),
    Text(String),
    Base64(String),
    Multipart(Vec<RecordedPart>),
}

impl RecordedBody {
    pub fn kind(&self) -> &'static str {
        match self {
            RecordedBody::Json(_) => "json",
            RecordedBody::Text(_) => "text",
            RecordedBody::Base64(_) => "binary",
            RecordedBody::Multipart(_) => "multipart",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RecordedRequest {
    pub method: String,
    pub url: String,
    pub headers: Headers,
    pub body: Option<RecordedBody>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RecordedResponse {
    pub status: u16,
    pub headers: Headers,
    pub body: Option<RecordedBody>,
}

impl RecordedResponse {
    pub fn header(&self, name: &str) -> Option<&str> {
        header(&self.headers, name)
    }

    pub fn json(&self) -> Option<&Value> {
        match &self.body {
            Some(RecordedBody::Json(v)) => Some(v),
            _ => None,
        }
    }

    /// Classifies raw body bytes by content type: JSON when the type says so and it parses, text
    /// when UTF-8 under a textual type, binary (base64) otherwise, none when empty.
    pub fn from_parts(status: u16, headers: Headers, bytes: &[u8]) -> RecordedResponse {
        let content_type = header(&headers, "content-type")
            .unwrap_or("")
            .to_ascii_lowercase();
        let body = if bytes.is_empty() {
            None
        } else if content_type.starts_with("application/json") {
            match serde_json::from_slice::<Value>(bytes) {
                Ok(v) => Some(RecordedBody::Json(v)),
                Err(_) => Some(text_or_binary(bytes)),
            }
        } else if content_type.starts_with("text/")
            || content_type.starts_with("application/xml")
            || content_type.starts_with("application/javascript")
            || content_type.is_empty()
        {
            Some(text_or_binary(bytes))
        } else {
            Some(RecordedBody::Base64(base64_encode(bytes)))
        };
        RecordedResponse {
            status,
            headers,
            body,
        }
    }
}

fn text_or_binary(bytes: &[u8]) -> RecordedBody {
    match std::str::from_utf8(bytes) {
        Ok(text) => RecordedBody::Text(text.to_string()),
        Err(_) => RecordedBody::Base64(base64_encode(bytes)),
    }
}

pub fn base64_encode(bytes: &[u8]) -> String {
    use base64::Engine;
    base64::engine::general_purpose::STANDARD.encode(bytes)
}

pub fn base64_decode(text: &str) -> Result<Vec<u8>> {
    use base64::Engine;
    base64::engine::general_purpose::STANDARD
        .decode(text)
        .context("cassette body is not base64")
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Cassette {
    /// 1-based position in the scenario.
    pub index: usize,
    pub name: String,
    pub auth: AuthMode,
    pub compare: Compare,
    pub volatile: Vec<String>,
    pub request: RecordedRequest,
    pub response: RecordedResponse,
}

impl Cassette {
    pub fn file_name(index: usize, name: &str) -> String {
        format!("{index:03}-{name}.json")
    }

    pub fn path(&self, dir: &Path) -> PathBuf {
        dir.join(Cassette::file_name(self.index, &self.name))
    }

    pub fn write(&self, dir: &Path) -> Result<PathBuf> {
        fs::create_dir_all(dir).with_context(|| format!("creating {}", dir.display()))?;
        let path = self.path(dir);
        let mut text = serde_json::to_string_pretty(self)?;
        text.push('\n');
        fs::write(&path, text).with_context(|| format!("writing {}", path.display()))?;
        Ok(path)
    }

    pub fn read(path: &Path) -> Result<Cassette> {
        let text =
            fs::read_to_string(path).with_context(|| format!("reading {}", path.display()))?;
        serde_json::from_str(&text).with_context(|| format!("parsing {}", path.display()))
    }

    /// Reads the cassette of step `index` named `name` from `dir`.
    pub fn read_step(dir: &Path, index: usize, name: &str) -> Result<Cassette> {
        let path = dir.join(Cassette::file_name(index, name));
        if !path.exists() {
            return Err(anyhow!(
                "no cassette {} (record the scenario first, or the scenario changed since it was recorded)",
                path.display()
            ));
        }
        Cassette::read(&path)
    }
}

/// `_meta.json`: how the cassette set was recorded.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Meta {
    pub base_url: String,
    pub recorded_at: String,
    /// The fixed clock the recorded server ran with, if any (`-Dregatta.now` / `REGATTA_FIXED_NOW`).
    pub fixed_now: Option<String>,
    pub steps: usize,
}

impl Meta {
    pub const FILE: &'static str = "_meta.json";

    pub fn write(&self, dir: &Path) -> Result<()> {
        fs::create_dir_all(dir)?;
        let mut text = serde_json::to_string_pretty(self)?;
        text.push('\n');
        fs::write(dir.join(Meta::FILE), text)?;
        Ok(())
    }

    pub fn read(dir: &Path) -> Result<Option<Meta>> {
        let path = dir.join(Meta::FILE);
        if !path.exists() {
            return Ok(None);
        }
        let text = fs::read_to_string(&path)?;
        Ok(Some(
            serde_json::from_str(&text).with_context(|| format!("parsing {}", path.display()))?,
        ))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn headers(pairs: &[(&str, &str)]) -> Headers {
        pairs
            .iter()
            .map(|(k, v)| (k.to_string(), Value::String(v.to_string())))
            .collect()
    }

    #[test]
    fn classifies_bodies() {
        let r = RecordedResponse::from_parts(
            200,
            headers(&[("content-type", "application/json; charset=UTF-8")]),
            br#"{"a":1}"#,
        );
        assert_eq!(r.body, Some(RecordedBody::Json(json!({"a": 1}))));
        let r = RecordedResponse::from_parts(
            200,
            headers(&[("content-type", "application/json")]),
            b"not json",
        );
        assert_eq!(r.body, Some(RecordedBody::Text("not json".into())));
        let r = RecordedResponse::from_parts(
            400,
            headers(&[("content-type", "text/plain; charset=UTF-8")]),
            b"oops",
        );
        assert_eq!(r.body, Some(RecordedBody::Text("oops".into())));
        let r = RecordedResponse::from_parts(
            200,
            headers(&[("content-type", "image/png")]),
            &[0x89, b'P'],
        );
        assert_eq!(r.body, Some(RecordedBody::Base64("iVA=".into())));
        let r = RecordedResponse::from_parts(204, headers(&[]), b"");
        assert_eq!(r.body, None);
        assert_eq!(r.header("Content-Type"), None);
    }

    #[test]
    fn cassette_round_trips_through_a_file() {
        let dir =
            std::env::temp_dir().join(format!("regatta-contract-cassette-{}", std::process::id()));
        let cassette = Cassette {
            index: 12,
            name: "login".into(),
            auth: AuthMode::Raw("Bearer none".into()),
            compare: Compare::JsonWithVolatile,
            volatile: vec!["/salt".into()],
            request: RecordedRequest {
                method: "POST".into(),
                url: "/v1/api/login".into(),
                headers: headers(&[
                    ("content-type", "application/json"),
                    ("authorization", "Bearer {{token}}"),
                ]),
                body: Some(RecordedBody::Json(
                    json!({"userName": "contract", "z": 1, "a": 2}),
                )),
            },
            response: RecordedResponse {
                status: 200,
                headers: headers(&[
                    ("content-type", "application/json; charset=UTF-8"),
                    ("date", "x"),
                ]),
                body: Some(RecordedBody::Json(json!({"id": 4, "salt": "s"}))),
            },
        };
        let path = cassette.write(&dir).unwrap();
        assert_eq!(path.file_name().unwrap(), "012-login.json");
        let text = fs::read_to_string(&path).unwrap();
        assert!(
            text.contains("\n  \"request\": {"),
            "pretty printed: {text}"
        );
        // Key order survives (preserve_order).
        assert!(text.find("\"z\"").unwrap() < text.find("\"a\"").unwrap());
        let back = Cassette::read_step(&dir, 12, "login").unwrap();
        assert_eq!(back, cassette);
        assert!(Cassette::read_step(&dir, 13, "nope").is_err());
        let meta = Meta {
            base_url: "http://x".into(),
            recorded_at: "now".into(),
            fixed_now: None,
            steps: 1,
        };
        meta.write(&dir).unwrap();
        assert_eq!(Meta::read(&dir).unwrap(), Some(meta));
        fs::remove_dir_all(&dir).unwrap();
    }
}

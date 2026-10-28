//! Response comparison for replay and diff: what counts as the same answer, as [`Mismatch`]es.
//!
//! Status exact; `Content-Type` (and `WWW-Authenticate`) as exact strings; JSON bodies equal as parsed values **and**
//! byte for byte after canonical re-serialisation, with key-order differences reported apart from value differences;
//! text and binary bodies exact; volatile JSON pointers compared by type only; fallback steps by status and media
//! type. `Content-Encoding`, `Content-Length`, `Date` and `Server` are recorded, never compared. The rules are T2 in
//! `docs/explore_implemented/rust_port.md`.

use crate::cassette::{base64_decode, RecordedBody, RecordedRequest, RecordedResponse};
use crate::step::Compare;
use serde_json::Value;
use std::fmt;

/// Response headers compared as exact strings when a step is compared in full.
pub const COMPARED_HEADERS: &[&str] = &["content-type", "www-authenticate"];
/// Response headers recorded for the reader, never compared.
pub const RECORDED_ONLY_HEADERS: &[&str] =
    &["content-encoding", "content-length", "date", "server"];

const MAX_LISTED: usize = 40;
const MAX_DIFF_LINES: usize = 80;

#[derive(Debug, Clone, PartialEq)]
pub enum ValueDiffKind {
    /// Present in the expected (left) side only.
    Missing,
    /// Present in the actual (right) side only.
    Extra,
    Length {
        expected: usize,
        actual: usize,
    },
    Value {
        expected: String,
        actual: String,
    },
}

#[derive(Debug, Clone, PartialEq)]
pub struct ValueDiff {
    pub pointer: String,
    pub kind: ValueDiffKind,
}

#[derive(Debug, Clone, PartialEq)]
pub struct OrderDiff {
    pub pointer: String,
    pub expected: Vec<String>,
    pub actual: Vec<String>,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Mismatch {
    Status {
        expected: u16,
        actual: u16,
    },
    Header {
        name: String,
        expected: Option<String>,
        actual: Option<String>,
    },
    MediaType {
        expected: Option<String>,
        actual: Option<String>,
    },
    BodyKind {
        expected: String,
        actual: String,
    },
    Text {
        diff: String,
    },
    Binary {
        expected_len: usize,
        actual_len: usize,
        first_difference: Option<usize>,
    },
    JsonValue(Vec<ValueDiff>),
    JsonOrder(Vec<OrderDiff>),
    Volatile {
        pointer: String,
        expected: String,
        actual: String,
    },
    /// The request the scenario builds now differs from the recorded one.
    RequestDrift(Vec<String>),
    Error(String),
}

impl fmt::Display for Mismatch {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Mismatch::Status { expected, actual } => {
                write!(f, "status: expected {expected}, got {actual}")
            }
            Mismatch::Header {
                name,
                expected,
                actual,
            } => {
                write!(
                    f,
                    "header {name}: expected {}, got {}",
                    show(expected),
                    show(actual)
                )
            }
            Mismatch::MediaType { expected, actual } => {
                write!(
                    f,
                    "media type: expected {}, got {}",
                    show(expected),
                    show(actual)
                )
            }
            Mismatch::BodyKind { expected, actual } => {
                write!(f, "body: expected {expected}, got {actual}")
            }
            Mismatch::Text { diff } => write!(f, "text body differs:\n{diff}"),
            Mismatch::Binary {
                expected_len,
                actual_len,
                first_difference,
            } => write!(
                f,
                "binary body differs: expected {expected_len} bytes, got {actual_len}{}",
                first_difference
                    .map(|i| format!(", first difference at byte {i}"))
                    .unwrap_or_default()
            ),
            Mismatch::JsonValue(diffs) => {
                writeln!(f, "json values differ ({} places):", diffs.len())?;
                for d in diffs.iter().take(MAX_LISTED) {
                    match &d.kind {
                        ValueDiffKind::Missing => writeln!(f, "  {}: missing", d.pointer)?,
                        ValueDiffKind::Extra => writeln!(f, "  {}: unexpected", d.pointer)?,
                        ValueDiffKind::Length { expected, actual } => writeln!(
                            f,
                            "  {}: expected {expected} elements, got {actual}",
                            d.pointer
                        )?,
                        ValueDiffKind::Value { expected, actual } => {
                            writeln!(f, "  {}: expected {expected}, got {actual}", d.pointer)?
                        }
                    }
                }
                if diffs.len() > MAX_LISTED {
                    writeln!(f, "  ... {} more", diffs.len() - MAX_LISTED)?;
                }
                Ok(())
            }
            Mismatch::JsonOrder(diffs) => {
                writeln!(
                    f,
                    "json key order differs (values equal) at {} objects:",
                    diffs.len()
                )?;
                for d in diffs.iter().take(MAX_LISTED) {
                    writeln!(
                        f,
                        "  {}: expected [{}], got [{}]",
                        if d.pointer.is_empty() {
                            "/"
                        } else {
                            &d.pointer
                        },
                        d.expected.join(", "),
                        d.actual.join(", ")
                    )?;
                }
                if diffs.len() > MAX_LISTED {
                    writeln!(f, "  ... {} more", diffs.len() - MAX_LISTED)?;
                }
                Ok(())
            }
            Mismatch::Volatile {
                pointer,
                expected,
                actual,
            } => {
                write!(f, "volatile {pointer}: expected a {expected}, got {actual}")
            }
            Mismatch::RequestDrift(lines) => {
                writeln!(
                    f,
                    "the scenario's request differs from the recorded one (re-record?):"
                )?;
                for line in lines {
                    writeln!(f, "  {line}")?;
                }
                Ok(())
            }
            Mismatch::Error(text) => write!(f, "error: {text}"),
        }
    }
}

fn show(value: &Option<String>) -> String {
    match value {
        Some(v) => format!("{v:?}"),
        None => "absent".to_string(),
    }
}

pub fn type_name(value: &Value) -> &'static str {
    match value {
        Value::Null => "null",
        Value::Bool(_) => "boolean",
        Value::Number(_) => "number",
        Value::String(_) => "string",
        Value::Array(_) => "array",
        Value::Object(_) => "object",
    }
}

/// Returns the media type of a `Content-Type` value: everything before the first `;`, trimmed, lower case.
pub fn media_type(content_type: Option<&str>) -> Option<String> {
    content_type.map(|ct| {
        ct.split(';')
            .next()
            .unwrap_or("")
            .trim()
            .to_ascii_lowercase()
    })
}

/// Compares `actual` to `expected` under the step's mode.
pub fn compare_responses(
    mode: Compare,
    volatile: &[String],
    expected: &RecordedResponse,
    actual: &RecordedResponse,
) -> Vec<Mismatch> {
    let mut out = Vec::new();
    if expected.status != actual.status {
        out.push(Mismatch::Status {
            expected: expected.status,
            actual: actual.status,
        });
    }
    if mode == Compare::StatusAndMediaType {
        let e = media_type(expected.header("content-type"));
        let a = media_type(actual.header("content-type"));
        if e != a {
            out.push(Mismatch::MediaType {
                expected: e,
                actual: a,
            });
        }
        return out;
    }
    for name in COMPARED_HEADERS {
        let e = expected.header(name).map(str::to_string);
        let a = actual.header(name).map(str::to_string);
        if e != a {
            out.push(Mismatch::Header {
                name: name.to_string(),
                expected: e,
                actual: a,
            });
        }
    }
    let volatile: &[String] = if mode == Compare::JsonWithVolatile {
        volatile
    } else {
        &[]
    };
    out.extend(compare_bodies(volatile, &expected.body, &actual.body));
    out
}

fn compare_bodies(
    volatile: &[String],
    expected: &Option<RecordedBody>,
    actual: &Option<RecordedBody>,
) -> Vec<Mismatch> {
    match (expected, actual) {
        (None, None) => Vec::new(),
        (Some(RecordedBody::Json(e)), Some(RecordedBody::Json(a))) => compare_json(volatile, e, a),
        (Some(RecordedBody::Text(e)), Some(RecordedBody::Text(a))) => {
            if e == a {
                Vec::new()
            } else {
                vec![Mismatch::Text {
                    diff: text_diff(e, a),
                }]
            }
        }
        (Some(RecordedBody::Base64(e)), Some(RecordedBody::Base64(a))) => {
            if e == a {
                return Vec::new();
            }
            let (eb, ab) = match (base64_decode(e), base64_decode(a)) {
                (Ok(eb), Ok(ab)) => (eb, ab),
                _ => {
                    return vec![Mismatch::Error(
                        "a recorded binary body is not base64".into(),
                    )]
                }
            };
            vec![Mismatch::Binary {
                expected_len: eb.len(),
                actual_len: ab.len(),
                first_difference: eb.iter().zip(ab.iter()).position(|(x, y)| x != y).or({
                    if eb.len() != ab.len() {
                        Some(eb.len().min(ab.len()))
                    } else {
                        None
                    }
                }),
            }]
        }
        (e, a) => vec![Mismatch::BodyKind {
            expected: e
                .as_ref()
                .map(RecordedBody::kind)
                .unwrap_or("no body")
                .to_string(),
            actual: a
                .as_ref()
                .map(RecordedBody::kind)
                .unwrap_or("no body")
                .to_string(),
        }],
    }
}

/// Compares two JSON bodies by parsed value, then by canonical bytes (key order), after masking volatile pointers.
pub fn compare_json(volatile: &[String], expected: &Value, actual: &Value) -> Vec<Mismatch> {
    let mut e = expected.clone();
    let mut a = actual.clone();
    let mut out = mask_volatile(volatile, &mut e, &mut a);
    if e != a {
        let mut diffs = Vec::new();
        value_diffs("", &e, &a, &mut diffs);
        out.push(Mismatch::JsonValue(diffs));
        return out;
    }
    let e_bytes = serde_json::to_string(&e).unwrap_or_default();
    let a_bytes = serde_json::to_string(&a).unwrap_or_default();
    if e_bytes != a_bytes {
        let mut diffs = Vec::new();
        order_diffs("", &e, &a, &mut diffs);
        if diffs.is_empty() {
            // Same values, same order, different bytes: not expected with serde_json, but reported.
            out.push(Mismatch::Text {
                diff: text_diff(&e_bytes, &a_bytes),
            });
        } else {
            out.push(Mismatch::JsonOrder(diffs));
        }
    }
    out
}

/// Replaces each volatile value on both sides with a marker when the JSON types agree.
fn mask_volatile(volatile: &[String], expected: &mut Value, actual: &mut Value) -> Vec<Mismatch> {
    let mut out = Vec::new();
    for pointer in volatile {
        let e_type = expected.pointer(pointer).map(type_name);
        let a_type = actual.pointer(pointer).map(type_name);
        match (e_type, a_type) {
            (Some(e), Some(a)) if e == a => {
                let marker = Value::String(format!("<volatile {e}>"));
                if let Some(slot) = expected.pointer_mut(pointer) {
                    *slot = marker.clone();
                }
                if let Some(slot) = actual.pointer_mut(pointer) {
                    *slot = marker;
                }
            }
            (None, None) => {}
            (e, a) => {
                // Reported once here; masked so the value comparison does not report it again.
                let marker = Value::String("<volatile>".to_string());
                if let Some(slot) = expected.pointer_mut(pointer) {
                    *slot = marker.clone();
                }
                if let Some(slot) = actual.pointer_mut(pointer) {
                    *slot = marker;
                }
                out.push(Mismatch::Volatile {
                    pointer: pointer.clone(),
                    expected: e.unwrap_or("missing value").to_string(),
                    actual: a.unwrap_or("missing value").to_string(),
                });
            }
        }
    }
    out
}

fn escape_key(key: &str) -> String {
    key.replace('~', "~0").replace('/', "~1")
}

fn brief(value: &Value) -> String {
    let text = value.to_string();
    if text.len() > 120 {
        format!(
            "{}...",
            &text[..text.char_indices().nth(117).map(|(i, _)| i).unwrap_or(117)]
        )
    } else {
        text
    }
}

pub fn value_diffs(pointer: &str, expected: &Value, actual: &Value, out: &mut Vec<ValueDiff>) {
    match (expected, actual) {
        (Value::Object(e), Value::Object(a)) => {
            for (key, ev) in e {
                let child = format!("{pointer}/{}", escape_key(key));
                match a.get(key) {
                    Some(av) => value_diffs(&child, ev, av, out),
                    None => out.push(ValueDiff {
                        pointer: child,
                        kind: ValueDiffKind::Missing,
                    }),
                }
            }
            for key in a.keys() {
                if !e.contains_key(key) {
                    out.push(ValueDiff {
                        pointer: format!("{pointer}/{}", escape_key(key)),
                        kind: ValueDiffKind::Extra,
                    });
                }
            }
        }
        (Value::Array(e), Value::Array(a)) => {
            if e.len() != a.len() {
                out.push(ValueDiff {
                    pointer: if pointer.is_empty() {
                        "/".to_string()
                    } else {
                        pointer.to_string()
                    },
                    kind: ValueDiffKind::Length {
                        expected: e.len(),
                        actual: a.len(),
                    },
                });
            }
            for (i, (ev, av)) in e.iter().zip(a.iter()).enumerate() {
                value_diffs(&format!("{pointer}/{i}"), ev, av, out);
            }
        }
        (e, a) => {
            if e != a {
                out.push(ValueDiff {
                    pointer: if pointer.is_empty() {
                        "/".to_string()
                    } else {
                        pointer.to_string()
                    },
                    kind: ValueDiffKind::Value {
                        expected: brief(e),
                        actual: brief(a),
                    },
                });
            }
        }
    }
}

/// Collects into `out` the objects whose keys are the same set in a different sequence (values already known equal).
pub fn order_diffs(pointer: &str, expected: &Value, actual: &Value, out: &mut Vec<OrderDiff>) {
    match (expected, actual) {
        (Value::Object(e), Value::Object(a)) => {
            let e_keys: Vec<String> = e.keys().cloned().collect();
            let a_keys: Vec<String> = a.keys().cloned().collect();
            if e_keys != a_keys {
                out.push(OrderDiff {
                    pointer: pointer.to_string(),
                    expected: e_keys,
                    actual: a_keys,
                });
            }
            for (key, ev) in e {
                if let Some(av) = a.get(key) {
                    order_diffs(&format!("{pointer}/{}", escape_key(key)), ev, av, out);
                }
            }
        }
        (Value::Array(e), Value::Array(a)) => {
            for (i, (ev, av)) in e.iter().zip(a.iter()).enumerate() {
                order_diffs(&format!("{pointer}/{i}"), ev, av, out);
            }
        }
        _ => {}
    }
}

/// Renders a unified diff of two texts, capped in length.
pub fn text_diff(expected: &str, actual: &str) -> String {
    let diff = similar::TextDiff::from_lines(expected, actual);
    let text = diff
        .unified_diff()
        .context_radius(2)
        .header("expected", "actual")
        .to_string();
    let lines: Vec<&str> = text.lines().collect();
    if lines.len() > MAX_DIFF_LINES {
        let mut cut = lines[..MAX_DIFF_LINES].join("\n");
        cut.push_str(&format!(
            "\n... {} more lines",
            lines.len() - MAX_DIFF_LINES
        ));
        cut
    } else {
        text
    }
}

/// Compares the recorded request with the one the scenario builds now (after token substitution).
pub fn compare_requests(expected: &RecordedRequest, actual: &RecordedRequest) -> Option<Mismatch> {
    let mut lines = Vec::new();
    if expected.method != actual.method {
        lines.push(format!(
            "method: recorded {}, now {}",
            expected.method, actual.method
        ));
    }
    if expected.url != actual.url {
        lines.push(format!(
            "url: recorded {}, now {}",
            expected.url, actual.url
        ));
    }
    if expected.headers != actual.headers {
        lines.push(format!(
            "headers: recorded {}, now {}",
            Value::Object(expected.headers.clone()),
            Value::Object(actual.headers.clone())
        ));
    }
    if expected.body != actual.body {
        lines.push("body differs".to_string());
        if let (Some(RecordedBody::Json(e)), Some(RecordedBody::Json(a))) =
            (&expected.body, &actual.body)
        {
            let mut diffs = Vec::new();
            value_diffs("", e, a, &mut diffs);
            for d in diffs.iter().take(10) {
                lines.push(format!("  {}: {:?}", d.pointer, d.kind));
            }
        }
    }
    if lines.is_empty() {
        None
    } else {
        Some(Mismatch::RequestDrift(lines))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::cassette::Headers;
    use serde_json::json;

    fn response(
        status: u16,
        content_type: Option<&str>,
        body: Option<RecordedBody>,
    ) -> RecordedResponse {
        let mut headers = Headers::new();
        if let Some(ct) = content_type {
            headers.insert("content-type".into(), Value::String(ct.into()));
        }
        headers.insert("date".into(), Value::String("Mon".into()));
        headers.insert("content-length".into(), Value::String("9".into()));
        RecordedResponse {
            status,
            headers,
            body,
        }
    }

    fn json_response(body: Value) -> RecordedResponse {
        response(
            200,
            Some("application/json; charset=UTF-8"),
            Some(RecordedBody::Json(body)),
        )
    }

    #[test]
    fn equal_responses_have_no_mismatch() {
        let a = json_response(json!({"id": 1, "name": "x", "r": 10.0}));
        let mut b = a.clone();
        b.headers.insert("date".into(), Value::String("Tue".into()));
        b.headers
            .insert("content-length".into(), Value::String("99".into()));
        b.headers
            .insert("content-encoding".into(), Value::String("gzip".into()));
        assert!(compare_responses(Compare::Full, &[], &a, &b).is_empty());
    }

    #[test]
    fn status_and_content_type_are_exact() {
        let a = json_response(json!({}));
        let mut b = a.clone();
        b.status = 204;
        b.headers.insert(
            "content-type".into(),
            Value::String("application/json".into()),
        );
        let m = compare_responses(Compare::Full, &[], &a, &b);
        assert_eq!(
            m[0],
            Mismatch::Status {
                expected: 200,
                actual: 204
            }
        );
        assert_eq!(
            m[1],
            Mismatch::Header {
                name: "content-type".into(),
                expected: Some("application/json; charset=UTF-8".into()),
                actual: Some("application/json".into())
            }
        );
    }

    #[test]
    fn value_differences_are_listed_by_pointer() {
        let a =
            json_response(json!({"id": 1, "items": [{"k": 1}, {"k": 2}], "f": 10.0, "gone": true}));
        let b = json_response(json!({"id": 2, "items": [{"k": 1}], "f": 10, "new": 1}));
        let m = compare_responses(Compare::Full, &[], &a, &b);
        assert_eq!(m.len(), 1);
        let Mismatch::JsonValue(diffs) = &m[0] else {
            panic!("{m:?}")
        };
        let pointers: Vec<&str> = diffs.iter().map(|d| d.pointer.as_str()).collect();
        assert_eq!(pointers, vec!["/id", "/items", "/f", "/gone", "/new"]);
        assert_eq!(
            diffs[2].kind,
            ValueDiffKind::Value {
                expected: "10.0".into(),
                actual: "10".into()
            }
        );
        assert_eq!(diffs[3].kind, ValueDiffKind::Missing);
        assert_eq!(diffs[4].kind, ValueDiffKind::Extra);
        let text = m[0].to_string();
        assert!(text.contains("/f: expected 10.0, got 10"), "{text}");
    }

    #[test]
    fn key_order_is_reported_separately_from_values() {
        let a = json_response(json!({"id": 1, "name": "x", "nested": {"p": 1, "q": 2}}));
        let b: Value =
            serde_json::from_str(r#"{"name": "x", "id": 1, "nested": {"q": 2, "p": 1}}"#).unwrap();
        let b = json_response(b);
        let m = compare_responses(Compare::Full, &[], &a, &b);
        assert_eq!(m.len(), 1);
        let Mismatch::JsonOrder(diffs) = &m[0] else {
            panic!("{m:?}")
        };
        assert_eq!(diffs.len(), 2);
        assert_eq!(diffs[0].pointer, "");
        assert_eq!(diffs[0].expected, vec!["id", "name", "nested"]);
        assert_eq!(diffs[0].actual, vec!["name", "id", "nested"]);
        assert_eq!(diffs[1].pointer, "/nested");
        assert!(m[0].to_string().contains("key order"));
    }

    #[test]
    fn volatile_pointers_compare_by_type_only() {
        let a = json_response(json!({"id": 4, "salt": "abc", "expires": "2026-01-01T00:00:00Z"}));
        let b = json_response(json!({"id": 4, "salt": "xyz", "expires": "2027-01-01T00:00:00Z"}));
        let volatile = vec!["/salt".to_string(), "/expires".to_string()];
        assert!(compare_responses(Compare::JsonWithVolatile, &volatile, &a, &b).is_empty());
        // Full mode ignores the list.
        assert!(!compare_responses(Compare::Full, &volatile, &a, &b).is_empty());
        // Wrong type under a volatile pointer is a mismatch.
        let c = json_response(json!({"id": 4, "salt": 1, "expires": "2027-01-01T00:00:00Z"}));
        let m = compare_responses(Compare::JsonWithVolatile, &volatile, &a, &c);
        assert_eq!(
            m,
            vec![Mismatch::Volatile {
                pointer: "/salt".into(),
                expected: "string".into(),
                actual: "number".into()
            }]
        );
        // Missing on one side is a mismatch too.
        let d = json_response(json!({"id": 4, "expires": "2027-01-01T00:00:00Z"}));
        let m = compare_responses(Compare::JsonWithVolatile, &volatile, &a, &d);
        assert!(matches!(&m[0], Mismatch::Volatile { pointer, .. } if pointer == "/salt"));
    }

    #[test]
    fn text_binary_and_kind_mismatches() {
        let a = response(
            400,
            Some("text/plain; charset=UTF-8"),
            Some(RecordedBody::Text(
                "A PHRF result needs a PHRF rating".into(),
            )),
        );
        let b = response(
            400,
            Some("text/plain; charset=UTF-8"),
            Some(RecordedBody::Text("A PHRF result needs a rating".into())),
        );
        let m = compare_responses(Compare::Full, &[], &a, &b);
        assert!(
            matches!(&m[0], Mismatch::Text { diff } if diff.contains("-A PHRF result needs a PHRF rating"))
        );
        let c = response(
            400,
            Some("application/json"),
            Some(RecordedBody::Json(json!("x"))),
        );
        assert!(matches!(
            &compare_responses(Compare::Full, &[], &a, &c)[1],
            Mismatch::BodyKind { .. }
        ));
        let p1 = response(
            200,
            Some("image/png"),
            Some(RecordedBody::Base64(base64::Engine::encode(
                &base64::engine::general_purpose::STANDARD,
                [1, 2, 3],
            ))),
        );
        let p2 = response(
            200,
            Some("image/png"),
            Some(RecordedBody::Base64(base64::Engine::encode(
                &base64::engine::general_purpose::STANDARD,
                [1, 9, 3, 4],
            ))),
        );
        assert_eq!(
            compare_responses(Compare::Full, &[], &p1, &p2),
            vec![Mismatch::Binary {
                expected_len: 3,
                actual_len: 4,
                first_difference: Some(1)
            }]
        );
        let none = response(204, None, None);
        assert!(compare_responses(Compare::Full, &[], &none, &none).is_empty());
        assert!(matches!(
            &compare_responses(Compare::Full, &[], &none, &a)[2],
            Mismatch::BodyKind { .. }
        ));
    }

    #[test]
    fn fallback_compares_status_and_media_type_only() {
        let a = response(
            200,
            Some("text/html; charset=UTF-8"),
            Some(RecordedBody::Text("<html>ktor".into())),
        );
        let b = response(
            200,
            Some("text/html"),
            Some(RecordedBody::Text("<html>leptos".into())),
        );
        assert!(compare_responses(Compare::StatusAndMediaType, &[], &a, &b).is_empty());
        let c = response(200, Some("application/json"), None);
        assert_eq!(
            compare_responses(Compare::StatusAndMediaType, &[], &a, &c),
            vec![Mismatch::MediaType {
                expected: Some("text/html".into()),
                actual: Some("application/json".into())
            }]
        );
    }

    #[test]
    fn request_drift() {
        let r = RecordedRequest {
            method: "GET".into(),
            url: "/v1/api/series?id=1".into(),
            headers: Headers::new(),
            body: None,
        };
        assert!(compare_requests(&r, &r).is_none());
        let mut other = r.clone();
        other.url = "/v1/api/series?id=2".into();
        assert!(
            matches!(compare_requests(&r, &other), Some(Mismatch::RequestDrift(lines)) if lines[0].contains("url"))
        );
    }
}

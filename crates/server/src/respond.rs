//! Response builders for the REST handlers, with the headers the contract cassettes record byte for byte.
//!
//! JSON is `application/json` with no charset, text is `text/plain; charset=UTF-8`, and empty answers carry no
//! `Content-Type`.

use axum::body::Body;
use axum::http::{header, HeaderValue, StatusCode};
use axum::response::Response;
use serde::Serialize;

pub const JSON: &str = "application/json";
pub const TEXT: &str = "text/plain; charset=UTF-8";
pub const HTML: &str = "text/html; charset=UTF-8";

/// Serializes `value` compactly as a 200 JSON response, or answers a 500 if it does not serialize.
pub fn json<T: Serialize>(value: &T) -> Response {
    match serde_json::to_vec(value) {
        Ok(body) => with_type(StatusCode::OK, JSON, body),
        Err(e) => crate::error::AppError::from(e).into_response_(),
    }
}

pub fn text(status: StatusCode, body: &str) -> Response {
    with_type(status, TEXT, body.as_bytes().to_vec())
}

pub fn empty(status: StatusCode) -> Response {
    Response::builder()
        .status(status)
        .body(Body::empty())
        .expect("empty response")
}

pub fn with_type(status: StatusCode, content_type: &str, body: Vec<u8>) -> Response {
    Response::builder()
        .status(status)
        .header(
            header::CONTENT_TYPE,
            HeaderValue::from_str(content_type).expect("content type"),
        )
        .body(Body::from(body))
        .expect("response")
}

impl crate::error::AppError {
    fn into_response_(self) -> Response {
        axum::response::IntoResponse::into_response(self)
    }
}

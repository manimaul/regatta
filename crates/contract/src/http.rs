//! The contract's HTTP client: sends resolved requests and records the responses.
//!
//! Automatic decompression is off so the `Content-Encoding` header is observed; gzip and deflate bodies are decoded
//! here ([`decode`]).

use crate::cassette::{Headers, RecordedResponse};
use crate::step::ResolvedRequest;
use anyhow::{Context, Result};
use serde_json::Value;
use std::io::Read;
use std::time::Duration;

pub struct HttpClient {
    client: reqwest::Client,
    base_url: String,
}

impl HttpClient {
    pub fn new(base_url: &str) -> Result<HttpClient> {
        let client = reqwest::Client::builder()
            .no_gzip()
            .redirect(reqwest::redirect::Policy::none())
            .timeout(Duration::from_secs(60))
            .build()
            .context("building the http client")?;
        Ok(HttpClient {
            client,
            base_url: base_url.trim_end_matches('/').to_string(),
        })
    }

    pub fn base_url(&self) -> &str {
        &self.base_url
    }

    pub async fn send(&self, request: &ResolvedRequest) -> Result<RecordedResponse> {
        let method = reqwest::Method::from_bytes(request.method.as_str().as_bytes())?;
        let url = format!("{}{}", self.base_url, request.url);
        let mut builder = self.client.request(method, &url);
        for (name, value) in &request.headers {
            builder = builder.header(name.as_str(), value.as_str());
        }
        if let Some(body) = &request.body {
            builder = builder.body(body.clone());
        }
        let response = builder
            .send()
            .await
            .with_context(|| format!("{} {url}", request.method))?;
        let status = response.status().as_u16();
        let mut headers = Headers::new();
        for (name, value) in response.headers() {
            let name = name.as_str().to_ascii_lowercase();
            let value = String::from_utf8_lossy(value.as_bytes()).to_string();
            match headers.get_mut(&name) {
                Some(Value::String(existing)) => {
                    existing.push_str(", ");
                    existing.push_str(&value);
                }
                _ => {
                    headers.insert(name, Value::String(value));
                }
            }
        }
        let raw = response
            .bytes()
            .await
            .context("reading the response body")?;
        let encoding = headers
            .get("content-encoding")
            .and_then(Value::as_str)
            .unwrap_or("")
            .to_ascii_lowercase();
        let bytes = decode(&encoding, &raw)?;
        Ok(RecordedResponse::from_parts(status, headers, &bytes))
    }
}

/// Decodes a body by its `Content-Encoding`: identity (or none), gzip or deflate.
///
/// # Errors
///
/// Returns `Err` for an unsupported encoding or a body that does not decode under its encoding.
pub fn decode(encoding: &str, raw: &[u8]) -> Result<Vec<u8>> {
    let mut out = Vec::new();
    match encoding.trim() {
        "" | "identity" => out.extend_from_slice(raw),
        "gzip" | "x-gzip" => {
            flate2::read::GzDecoder::new(raw)
                .read_to_end(&mut out)
                .context("decoding the gzip body")?;
        }
        "deflate" => {
            // HTTP deflate is a zlib stream, but some servers send raw deflate: try zlib first.
            if flate2::read::ZlibDecoder::new(raw)
                .read_to_end(&mut out)
                .is_err()
            {
                out.clear();
                flate2::read::DeflateDecoder::new(raw)
                    .read_to_end(&mut out)
                    .context("decoding the deflate body")?;
            }
        }
        other => anyhow::bail!("unsupported Content-Encoding {other:?}"),
    }
    Ok(out)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    #[test]
    fn decodes_gzip_and_identity() {
        let mut enc = flate2::write::GzEncoder::new(Vec::new(), flate2::Compression::default());
        enc.write_all(b"hello report").unwrap();
        let gz = enc.finish().unwrap();
        assert_eq!(decode("gzip", &gz).unwrap(), b"hello report");
        assert_eq!(decode("", b"plain").unwrap(), b"plain");
        let mut z = flate2::write::ZlibEncoder::new(Vec::new(), flate2::Compression::default());
        z.write_all(b"zz").unwrap();
        assert_eq!(decode("deflate", &z.finish().unwrap()).unwrap(), b"zz");
        assert!(decode("br", b"x").is_err());
    }
}

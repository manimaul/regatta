//! Lookups against ORC's public data site (= `orc/OrcClient.kt`). The site sends no CORS
//! headers, so the browser cannot call it; the server does, on behalf of the admin adding a
//! certificate.
//!
//! Two requests make a certificate: the RMS data (`DownBoatRMS`, the rating fields) and the row
//! of the active-certificate list (`activecerts` filtered by country and reference number), which
//! carries the file id `dxtID` that keys the certificate's public page. The list only has active
//! certificates, so the id must be captured while the certificate is current.

use anyhow::{anyhow, bail, Context, Result};
use quick_xml::events::Event;
use quick_xml::Reader;
use regatta_common::{OrcCertificate, OrcResponse};
use std::sync::OnceLock;
use std::time::Duration;

const BASE: &str = "https://data.orc.org/public/WPub.dll";
const BOM: char = '\u{feff}';

fn http() -> &'static reqwest::Client {
    static CLIENT: OnceLock<reqwest::Client> = OnceLock::new();
    CLIENT.get_or_init(|| {
        reqwest::Client::builder()
            .redirect(reqwest::redirect::Policy::limited(10))
            .connect_timeout(Duration::from_secs(10))
            .timeout(Duration::from_secs(30))
            .build()
            .expect("reqwest client")
    })
}

/// The certificate `ref_no` with its file id when ORC still lists it as active, or `None` when
/// ORC has no RMS data for it.
pub async fn lookup(ref_no: &str) -> Result<Option<OrcCertificate>> {
    let Some(mut cert) = fetch_certificate(ref_no).await? else {
        return Ok(None);
    };
    let dxt_id = match fetch_dxt_id(&cert.nat_auth, ref_no).await {
        Ok(id) => id,
        Err(e) => {
            tracing::warn!("ORC file id lookup failed for {ref_no}: {e:#}");
            None
        }
    };
    if dxt_id.is_none() {
        tracing::info!("ORC certificate {ref_no} has no file id (not on the active list)");
    }
    cert.dxt_id = dxt_id;
    Ok(Some(cert))
}

pub async fn fetch_certificate(ref_no: &str) -> Result<Option<OrcCertificate>> {
    let body = get(
        &format!(
            "{BASE}?action=DownBoatRMS&RefNo={}&ext=json",
            encode(ref_no)
        ),
        "application/json",
    )
    .await?;
    parse_certificate(&body)
}

pub async fn fetch_dxt_id(country_id: &str, ref_no: &str) -> Result<Option<i64>> {
    let body = get(
        &format!(
            "{BASE}?action=activecerts&CountryId={}&RefNo={}",
            encode(country_id),
            encode(ref_no)
        ),
        "text/xml",
    )
    .await?;
    parse_dxt_id(&body, ref_no)
}

/// First certificate of an RMS JSON document; a leading UTF-8 BOM (the country feeds have one)
/// and unknown keys are ignored.
pub fn parse_certificate(body: &str) -> Result<Option<OrcCertificate>> {
    let body = body.strip_prefix(BOM).unwrap_or(body);
    let response: OrcResponse = serde_json::from_str(body).context("ORC RMS document")?;
    Ok(response.rms.into_iter().next())
}

/// The `dxtID` of the `ROW` whose `RefNo` is `ref_no` (case-insensitive) in an `activecerts` XML
/// document, or `None` when there is none. A document with a DOCTYPE or entity references is
/// refused, like the Kotlin parser's secure configuration.
pub fn parse_dxt_id(xml: &str, ref_no: &str) -> Result<Option<i64>> {
    let xml = xml.strip_prefix(BOM).unwrap_or(xml);
    let mut reader = Reader::from_str(xml);
    let mut path: Vec<String> = Vec::new();
    let mut in_row = false;
    let mut row_ref_no: Option<String> = None;
    let mut row_dxt_id: Option<String> = None;
    let mut text = String::new();
    loop {
        match reader.read_event().context("ORC activecerts document")? {
            Event::Eof => {
                if !path.is_empty() {
                    bail!(
                        "ORC activecerts document is truncated (unclosed <{}>)",
                        path[path.len() - 1]
                    );
                }
                break;
            }
            Event::DocType(_) => bail!("ORC activecerts document has a DOCTYPE"),
            Event::GeneralRef(_) => bail!("ORC activecerts document has an entity reference"),
            Event::Start(e) => {
                let name = e.local_name().as_ref().to_string();
                if name == "ROW" && !in_row {
                    in_row = true;
                    row_ref_no = None;
                    row_dxt_id = None;
                }
                path.push(name);
                text.clear();
            }
            Event::Text(t) => text.push_str(&t),
            Event::CData(c) => text.push_str(&c),
            Event::End(e) => {
                let name = e.local_name().as_ref().to_string();
                if in_row {
                    // getElementsByTagName(tag).item(0): the first element of that name in the row
                    if name == "RefNo" && row_ref_no.is_none() {
                        row_ref_no = Some(text.trim().to_string());
                    } else if name == "dxtID" && row_dxt_id.is_none() {
                        row_dxt_id = Some(text.trim().to_string());
                    }
                }
                path.pop();
                if name == "ROW" && in_row && !path.iter().any(|p| p == "ROW") {
                    in_row = false;
                    if row_ref_no
                        .as_deref()
                        .is_some_and(|r| r.eq_ignore_ascii_case(ref_no))
                    {
                        return Ok(row_dxt_id.as_deref().and_then(|s| s.parse::<i64>().ok()));
                    }
                }
                text.clear();
            }
            _ => {}
        }
    }
    Ok(None)
}

async fn get(url: &str, accept: &str) -> Result<String> {
    let response = http()
        .get(url)
        .header(reqwest::header::ACCEPT, accept)
        .send()
        .await
        .with_context(|| format!("GET {url}"))?;
    let status = response.status();
    if !status.is_success() {
        return Err(anyhow!("ORC answered {} for {url}", status.as_u16()));
    }
    response
        .text()
        .await
        .with_context(|| format!("reading {url}"))
}

/// `URLEncoder.encode(value.trim(), UTF_8)`: `application/x-www-form-urlencoded`, space as `+`.
fn encode(value: &str) -> String {
    let mut out = String::new();
    for byte in value.trim().bytes() {
        match byte {
            b'a'..=b'z' | b'A'..=b'Z' | b'0'..=b'9' | b'*' | b'-' | b'.' | b'_' => {
                out.push(byte as char)
            }
            b' ' => out.push('+'),
            other => out.push_str(&format!("%{other:02X}")),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A copy of a real response from data.orc.org (2026-10-15).
    const ACTIVE_ROW: &str = include_str!("../tests/fixtures/activecerts_0456000518S.xml");

    const NO_ROWS: &str = r#"<?xml version="1.0" encoding="UTF-8"?>
        <?xml-stylesheet type="text/xsl" href="activecerts.xsl" ?>
        <ROOT><script xmlns="http://www.w3.org/1999/xhtml" src="/xslt-polyfill.min.js"/>
          <DATA/>
          <ERRORS/>
        </ROOT>"#;

    /// `OrcClientTest.fileIdOfTheRowWithTheReferenceNumber`.
    #[test]
    fn file_id_of_the_row_with_the_reference_number() {
        assert_eq!(
            parse_dxt_id(ACTIVE_ROW, "0456000518S").unwrap(),
            Some(264182)
        );
        assert_eq!(
            parse_dxt_id(ACTIVE_ROW, "0456000518s").unwrap(),
            Some(264182),
            "reference numbers compare case-insensitively"
        );
    }

    /// `OrcClientTest.noRowMeansNoFileId`.
    #[test]
    fn no_row_means_no_file_id() {
        assert_eq!(
            parse_dxt_id(NO_ROWS, "04560003WR9").unwrap(),
            None,
            "an expired certificate is not on the active list"
        );
        assert_eq!(
            parse_dxt_id(ACTIVE_ROW, "04560003WR9").unwrap(),
            None,
            "a row for another certificate does not count"
        );
    }

    /// `OrcClientTest.rmsDocumentWithByteOrderMarkAndUnknownKeys`.
    #[test]
    fn rms_document_with_byte_order_mark_and_unknown_keys() {
        let body = "\u{feff}{\"rms\":[{\"NatAuth\":\"USA\",\"RefNo\":\"0456000518S\",\"YachtName\":\"FLICKA\",\"Unknown\":1}],\"Countries\":[]}";
        let cert = parse_certificate(body).unwrap().unwrap();
        assert_eq!(cert.ref_no, "0456000518S");
        assert_eq!(cert.nat_auth, "USA");
        assert_eq!(cert.dxt_id, None);
        assert_eq!(parse_certificate("{\"rms\":[]}").unwrap(), None);
    }

    /// An RMS fixture in the shape `DownBoatRMS` answers: the rating fields are read into the
    /// `US_…` keyed certificate, with a BOM and the extra `Countries` key ignored.
    #[test]
    fn rms_fixture_with_rating_fields() {
        let body = include_str!("../tests/fixtures/rms_0456000518S.json");
        let cert = parse_certificate(body).unwrap().unwrap();
        assert_eq!(cert.ref_no, "0456000518S");
        assert_eq!(cert.yacht_name, "FLICKA");
        assert_eq!(cert.c_type, "CLUB");
        assert_eq!(cert.issue_date.to_string(), "2025-04-08T19:32:22.815Z");
        assert_eq!(cert.all_purpose_tod, 495.0);
        assert_eq!(cert.five_band_wl_low_med_tot, 0.8209);
        assert_eq!(cert.us_wl6040_h_tod, 524.5);
        assert_eq!(cert.dxt_id, None, "the RMS data has no file id");
    }

    #[test]
    fn doctype_and_entities_are_refused() {
        let doctype = r#"<!DOCTYPE ROOT [<!ENTITY x "y">]><ROOT><DATA><ROW><RefNo>A</RefNo><dxtID>1</dxtID></ROW></DATA></ROOT>"#;
        assert!(parse_dxt_id(doctype, "A").is_err());
        let entity = r#"<ROOT><DATA><ROW><RefNo>&x;</RefNo><dxtID>1</dxtID></ROW></DATA></ROOT>"#;
        assert!(parse_dxt_id(entity, "A").is_err());
        assert!(
            parse_dxt_id("<ROOT><DATA>", "A").is_err(),
            "truncated document"
        );
    }

    #[test]
    fn dxt_id_must_be_a_number() {
        let xml = r#"<ROOT><DATA><ROW><RefNo>A</RefNo><dxtID>x</dxtID></ROW></DATA></ROOT>"#;
        assert_eq!(parse_dxt_id(xml, "a").unwrap(), None);
        let no_id = r#"<ROOT><DATA><ROW><RefNo>A</RefNo></ROW><ROW><RefNo>B</RefNo><dxtID>7</dxtID></ROW></DATA></ROOT>"#;
        assert_eq!(parse_dxt_id(no_id, "A").unwrap(), None);
        assert_eq!(parse_dxt_id(no_id, "B").unwrap(), Some(7));
    }

    #[test]
    fn form_encoding_like_url_encoder() {
        assert_eq!(encode(" 0456000518S "), "0456000518S");
        assert_eq!(encode("USA 69262"), "USA+69262");
        assert_eq!(encode("a/b&c=é"), "a%2Fb%26c%3D%C3%A9");
    }
}

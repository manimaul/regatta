//! Parsing a data.orc.org RMS response, the preferred certificate, and the certificate file id.

use regatta_common::{
    find_preferred, orc_certificate_url, OrcCertificate, OrcResponse, RaceReportCard,
};
use std::path::Path;

/// The RMS fixture (`tests/fixtures/waymaker.json`, a real data.orc.org response), read in
/// place; relative to this crate.
const WAYMAKER_JSON: &str = "tests/fixtures/waymaker.json";

fn cert(ref_no: &str, c_type: &str, dxt_id: Option<i64>) -> OrcCertificate {
    OrcCertificate {
        ref_no: ref_no.into(),
        c_type: c_type.into(),
        dxt_id,
        ..Default::default()
    }
}

#[test]
fn test_encode() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR")).join(WAYMAKER_JSON);
    let content =
        std::fs::read_to_string(&path).unwrap_or_else(|e| panic!("{}: {e}", path.display()));
    // Unknown keys in the RMS response are ignored, serde's default.
    let response: OrcResponse = serde_json::from_str(&content).unwrap();
    assert_eq!(response.rms[0].ref_no, "04560003WR9");
    assert_eq!(
        response.rms[0].dxt_id, None,
        "the RMS data carries no file id"
    );
}

#[test]
fn certificate_url_from_file_id() {
    assert_eq!(
        Some("https://data.orc.org/public/WPub.dll/CC/264182".to_string()),
        cert("0456000518S", "", Some(264182)).certificate_url()
    );
    assert_eq!(None, cert("0456000518S", "", None).certificate_url());
    assert_eq!(None, orc_certificate_url(None));
    assert_eq!(
        Some("https://data.orc.org/public/WPub.dll/CC/7".to_string()),
        RaceReportCard {
            orc_dxt_id: Some(7),
            ..Default::default()
        }
        .orc_cert_url()
    );
    assert_eq!(None, RaceReportCard::default().orc_cert_url());
}

/// The golden files write every default, and must not change for certificates without an id.
#[test]
fn file_id_is_never_encoded_while_null() {
    assert!(!serde_json::to_string(&cert("001", "", None))
        .unwrap()
        .contains("dxtId"));
    assert!(serde_json::to_string(&cert("001", "", Some(264182)))
        .unwrap()
        .contains("\"dxtId\":264182"));
    assert!(!serde_json::to_string(&RaceReportCard::default())
        .unwrap()
        .contains("orcDxtId"));
    let card = RaceReportCard {
        orc_dxt_id: Some(264182),
        ..Default::default()
    };
    assert!(serde_json::to_string(&card)
        .unwrap()
        .contains("\"orcDxtId\":264182"));
    // A stored certificate without the key decodes to None; one with it keeps the id.
    assert_eq!(
        None,
        serde_json::from_str::<OrcCertificate>("{\"RefNo\":\"001\"}")
            .unwrap()
            .dxt_id
    );
    assert_eq!(
        Some(264182),
        serde_json::from_str::<OrcCertificate>("{\"RefNo\":\"001\",\"dxtId\":264182}")
            .unwrap()
            .dxt_id
    );
}

#[test]
fn test_pref() {
    let certs = vec![
        cert("001", "NSCL", None),
        cert("002", "CLUB", None),
        cert("003", "DHCL", None),
    ];
    assert_eq!(
        Some("002"),
        find_preferred(&certs).map(|c| c.ref_no.as_str())
    );
    assert_eq!(None, find_preferred(&[]));
}

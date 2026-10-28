//! The REST ORC certificate routes: the live lookup on data.orc.org and the stored certificates.

use crate::error::AppResult;
use crate::extract::{Admin, Json, Params};
use crate::respond::{empty, json, text};
use crate::AppState;
use axum::extract::State;
use axum::http::StatusCode;
use regatta_common::OrcCertificate;
use regatta_service::db::orc_table::InvalidCertificate;
use regatta_service::orc;

/// Answers `GET orcCert?refNo=` with the certificate fetched from data.orc.org, with its `dxtId` file id.
///
/// 400 without a reference number, 204 when ORC has none. The contract cassettes skip this route: it is a live
/// lookup.
pub async fn get_cert(_admin: Admin, params: Params) -> AppResult {
    match params.get("refNo").map(str::trim).filter(|r| !r.is_empty()) {
        Some(ref_no) => match orc::lookup(ref_no).await? {
            Some(cert) => Ok(json(&cert)),
            None => Ok(empty(StatusCode::NO_CONTENT)),
        },
        None => Ok(empty(StatusCode::BAD_REQUEST)),
    }
}

pub async fn all_certs(_admin: Admin, State(state): State<AppState>) -> AppResult {
    Ok(json(&state.db.all_certificates().await?))
}

/// Replaces a stored certificate for `POST orcCert`, keeping its boat link.
///
/// 404 text for an unknown reference number, 400 text for a certificate with a zero rating.
pub async fn post_cert(
    _admin: Admin,
    State(state): State<AppState>,
    Json(cert): Json<OrcCertificate>,
) -> AppResult {
    match state.db.update_certificate(&cert).await {
        Ok(changed) if changed > 0 => Ok(json(&cert)),
        Ok(_) => Ok(text(
            StatusCode::NOT_FOUND,
            &format!("no certificate with reference number {}", cert.ref_no),
        )),
        Err(e) if e.downcast_ref::<InvalidCertificate>().is_some() => {
            Ok(text(StatusCode::BAD_REQUEST, &e.to_string()))
        }
        Err(e) => Err(e.into()),
    }
}

//! The `orc` table: ORC certificates as `jsonb`, keyed by reference number, linked to a boat.

use anyhow::Result;
use regatta_common::OrcCertificate;
use sqlx::types::Json;
use sqlx::{FromRow, PgConnection};
use std::collections::HashMap;

/// The error for a certificate that fails [`validate`] (a zero rating).
///
/// The certificate update route (`crates/server/src/routes/orc.rs`) answers it with a 400.
#[derive(Debug, thiserror::Error)]
#[error("invalid certificate properties")]
pub struct InvalidCertificate;

#[derive(FromRow)]
struct Row {
    orc_ref: String,
    boat_id: Option<i64>,
    cert: Json<OrcCertificate>,
}

const COLUMNS: &str = "orc_ref, boat_id, cert";

/// Stores a certificate by reference number and links it to `boat`, replacing a stored one.
///
/// # Errors
///
/// - Returns [`InvalidCertificate`] if the certificate fails [`validate`]; nothing is written.
/// - Returns `Err` if the statement fails.
pub async fn upsert_cert(
    conn: &mut PgConnection,
    boat: i64,
    certificate: &OrcCertificate,
) -> Result<()> {
    if !validate(certificate) {
        return Err(InvalidCertificate.into());
    }
    sqlx::query(
        "INSERT INTO orc (orc_ref, boat_id, cert) VALUES ($1, $2, $3) \
         ON CONFLICT (orc_ref) DO UPDATE SET boat_id = EXCLUDED.boat_id, cert = EXCLUDED.cert",
    )
    .bind(&certificate.ref_no)
    .bind(boat)
    .bind(Json(certificate))
    .execute(conn)
    .await?;
    Ok(())
}

/// Replaces the stored certificate of the same reference number, keeping its boat link; returns the rows changed.
///
/// # Errors
///
/// - Returns [`InvalidCertificate`] if the certificate fails [`validate`]; nothing is written.
/// - Returns `Err` if the statement fails.
pub async fn update_cert(conn: &mut PgConnection, certificate: &OrcCertificate) -> Result<u64> {
    if !validate(certificate) {
        return Err(InvalidCertificate.into());
    }
    Ok(sqlx::query("UPDATE orc SET cert = $1 WHERE orc_ref = $2")
        .bind(Json(certificate))
        .bind(&certificate.ref_no)
        .execute(conn)
        .await?
        .rows_affected())
}

/// Returns every stored certificate.
///
/// # Errors
///
/// Returns `Err` if the query fails or a stored JSON does not decode.
pub async fn all_certificates(conn: &mut PgConnection) -> Result<Vec<OrcCertificate>> {
    let rows: Vec<Json<OrcCertificate>> = sqlx::query_scalar("SELECT cert FROM orc")
        .fetch_all(conn)
        .await?;
    Ok(rows.into_iter().map(|j| j.0).collect())
}

/// Returns the stored certificate `ref_no`, or `None` if it is not stored or `ref_no` is `None`.
///
/// # Errors
///
/// Returns `Err` if the query fails or the stored JSON does not decode.
pub async fn find_certificate(
    conn: &mut PgConnection,
    ref_no: Option<&str>,
) -> Result<Option<OrcCertificate>> {
    match ref_no {
        Some(r) => Ok(find_certificates(conn, &[r.to_string()]).await?.remove(r)),
        None => Ok(None),
    }
}

/// Returns the certificates with `refs`, by reference number, in one query.
///
/// # Errors
///
/// Returns `Err` if the query fails or a stored JSON does not decode.
pub async fn find_certificates(
    conn: &mut PgConnection,
    refs: &[String],
) -> Result<HashMap<String, OrcCertificate>> {
    if refs.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM orc WHERE orc_ref = ANY($1)"
    )))
    .bind(refs)
    .fetch_all(conn)
    .await?;
    Ok(rows.into_iter().map(|r| (r.orc_ref, r.cert.0)).collect())
}

/// Returns the certificates linked to `boat`.
///
/// # Errors
///
/// Returns `Err` if the query fails or a stored JSON does not decode.
pub async fn find_certificates_of_boat(
    conn: &mut PgConnection,
    boat: i64,
) -> Result<Vec<OrcCertificate>> {
    Ok(find_certificates_by_boat(conn, &[boat])
        .await?
        .remove(&boat)
        .unwrap_or_default())
}

/// Returns the certificates linked to each boat in `boat_ids`, in one query; boats without any are absent.
///
/// # Errors
///
/// Returns `Err` if the query fails or a stored JSON does not decode.
pub async fn find_certificates_by_boat(
    conn: &mut PgConnection,
    boat_ids: &[i64],
) -> Result<HashMap<i64, Vec<OrcCertificate>>> {
    if boat_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<Row> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS} FROM orc WHERE boat_id = ANY($1)"
    )))
    .bind(boat_ids)
    .fetch_all(conn)
    .await?;
    let mut grouped: HashMap<i64, Vec<OrcCertificate>> = HashMap::new();
    for row in rows {
        if let Some(boat) = row.boat_id {
            grouped.entry(boat).or_default().push(row.cert.0);
        }
    }
    Ok(grouped)
}

/// Unlinks every certificate from `boat`, keeping the certificates; returns the rows changed.
///
/// # Errors
///
/// Returns `Err` if the statement fails.
pub async fn unlink_certs(conn: &mut PgConnection, boat: i64) -> Result<u64> {
    Ok(
        sqlx::query("UPDATE orc SET boat_id = NULL WHERE boat_id = $1")
            .bind(boat)
            .execute(conn)
            .await?
            .rows_affected(),
    )
}

/// Tells whether every rating a scoring option can pick is set (non-zero).
///
/// The triple-number windward/leeward TODs are not checked, so a certificate without them still validates.
pub fn validate(c: &OrcCertificate) -> bool {
    let values = [
        c.all_purpose_tod,
        c.all_purpose_tot,
        c.wl_single_number_tod,
        c.wl_single_number_tot,
        c.triple_number_all_purpose_low_tod,
        c.triple_number_all_purpose_low_tot,
        c.triple_number_all_purpose_med_tod,
        c.triple_number_all_purpose_med_tot,
        c.triple_number_all_purpose_hi_tod,
        c.triple_number_all_purpose_hi_tot,
        c.triple_number_wl_low_tot,
        c.triple_number_wl_med_tot,
        c.triple_number_wl_hi_tot,
        c.single_number_predominant_upwind_tot,
        c.single_number_predominant_upwind_tod,
        c.single_number_predominant_reaching_tot,
        c.single_number_predominant_reaching_tod,
        c.single_number_predominant_downwind_tot,
        c.single_number_predominant_downwind_tod,
        c.predominant_upwind_low_tot,
        c.predominant_upwind_low_tod,
        c.predominant_upwind_low_med_tot,
        c.predominant_upwind_low_med_tod,
        c.predominant_upwind_med_tot,
        c.predominant_upwind_med_tod,
        c.predominant_upwind_med_hi_tot,
        c.predominant_upwind_med_hi_tod,
        c.predominant_upwind_hi_tot,
        c.predominant_upwind_hi_tod,
        c.predominant_downwind_low_tot,
        c.predominant_downwind_low_tod,
        c.predominant_downwind_low_med_tot,
        c.predominant_downwind_low_med_tod,
        c.predominant_downwind_med_tot,
        c.predominant_downwind_med_tod,
        c.predominant_downwind_med_hi_tot,
        c.predominant_downwind_med_hi_tod,
        c.predominant_downwind_hi_tot,
        c.predominant_downwind_hi_tod,
        c.predominant_reaching_low_tot,
        c.predominant_reaching_low_tod,
        c.predominant_reaching_low_med_tot,
        c.predominant_reaching_low_med_tod,
        c.predominant_reaching_med_tot,
        c.predominant_reaching_med_tod,
        c.predominant_reaching_med_hi_tot,
        c.predominant_reaching_med_hi_tod,
        c.predominant_reaching_hi_tot,
        c.predominant_reaching_hi_tod,
        c.five_band_wl_low_tot,
        c.five_band_wl_low_tod,
        c.five_band_wl_low_med_tot,
        c.five_band_wl_low_med_tod,
        c.five_band_wl_med_tot,
        c.five_band_wl_med_tod,
        c.five_band_wl_med_hi_tot,
        c.five_band_wl_med_hi_tod,
        c.five_band_wl_hi_tot,
        c.five_band_wl_hi_tod,
        c.us_wl6040_l_tod,
        c.us_wl6040_l_tot,
        c.us_wl6040_lm_tod,
        c.us_wl6040_lm_tot,
        c.us_wl6040_m_tod,
        c.us_wl6040_m_tot,
        c.us_wl6040_mh_tod,
        c.us_wl6040_mh_tot,
        c.us_wl6040_h_tod,
        c.us_wl6040_h_tot,
        c.five_band_all_purpose_low_tot,
        c.five_band_all_purpose_low_tod,
        c.five_band_all_purpose_low_med_tot,
        c.five_band_all_purpose_low_med_tod,
        c.five_band_all_purpose_med_tot,
        c.five_band_all_purpose_med_tod,
        c.five_band_all_purpose_med_hi_tot,
        c.five_band_all_purpose_med_hi_tod,
        c.five_band_all_purpose_hi_tot,
        c.five_band_all_purpose_hi_tod,
    ];
    values.iter().all(|v| *v != 0.0)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validate_rejects_a_zero_rating() {
        assert!(!validate(&OrcCertificate::default()));
        let json = include_str!("../../tests/fixtures/rms_0456000518S.json");
        let json = json.strip_prefix('\u{feff}').unwrap();
        let response: regatta_common::OrcResponse = serde_json::from_str(json).unwrap();
        let cert = response.rms.into_iter().next().unwrap();
        assert!(validate(&cert));
        let zeroed = OrcCertificate {
            five_band_wl_med_tot: 0.0,
            ..cert.clone()
        };
        assert!(!validate(&zeroed));
        // only the stored ratings are checked; the identity fields may be blank
        let blank = OrcCertificate {
            yacht_name: String::new(),
            ..cert
        };
        assert!(validate(&blank));
    }
}

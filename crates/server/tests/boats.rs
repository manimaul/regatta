//! `upsert_boat` over the golden dump: a boat's certificates are linked to the id the boat row
//! got, so a new boat can be saved with its certificate (the `orc.boat_id` foreign key).

mod support;

use pretty_assertions::assert_eq;
use regatta_common::{Boat, OrcCertificate, RatingType};
use sqlx::PgPool;

async fn linked_boat(pool: &PgPool, ref_no: &str) -> Option<i64> {
    sqlx::query_scalar("SELECT boat_id FROM orc WHERE orc_ref = $1")
        .bind(ref_no)
        .fetch_one(pool)
        .await
        .unwrap()
}

async fn boat_count(pool: &PgPool) -> i64 {
    sqlx::query_scalar("SELECT count(*) FROM boat")
        .fetch_one(pool)
        .await
        .unwrap()
}

#[tokio::test(flavor = "multi_thread")]
async fn certificates_link_to_the_saved_boat() {
    let golden = support::start().await;
    let stored = golden
        .db
        .find_certificate(Some("04560003NFK"))
        .await
        .unwrap()
        .expect("certificate in the dump");
    let cert = OrcCertificate {
        ref_no: "TESTBOAT01".to_string(),
        dxt_id: None,
        ..stored
    };

    // A new ORC boat carrying its certificate.
    let created = golden
        .db
        .upsert_boat(&Boat {
            name: "Certified Boat".to_string(),
            rating_type: RatingType::Orc,
            orc_certs: vec![cert.clone()],
            ..Default::default()
        })
        .await
        .unwrap()
        .expect("saved boat");
    assert!(created.id > 0);
    assert_eq!(vec![cert.clone()], created.orc_certs);
    assert_eq!(
        Some(created.id),
        linked_boat(&golden.pool, "TESTBOAT01").await
    );

    // Saved again without it: the certificate stays stored, unlinked.
    let updated = golden
        .db
        .upsert_boat(&Boat {
            orc_certs: Vec::new(),
            rating_type: RatingType::CruisingNonFlyingSails,
            ..created.clone()
        })
        .await
        .unwrap()
        .expect("saved boat");
    assert_eq!(created.id, updated.id);
    assert!(updated.orc_certs.is_empty());
    assert_eq!(None, linked_boat(&golden.pool, "TESTBOAT01").await);

    // An invalid certificate writes nothing, not even the boat row.
    let boats = boat_count(&golden.pool).await;
    let invalid = OrcCertificate {
        ref_no: "TESTBOAT02".to_string(),
        ..Default::default()
    };
    assert!(golden
        .db
        .upsert_boat(&Boat {
            name: "Zero Cert Boat".to_string(),
            rating_type: RatingType::Orc,
            orc_certs: vec![invalid],
            ..Default::default()
        })
        .await
        .is_err());
    assert_eq!(boats, boat_count(&golden.pool).await);
}

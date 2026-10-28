//! The `boat` table: boats with their skipper, rating, race count and linked ORC certificates.

use super::orc_table::InvalidCertificate;
use super::{enum_from_name, orc_table, race_results};
use anyhow::{anyhow, Result};
use regatta_common::{Boat, BoatSkipper, OrcCertificate, Person, RatingType, RATING_DEFAULT};
use regatta_scoring::sort::java_sort;
use sqlx::{FromRow, PgConnection};
use std::cmp::Ordering;
use std::collections::HashMap;

#[derive(FromRow)]
pub(crate) struct Row {
    pub id: i64,
    pub name: String,
    pub sail_number: String,
    pub boat_type: String,
    pub phrf_rating: Option<i32>,
    pub rating_type: String,
}

/// A boat row left-joined to its skipper (`p_*` are `None` when there is none).
#[derive(FromRow)]
struct JoinedRow {
    #[sqlx(flatten)]
    boat: Row,
    p_id: Option<i64>,
    p_first: Option<String>,
    p_last: Option<String>,
    p_club_member: Option<bool>,
    p_active: Option<bool>,
}

pub(crate) const COLUMNS: &str =
    "boat.id, boat.name, boat.sail_number, boat.boat_type, boat.phrf_rating, boat.rating_type";
const PERSON_COLUMNS: &str = "person.id AS p_id, person.first AS p_first, person.last AS p_last, \
                              person.club_member AS p_club_member, person.active AS p_active";

impl JoinedRow {
    fn skipper(&self) -> Option<Person> {
        Some(Person {
            id: self.p_id?,
            first: self.p_first.clone()?,
            last: self.p_last.clone()?,
            club_member: self.p_club_member?,
            active: self.p_active?,
        })
    }
}

/// Builds a [`Boat`] from its row, race count and certificates.
///
/// `skipper_id` and `active` are not read: the boat JSON always carries `None` and `true` for them, which the
/// golden files and contract cassettes pin, so the row does not select them.
pub(crate) fn to_boat(
    row: &Row,
    number_of_races: i64,
    orc_certs: Vec<OrcCertificate>,
) -> Result<Boat> {
    Ok(Boat {
        id: row.id,
        name: row.name.clone(),
        sail_number: row.sail_number.clone(),
        boat_type: row.boat_type.clone(),
        phrf_rating: row.phrf_rating,
        orc_certs,
        skipper_id: None,
        rating_type: enum_from_name(&row.rating_type)?,
        number_of_races,
        active: true,
    })
}

/// Unlinks the person from every boat they skipper, returning the rows changed.
///
/// # Errors
///
/// Returns `Err` if the statement fails.
pub async fn remove_person(conn: &mut PgConnection, person_id: i64) -> Result<u64> {
    Ok(
        sqlx::query("UPDATE boat SET skipper_id = NULL WHERE skipper_id = $1")
            .bind(person_id)
            .execute(conn)
            .await?
            .rows_affected(),
    )
}

/// Returns the boat `boat_id` with its skipper, or `None` if it does not exist.
///
/// # Errors
///
/// Returns `Err` if a query fails or the stored rating type is unknown.
pub async fn find_boat_skipper(
    conn: &mut PgConnection,
    boat_id: i64,
) -> Result<Option<BoatSkipper>> {
    Ok(find_boat_skippers(conn, &[boat_id]).await?.remove(&boat_id))
}

/// Returns the boats with `boat_ids` and their skippers, by boat id; three statements for any number of boats.
///
/// # Errors
///
/// Returns `Err` if a query fails or a stored rating type is unknown.
pub async fn find_boat_skippers(
    conn: &mut PgConnection,
    boat_ids: &[i64],
) -> Result<HashMap<i64, BoatSkipper>> {
    if boat_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<JoinedRow> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS}, {PERSON_COLUMNS} FROM boat LEFT JOIN person ON person.id = boat.skipper_id WHERE boat.id = ANY($1)"
    )))
    .bind(boat_ids)
    .fetch_all(&mut *conn)
    .await?;
    Ok(to_boat_skippers(conn, &rows)
        .await?
        .into_iter()
        .map(|bs| (bs.boat.as_ref().map(|b| b.id).unwrap_or(0), bs))
        .collect())
}

/// Inserts the boat (id 0) or upserts it by id, then links its `orc_certs`, returning it as stored.
///
/// The boat row goes first so a new boat's certificates can reference the id it got (the `orc.boat_id` foreign
/// key); an existing boat's old certificates are unlinked. An invalid certificate fails the call before anything
/// is written, without taking a boat id from the sequence.
///
/// # Errors
///
/// - Returns [`InvalidCertificate`] in the chain if a certificate has a zero rating.
/// - Returns `Err` if a statement fails or the stored rating type is unknown.
pub async fn upsert_boat(conn: &mut PgConnection, boat: &Boat) -> Result<Option<Boat>> {
    if !boat.orc_certs.iter().all(orc_table::validate) {
        return Err(InvalidCertificate.into());
    }
    let set = "name = EXCLUDED.name, sail_number = EXCLUDED.sail_number, boat_type = EXCLUDED.boat_type, \
               phrf_rating = EXCLUDED.phrf_rating, skipper_id = EXCLUDED.skipper_id, active = EXCLUDED.active, \
               rating_type = EXCLUDED.rating_type";
    let returning = "id, name, sail_number, boat_type, phrf_rating, rating_type";
    let row: Option<Row> = if boat.id > 0 {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO boat (id, name, sail_number, boat_type, phrf_rating, skipper_id, active, rating_type) \
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8) ON CONFLICT (id) DO UPDATE SET {set} RETURNING {returning}"
        )))
        .bind(boat.id)
        .bind(boat.name.trim())
        .bind(boat.sail_number.trim())
        .bind(boat.boat_type.trim())
        .bind(boat.phrf_rating)
        .bind(boat.skipper_id)
        .bind(boat.active)
        .bind(boat.rating_type.name())
        .fetch_optional(&mut *conn)
        .await?
    } else {
        sqlx::query_as(sqlx::AssertSqlSafe(format!(
            "INSERT INTO boat (name, sail_number, boat_type, phrf_rating, skipper_id, active, rating_type) \
             VALUES ($1, $2, $3, $4, $5, $6, $7) ON CONFLICT (id) DO UPDATE SET {set} RETURNING {returning}"
        )))
        .bind(boat.name.trim())
        .bind(boat.sail_number.trim())
        .bind(boat.boat_type.trim())
        .bind(boat.phrf_rating)
        .bind(boat.skipper_id)
        .bind(boat.active)
        .bind(boat.rating_type.name())
        .fetch_optional(&mut *conn)
        .await?
    };
    match row {
        Some(row) => {
            if boat.id > 0 {
                orc_table::unlink_certs(conn, row.id).await?;
            }
            for cert in &boat.orc_certs {
                orc_table::upsert_cert(conn, row.id, cert).await?;
            }
            let races = race_results::race_count(conn, row.id).await?;
            let certs = orc_table::find_certificates_of_boat(conn, row.id).await?;
            Ok(Some(to_boat(&row, races, certs)?))
        }
        None => Ok(None),
    }
}

/// Unlinks the boat's certificates, then deletes the boat, returning the rows deleted.
///
/// # Errors
///
/// Returns `Err` if a statement fails (a result still references the boat, say).
pub async fn delete_boat(conn: &mut PgConnection, boat_id: i64) -> Result<u64> {
    orc_table::unlink_certs(conn, boat_id).await?;
    Ok(sqlx::query("DELETE FROM boat WHERE id = $1")
        .bind(boat_id)
        .execute(conn)
        .await?
        .rows_affected())
}

/// Returns the one boat the person skippers, or `None` when they skipper none or several.
///
/// # Errors
///
/// Returns `Err` if a query fails or the stored rating type is unknown.
pub async fn find_boat_for_skipper(
    conn: &mut PgConnection,
    person_id: i64,
) -> Result<Option<BoatSkipper>> {
    let rows: Vec<JoinedRow> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS}, {PERSON_COLUMNS} FROM boat INNER JOIN person ON person.id = boat.skipper_id WHERE boat.skipper_id = $1"
    )))
    .bind(person_id)
    .fetch_all(&mut *conn)
    .await?;
    let mut boats = to_boat_skippers(conn, &rows).await?;
    Ok(if boats.len() == 1 { boats.pop() } else { None })
}

/// Returns every boat with its skipper, sorted for the boats page; three statements in all.
///
/// Boats with a skipper come first; then PHRF boats by rating, ORC boats by their best virtual PHRF rating and
/// other pairs by rating type. That comparator is not a total order across rating types, so both stable sorts use
/// [`java_sort`], which never panics on it and gives the order the contract cassettes recorded.
///
/// # Errors
///
/// - Returns `Err` if a query fails or a stored rating type is unknown.
/// - Returns `Err` if an ORC boat has no certificate to sort it by.
pub async fn select_all_boats(conn: &mut PgConnection) -> Result<Vec<BoatSkipper>> {
    let rows: Vec<JoinedRow> = sqlx::query_as(sqlx::AssertSqlSafe(format!(
        "SELECT {COLUMNS}, {PERSON_COLUMNS} FROM boat LEFT JOIN person ON person.id = boat.skipper_id"
    )))
    .fetch_all(&mut *conn)
    .await?;
    let mut boats = to_boat_skippers(conn, &rows).await?;
    java_sort(&mut boats, |a, b| {
        a.skipper.is_none().cmp(&b.skipper.is_none())
    });
    // an ORC boat without certificates cannot be compared; the first such error is carried out of the sort
    let mut failure: Option<anyhow::Error> = None;
    java_sort(&mut boats, |lhs, rhs| match compare_boats(lhs, rhs) {
        Ok(ordering) => ordering,
        Err(e) => {
            failure.get_or_insert(e);
            Ordering::Equal
        }
    });
    match failure {
        Some(e) => Err(e),
        None => Ok(boats),
    }
}

fn ordinal(rating_type: RatingType) -> usize {
    RatingType::ENTRIES
        .iter()
        .position(|t| *t == rating_type)
        .unwrap_or(0)
}

fn max_virtual_phrf(boat: &Option<Boat>) -> Result<i32> {
    match boat {
        None => Ok(0),
        Some(b) => b
            .orc_certs
            .iter()
            .map(OrcCertificate::virtual_phrf)
            .max()
            .ok_or_else(|| anyhow!("ORC boat {} ({}) has no certificates", b.id, b.name)),
    }
}

fn compare_boats(lhs: &BoatSkipper, rhs: &BoatSkipper) -> Result<Ordering> {
    let default = RatingType::CruisingNonFlyingSails;
    let left = lhs.boat.as_ref().map(|b| b.rating_type).unwrap_or(default);
    let right = rhs.boat.as_ref().map(|b| b.rating_type).unwrap_or(default);
    if left.is_phrf() && right.is_phrf() {
        let l = lhs
            .boat
            .as_ref()
            .and_then(|b| b.phrf_rating)
            .unwrap_or(RATING_DEFAULT);
        let r = rhs
            .boat
            .as_ref()
            .and_then(|b| b.phrf_rating)
            .unwrap_or(RATING_DEFAULT);
        Ok(l.cmp(&r))
    } else if left.is_orc() && right.is_orc() {
        let rmin = max_virtual_phrf(&rhs.boat)?;
        let lmin = max_virtual_phrf(&lhs.boat)?;
        Ok(lmin.cmp(&rmin))
    } else {
        Ok(ordinal(left).cmp(&ordinal(right)))
    }
}

/// Hydrates boat rows joined to the person table (inner or left) with their race counts and ORC
/// certificates from one grouped query each. A row with no person columns has no skipper.
async fn to_boat_skippers(conn: &mut PgConnection, rows: &[JoinedRow]) -> Result<Vec<BoatSkipper>> {
    let boat_ids: Vec<i64> = rows.iter().map(|r| r.boat.id).collect();
    let race_counts = race_results::race_counts_by_boat(conn, &boat_ids).await?;
    let mut certs = orc_table::find_certificates_by_boat(conn, &boat_ids).await?;
    rows.iter()
        .map(|row| {
            let id = row.boat.id;
            Ok(BoatSkipper {
                boat: Some(to_boat(
                    &row.boat,
                    race_counts.get(&id).copied().unwrap_or(0),
                    certs.remove(&id).unwrap_or_default(),
                )?),
                skipper: row.skipper(),
            })
        })
        .collect()
}

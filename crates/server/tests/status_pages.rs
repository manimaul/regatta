//! Tests of the REST error layer: a failure's text never reaches the client.

use axum::body::Body;
use axum::http::{Request, StatusCode};
use axum::routing::get;
use axum::Router;
use http_body_util::BodyExt;
use regatta_server::error::{AppError, AppResult};
use sqlx::error::{DatabaseError, ErrorKind};
use std::borrow::Cow;
use std::error::Error;
use std::fmt;
use tower::ServiceExt;

#[derive(Debug)]
struct FkViolation;

impl fmt::Display for FkViolation {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "update or delete on table \"race\" violates foreign key constraint"
        )
    }
}

impl Error for FkViolation {}

impl DatabaseError for FkViolation {
    fn message(&self) -> &str {
        "update or delete on table \"race\" violates foreign key constraint"
    }
    fn code(&self) -> Option<Cow<'_, str>> {
        Some(Cow::Borrowed("23503"))
    }
    fn as_error(&self) -> &(dyn Error + Send + Sync + 'static) {
        self
    }
    fn as_error_mut(&mut self) -> &mut (dyn Error + Send + Sync + 'static) {
        self
    }
    fn into_error(self: Box<Self>) -> Box<dyn Error + Send + Sync + 'static> {
        self
    }
    fn kind(&self) -> ErrorKind {
        ErrorKind::ForeignKeyViolation
    }
}

async fn boom() -> AppResult {
    Err(AppError(anyhow::anyhow!(
        "secret detail: table raceresults id 86"
    )))
}

async fn fk() -> AppResult {
    let db_error: sqlx::Error = sqlx::Error::Database(Box::new(FkViolation));
    Err(AppError(anyhow::Error::new(db_error).context("wrapped")))
}

fn failing_routes() -> Router {
    Router::new()
        .route("/boom", get(boom))
        .route("/fk", get(fk))
}

async fn get_text(path: &str) -> (StatusCode, Option<String>, String) {
    let response = failing_routes()
        .oneshot(Request::builder().uri(path).body(Body::empty()).unwrap())
        .await
        .unwrap();
    let status = response.status();
    let content_type = response
        .headers()
        .get("content-type")
        .map(|v| v.to_str().unwrap().to_string());
    let body = response.into_body().collect().await.unwrap().to_bytes();
    (
        status,
        content_type,
        String::from_utf8(body.to_vec()).unwrap(),
    )
}

/// An unexpected error answers the generic 500 text, without the error's detail.
#[tokio::test]
async fn unexpected_error_is_a_generic_500() {
    let (status, content_type, body) = get_text("/boom").await;
    assert_eq!(status, StatusCode::INTERNAL_SERVER_ERROR);
    assert_eq!(body, "Internal server error");
    assert!(
        !body.contains("secret"),
        "exception text must not reach the client"
    );
    assert_eq!(content_type.as_deref(), Some("text/plain; charset=UTF-8"));
}

/// A foreign key violation anywhere in the cause chain answers the generic 409 text, without the database detail.
#[tokio::test]
async fn foreign_key_violation_is_a_generic_409() {
    let (status, content_type, body) = get_text("/fk").await;
    assert_eq!(status, StatusCode::CONFLICT);
    assert!(
        !body.contains("raceresults") && !body.contains("constraint"),
        "database detail must not reach the client"
    );
    assert_eq!(
        body,
        "This record is still referenced by other records and cannot be deleted."
    );
    assert_eq!(content_type.as_deref(), Some("text/plain; charset=UTF-8"));
}

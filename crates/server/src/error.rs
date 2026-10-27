//! = `plugins/StatusPages.kt`. Error responses never carry exception text: the detail (SQL,
//! table names, ids) goes to the server log and the client gets a generic message. A foreign key
//! violation is the client's doing (deleting something still in use), so it is a 409 rather than
//! a 500.

use crate::respond::text;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};

/// SQLSTATE class 23503: a delete or update would leave a foreign key dangling.
pub const FOREIGN_KEY_VIOLATION: &str = "23503";
pub const FOREIGN_KEY_TEXT: &str =
    "This record is still referenced by other records and cannot be deleted.";
pub const INTERNAL_TEXT: &str = "Internal server error";

/// Any failure in a handler; rendered by [`IntoResponse`].
#[derive(Debug)]
pub struct AppError(pub anyhow::Error);

impl<E: Into<anyhow::Error>> From<E> for AppError {
    fn from(e: E) -> Self {
        AppError(e.into())
    }
}

/// Whether the error's cause chain holds a Postgres foreign key violation.
pub fn is_foreign_key_violation(error: &anyhow::Error) -> bool {
    error.chain().any(|cause| {
        cause
            .downcast_ref::<sqlx::Error>()
            .and_then(|e| e.as_database_error())
            .and_then(|db| db.code())
            .is_some_and(|code| code == FOREIGN_KEY_VIOLATION)
    })
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        if is_foreign_key_violation(&self.0) {
            tracing::warn!("rejected: record is still referenced by other records");
            text(StatusCode::CONFLICT, FOREIGN_KEY_TEXT)
        } else {
            tracing::error!("request failed: {:#}", self.0);
            text(StatusCode::INTERNAL_SERVER_ERROR, INTERNAL_TEXT)
        }
    }
}

pub type AppResult<T = Response> = Result<T, AppError>;

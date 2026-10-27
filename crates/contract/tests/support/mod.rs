//! An in-process stand-in for the regatta server: just enough of the API (bootstrap, admin
//! creation, login with the real hashing, bearer validation, id-assigning echoes, image upload
//! and download, a stored certificate, the SPA fallback) for the scenario to run end to end.
//! Knobs change its answers so replay and diff have something to report.

use axum::body::Bytes;
use axum::extract::{Path, Query, State};
use axum::http::{header, HeaderMap, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::routing::{get, post};
use axum::Router;
use regatta_contract::auth;
use serde_json::{json, Map, Value};
use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex};

/// Shared across stubs in one process so every login gets a different salt (volatility).
static SALT_COUNTER: AtomicU64 = AtomicU64::new(1);

#[derive(Clone, Default)]
pub struct StubConfig {
    /// Appended to the name of every series the stub echoes back (a value difference).
    pub series_suffix: String,
    /// Echo series objects with their keys reversed (an order difference).
    pub reorse_series_keys: bool,
}

struct Store {
    next_id: i64,
    admin: Option<(i64, String, String)>,
    image: Option<(String, Vec<u8>)>,
}

struct Stub {
    config: StubConfig,
    store: Mutex<Store>,
}

type Shared = Arc<Stub>;

fn json_response(status: StatusCode, value: &Value) -> Response {
    (
        status,
        [(header::CONTENT_TYPE, "application/json; charset=UTF-8")],
        serde_json::to_string(value).unwrap(),
    )
        .into_response()
}

fn text_response(status: StatusCode, text: &str) -> Response {
    (
        status,
        [(header::CONTENT_TYPE, "text/plain; charset=UTF-8")],
        text.to_string(),
    )
        .into_response()
}

fn unauthorized() -> Response {
    (
        StatusCode::UNAUTHORIZED,
        [(header::WWW_AUTHENTICATE, "Bearer realm=\"admin access\"")],
    )
        .into_response()
}

/// Bootstrap: open while no admin exists; otherwise a correctly signed, unexpired token.
fn authorize(stub: &Stub, headers: &HeaderMap) -> Result<(), Box<Response>> {
    let store = stub.store.lock().unwrap();
    let Some((id, stored_hash, _)) = &store.admin else {
        return Ok(());
    };
    let value = headers
        .get(header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .ok_or_else(unauthorized)?;
    let token = value.strip_prefix("Bearer ").ok_or_else(unauthorized)?;
    let decoded = auth::decode_token(token)
        .map_err(|_| text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error"))?;
    let token_id = auth::validate_token(token, stored_hash).map_err(|_| unauthorized())?;
    if token_id != *id {
        return Err(Box::new(unauthorized()));
    }
    let expires = decoded["expires"].as_str().unwrap_or("");
    if auth::epoch_seconds(expires).unwrap_or(0) < jiff::Timestamp::now().as_second() {
        return Err(Box::new(unauthorized()));
    }
    Ok(())
}

fn assign_ids(store: &mut Store, value: Value) -> Value {
    match value {
        Value::Object(map) => {
            let mut out = Map::new();
            if !map.contains_key("id") {
                // Defaults-only bodies: the real server fills `id = 0` and assigns one.
                store.next_id += 1;
                out.insert("id".to_string(), json!(store.next_id));
            }
            for (k, v) in map {
                let v = if k == "id" && v == json!(0) {
                    store.next_id += 1;
                    json!(store.next_id)
                } else {
                    assign_ids(store, v)
                };
                out.insert(k, v);
            }
            Value::Object(out)
        }
        Value::Array(items) => {
            Value::Array(items.into_iter().map(|v| assign_ids(store, v)).collect())
        }
        other => other,
    }
}

fn reverse_keys(value: Value) -> Value {
    match value {
        Value::Object(map) => {
            let mut entries: Vec<(String, Value)> = map.into_iter().collect();
            entries.reverse();
            Value::Object(
                entries
                    .into_iter()
                    .map(|(k, v)| (k, reverse_keys(v)))
                    .collect(),
            )
        }
        Value::Array(items) => Value::Array(items.into_iter().map(reverse_keys).collect()),
        other => other,
    }
}

async fn create_admin(State(stub): State<Shared>, headers: HeaderMap, body: Bytes) -> Response {
    if let Err(r) = authorize(&stub, &headers) {
        return *r;
    }
    let Ok(value) = serde_json::from_slice::<Value>(&body) else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    let (Some(hash), Some(user)) = (value["hash"].as_str(), value["userName"].as_str()) else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    let mut store = stub.store.lock().unwrap();
    if store.admin.as_ref().is_some_and(|(_, _, u)| u == user) {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    }
    store.next_id += 1;
    let id = store.next_id;
    store.admin = Some((id, hash.to_string(), user.to_string()));
    json_response(
        StatusCode::OK,
        &json!({"id": id, "hash": hash, "userName": user}),
    )
}

async fn login(State(stub): State<Shared>, body: Bytes) -> Response {
    let Ok(value) = serde_json::from_slice::<Value>(&body) else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    let (Some(user), Some(hash_of_hash), Some(salt), Some(time)) = (
        value["userName"].as_str(),
        value["hashOfHash"].as_str(),
        value["salt"].as_str(),
        value["time"].as_str(),
    ) else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    let store = stub.store.lock().unwrap();
    let Some((id, stored_hash, admin_user)) = &store.admin else {
        return StatusCode::UNAUTHORIZED.into_response();
    };
    if admin_user != user {
        return StatusCode::UNAUTHORIZED.into_response();
    }
    let expected = auth::time_stamp_hash(auth::epoch_seconds(time).unwrap_or(0), salt, stored_hash);
    if expected != hash_of_hash {
        return StatusCode::UNAUTHORIZED.into_response();
    }
    let expires = "2099-01-01T00:00:00Z";
    let server_salt = format!("stub-salt-{}", SALT_COUNTER.fetch_add(1, Ordering::SeqCst));
    let signed = auth::time_stamp_hash(
        auth::epoch_seconds(expires).unwrap(),
        &server_salt,
        stored_hash,
    );
    let text = auth::login_response_json(*id, &signed, &server_salt, expires);
    (
        StatusCode::OK,
        [(header::CONTENT_TYPE, "application/json; charset=UTF-8")],
        text,
    )
        .into_response()
}

async fn orc_certs(State(stub): State<Shared>, headers: HeaderMap) -> Response {
    if let Err(r) = authorize(&stub, &headers) {
        return *r;
    }
    json_response(
        StatusCode::OK,
        &json!([{"RefNo": "04560003L6C", "APHD": 498.9}, {"RefNo": "04560003NFK", "APHD": 495.0, "YachtName": "Stub"}]),
    )
}

async fn orc_cert(State(stub): State<Shared>, headers: HeaderMap, body: Bytes) -> Response {
    if let Err(r) = authorize(&stub, &headers) {
        return *r;
    }
    let Ok(value) = serde_json::from_slice::<Value>(&body) else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    match value["RefNo"].as_str() {
        Some("04560003NFK") if value["APHD"] != json!(0.0) => json_response(StatusCode::OK, &value),
        Some("04560003NFK") => {
            text_response(StatusCode::BAD_REQUEST, "invalid certificate properties")
        }
        Some(other) => text_response(
            StatusCode::NOT_FOUND,
            &format!("no certificate with reference number {other}"),
        ),
        None => text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error"),
    }
}

async fn upload_image(
    State(stub): State<Shared>,
    headers: HeaderMap,
    Query(q): Query<HashMap<String, String>>,
    body: Bytes,
) -> Response {
    if let Err(r) = authorize(&stub, &headers) {
        return *r;
    }
    let Some(race_id) = q.get("raceId") else {
        return StatusCode::BAD_REQUEST.into_response();
    };
    let Ok(race_id) = race_id.parse::<i64>() else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    if race_id >= 90000 {
        return text_response(
            StatusCode::CONFLICT,
            "This record is still referenced by other records and cannot be deleted.",
        );
    }
    // Takes the first image/png part's bytes, if any.
    let text = String::from_utf8_lossy(&body).to_string();
    if let Some(at) = text.find("Content-Type: image/png\r\n\r\n") {
        let start = at + "Content-Type: image/png\r\n\r\n".len();
        let end = text[start..]
            .find("\r\n--")
            .map(|e| start + e)
            .unwrap_or(text.len());
        let name = text[..at]
            .rsplit("filename=\"")
            .next()
            .and_then(|s| s.split('"').next())
            .unwrap_or("image.png")
            .to_string();
        stub.store.lock().unwrap().image = Some((name, body[start..end].to_vec()));
    }
    StatusCode::ACCEPTED.into_response()
}

/// `GET /v1/api/image<name>`: the real route is one segment with a literal prefix.
fn get_image(stub: &Stub, name: &str) -> Response {
    let store = stub.store.lock().unwrap();
    match &store.image {
        Some((stored, bytes)) if stored == name => (
            StatusCode::OK,
            [(header::CONTENT_TYPE, "image/png")],
            bytes.clone(),
        )
            .into_response(),
        _ => StatusCode::NOT_FOUND.into_response(),
    }
}

const ADMIN_READS: &[&str] = &["allSkippers", "skipper", "find/skipper", "allBoat", "boat"];

async fn generic_get(
    State(stub): State<Shared>,
    headers: HeaderMap,
    Path(rest): Path<String>,
    Query(q): Query<HashMap<String, String>>,
) -> Response {
    if ADMIN_READS.contains(&rest.as_str()) || rest.starts_with("image") {
        if let Err(r) = authorize(&stub, &headers) {
            return *r;
        }
    }
    if let Some(name) = rest.strip_prefix("image") {
        return get_image(&stub, name);
    }
    if let Some(id) = q.get("id").or_else(|| q.get("raceId")) {
        return match id.parse::<i64>() {
            Ok(n) if n < 90000 => json_response(StatusCode::OK, &json!({"id": n, "path": rest})),
            Ok(_) => StatusCode::NO_CONTENT.into_response(),
            Err(_) => text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error"),
        };
    }
    if rest.starts_with("all") || rest.starts_with("find/") || rest == "years" {
        return json_response(StatusCode::OK, &json!([]));
    }
    StatusCode::NO_CONTENT.into_response()
}

async fn generic_post(
    State(stub): State<Shared>,
    headers: HeaderMap,
    Path(rest): Path<String>,
    body: Bytes,
) -> Response {
    if let Err(r) = authorize(&stub, &headers) {
        return *r;
    }
    if headers
        .get(header::CONTENT_TYPE)
        .and_then(|v| v.to_str().ok())
        != Some("application/json")
    {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    }
    let Ok(value) = serde_json::from_slice::<Value>(&body) else {
        return text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error");
    };
    let mut store = stub.store.lock().unwrap();
    let mut echoed = assign_ids(&mut store, value);
    if rest == "series" {
        if !stub.config.series_suffix.is_empty() {
            if let Some(items) = echoed.as_array_mut() {
                for item in items {
                    if let Some(name) = item.get("name").and_then(Value::as_str) {
                        let renamed = format!("{name}{}", stub.config.series_suffix);
                        item["name"] = json!(renamed);
                    }
                }
            }
        }
        if stub.config.reorse_series_keys {
            echoed = reverse_keys(echoed);
        }
    }
    json_response(StatusCode::OK, &echoed)
}

async fn generic_delete(
    State(stub): State<Shared>,
    headers: HeaderMap,
    Query(q): Query<HashMap<String, String>>,
) -> Response {
    if let Err(r) = authorize(&stub, &headers) {
        return *r;
    }
    match q.get("id") {
        None => StatusCode::NO_CONTENT.into_response(),
        Some(id) if id == "1" => text_response(
            StatusCode::CONFLICT,
            "This record is still referenced by other records and cannot be deleted.",
        ),
        Some(id) if id.parse::<i64>().is_ok() => StatusCode::OK.into_response(),
        Some(_) => text_response(StatusCode::INTERNAL_SERVER_ERROR, "Internal server error"),
    }
}

async fn fallback() -> Response {
    (
        StatusCode::OK,
        [(header::CONTENT_TYPE, "text/html; charset=UTF-8")],
        "<!doctype html><html><body>stub</body></html>",
    )
        .into_response()
}

/// Starts a stub on a free port and returns its base url.
pub async fn spawn(config: StubConfig) -> String {
    let stub: Shared = Arc::new(Stub {
        config,
        store: Mutex::new(Store {
            next_id: 1000,
            admin: None,
            image: None,
        }),
    });
    let app = Router::new()
        .route("/v1/api/auth", post(create_admin))
        .route("/v1/api/login", post(login))
        .route("/v1/api/orcCerts", get(orc_certs))
        .route("/v1/api/orcCert", post(orc_cert))
        .route("/v1/api/image", post(upload_image))
        .route(
            "/v1/api/{*rest}",
            get(generic_get).post(generic_post).delete(generic_delete),
        )
        .fallback(fallback)
        .with_state(stub);
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let addr = listener.local_addr().unwrap();
    tokio::spawn(async move {
        axum::serve(listener, app).await.unwrap();
    });
    format!("http://{addr}")
}

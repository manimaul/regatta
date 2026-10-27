//! = `Auth.kt`: the browser-side password hashing. `hash(&["foo", "bar"])` is
//! `base64(sha512("bar" + base64(sha512("foo"))))`, the same vectors as the server's `TokenTest`
//! (`tests/auth_test.rs`).

use base64::prelude::{Engine, BASE64_STANDARD};
use sha2::{Digest, Sha512};

fn hash_internal(data: &str) -> String {
    BASE64_STANDARD.encode(Sha512::digest(data.as_bytes()))
}

/// Chained hashing: each element is hashed together with the previous result.
pub fn hash(data: &[&str]) -> String {
    let mut result = String::new();
    for item in data {
        result = hash_internal(&format!("{item}{result}"));
    }
    result
}

/// A fresh login salt: `crypto.randomUUID()` in the browser, a v4 UUID on the server.
pub fn salt() -> String {
    crate::utils::browser::random_uuid()
}

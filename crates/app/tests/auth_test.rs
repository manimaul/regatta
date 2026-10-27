//! = `AuthTest.kt`, with the vectors of the server's `TokenTest`.
#![cfg(not(target_arch = "wasm32"))]

use regatta_app::auth_hash::hash;

#[test]
fn hash_test() {
    assert_eq!(
        hash(&["foo"]),
        "9/u6bgY2+JDlb7vzKD5STG+jIErimDgtYkdB0NxmODJuKCxBvl5CVNiCB3LFUYosWowMf37aGVlKfrU5RT4e1w=="
    );
}

#[test]
fn hash_many_test() {
    assert_eq!(
        hash(&["foo", "bar", "baz"]),
        "+UgXfdhOoVIqZNkC04ECxn5wEV+Oaij6/7UCPjgx4WENCMm3tq1wqv4Eslte9C5OHtRnoqkHDFhyGb8UWVQI5w=="
    );
}

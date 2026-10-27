# regatta-contract

The REST API contract of the regatta server as a scripted scenario, recorded into cassettes
from one server and replayed against another (plan: `docs/explore_unimplemented/rust_port.md`,
"The API contract", T2 and T3). The Kotlin server is the reference; the Rust port must answer
every step the same way.

```shell
cargo run -p regatta-contract -- record --base-url http://localhost:8888 --out crates/contract/cassettes [--fixed-now 2026-10-14T02:10:11Z]
cargo run -p regatta-contract -- replay --base-url http://localhost:8889 --cassettes crates/contract/cassettes
cargo run -p regatta-contract -- diff --left http://localhost:8888 --right http://localhost:8889 [--random 200 --seed 42] [--fixed-now ...]
```

The committed `cassettes/` (274 steps) were recorded from the Kotlin server started as
`JAVA_OPTS="-Djdbcurl=jdbc:postgresql://localhost:5433/regatta -Duser=regatta_admin -Dpassword=mysecretpassword -Dregatta.now=2026-10-14T02:10:11Z" server/build/install/regatta-jvm/bin/regatta`
against a `postgres:16-bookworm` container holding the dump; the recorded facts are summarised in
the plan's "Quirks to reproduce" section.

Every run needs the server(s) on a **fresh load of the golden dump**
(`server/src/jvmTest/resources/golden/*.sql`, `python3 -I database/db_actions.py restore-dev --dump ...`):
the scenario references its ids (series 1, race 1, class 1, bracket 16, person 2 and 31, boat 1,
certificate `04560003NFK`), its `auth` table is empty (the run starts in bootstrap mode) and its
sequences decide the ids of the records the scenario creates. `replay` and `diff` exit 1 on any
mismatch and print the differences per step; `--quiet` drops the per-step progress on stderr.

The three subcommands are also a library (`regatta_contract::{record, replay, diff}` over
`regatta_contract::scenario(&ScenarioOptions)`), so the server crate's tests can start a server
in-process and replay the committed cassettes.

## The scenario

`src/scenario.rs` builds a `Vec<Step>` from a small builder (`src/step.rs`): each step has a
name, method, path (relative to `/v1/api`, or absolute for the fallback steps), query, headers,
body (JSON / text under any content type / raw bytes / multipart parts with a fixed boundary /
a value captured from an earlier response with pointer overrides / none), an auth mode, a compare
mode, volatile JSON pointers and captures. Placeholders `{{name}}` in paths, query values and
JSON bodies are filled from captures (`{{series_id}}` stays a number inside JSON).

In order:

1. **Bootstrap**: admin routes while the `auth` table is empty. Ktor's bearer provider challenges
   (401) when the header is absent or empty even then, so the bootstrap steps send `Bearer none`
   (what the web client sends when logged out; any non-empty value works); `POST auth` creates the
   admin `contract` with `hash("contract-password")`; the same routes are closed afterwards.
2. **Login**: wrong password, unknown user, missing fields, malformed JSON, empty body, then the
   real login with a fixed salt and time (the `Login` body is deterministic; the server checks no
   time window). The token is `base64` of the response body, as the web client stores it.
3. **Auth modes** on admin routes: live token, missing header, `Bearer none` (what the web client
   sends when logged out), malformed base64, valid base64 that is not JSON, JSON of the wrong
   shape, an expired token, a forged valid token (client-chosen salt, signed with the stored
   hash), a forged token with a wrong hash, an unknown auth id, a `Basic` header, an empty bearer;
   a mutation under each bad mode, read back to prove it was not applied.
4. **Public reads**, every route with: the happy path, a missing id (204), no id, a non-numeric
   id (`toLong()` routes vs `toLongOrNull()` ones), an overflowing id, a double-slash path.
5. **Admin reads**, same shape; `boat` by `id` and by `person_id`.
6. **Mutations** per aggregate (series, class + brackets, person + boat, race + schedule +
   results, image, ORC certificate): create, read back, update, then the error paths: unknown
   JSON key, bad enum name, empty body, wrong `Content-Type`, not-a-list, defaults-only body,
   `validateRace` / `validateResult` 400 texts, foreign-key 409s (unknown series / class /
   skipper / boat / certificate on insert; class, bracket, person, boat, series in use on delete),
   delete-race-with-results 409, duplicate names, unicode names.
7. **Image**: multipart with an `image/png` part (a 73-byte PNG built in `src/png.rs`) and a
   `text/plain` part the server ignores; only the text part; no parts; no `raceId`; a non-numeric
   one; an unknown race; not multipart; no token. Then the download: the route is
   `/v1/api//image{name}`, one segment with a literal prefix, so the image is at
   `/v1/api/imagecontract.png` (`GET image<name>`), with a 404, an empty name, no token,
   `Bearer none`, the `/v1/api/image/<name>` path (the SPA fallback), and the schedule/report of
   race 1 showing the image. The image goes to race 1 because there is no
   delete route for images and race 1 (with results) can never be deleted anyway.
8. **ORC**: `GET orcCerts`; `POST orcCert` with a stored certificate sent back unchanged, an
   unknown reference (404 text), a zeroed field (400 text), an unknown key. `GET orcCert` is
   skipped: it calls data.orc.org.
9. **Deletes** of everything created, in dependency order, with the no-id / non-numeric /
   missing-id variants, so a run leaves only the admin and race 1's image behind.
10. **Fallback** (`compare = status_and_media_type`): `/`, an unknown page, an unknown
    `/v1/api/...` path, a wrong method on a known path, `/v1/api/image/`, `/v1/api`.
11. With `diff --random N`: N proptest-generated `POST results` / `race` / `series` / `skipper`
    steps (fields omitted, null or set; every scoring option and band; unicode names; unknown
    ids), deterministic from `--seed`.

`gzip`: one `GET report?raceId=1` sends `Accept-Encoding: gzip`; bodies are decoded before
recording and comparison, the `Content-Encoding` header is recorded.

A unit test checks the step list against `ROUTES`, the plan's route table, in both directions.

## Tokens and volatile fields

The harness implements the hashing of `Token.kt` / `Auth.kt` byte for byte (`src/auth.rs`,
pinned by the `TokenTest` vectors). The live token is written to cassettes as
`Bearer {{token}}` and substituted on replay; forged tokens are deterministic (fixed salt and
expiry, signed with the stored hash, auth id from the dump's sequence) and recorded verbatim.

`LoginResponse.salt` and `hashOfHash` are always volatile (random server salt): compared by JSON
type only. `expires` is `now + 30 days`, so it is volatile too unless the server runs with the
test-only fixed clock (`-Dregatta.now` on Kotlin, `REGATTA_FIXED_NOW` on Rust) and `record` /
`diff` get `--fixed-now <instant>`; the instant is written to `_meta.json`, and a replay against
a server without that clock reports `expires` as a mismatch. The fixed clock also keeps the
series standings (`NotYetSailed` vs `Cancelled`) stable over time; `2026-10-14T02:10:11Z`, the
dump's instant, is the one to use.

## Comparison

`src/compare.rs`, for a step compared in full:

- status exact;
- `Content-Type` and `WWW-Authenticate` as exact strings; `Content-Encoding`, `Content-Length`,
  `Date`, `Server` and every other header recorded but never compared;
- JSON bodies: equal as parsed values **and** byte for byte after canonical (compact serde_json)
  re-serialisation. `serde_json` runs with `preserve_order`, so the second check fails only on key
  order, reported as a separate `JsonOrder` mismatch listing each object's key sequence; value
  differences are listed by JSON pointer (missing, unexpected, length, value);
- text and binary bodies exact (unified diff / first differing byte);
- volatile pointers: same JSON type, then masked;
- fallback steps: status and media type (`text/html`) only, since SSR changes the body.

Replay also checks that the request the scenario builds now equals the recorded one
(`RequestDrift`), so a changed scenario is reported rather than silently compared against stale
cassettes.

## Cassette format

One pretty-printed JSON file per step, `NNN-<name>.json` (1-based), plus `_meta.json`
(`base_url`, `recorded_at`, `fixed_now`, `steps`):

```json
{
  "index": 19,
  "name": "login",
  "auth": "none",
  "compare": "json_with_volatile",
  "volatile": ["/salt", "/hashOfHash"],
  "request": {
    "method": "POST",
    "url": "/v1/api/login",
    "headers": { "content-type": "application/json" },
    "body": { "json": { "userName": "contract", "hashOfHash": "...", "salt": "contract-login-salt", "time": "2026-10-14T02:10:11Z" } }
  },
  "response": {
    "status": 200,
    "headers": { "content-type": "application/json", "content-length": "179" },
    "body": { "json": { "id": 4, "hashOfHash": "...", "salt": "...", "expires": "..." } }
  }
}
```

`auth` is `"none"`, `"admin"`, `{"raw": "Bearer none"}` or `{"forged": {...}}`. Bodies are
`{"json": ...}`, `{"text": "..."}` (any UTF-8 body under a textual content type, including
error texts and the HTML fallback), `{"base64": "..."}` (binary, e.g. the PNG) or
`{"multipart": [{"name", "filename", "content_type", "text" | "base64"}]}`; `null` when empty.
Response bodies are classified by the `Content-Type` received; a JSON content type whose body
does not parse is kept as text.

## Tests

`cargo test -p regatta-contract` needs no server: unit tests for the hash vectors, placeholder
filling, request building (multipart bytes, forged tokens), body classification, cassette round
trip, the PNG, gzip decoding, every comparison outcome, the generator's determinism and the
scenario's shape (route coverage, unique names, captures before use, bootstrap before admin,
deletes after creates); and `tests/e2e.rs`, which runs record, replay and diff over real HTTP
against a small in-process axum stub (`tests/support`) with knobs that provoke a value
mismatch and a key-order mismatch.

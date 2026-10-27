# Rust port: axum server + Leptos UI

Status: plan, nothing implemented. Branch `rust_port`.

Replace the Kotlin/Ktor server and the Compose HTML SPA with Rust: an **axum** server and a
**Leptos** front end, **server-side rendered and hydrated** in the browser, against the **same
PostgreSQL database and schema**, with a REST API that returns the **same responses** as the Kotlin
server. The approved golden files (`server/src/jvmTest/resources/golden/approved`) and a recorded
set of HTTP exchanges are the contract; neither is re-approved during the port.

Out of scope: new features, schema changes, new API URLs, a new token format (existing logins and
copied auth tokens keep working), a new look (`app.css` carries over).

## Decisions

- **D1 axum, not actix.** hyper 1 + tower; `tower-http` gives compression and tracing, and
  `leptos_axum` is the first-party Leptos integration.
- **D2 sqlx with hand-written SQL, not an ORM.** The Kotlin side already drops to raw SQL where it
  matters (`Database.kt`), the ORC certificate is a `jsonb` column, and sqlx's offline mode
  (`.sqlx/`) checks every query against the real schema at build time. No schema generation from
  Rust types: the schema is frozen (D6).
- **D3 Leptos SSR + hydration, built with cargo-leptos.** One binary serves the REST API, the
  server-rendered pages and the hydration bundle. The UI reads and writes through Leptos **server
  functions** that call the same service layer the REST handlers call, so a page arrives rendered
  with its data and the browser never round-trips through `/v1/api` for its own screens. The REST
  API stays complete and is the tested contract: `database/*.py`, the contract harness and any
  third party use it. Consequences: the auth token moves from `localStorage` to a **cookie** with
  the same value (the server must know who is logged in to render admin screens), and the theme
  choice is mirrored into a cookie so the server renders `data-theme` without a flash.
- **D4 Tailwind v4 through cargo-leptos** (`tailwind-input-file`; no node, no webpack). Tailwind
  scans the `.rs` sources, so the "class names are literal strings" rule stays.
- **D5 jiff for time.** `jiff::Timestamp` is `Instant`, `jiff::SignedDuration` is `Duration`, and
  `America/Los_Angeles` comes from jiff's bundled tz database. JSON formats are **not** jiff's: the
  kotlinx formats are reproduced by two small serde modules (see "JSON rules").
- **D6 Schema frozen at `metadata.version = 5`, and only 5 is supported.** The Rust server never
  auto-creates or alters tables the way Exposed's `SchemaUtils` does. On startup: no `metadata`
  table → apply the baseline (the schema the Kotlin server creates from an empty database, version
  row `5`); version `5`, or `15` (what a fresh Kotlin install writes, `currentDbVersion + 10`) →
  serve; anything else → refuse with "run the last Kotlin release first". No upgrade steps are
  ported. The golden snapshot is at 4 today and is brought to 5 in Phase 0. Later changes are
  sqlx migrations.
- **D7 `timestamp without time zone` columns are UTC.** The Kotlin server forces the JVM default
  time zone to UTC for exactly this; sqlx reads them as naive datetimes and the mapping layer tags
  them UTC.
- **D8 Scoring is its own pure crate** with the same seams the test audit extracted
  (`card`, `report`, `standings`, `effective_race_class`, `validate_*`), so the rule tests and the
  golden test run without a database and the server is a thin wrapper.
- **D9 Build and deploy tasks move from Gradle to `cargo xtask`**, same names (`run`, `makeImg`,
  `pubImg`, `k8sApplySecrets`, `k8sApplyServer`, `rolloutRestart`, `deployServer`, `golden`,
  `contract`), same `~/.gradle/gradle.properties` credentials until the deploy docs are rewritten.
  Day-to-day development is `cargo leptos watch`.
- **D10 The Kotlin tree stays in place and green until cutover.** The Cargo workspace lives beside
  it (`Cargo.toml` at the root, crates under `crates/`). The parent monorepo keeps including the
  three Gradle projects until the cutover PR removes them from its `settings.gradle.kts`.

- **D11 Every UI route renders in `SsrMode::Async`, and `<Routes>` sits under no `<Suspense>`.**
  leptos_axum builds its route list by rendering the app synchronously; a `<Suspense>` renders only
  its fallback there, so routes nested inside one never register, the file handler serves every
  page out of order (an empty body plus one `<template>` chunk), and a `<Redirect>` can never set
  a status. The nav bar and each screen (`Screen`) carry their own boundaries instead, and response
  side effects (Home's redirect) wait for `LoginContext::resolved()` so the registering pass never
  fires them. `build_router` logs every generated route at debug level as the regression guard.

## Layout

The cargo-leptos three-crate shape: the UI is a library compiled twice (`ssr` into the server,
`hydrate` into wasm).

```
Cargo.toml                      workspace; [workspace.dependencies] is the version catalog;
                                [[workspace.metadata.leptos]] configures cargo-leptos
rust-toolchain.toml             pinned stable + wasm32-unknown-unknown
crates/common/                  regatta-common: models, ApiPaths, labels, Utils (= Kotlin `common`)
crates/scoring/                 regatta-scoring: RaceResultReporter, validation, fixture DSL (= results/)
crates/app/                     regatta-app: Leptos components, routes, server functions, app.css
                                (= Kotlin `web`; features `ssr` / `hydrate`)
crates/frontend/                regatta-frontend: cdylib, the hydrate entry point (a few lines)
crates/server/                  regatta-server: axum bin; REST routes, service layer, sqlx, auth,
                                ORC client, leptos_axum routes, static site
crates/contract/                regatta-contract: record / replay / diff harness and its cassettes
crates/xtask/                   image, deploy, golden and contract tasks
migrations/                     0001_baseline.sql
Containerfile                   multi-stage: rust + cargo-leptos builder -> distroless runtime
```

Kotlin module → crate: `common` → `common`; `server/results` → `scoring`; the rest of `server` →
`server`; `web` → `app` + `frontend`. `common/src/commonMain/resources/static/` (burgee,
`ref_num.png`) moves to `crates/app/public/`; `index.html` becomes the Leptos shell component.

The service layer (`crates/server/src/service/`) is the one place the database is called from;
REST handlers and server functions are both thin adapters over it, so the contract tests on the
REST side also exercise what the UI uses.

## Library inventory

Pin exact versions with `cargo add` when the workspace is created and keep them in
`[workspace.dependencies]`; the versions below are floors, not targets.

| Crate | Floor | Used by | Role (Kotlin counterpart) |
|---|---|---|---|
| `tokio` (`rt-multi-thread`, `macros`, `net`, `signal`) | 1.40 | server, contract | async runtime (coroutines / Netty) |
| `axum` (`json`, `query`, `multipart`, `macros`) | 0.8 | server | HTTP router, extractors (Ktor routing, ContentNegotiation, receiveMultipart) |
| `axum-extra` (`cookie`) | 0.10 | server | the auth and theme cookies |
| `tower` | 0.5 | server | middleware plumbing |
| `tower-http` (`fs`, `compression-gzip`, `compression-deflate`, `compression-br`, `trace`, `set-header`) | 0.6 | server | precompressed static site, Compression plugin, request logging |
| `hyper`, `http`, `http-body-util` | 1.x | server, contract | low-level HTTP types |
| `leptos` (`ssr` in server, `hydrate` in frontend), `leptos_router`, `leptos_meta` | 0.8 | app, frontend, server | Compose HTML + runtime; `Router.kt`, `RouteMatcher.kt`; `<Title>`, `<Html attr:data-theme>` |
| `leptos_axum` | 0.8 | server | SSR routes, server function handlers, `file_and_error_handler`, extractors inside server functions |
| `server_fn` (via leptos) | 0.8 | app | the UI's data calls (`Api.kt` + `Network.kt`) |
| `cargo-leptos` (tool, not a crate) | 0.2 | — | build/watch: ssr bin + hydrate wasm, Tailwind v4, `--precompress` (webpack, `jsBrowserDevelopmentRun`, `precompress.js`) |
| `serde`, `serde_json` | 1.0 | all | `@Serializable` / kotlinx.serialization JSON |
| `jiff` (`serde`; server adds `tzdb-bundle-always`) | 0.2 | common, scoring, server | `Instant`, `Duration`, Pacific calendar year (kotlinx-datetime) |
| `sqlx` (`runtime-tokio`, `tls-rustls`, `postgres`, `json`, `migrate`) | 0.8 | server | Postgres pool, queries, `jsonb`, baseline migration (Exposed + pgjdbc) |
| `sha2` | 0.10 | server, app | SHA-512 (`MessageDigest`, `crypto.subtle.digest`) |
| `base64` | 0.22 | server, app | standard alphabet with padding (`java.util.Base64`, `kotlin.io.encoding.Base64`) |
| `rand` (`getrandom` with `js` on wasm) | 0.9 | server, app | token salt (`SecureRandom`, `crypto.randomUUID`) |
| `uuid` (`v4`, `js`) | 1.x | app | `crypto.randomUUID()` for the login salt |
| `reqwest` (`rustls-tls`, `json`) | 0.12 | server, contract | ORC lookups on data.orc.org (`java.net.http.HttpClient`); contract harness client |
| `quick-xml` | 0.37 | server | `activecerts` XML, DOCTYPE/entities rejected (`DocumentBuilderFactory` with secure features) |
| `tracing`, `tracing-subscriber` (`env-filter`, `json`) | 0.1 / 0.3 | server | logging (slf4j/logback) |
| `thiserror`, `anyhow` | 2 / 1 | all | error types |
| `clap` (`derive`, `env`) | 4 | server, contract, xtask | config from env/flags (`-Djdbcurl`, `-Duser`, `-Dpassword`) |
| `wasm-bindgen`, `wasm-bindgen-futures`, `js-sys`, `web-sys` | 0.2 / 0.3 | app (hydrate) | `<dialog>`, ResizeObserver, history; interop with the CDN globals `maplibregl` and `Sortable` |
| `console_error_panic_hook`, `console_log` | — | frontend | browser console logging |
| `wasm-bindgen-test` | 0.3 | app (dev) | hydrated component tests in headless Chrome and Firefox (Karma) |
| `testcontainers` + `testcontainers-modules` (`postgres`) | 0.23 | scoring, server, contract (dev) | throwaway Postgres loaded with the golden dump (Testcontainers) |
| `proptest` | 1 | scoring, contract (dev) | invariant tests (`InvariantsTest`), random payloads for the differential run |
| `pretty_assertions`, `similar` | 1 / 2 | dev | readable JSON diffs on golden and contract mismatches |
| `xshell` or `duct` | — | xtask | shelling out to podman, kubectl, cargo-leptos |
| `vergen-gitcl` (or a 30-line `build.rs`) | 1 | common | `VersionInfo` (git hash, branch, dirty, build date) |

Not ported: Netty, Exposed, logback, kotlinx-coroutines, Compose, webpack, PostCSS, Karma, Trunk
(not needed with cargo-leptos). Unchanged: PostgreSQL 16, pgbouncer sidecar, MapLibre GL JS 4.7.1
and SortableJS 1.15.2 from the CDN, Tailwind CSS 4 (`@tailwindcss/forms` via `@plugin`), the
`database/*.py` scripts.

## The API contract

Every route is `/v1/api/<path>`; `standings` is `series/standings` (no leading slash in
`ApiPaths`, same URL once prefixed). "Admin" means the `authenticate` block: bearer token
required unless the `auth` table is empty (bootstrap).

| Method | Path | Auth | Input | Responses |
|---|---|---|---|---|
| GET | series | — | `id` | Series · 204 |
| GET | allSeries | — | | [Series] |
| GET | find/series | — | `name` | [Series] · 204 |
| GET | series/standings | — | `id`, `year` | StandingsSeries · 204 |
| POST | series | admin | [Series] | [Series] |
| DELETE | series | admin | `id` | 200 · 204 |
| GET | raceClass | — | `id` | RaceClass · 204 |
| GET | allClasses | — | | [RaceClassBrackets] |
| POST | raceClass | admin | RaceClassBrackets | RaceClassBrackets · 204 |
| POST | raceClassList | admin | [RaceClass] | [RaceClassBrackets] |
| DELETE | raceClass | admin | `id` | 200 · 204 |
| GET | bracket | — | `id` | Bracket · 204 |
| POST | bracket | admin | Bracket | Bracket · 500 |
| DELETE | bracket | admin | `id` | 200 · 204 |
| POST | login | — | Login | LoginResponse · 401 |
| POST | auth | admin | AuthRecord | AuthRecord · 500 |
| GET | years | — | | [String] |
| GET | allRaces | — | `year` | [RaceSchedule] · 400 |
| GET | race | — | `id` | Race · 204 |
| POST | race | admin | Race | Race · 400 text (validateRace) · 409 |
| DELETE | race | admin | `id` | 200 · 204 · 409 text (has results) |
| GET | raceSchedule | — | `id` | RaceSchedule · 204 |
| POST | raceSchedule | admin | RaceSchedule | RaceSchedule · 400 text · 400 |
| GET | results | — | `raceId` | [RaceResult] · 204 |
| POST | results | admin | RaceResult | RaceResult · 400 text (validateResult) · 409 |
| DELETE | results | admin | `id` | 200 · 204 |
| GET | scheduleResults | — | `raceId` | RaceScheduleResults · 204 |
| GET | resultCount | — | `raceId` | Long · 400 |
| GET | report | — | `raceId` | RaceReport · 204 |
| POST | image | admin | `raceId`, multipart (image/* parts only) | 202 · 400 |
| GET | image/{name} | admin | | bytes, content type from extension · 404 |
| GET | allSkippers | admin | | [Person] |
| GET | skipper | admin | `id` | Person · 204 |
| GET | find/skipper | admin | `name` | [Person] · 204 |
| POST | skipper | admin | Person | Person · 409 |
| DELETE | skipper | admin | `id` | 200 · 204 |
| GET | allBoat | admin | | [BoatSkipper] |
| GET | boat | admin | `id` or `person_id` | BoatSkipper · 204 · 400 |
| POST | boat | admin | Boat | Boat · 500 |
| DELETE | boat | admin | `id` | 200 · 204 |
| GET | orcCert | admin | `refNo` | OrcCertificate (from data.orc.org) · 204 · 400 |
| GET | orcCerts | admin | | [OrcCertificate] |
| POST | orcCert | admin | OrcCertificate | OrcCertificate · 404 text · 400 text |
| any | anything else | — | | 200 `text/html` (today `index.html`; after the port the server-rendered shell with the NotFound view) |

Cross-cutting (`StatusPages.kt`): an exception whose cause chain has SQLSTATE `23503` → 409 text
`This record is still referenced by other records and cannot be deleted.`; any other → 500 text
`Internal server error`; never exception text. Responses are gzip/deflate compressed per
`Accept-Encoding`; the hydration bundle and CSS are served precompressed (br, gz).

The fallback row is the one place the body legitimately changes: SSR renders markup where Ktor
returned the empty shell. The contract compares status and media type for it, not the body.
Server-function endpoints (`/api/<fn>` by default; configure a prefix that cannot collide with
`/v1/api`) are new and outside the contract.

### JSON rules

The server's JSON is Ktor's `DefaultJson` (`encodeDefaults = true`, nulls written) and the
golden files are the same with `prettyPrint` (4-space indent). The Rust types must reproduce it:

- **Field order** = Kotlin declaration order (serde keeps struct order; mirror the Kotlin order).
- **Defaults are written**, so every field is present; on input every Kotlin field with a default
  is optional → `#[serde(default)]` per field, and `Option` fields default to `None`.
- **Nulls are written** (`"reportImage": null`), except `RaceReportCard.orcDxtId`
  (`@EncodeDefault(NEVER)`) → `#[serde(skip_serializing_if = "Option::is_none")]`.
- **Enums by name** (`"ORC_PHRF"`, `"FiveBandWindwardLeeward"`, `"DNS_RC"`): no renaming.
- **`Instant`** → kotlinx `Instant.toString()`: UTC, `Z`, fractional seconds only when non-zero,
  in groups of 3 digits (`2026-07-09T10:45:56.233Z`, never `.2` or `.2330`). Custom serde module
  `kotlin_instant` over `jiff::Timestamp`.
- **`Duration`** → kotlin `Duration.toIsoString()`: `PT4H5M29S`, `PT1H0M10.173160173S`, `PT0S`,
  negative as `-PT…`; hours, minutes and seconds components as Kotlin emits them, fractional
  seconds with trailing zeros trimmed. Custom serde module `kotlin_duration` over
  `jiff::SignedDuration`. The approved files are the test vectors.
- **Numbers**: Kotlin `Long`/`Int` → `i64`/`i32`; `Float` (bracket ratings) → `f32`; `Double` →
  `f64`. JDK 21 and serde_json both print the shortest round-trip form (`1.0030864197530864`,
  `10.0`), differing only in exponent thresholds that no field reaches; the golden byte comparison
  proves it.
- **Corrected time arithmetic**: `ms = (finish - start).inWholeMilliseconds * tcf` as `f64`, then
  `Double.toDuration(MILLISECONDS)` = nanoseconds rounded to nearest (`roundToLong`); the ISO
  string carries those nanos. Same operation order in Rust, no shortcuts.
- **Computed Kotlin properties** (`by lazy`, functions) are not serialized; don't add fields.
- **ORC certificate**: every `@SerialName("US_…")` key verbatim; `dxtId` never written while null;
  unknown keys from ORC ignored on the ORC client's parser only (`ignoreUnknownKeys = true` there).
- **Request parsing**: what Kotlin does with unknown keys, missing required fields (`Login`,
  `AuthRecord.hash`), bad enum names and non-numeric `id` query params (`toLong()` throws → 500;
  `toLongOrNull()` on `allRaces`/`DELETE race` → 400/204) is **recorded, not assumed** (Phase 0).
- Server functions use the same `common` types, so the UI and the REST API cannot drift; their
  wire encoding (server_fn's default JSON) is not the contract.

### Auth, byte for byte

`Token.kt` ⇄ `Auth.kt`: `hash(a, b, c)` = `b64(sha512(c + b64(sha512(b + b64(sha512(a))))))`,
standard base64 with padding. Admin creation stores `hash(password)` computed in the browser; login
sends `hash(salt, epochSeconds, hash(password))`; the server answers `LoginResponse` with
`hash(serverSalt, expiresEpochSeconds, storedHash)` and a 30-day `expires`; the bearer token is
`base64(json(LoginResponse))` and is validated by recomputing that hash. `TokenTest` vectors
(`"foo"` → `9/u6bgY2+…`) pin the implementation on both the server and the wasm side; the wasm side
must match or existing admins cannot log in.

With SSR the browser keeps the same token string in a cookie (`regatta_token`, `HttpOnly`,
`SameSite=Lax`, `Secure` behind TLS) instead of `localStorage`; server functions read it with the
`leptos_axum` cookie extractor and validate it with the same `Token` code as the REST bearer
extractor. Logins made before the cutover are lost (they were in `localStorage`), which is a
re-login, not a migration. "Copy auth token" on the home page keeps producing the bearer value the
Python scripts take.

## Testing strategy

**T1 Golden, byte for byte.** `crates/scoring/tests/golden.rs` loads the golden dump (at version
5 after Phase 0) into a testcontainers Postgres through the Rust server's own startup path
(version check), scores every race report and every series standings with `now = dumpInstant`,
pretty-prints with 4-space indent and compares **byte for byte** with the existing approved files.
No re-approval during the port: a diff is a porting bug. The GoldenTest's
`schemaVersionIsCurrentAfterStartup` is ported too. `database/audit_scoring.py` keeps running over
the same files and `deviations.json` stays empty.

**T2 Contract cassettes: record from Kotlin, replay against Rust.** `crates/contract` has two
modes. `record` drives a running server through a scripted scenario and writes one cassette per
step (`cassettes/NNN-<name>.json`: method, path, query, headers sent, body sent; status, headers,
body received, plus a list of volatile JSON pointers). `replay` runs the same scenario against a
server and compares: status exact; `Content-Type` exact (set the same header values Ktor sends,
e.g. `application/json; charset=UTF-8`, `text/plain; charset=UTF-8`); JSON bodies compared as
parsed values **and** byte for byte after canonical re-serialisation; text and binary bodies
exact; volatile fields (`LoginResponse.salt`, `hashOfHash`) checked for type and shape only;
fallback steps by status and media type. The scenario starts from a fresh load of the golden dump
so ids and sequences match, and:
- exercises every row of the route table above, every listed status, with and without a token,
  with a bad token, a malformed token, an expired token and no header;
- covers bootstrap (empty `auth` table: every admin route open; `POST auth` creates the admin; the
  same calls afterwards need the token);
- covers the error paths: `validateRace`/`validateResult` messages, delete-race-with-results 409,
  foreign-key 409 (delete a bracket in use), non-numeric and missing query params, unknown JSON
  keys, bad enum names, empty bodies, wrong `Content-Type`;
- covers the multipart image upload (image part, non-image part ignored, no part) and download;
- covers the fallback: unknown path, unknown `/v1/api/...` path, wrong method on a known path;
- covers `Accept-Encoding: gzip` (bodies are compared decoded; the encoding header is recorded);
- skips `GET orcCert` (live data.orc.org call; see T4) but records `GET/POST orcCerts`.
Both servers take a test-only fixed clock (`REGATTA_FIXED_NOW` / `-Dregatta.now`, Phase 0) so
`RaceStatus.NotYetSailed` vs `Cancelled` and `LoginResponse.expires` are stable across time.
`cargo xtask contract record` needs the Kotlin server up against the dump
(`python3 -I database/db_actions.py restore-dev --dump <golden dump>`); `cargo test -p
regatta-contract` starts the Rust server in-process against a container and replays.

**T3 Differential run.** The same harness with `--kotlin http://localhost:8888 --rust
http://localhost:8889`, both on fresh loads of the dump, runs the scripted scenario plus a
proptest-generated stream of valid and invalid payloads (random `RaceResult`s, `Race`s with every
`OrcScoringOption`×band combination, series lists, unicode names) and diffs every response. Run
before cutover and whenever the cassettes are suspected stale.

**T4 Ported unit tests**, same names so coverage can be ticked off against the Kotlin suite:

| Kotlin | Rust |
|---|---|
| `common/jvmTest`: `RatingBandsTest`, `LabelsTest`, `OrcTest`, `PhrfTest` | `crates/common` unit tests |
| `results/`: `CorrectedTimeTest`, `OrcTcfTest`, `RacePlacingTest`, `SeriesStandingsTest`, `InvariantsTest`, `RaceResultReporterKtTest`, `RaceValidationTest`, `ResultValidationTest`, `Fixture.kt` | `crates/scoring` tests + fixture DSL |
| `auth/TokenTest` | `crates/server` auth tests + the same vectors in `crates/app` |
| `orc/OrcClientTest` (+ `resources/orc/activecerts_0456000518S.xml`, an RMS JSON fixture) | `crates/server` orc parser tests |
| `plugins/StatusPagesTest` | `crates/server` axum tests with `tower::ServiceExt::oneshot` |
| `web/jsTest`: `RaceReportLabelsTest`, `StandingsLabelsTest`, `RatedLabelTest`, `RaceConfigTest`, `RouteMatcherTest`, `LightSectorTest`, `AuthTest` | pure functions → `cargo test -p regatta-app` (native) |
| `web/jsTest`: `LayoutTest`, `LeafComponentsTest`, `ModalTest`, `NavBarTest`, `ThemeToggleTest` | SSR render tests natively (`leptos::ssr::render_to_string`, assert on the HTML) for markup and classes; `wasm-bindgen-test` in headless Chrome and Firefox for the interactive parts (dialog open/close, toggle) |

**T5 Serde round trip.** For every approved golden file and every recorded JSON body: parse into
the typed Rust structs, re-serialize, compare byte for byte. This catches a missing or mistyped
field anywhere in the ~100-field certificate and the nested report types without hand-written
cases.

**T6 UI smoke.** After the Leptos port, walk the screens against the dump with the browser
(create admin, log in, add boat, add race, enter results, view report and standings, course map,
theme toggle, a hard reload on every route to prove SSR + hydration) and record a GIF for the PR.

**T7 SSR integration.** axum `oneshot` tests: `GET /` and each UI route return 200 `text/html`
with the rendered content (e.g. the series names from the dump), with and without the auth cookie
(admin nav present or absent), with `theme=dark` cookie (`data-theme="dark"` on `<html>`).

## Phases

Tick each box as it lands. Each phase ends with `cargo test --workspace`, `cargo clippy
--workspace --all-targets -- -D warnings` and, until cutover, the Kotlin tests still green.

### Phase 0: Characterize the Kotlin server and set up the workspace

- [x] Add the test-only fixed clock to the Kotlin server: `-Dregatta.now=<instant>` used by the
      `series/standings` route (`getStandingsReport(now = …)`) and by `Token` for `expires`. No
      behavior change without the property. Record its name in CLAUDE.md.
- [x] Bring the golden snapshot to version 5 (`regatta-2026-10-13-v5.sql`): load `regatta-2026-10-13.sql` into a dev Postgres,
      start the Kotlin server once (runs 4→5), `pg_dump`, scrub (`database/scrub_dump.py`), commit
      as the new `GoldenDb.DUMP` (same `dumpInstant`). Run `:regattaServer:jvmGoldenTest`: the
      approved files must be byte-identical. From here on no Rust code ever sees version 4.
- [x] Write the contract scenario (T2) as data (a list of steps), with the volatile-field pointers.
- [x] Build `crates/contract` `record` mode (reqwest, cassettes as JSON files) and run it against the
      Kotlin server on the dump; commit `crates/contract/cassettes/`. Review the cassettes for the
      quirks under "Request parsing" and the double `respond` in `GET report`; note each quirk the
      port must reproduce in this doc's "Quirks" list below.
- [x] Generate `migrations/0001_baseline.sql`: run the Kotlin server once against an empty
      `postgres:16-bookworm`, `pg_dump --schema-only`, strip owner/ACL noise, replace the
      `metadata` row's `15` with `5`. Check it against `pg_dump --schema-only` of the version-5
      golden database: same tables, columns, types, constraints and indexes (the `checkin` table in
      the snapshot is not in the Kotlin `tables` array and stays out of the baseline).
- [x] Create the Cargo workspace, `rust-toolchain.toml`, `[workspace.dependencies]`,
      `[[workspace.metadata.leptos]]`, `.gitignore` additions (`target/`, `*.received.json` is
      already there), `crates/xtask` with `golden`/`contract` stubs. Install `cargo-leptos`.
- [ ] CI (or a local `cargo xtask check`): fmt, clippy, tests, `cargo leptos build`, Kotlin tests.

### Phase 1: `regatta-common` (models and serde parity)

- [x] Port every type in `RaceData.kt`, `LoginData.kt`, `FinishCode.kt`, `AboutInfo.kt`,
      `OrcCertificate.kt` (all fields, all `@SerialName`s, `OrcResponse`), `ApiPaths.kt`,
      `Utils.kt` (`versioned_api`, `score_text`, `Duration::display`, `move_item`,
      `correctionFactorDefault`, `ratingDefault`) and the enum helpers (`isCruising`, `isPHRF`,
      `isCompatible`, `ratedLabel`, `label()`s, `PhrfBFactor::from_b_factor`, `Orc3Band` /
      `Orc5Band::from_phrf_b_factor`, `uses3Band`/`uses5Band`, `orcBandLabel`, `virtual_phrf`).
- [x] `kotlin_instant` and `kotlin_duration` serde modules with unit tests taken from the golden
      files (whole seconds, 3/6/9-digit fractions, `PT0S`, negative).
- [x] `#[serde(default)]` / `skip_serializing_if` exactly as "JSON rules" says; field order checked
      by a test that compares key order against `race-1.json` and `standings-1-2024.json`.
- [x] T5 round-trip test over all 140 approved files.
- [x] Port `common/jvmTest` (`RatingBandsTest`, `LabelsTest`, `OrcTest`, `PhrfTest`).
- [x] `VersionInfo` from `build.rs` (git hash, branch, dirty flag, build date), consumed by the
      footer; nothing written into `src/`.
- [x] The crate builds for `wasm32-unknown-unknown` (jiff without the tz bundle, no `std::time`
      `SystemTime` on wasm paths).

### Phase 2: `regatta-scoring` (pure scoring) and the golden test

- [x] Port `RaceResultReporter.kt`: `phrf_tcf`, `orc_tcf` (every option × band), `boat_corrected_time`
      (nanosecond rounding as above), `compare`/comparators, `PlaceAmong`, `place` (D3, D4, D7),
      `card`, `report`, `class_card`, `effective_race_class` (the 2025→2026 PHRF-only switch),
      `standings`, `group_scores`, `round_to_tenth`, `tie_breaker`, `assign_places`, D9 ORC rows.
      Keep the GSI/decision comments; they are the spec.
- [x] Port `RaceValidation.kt`, `ResultValidation.kt` with their messages verbatim.
- [x] Port the fixture DSL (`Fixture.kt`): `race(date, start, |r| …)`, `phrf_class`,
      `orc_phrf_class`, `orc_class`, `cruising_class`, `bracket`,
      `boat(name, rating).finishes("20:51:26")`, `.code(DNF)`, `rc_volunteer`, `legacy()`,
      `series { … }`, Pacific local times via jiff, `assert_bracket_scores` /
      `assert_class_scores` / `assert_bracket_totals` / `assert_class_totals`.
- [x] Port every test in `server/src/jvmTest/.../results/` by name (`gsi_14_1_4_…`, `d4_…`, …);
      `InvariantsTest` as proptest strategies where it was property-style.
- [x] Database-free `report`/`standings` entry points take `now: Timestamp`.
- [x] T1 golden test: testcontainers Postgres + version-5 dump + Rust startup path; byte-for-byte
      against the current approved directory (path configurable; moved in Phase 7).
      `cargo xtask golden` sets `DOCKER_HOST` from `podman machine inspect` like the Gradle task (lives in `crates/server/tests/golden.rs`; `crates/scoring/tests/golden_test.rs` is the database-free variant over the approved files' own inputs)
- [x] `database/audit_scoring.py` unchanged and `deviations.json` still empty.

### Phase 3: `regatta-server` (axum + sqlx, REST only)

- [x] Config: `REGATTA_DB_URL` (`postgres://user:pass@localhost:5432/regatta`),
      `LEPTOS_SITE_ADDR` (`0.0.0.0:8888`), `REGATTA_FIXED_NOW` (tests only), `RUST_LOG`; defaults
      match `database/docker-compose.yaml`. Document the mapping from `-Djdbcurl`/`-Duser`/`-Dpassword`.
- [x] Startup (D6): pool; no `metadata` table → run `0001_baseline.sql`; version 5 or 15 → serve;
      otherwise exit non-zero with the "run the last Kotlin release first" message. Log the
      version like `db migration version = N`.
- [x] Service layer: port each table module as a sqlx query module with the same column names
      (`AuthTable`, `BoatTable`, `BracketTable`, `ImageTable`, `OrcTable` jsonb, `PersonTable`,
      `RaceBracketJunction`, `RaceClassTable`, `RaceResultsTable`, `RaceTable`, `RaceTimeTable`,
      `SeriesTable`, `CustomExpressions`), the upserts (`ON CONFLICT`), the batched lookups
      (`countsByRace`, `findCertificatesByBoat`, `reportImageNames`, `findRaceSchedules`) and the
      year-bounds queries with the Pacific calendar (D7 for timestamps). Transactions where
      `dbQuery` wrapped several statements (`deletePerson`, `deleteRace`, `insertSchedule`).
      Every `RegattaDatabase` method becomes a service function the REST handlers and, in Phase 4,
      the server functions call.
- [ ] `.sqlx/` offline data committed; `SQLX_OFFLINE=true` in the image build. — not applicable: the service layer uses the non-macro sqlx API (sqlx 0.9 has no jiff types, timestamps go through `::text`), so there is no `.sqlx/` and builds need no database.
- [x] Auth: `Token` port (sha2 + base64), bootstrap rule, bearer extractor reproducing the
      recorded behavior for missing/malformed/expired tokens; `TokenTest` vectors.
- [x] Routes: one handler per row of the route table, nested under `/v1/api`, same query parameter
      names, same statuses and text bodies, `Content-Type` values matching the cassettes,
      `GET image/{name}` content type from the extension, multipart `image/*` filter, 202.
- [x] Error layer = `StatusPages.kt`: SQLSTATE `23503` → 409 text, else 500 text, detail to the log
      only; port `StatusPagesTest`.
- [x] ORC client: reqwest with redirects and 10 s connect / 30 s request timeouts, BOM stripping,
      `parse_certificate`, `parse_dxt_id` (quick-xml, DOCTYPE rejected), case-insensitive `RefNo`
      match; port `OrcClientTest` with the same fixture files.
- [x] Temporary fallback (until Phase 4): every unmatched route → 200 `text/html` with a static
      placeholder shell, so the contract's fallback steps pass before the UI exists.
      `CompressionLayer` on (superseded by the Leptos shell in Phase 4)
- [x] T2 replay passes against the Rust server (`cargo test -p regatta-contract`).
- [ ] T3 differential run passes (scripted + proptest).
- [ ] `database/fix_orc_bands.py` and `database/orc_backfill.py` run unchanged against the Rust
      server (`--base-url`), dry run and `--apply` on a dev restore.

### Phase 4: `regatta-app` + `regatta-frontend` (Leptos SSR)

Screen and component map (Kotlin → Rust module in `crates/app/src/`):

| Kotlin | Rust |
|---|---|
| `Main.js.kt`, `Router.kt`, `RouteViewModel.kt`, `RouteMatcher.kt`, `index.html` | `lib.rs` (`shell()` with the `<head>`, CDN tags, theme attribute), `app.rs` (`<Router>`, the 15 `Route` paths incl. NotFound, nav, alerts, footer); `crates/frontend/src/lib.rs` hydrates |
| `components/routes/*` (Home, Races, RaceResults, RaceResultsView, Series, SeriesStandings, People, Boats, Classes, Course, Rc, Admin) | `routes/*.rs` |
| `components/*` (NavBar, Alerts, Modal, RgTable, RgGrid, RgForm, RgDropdown, RgButton, RgSwitch, RgSortable, RgSpinner, RgConfirm, TimeRow, RatingSelections, RaceConfigForm, AddEditBoatModal, FinishCodeDrop, RatedLabel, CellLines, ErrorDisplay, ThemeToggle, RgRaceYearSelector) | `components/*.rs`, same `.rg-*` classes |
| `viewmodel/*` (`BaseViewModel` + 17 VMs) | per-feature state: `Resource`s over server functions for loads (SSR-rendered, streamed with `<Suspense>`), `ServerAction`s for mutations, `RwSignal`/`Store` for form state; `Operation`/`Async` → `Result` + pending flags |
| `utils/Api.kt`, `Network.kt` | `server_fns.rs`: one `#[server]` per `Api` call, bodies in the `ssr` feature call the service layer; the plain-text error messages (`validateRace`, `validateResult`, FK 409) surface as `ServerFnError` payloads the UI shows like today |
| `utils/Auth.kt`, `LocalStorage.kt`, `LoginViewModel.kt` | `auth.rs`: hashing in the browser (sha2/base64, same vectors), `login`/`logout`/`create_admin` server functions that set/clear the cookie, a `current_login` resource for the nav and the expiry countdown, "Copy auth token" |
| `utils/MapLibre.kt`, `ChartController.js.kt`, `ChartViewModel.kt`, `LightSector.kt`, `ResizeObserver.kt` | `chart/*.rs` (hydrate-only: wasm-bindgen externs to `window.maplibregl`, `styleimagemissing` closure, OpenENC day/dusk styles; the map container renders empty on the server) |
| `lib/Sortable.kt`, `SortableUtil.kt` | `sortable.rs` (hydrate-only externs to `window.Sortable`) |
| `utils/RaceReport.kt`, `StandingsReport.kt`, `RaceConfig.kt`, `DateUtil.kt`, `StringUtil.kt`, `Scopes.kt` | `labels.rs`, `race_config.rs`, `date.rs` (pure; native unit tests). Dates render in Pacific time on both sides (jiff on the server, the same formatting in wasm) so SSR and hydration agree |
| `ThemeViewModel.kt`, the inline theme script | `theme.rs`: cookie `theme` read by `shell()` for `data-theme`; toggle sets cookie + attribute; the inline script stays as a fallback for a stale cookie-less `localStorage["theme"]` |
| `styles/AppStyle.kt` | folded into `app.css` |
| `resources/app.css`, burgee, `ref_num.png` | `crates/app/style/app.css`, `crates/app/public/` |

- [x] `[[workspace.metadata.leptos]]`: `site-root target/site`, `site-pkg-dir pkg`,
      `site-addr 0.0.0.0:8888`, `style-file`/`tailwind-input-file crates/app/style/app.css`,
      `assets-dir crates/app/public`, `bin-package regatta-server`, `lib-package
      regatta-frontend`, `env = "DEV"`/`"PROD"`; `@source "../src/**/*.rs"`, `@plugin
      "@tailwindcss/forms"`, the `@source inline(...)` safelist, the chart popup overrides outside
      `@layer`. `cargo leptos watch` replaces `jsBrowserDevelopmentRun --continuous` + the server run. (no `env` key: cargo-leptos sets LEPTOS_ENV itself)
- [x] Shell: `<head>` from `index.html` (viewport, author, title via `leptos_meta`, MapLibre CDN
      tags, SortableJS CDN tag since webpack no longer bundles it), `data-theme` from the cookie,
      `HydrationScripts`, `AutoReload` in dev.
- [x] Mount in the server: `leptos_routes` for the UI, `handle_server_fns` under a prefix that is
      not `/v1/api`, REST router nested at `/v1/api`, `file_and_error_handler` serving
      `target/site` precompressed and rendering the shell with NotFound for everything else;
      remove the Phase 3 placeholder fallback. T7 tests. (`build_router` logs every generated UI route at debug level, the D11 regression guard)
- [x] Auth cookie extractor shared by server functions; admin-only server functions return
      `ServerFnError` on a missing/invalid cookie unless bootstrap (empty `auth` table), the same
      rule as the REST side; admin routes render the "log in" state server-side rather than 401.
- [x] Router and NavBar, theme toggle, footer with VersionInfo, alerts.
- [x] Auth flow (create admin, login, logout, expiry countdown, "Copy auth token").
- [x] Port one screen end to end first to settle the pattern (Series: list, sortable, edit, delete,
      server functions, SSR test, hydrate test), then: Home, People, Boats, Classes, Races (+ race
      edit modal, schedule editor), Rc, RaceResults add/edit, RaceResultsView, SeriesStandings,
      Course (map), Admin. Match the current markup and classes so `app.css` needs no changes.
- [x] Port the pure `jsTest`s natively, the markup tests as SSR render tests, the interactive ones
      with `wasm-bindgen-test` (Chrome headless and Firefox; `FIREFOX_BIN` honored). (the 12 wasm-bindgen-test cases compile for wasm32; not executed: no wasm-pack / browser driver installed)
- [ ] T6 smoke walk-through with a GIF (curl smoke of every route and the login flow done; browser walk-through pending). T7 green: `crates/server/tests/ssr.rs`. Render threads need a 32 MiB stack (`RENDER_THREAD_STACK`): the Classes screen overflows the default 2 MiB in a debug build.
- [ ] Bundle size check (wasm + js, gzip) against today's `web.js`; `wasm-opt` via cargo-leptos,
      `opt-level = "z"`/`lto`/`codegen-units = 1` in the `wasm-release` profile.

### Phase 5: Build, image, deploy

- [ ] `cargo xtask run`: `cargo leptos serve --release` (needs `database/podman-compose up`).
- [ ] `cargo leptos build --release --precompress` produces the server binary and
      `target/site` with `.br`/`.gz` siblings (replaces `installJvmDist` + `precompress.js`).
- [ ] `Containerfile`: stage 1 `rust:<pinned>-bookworm` with `wasm32-unknown-unknown`,
      `cargo-leptos` (via `cargo-binstall`), `SQLX_OFFLINE=true`, `--platform linux/amd64`;
      stage 2 `gcr.io/distroless/cc-debian12` with the server binary and `site/`;
      `LEPTOS_SITE_ROOT=site`, `LEPTOS_SITE_ADDR=0.0.0.0:8888`, `LEPTOS_ENV=PROD`; `EXPOSE 8888`;
      env defaults replacing `JDK_JAVA_OPTIONS`.
- [ ] `k8s_deploy/server.yaml`: `JDK_JAVA_OPTIONS` → `REGATTA_DB_URL` built from the same
      `{REGATTA_PG_USER}`/`{REGATTA_PG_PASS}` placeholders; readiness probe on `/`; memory
      request/limit revisited (JVM 256Mi/768Mi → far less).
- [ ] `cargo xtask makeImg | pubImg | k8sApplySecrets | k8sApplyServer | rolloutRestart |
      deployServer`, reading `~/.gradle/gradle.properties` for the credentials as today.
- [ ] pgbouncer: confirm `pool_mode` in `regatta-pgbouncer-ini`; with transaction pooling set
      sqlx's `statement_cache_capacity(0)` or require pgbouncer ≥ 1.21 with
      `max_prepared_statements`.
- [ ] Ingress: the cookie is `Secure`; confirm haproxy forwards `X-Forwarded-Proto` or set the
      flag from config.
- [ ] Deploy to a staging namespace or a second deployment name, point it at a prod restore, run
      T3 against prod-Kotlin vs staging-Rust and T6 against staging.

### Phase 6: Cutover

- [ ] Back up prod (`db_actions.py backup`); confirm `metadata.version = 5` there.
- [ ] `deployServer` the Rust image; watch the rollout, `/`, login with an existing admin (a
      re-login: the old `localStorage` session is gone), a results entry, a standings page.
- [ ] Keep the last Kotlin image tag ready for `kubectl rollout undo` for a week.
- [ ] Parent monorepo: remove the three Gradle projects from its `settings.gradle.kts`
      (separate PR there); nothing in this repo may depend on the parent.

### Phase 7: Cleanup

- [ ] Move `golden/` (dump + approved + deviations) to `crates/scoring/tests/golden/` and update
      `audit_scoring.py` defaults, `scrub_dump.py`, CLAUDE.md.
- [ ] Delete `common/`, `server/`, `web/`, `buildSrc/`, `gradle*`, `settings.gradle.kts`,
      `build.gradle.kts`, `kotlin-js-store/`, `gradle/libs.versions.toml`.
- [ ] Rewrite CLAUDE.md (commands, architecture, the JSON rules above, the fixed-clock knob, the
      contract harness, the service layer / server function / REST split, "run image tasks from
      `regatta/`" note retired), `README.MD`, `database/README.md`.
- [ ] Move this document to `docs/explore_implemented/`.

## Running the Rust stack locally

Prerequisites: the pinned toolchain from `rust-toolchain.toml` (rustup adds `wasm32-unknown-unknown`
from it), `cargo install cargo-leptos` (0.3.x), podman for the database and the container-backed
tests. The Kotlin build is untouched and still works beside this.

Database: either the dev compose database (`cd database && podman-compose up`, port 5432, empty →
the server applies `migrations/0001_baseline.sql` on first start and then runs in bootstrap mode
until an admin is created), or a copy of the golden snapshot:

```shell
podman run -d --name regatta-dev -e POSTGRES_USER=regatta_admin -e POSTGRES_PASSWORD=mysecretpassword \
  -e POSTGRES_DB=regatta -p 5435:5432 postgres:16-bookworm
PGPASSWORD=mysecretpassword psql -h localhost -p 5435 -U regatta_admin -d regatta \
  -f server/src/jvmTest/resources/golden/regatta-2026-10-13-v5.sql
export REGATTA_DB_URL=postgres://regatta_admin:mysecretpassword@localhost:5435/regatta
```

Run (SSR server + hydration bundle + Tailwind, served on http://127.0.0.1:8888):

```shell
cargo leptos watch            # rebuild on change, live reload
cargo leptos serve            # one build, then serve
cargo leptos build --release  # target/server/release/regatta + target/site
```

Environment: `REGATTA_DB_URL` (default = the compose database), `REGATTA_BIND` (default
`0.0.0.0:8888`; `LEPTOS_SITE_ADDR` from cargo-leptos wins when set), `REGATTA_FIXED_NOW`
(tests only, the `-Dregatta.now` of the Kotlin server), `RUST_LOG`. The REST API is
`/v1/api/...` as before; the UI's own calls go through Leptos server functions under `/api/<fn>`,
which are outside the contract. The admin session is the `regatta_token` cookie (HttpOnly,
SameSite=Lax); its value is the same bearer token the REST API and the `database/*.py` scripts
take, shown by "Copy auth token" on the home page.

Tests:

```shell
cargo test --workspace                       # unit, SSR render, contract replay, golden (podman needed)
cargo test -p regatta-server --test contract # the 274 recorded Kotlin exchanges against the Rust server
cargo test -p regatta-server --test golden   # 118 race reports + 22 standings, byte for byte
cargo run -p regatta-contract -- replay --base-url http://127.0.0.1:8888 --cassettes crates/contract/cassettes
cargo clippy --workspace --all-targets -- -D warnings
```

The container-backed tests set `DOCKER_HOST` from `podman machine inspect` themselves. Dev
builds use `debug = "line-tables-only"`: full debuginfo for this dependency set is about 20 GB of
`target/`.

Build caveats learned the hard way:
- **Build the server with `cargo leptos`**, not a plain `cargo build`: leptos bakes the hydration
  bundle's file name in at compile time from `LEPTOS_OUTPUT_NAME` (`regatta.wasm` with it,
  `regatta_bg.wasm` without), so a plain build asks the browser for a file that does not exist and
  the page never hydrates. `.cargo/config.toml` pins the variable so plain builds, tests and
  rust-analyzer agree with cargo-leptos anyway.
- Only `tailwind-input-file` is set in the leptos metadata. With `style-file` pointing at the same
  CSS, the raw file (with `@import "tailwindcss"` still in it) replaced the Tailwind output.
- The shell links `/pkg/regatta.css` with a plain `<link>`: `<Stylesheet>` needs the meta context
  that `App` provides inside `<body>`, so in the shell's `<head>` it rendered nothing.
- **Never put a block element inside `<p>`** (a dropdown's `div`, `RgCheck`, `RgTime`, `EditBracket`).
  Compose built the DOM through the API, where that works; SSR markup goes through the HTML parser,
  which closes the `<p>` early, and hydration then panics on the mismatch. Use
  `<div class="mb-4">` (the Reboot paragraph margin) as the wrapper.

## Quirks to reproduce (recorded in Phase 0)

Facts from `crates/contract/cassettes/` (274 steps recorded from the Kotlin server on the
version-5 golden dump with `-Dregatta.now=2026-10-14T02:10:11Z`; file names cited). The Rust
server must answer the same way until Phase 7 decides otherwise.

**Content types.** JSON responses are `Content-Type: application/json` with **no charset**
(`044-all-series.json`, `088-report.json`); the plan's `; charset=UTF-8` guess was wrong. Text
responses (`respondText`, StatusPages) are `text/plain; charset=UTF-8` (`041-series-non-numeric-id.json`,
`181-result-create-phrf-without-rating.json`). The SPA fallback is `text/html; charset=UTF-8`
(`266-fallback-root.json`). The image is `image/png` from the file extension (`209-image-get.json`).
Empty answers (`204`, `400`, `401`, `202`, `200` from the deletes) carry no `Content-Type`;
`204` has no `Content-Length` either, the others have `Content-Length: 0` (`039-series-missing.json`,
`067-all-races-no-year.json`, `201-image-upload.json`, `222-result-delete.json`).

**Bearer auth and bootstrap.** Ktor's bearer provider challenges before the token is validated,
so with the `auth` table **empty** a request with no `Authorization` header or with `Bearer `
(empty) still gets `401` with `WWW-Authenticate: Bearer realm="admin access"` and an empty body
(`001-bootstrap-missing-header.json`, `002-bootstrap-empty-bearer.json`). Bootstrap is open to any
non-empty bearer value: `Bearer none` (what the web client sends when logged out) and
`Bearer anything` answer `200` (`004-bootstrap-any-bearer.json`, `005-bootstrap-all-skippers.json`).
A value Ktor cannot parse as an auth header (`Bearer not*base64!`, `ParseException` →
`BadRequestException: Invalid auth header`) is `500 Internal server error` even in bootstrap
(`003-bootstrap-malformed-bearer.json`, `025-auth-malformed-base64.json`). Once the admin exists:
no header, empty bearer, `Basic ...`, an expired token, a wrongly signed token and an unknown auth
id all answer `401` with the same `WWW-Authenticate` header and an empty body (`023`, `033`, `032`,
`028`, `030`, `031-auth-forged-unknown-id.json`); a token that decodes as base64 but is not a
`LoginResponse` (`Bearer none`, `Bearer aGVsbG8gd29ybGQ=`, `{"foo":1}`) is `500` text, because
`parseToken` catches `IOException` only and kotlinx throws `SerializationException`
(`024-auth-bearer-none.json`, `026`, `027`, `012-bootstrap-closed-bearer-none.json`); on a mutation
the `500` is answered before anything is changed (`035-auth-mutation-bearer-none.json`,
`037-auth-mutation-not-applied.json`). A forged token with a client-chosen salt, signed with the
stored hash, is accepted (`029-auth-forged-valid-token.json`). `POST auth` with the first admin
answers id `4` (`auth_id_seq` is at 3 in the dump; `011-auth-create-admin.json`); a second record
with the same user name is `500` (unique index; `021-auth-duplicate-user-name.json`).

**Login.** Wrong password and unknown user: `401`, empty body, no `WWW-Authenticate`
(`014`, `015`). A `Login` body missing fields, malformed JSON or an empty body: `500` text
(`016`, `017`, `018`). `expires` = fixed now + 30 days (`2026-11-13T02:10:11Z`), `salt` is random
(`019-login.json`).

**Request parsing.** Every body kotlinx cannot decode is `500 Internal server error`, never
`400`: unknown JSON keys (`112-series-create-unknown-key.json`, `133`, `142`, `152`, `169`, `175`,
`188`, `218-orc-cert-unknown-key.json`), a bad enum name (`122-class-create-bad-enum.json`, `127`,
`151`, `165`, `186`), a bad instant (`187-result-create-bad-instant.json`), an empty body (`113`,
`124`, `134`, `143`, `167`, `176`, `189`, `219`), an object where a list is expected (`115`), and a
JSON body sent as `text/plain` (`114-series-create-wrong-content-type.json`, `168`,
`207-image-upload-not-multipart.json`). Missing fields with defaults are filled: `[{"name": ...}]`
creates a series (`116-series-create-defaults-only.json`), `{"name": ...}` a race with
`phrfBFactor 600 / FiveBandWindwardLeeward / Low / LowMedium` (`170-race-create-defaults-only.json`),
`{raceId, boatId, bracketId, raceClassId}` a `RET` result with `CruisingNonFlyingSails`
(`192-result-create-defaults-only.json`). Names are trimmed on write (`119-series-update.json`,
`148-skipper-update.json`, `172-race-update.json`). A boat posted with an `orcCerts` entry that
fails `OrcTable.validate` is `500` (`153-boat-create-invalid-cert.json`).

**Query parameters.** A non-numeric or overflowing `id`/`raceId`/`year` on a `toLong()`/`toInt()`
route is `500` text (`041`, `042-series-id-overflow.json`, `052`, `053`, `057`, `062`, `071`, `075`,
`079`, `083`, `087`, `092`, `097`, `107`, `108`, `205-image-upload-non-numeric-race-id.json`, and the
deletes `228`, `242`, `245`, `250`, `256`, `263`); `allRaces` uses `toIntOrNull()` → `400`
(`066`, `067`) and `DELETE race` uses `toLongOrNull()` → `204` (`233`, `234`). A missing `id` is `204`
on the GETs (`040`, `047`, `056`, `061`, `070`, `074`, `078`, `082`, `091`, `096`, `100`) and on the
deletes (`227`, `241`, `244`, `249`, `255`, `262`), `400` on `resultCount` and `image`
(`086`, `204`). `GET boat` with an unknown `id` is `400`, not `204`: the `id` branch falls through
to the `person_id` branch, which is absent (`103-boat-missing-id.json`, `252-boat-deleted.json`);
an unknown `person_id` is `204` (`105`), no parameter `400` (`106`). `find/series` and
`find/skipper` answer `200 []` for no match (`046`, `099`), `204` without `name` (`047`, `100`);
`results` is `200 []` for an unknown race (`077`), `resultCount` `200 0` (`085`). `series/standings`
is `204` without `year` and for an unknown series (`050`, `051`). The path `/v1/api//series` (double
slash, what `ApiPaths.series.versionedApi()` literally produces) answers like `/v1/api/series`
(`043-series-double-slash.json`).

**Deletes.** `DELETE` of an unknown id is `200` (the row count is non-null): `226`, `235`, `240`,
`246`, `251`, `257`, `264`. A foreign-key violation on delete or insert is `409` text
`This record is still referenced by other records and cannot be deleted.` (`137-class-delete-in-use.json`,
`138-bracket-delete-in-use.json`, `159-skipper-delete-in-use.json`, `160-boat-delete-in-use.json`,
`261-series-delete-in-use.json`; inserts `132-bracket-create-unknown-class.json`,
`150-boat-create-unknown-skipper.json`, `166-race-create-unknown-series.json`,
`184-result-create-unknown-boat.json`, `185-result-create-unknown-cert.json`,
`206-image-upload-unknown-race.json`). `DELETE race` with results is `409` text
`This race has recorded results and cannot be deleted.` (`200`, `238`). A duplicate person name is
`500` (unique index, not a foreign key; `140-skipper-create-duplicate-name.json`). `POST raceClass`
deletes the class's brackets missing from the list (`135`, `136-bracket-dropped.json`).

**Validation texts.** `400` text: `PHRF B factor 600 is Low/Medium, but the ORC wind band is High`
(`162`, `174`), `PHRF B factor 480 is High, but the ORC wind band is Low` (`163`), `A PHRF result
needs a PHRF rating` (`181`), `An ORC result needs an ORC certificate` (`182`), `A ORC, PHRF result
needs a PHRF rating` (`183`); a single-number option takes any band (`164`). `POST orcCert`: `404`
text `no certificate with reference number CONTRACT-NOPE` (`216`), `400` text
`invalid certificate properties` (`217`); an update moves the row to the end of `GET orcCerts`
(`221-orc-certs-after.json`, physical order).

**Image.** The download route is `ApiPaths.image.versionedApi() + "{name}"` =
`/v1/api//image{name}`: one segment with a literal prefix, so the image is at
`/v1/api/imagecontract.png` (`209-image-get.json`, 73 bytes `image/png`), an unknown name is `404`
empty (`210`), `GET /v1/api/image` (empty name) is `404` (`211`), and `/v1/api/image/contract.png`
is the SPA fallback (`214-image-get-slash-path.json`). Nothing in the web client reads
`reportImage` or this route. Upload: `202` empty for an `image/png` part, for a `text/plain` part
alone (ignored, image kept) and for no parts (`201`, `202`, `203`); a body that is not multipart is
`500` (`207`); the race's `reportImage` becomes the file name (`213-race-schedule-with-image.json`).

**`GET report`.** The handler calls `respond` twice; the first wins and nothing is logged
(`088-report.json` `200`, `090-report-missing.json` and `091-report-no-race-id.json` `204`).
With `Accept-Encoding: gzip` the body is gzip-compressed, `Content-Encoding: gzip`,
`Vary: Accept-Encoding`, chunked (`089-report-gzip.json`); without it `Content-Length` is set.

**Fallback.** Unknown pages, unknown `/v1/api/...` paths, wrong methods on known paths
(`PUT /v1/api/series`, `DELETE /v1/api/allSeries`), `/v1/api/image/`, `/v1/api` all answer `200`
`text/html; charset=UTF-8` with `index.html` (`266`-`273`).

**Lists.** `POST series []` and `POST raceClassList []` answer `200 []` (`110`, `126`).
`raceClassList` answers every posted class with its brackets, sorted by `sort` (`125`).
`allSkippers` is sorted by first name, `allBoat` by rating, `orcCerts` and `allRaces` within equal
start times in physical row order; all reproduced identically on a fresh dump load (replay clean).

## Risks

- **Float and time formatting**: the byte-for-byte golden test is the backstop; if an approved file
  ever differs only in number formatting, fix the formatter, never the file.
- **Two data paths**: server functions and REST handlers must stay thin over one service layer, or
  the contract tests stop protecting what the UI does. Review for logic in either adapter.
- **Hydration mismatches**: anything that differs between server and browser render (dates, "now",
  random ids) shows up as a hydration error; render times in a fixed zone and keep "now"-dependent
  text (the login countdown) client-only (`<Suspense>`/effects).
- **Reactive model**: `BaseViewModel` + `StateFlow` is not Leptos' fine-grained signals; the first
  screen (Series) settles the pattern before the rest.
- **wasm size and CDN interop**: MapLibre and SortableJS stay out of the bundle; wasm-bindgen
  externs fail at runtime, not build time, so the course map gets a wasm test.
- **Tailwind scanning**: class names in `format!` strings are invisible to Tailwind; the safelist
  stays, and the SSR markup tests catch missing utilities.
- **pgbouncer + prepared statements** (Phase 5).
- **Parent monorepo**: the Kotlin build must keep working from both roots until Phase 6.
- **Time**: this is a rewrite of ~17 k lines of Kotlin; the contract and golden tests are what
  make it safe, so Phases 0–2 are not to be rushed.

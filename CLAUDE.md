# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Regatta is a web application for scoring sailing regattas, supporting ORC, PHRF rating systems as well as non-rated scoring
(Cruising Flying Sails, and Cruising Non-Flying Sails). Built for the Corinthian Yacht Club of Tacoma (CYCT).

It is a Rust workspace: an axum server that renders a Leptos UI on the server (SSR) and hydrates it in the browser
from a wasm bundle, over PostgreSQL 16 through sqlx. cargo-leptos builds both halves and runs Tailwind.

## Commands

Prerequisites: rustup (the pinned toolchain and the `wasm32-unknown-unknown` target come from `rust-toolchain.toml`),
`cargo install cargo-leptos --locked --version 0.3.11`, and podman for the database, the container-backed tests and
the image.

### Development

Start the database first (required):
```shell
cd database && podman-compose up
```

Run the server with the UI (http://127.0.0.1:8888):
```shell
cargo leptos watch            # rebuild on change, live reload
cargo leptos serve            # one build, then serve
cargo xtask run               # release build, then serve
cargo leptos build --release --precompress   # target/release/regatta + target/site
```

An empty database gets `migrations/0001_baseline.sql` on first start and then runs in bootstrap mode until an admin
is created. To work against real data instead, load the golden snapshot into a scratch container:
```shell
podman run -d --name regatta-dev -e POSTGRES_USER=regatta_admin -e POSTGRES_PASSWORD=mysecretpassword \
  -e POSTGRES_DB=regatta -p 5435:5432 postgres:16-bookworm
PGPASSWORD=mysecretpassword psql -h localhost -p 5435 -U regatta_admin -d regatta \
  -f crates/scoring/tests/golden/regatta-2026-10-13-v5.sql
export REGATTA_DB_URL=postgres://regatta_admin:mysecretpassword@localhost:5435/regatta
```

Configuration (`crates/service/src/config.rs`, environment or the matching `--flag`):

| Variable | Default | |
|---|---|---|
| `REGATTA_DB_URL` | `postgres://regatta_admin:mysecretpassword@localhost:5432/regatta` | the compose database |
| `REGATTA_BIND` | `0.0.0.0:8888` | `LEPTOS_SITE_ADDR` wins when set (cargo-leptos and the image set it) |
| `REGATTA_FIXED_NOW` | unset | tests only, see below |
| `RUST_LOG` | `info` | tracing filter |

**Always build the server through cargo-leptos**, not a plain `cargo build`, for anything a browser will load: leptos
bakes the hydration bundle's file name in at compile time from `LEPTOS_OUTPUT_NAME`. `.cargo/config.toml` pins it to
`regatta` so plain builds, tests and rust-analyzer agree with cargo-leptos anyway. Dev builds carry line tables only
(`debug = "line-tables-only"`): full debuginfo for this dependency set is about 20 GB of `target/`.

### Tests

```shell
cargo test --workspace                          # everything; the container-backed tests need podman
cargo test -p regatta-common                     # models, JSON round trip of the golden files, ORC helpers, labels
cargo test -p regatta-scoring                    # scoring rules, corrected time, seeded invariants, database-free golden check
cargo test -p regatta-service                    # config, auth tokens, ORC client parsing
cargo test -p regatta-app                        # SSR render tests of the components
cargo test -p regatta-server --test ssr          # every screen rendered through the router
cargo xtask golden                               # = cargo test -p regatta-server --test golden
cargo xtask contract                             # = cargo test -p regatta-server --test contract
cargo clippy --workspace --all-targets -- -D warnings
cargo fmt --all
```

Run a single test with a name filter:
```shell
cargo test -p regatta-scoring --test race_placing_test gsi_14_1_4
```

The container-backed tests (golden, contract) start a throwaway `postgres:16-bookworm` with testcontainers and set
`DOCKER_HOST` from `podman machine inspect` themselves when it is unset. The interactive `wasm-bindgen-test` cases in
`crates/app/tests/wasm.rs` (dialogs, dropdowns, the hamburger, the theme toggle) run in a browser:
```shell
wasm-pack test --headless --chrome crates/app --features hydrate
cargo test -p regatta-app --features hydrate --target wasm32-unknown-unknown --no-run   # compile check only
```

Test-only fixed clock: `REGATTA_FIXED_NOW=<ISO-8601 instant>` (e.g. `2026-10-14T02:10:11Z`) makes series standings
and `LoginResponse.expires` use that instant instead of the wall clock, so contract recordings are stable. Unset,
nothing changes; the server logs a warning when it is set.

**Golden regression.** `crates/scoring/tests/golden/` holds a scrubbed production snapshot
(`regatta-2026-10-13-v5.sql`), the approved race reports and series standings over it (`approved/`), and
`deviations.json`. `cargo xtask golden` loads the dump into a container through the server's own startup path and
compares every report and standings page byte for byte; `crates/scoring/tests/golden_test.rs` re-scores the approved
files from their own inputs without a database. A mismatch is a bug: there is no re-approve mode.
```shell
python3 -I database/audit_scoring.py      # re-score the approved files, writes golden/deviations.json
```
`deviations.json` must stay empty. `database/scrub_dump.py` produces a snapshot from a `pg_dump`. Refreshing the
snapshot means scrubbing a new dump into `golden/`, updating `DUMP` and `DUMP_INSTANT` in
`crates/server/tests/support/mod.rs`, and writing the approved files for the new races (existing files must not
change). Details: `docs/explore_implemented/test_audit_and_plan.md` §5.

**Scoring tests** build fleets with the fixture DSL in `crates/scoring/src/fixture.rs` (`race_on("2026-06-17", "18:00",
|r| r.phrf_class("Fast", |c| c.bracket("I", |b| { b.boat("A", 48).finishes("20:51:26"); })))`, plus a series
builder) and assert with `assert_bracket_scores` / `assert_bracket_totals`. Rule tests are named after the GSI section
or decision they enforce (`gsi_14_1_4_...`, `d3_...`); the scoring rules and their rationale are in the "Decisions"
section of `docs/explore_implemented/test_audit_and_plan.md`. ORC series scoring (D9: each rating placed and scored
among its own entries, the ORC side of an ORC class alongside the PHRF one) is recorded in
`docs/explore_implemented/orc_series_standings.md`.

**REST contract.** `crates/contract` scripts every `/v1/api` route as a scenario and keeps the recorded exchanges in
`crates/contract/cassettes`; `cargo xtask contract` replays them against an in-process server on a fresh load of the
golden dump. See `crates/contract/README.md` for record, replay and diff against a running server.

### Container image and deploy

The image is **linux/amd64 only** (`Containerfile`: a `rust:bookworm` build stage running
`cargo leptos build --release --precompress`, then `gcr.io/distroless/cc-debian12` with the binary and `site/` under
`/opt/regatta`). The build stage runs on the host's own architecture and cross-compiles the server for
`x86_64-unknown-linux-gnu` (`LEPTOS_BIN_TARGET_TRIPLE`, `LEPTOS_BIN_EXE_NAME`, the Debian x86_64 gcc), because rustc
crashes under qemu emulation. cargo-leptos comes from its checksum-verified release binary: compiling it needs more
memory than the podman VM has. Run a local build with `podman run --pull=never ...`; with `--platform`, podman pulls
the published image over the local tag.

```shell
cargo xtask make-img            # build ghcr.io/manimaul/regatta:latest for linux/amd64 (checks the architecture)
cargo xtask pub-img             # build, then push to ghcr.io (podman login ghcr.io first)
cargo xtask k8s-apply-secrets   # the pgbouncer sidecar's secrets; first deploy and credential changes
cargo xtask deploy-server       # pub-img, k8s-apply-server, rollout-restart
cargo xtask k8s-apply-server --dry-run   # validate the filled manifest without applying it
```

The k8s tasks fill the `{REGATTA_PG_USER}`, `{REGATTA_PG_PASS}` and `{REGATTA_DB_URL}` placeholders in
`k8s_deploy/*.yaml` from the `REGATTA_PG_USER` / `REGATTA_PG_PASS` environment variables and pipe the result to
`kubectl` on stdin, so filled manifests never land on disk. The server reaches the database through a pgbouncer
sidecar on the pod loopback (transaction pooling, `max_prepared_statements` set for sqlx's prepared statements). The
deployment pins `kubernetes.io/arch: amd64`. `k8s_deploy/k8s_init.sh` creates the ghcr.io pull secret. Backups and
restores (prod and dev) are `database/db_actions.py`, see `database/README.md`.

No dedicated lint task beyond `cargo clippy` and `cargo fmt`.

### Commits

Only commit changes if explicitly instructed by the user.

Commit messages are **one line of keywords** describing the change, matching the existing history
(`race edit - modal`, `db query - perf`). No body, no explanation, no trailers or attribution lines
of any kind. For example: `orc places - phrf entries only`.

## Architecture

A Cargo workspace (`Cargo.toml` holds every dependency version under `[workspace.dependencies]`; crates add only
features) with these crates:

| Crate | Path | Role |
|---|---|---|
| `regatta-common` | `crates/common` | `serde` domain models (`Race`, `Boat`, `Person`, `Series`, `Bracket`, `RaceClass`, `RaceResult`, `OrcCertificate`, reports, standings), the REST paths (`api_paths.rs`), label helpers, version info |
| `regatta-scoring` | `crates/scoring` | pure race and series scoring: `card`, `report`, `standings`, validation; the fixture DSL |
| `regatta-service` | `crates/service` | config, startup schema check, the sqlx data layer (`db/`, one module per table), report/standings wrappers, auth tokens, the ORC client |
| `regatta-server` | `crates/server` | the `regatta` binary: axum router with the REST API, Leptos SSR routes and server functions, error layer, site fallback |
| `regatta-app` | `crates/app` | the Leptos UI, compiled with `ssr` into the server and with `hydrate` into the frontend |
| `regatta-frontend` | `crates/frontend` | the wasm hydrate entry point, served from `/pkg` |
| `regatta-contract` | `crates/contract` | REST contract scenario and cassettes |
| `regatta-xtask` | `crates/xtask` | `cargo xtask` run, image and deploy tasks |

### Server (`crates/server`, `crates/service`)
- **Entry point:** `crates/server/src/main.rs` reads `Config`, connects, runs the startup check, then serves on
  `LEPTOS_SITE_ADDR` or `REGATTA_BIND` with graceful shutdown on Ctrl-C.
- **One router, three faces** (`crates/server/src/lib.rs`): the REST API under `/v1/api` (`routes/`, each handler a
  thin adapter over `regatta-service`; its statuses and bodies are pinned by the contract cassettes), the Leptos
  server functions under `/api/<fn>` that the UI calls (`crates/app/src/server_fns.rs`), and the server-rendered UI.
  `fallback.rs` serves files under the site root first (`.br`/`.gz` siblings when precompressed), else renders the
  shell for the path so client routes deep-link.
- **Render threads** need a large stack: the runtime builds its workers with `RENDER_THREAD_STACK` (32 MiB). The
  Classes screen overflows the 2 MiB default in a debug build.
- **Database:** `regatta-service`'s `Db` over a `PgPool`, using the non-macro sqlx API (no `.sqlx/` offline data;
  builds need no database). Timestamps go through `::text` and `jiff`.
- **Schema:** `startup::prepare` applies `migrations/0001_baseline.sql` to an empty database (no `metadata` table),
  then requires `metadata.version` to be the supported schema version (5, or 15 for a fresh install) and refuses to
  start otherwise. Nothing is altered at startup. A schema change needs a new SQL migration, an upgrade step in
  `crates/service/src/startup.rs` and a bump of `SCHEMA_VERSION`, plus a refreshed golden snapshot.
- **Auth:** `crates/service/src/auth.rs` holds stateless SHA-512 salted bearer tokens with 30-day expiry; the token is
  `base64(json(LoginResponse))`. All admin mutations require it: REST callers send `Authorization: Bearer`, the UI
  keeps the same value in the `regatta_token` cookie (`HttpOnly`, `SameSite=Lax`, `Secure` when the request or
  `X-Forwarded-Proto` is https). "Copy auth token" on the home page shows it for the `database/*.py` scripts.
  **Bootstrap behavior:** if the `auth` table is empty, every request is admin, so the first admin can be created.
- **JSON:** the report and standings JSON is pinned byte for byte by the golden files: field order is declaration
  order, defaults and nulls are written (except `RaceReportCard.orcDxtId` while null), instants and durations use
  their own string formats, pretty-printed with a 4-space indent. Changing a serialized type changes the golden
  files, so keep `crates/common/tests/golden_round_trip.rs` green.

### UI (`crates/app`, `crates/frontend`)
- **Shell and routes:** `crates/app/src/lib.rs` (`shell`) renders `<html>`, the theme script, stylesheets and CDN
  scripts; `app.rs` mounts the router; one module per screen in `routes/` (Home, Races, RaceResults, Series,
  SeriesStandings, People, Boats, Classes, Course, Rc, Admin); shared components in `components/`; app state
  (login, alerts, route, theme) in `state/`.
- **SSR + hydration:** every page renders on the server and hydrates from `/pkg/regatta.wasm`. **Never put a block
  element inside `<p>`** (a dropdown's `div`, `RgCheck`, `RgTime`, `EditBracket`): the HTML parser closes the `<p>`
  early and hydration panics on the mismatch. Use `<div class="mb-4">` as the wrapper. The shell links
  `/pkg/regatta.css` with a plain `<link>` because `<Stylesheet>` needs the meta context that only exists inside
  `<body>`.
- **Maps:** `chart/` (hydrate only) binds `window.maplibregl` through wasm-bindgen. maplibre-gl and SortableJS are
  **not bundled**: the shell loads them from jsDelivr (`MAPLIBRE_JS`, `MAPLIBRE_CSS`, `SORTABLE_JS` in
  `crates/app/src/lib.rs`); the map container renders empty on the server. The OpenENC style's `LIGHTS_sector`
  layer names its icons `sector_<bearings>_<colors>_<radius>`; `utils/light_sector.rs` parses that id and draws the
  SVG, and the chart controller registers it on `styleimagemissing` in the current theme's palette.
- **Styles:** Tailwind CSS v4 run by cargo-leptos from `crates/app/style/app.css` (theme tokens, a Reboot-like base
  layer, and all `.rg-*` component classes); `crates/app/public/` holds the static assets. Components keep their look
  in `.rg-*` classes; Rust code uses Tailwind utilities only for one-off layout. Tailwind finds classes by scanning
  `crates/app/src/**/*.rs` (`@source`), so **class names must be literal strings**, never built with `format!`;
  runtime-built spacing classes are covered by the `@source inline(...)` safelist in `app.css`. Only
  `tailwind-input-file` is set in the leptos metadata: setting `style-file` to the same file replaced the Tailwind
  output with the raw CSS. Modals are native `<dialog>`s (`components/modal.rs`). Tailwind emits `@layer
  base/components` as native cascade layers, so an **unlayered** third-party stylesheet (maplibre-gl.css) beats
  anything inside them; overrides for such styles (the chart popup block at the end of `app.css`) must sit outside
  any `@layer`.
- **Light/dark themes:** components read only the `--rg-*` semantic tokens in `app.css` (`:root` = light,
  `:root[data-theme="dark"]` = dark, built from the burgee colors), never palette colors directly; use the semantic
  utilities (`text-fg`, `text-muted`, `border-line`, `text-danger-fg`) or `dark:`, which follows `data-theme`. The
  theme defaults to light and is switched by `ThemeToggle` (`state/theme.rs`), which writes `data-theme`,
  `localStorage["theme"]` and a `theme` cookie so the next server render starts in the chosen theme.

### Versions
`crates/common/build.rs` captures the git hash, branch, dirty flag and build date as compile-time env values
(`version_info.rs`). The container build has no `.git`, so `cargo xtask make-img` passes them as build arguments
(`REGATTA_GIT_HASH`, `REGATTA_GIT_BRANCH`, `REGATTA_GIT_DIRTY`), which win over git when set.

The `database/` directory holds the dev Docker Compose file and the backup, restore, audit and maintenance scripts;
it is not a crate.

## Key Technologies

| Layer | Technology |
|---|---|
| Language | Rust (toolchain pinned in `rust-toolchain.toml`), wasm32 for the browser |
| Build | Cargo workspace, cargo-leptos 0.3, `cargo xtask` |
| Server | axum 0.8, tokio, tower-http |
| UI | Leptos 0.8 (SSR + hydrate) |
| Database | PostgreSQL 16 via sqlx 0.9 |
| Serialization | serde / serde_json |
| Time | jiff |
| Styles | Tailwind CSS 4 |
| Maps | MapLibre GL JS 4.7.1 (CDN) |
| Drag-and-drop | SortableJS 1.15.2 (CDN) |
| Deployment | linux/amd64 container on Kubernetes (`k8s_deploy/`) or a bare-metal install (`README.MD`) |

## ORC Scoring

`crates/common/src/data/orc_certificate.rs` is large and complex: it maps every US ORC scoring option across
3-band and 5-band wind conditions (~100 fields per certificate). Its `virtual_phrf` converts an ORC TOT number to an
equivalent PHRF rating for bracket sorting purposes.

Certificates are fetched from data.orc.org by the server (`crates/service/src/orc.rs`, route
`GET /v1/api/orcCert?refNo=`), never by the browser: the site has no CORS headers. Besides the RMS data the client
resolves `dxtId`, ORC's file id from the active-certificate list, which keys the certificate's public page
(`https://data.orc.org/public/WPub.dll/CC/<dxtId>`) linked from the results tables. Only active certificates can be
resolved; `python3 -I database/orc_backfill.py --apply` fills the id for stored certificates still on the list.
`dxtId` / `RaceReportCard.orcDxtId` are never encoded while null, so the golden files are unaffected.

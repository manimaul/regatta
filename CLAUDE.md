# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Regatta is a web application for scoring sailing regattas, supporting ORC, PHRF rating systems as well as non-rated scoring 
(Cruising Flying Sails, and Cruising Non-Flying Sails). Built for the Corinthian Yacht Club of Tacoma (CYCT).

## Commands

### Development

Start the database first (required):
```shell
cd database && podman-compose up
```

Run the server:
```shell
./gradlew :server:installJvmDist && ./server/build/install/regatta-jvm/bin/regatta
```

Run the web frontend with hot reload:
```shell
./gradlew :web:jsBrowserDevelopmentRun --continuous
```

### Tests

```shell
./gradlew :common:jvmTest   # ORC/PHRF scoring tests (OrcTest, PhrfTest)
./gradlew :server:jvmTest   # TokenTest, RaceResultReporterKtTest
./gradlew :common:jsTest
./gradlew :web:jsBrowserTest
```

Run a single JVM test class or method with `--tests`:
```shell
./gradlew :server:jvmTest --tests "com.mxmariner.regatta.results.RaceResultReporterKtTest"
./gradlew :common:jvmTest --tests "com.mxmariner.regatta.OrcTest.someTestName"
```

JS tests run in Chrome Headless and Firefox via Karma (Firefox must be installed, or `FIREFOX_BIN` set). Web component tests use `org.jetbrains.compose.html:html-test-utils` (`runTest { composition { ... } }`); prefer polling or `delay` over `waitForRecompositionComplete()` when nothing is expected to recompose, since it never returns in that case.

### Production Build & Deploy

```shell
./gradlew makeImg       # build Docker image ghcr.io/manimaul/regatta:latest
./gradlew pubImg        # push image to ghcr.io
./gradlew deployServer  # full k8s deploy pipeline
./gradlew :server:buildDeb  # build .deb for bare-metal install
```

No dedicated lint task — relies on Kotlin compiler warnings. Code style: `kotlin.code.style=official`.

## Architecture

This is a **Kotlin Multiplatform (KMP) monorepo** with three Gradle subprojects: `common`, `server`, and `web`.

### `common` — Shared Models & API Contracts
Targets both JVM and JS. Contains all `@Serializable` domain data classes (`Race`, `Boat`, `Person`, `Series`, `Bracket`, `RaceClass`, `RaceResult`, `OrcCertificate`, etc.) and shared REST API path constants (`ApiPaths.kt`). Both the server and web depend on this module.

### `server` — Ktor HTTP Server (JVM, port 8888)
- **Entry point:** `Application.kt` — Ktor/Netty setup with auth, compression, content negotiation
- **Routing:** `plugins/Routing.kt` — all HTTP routes calling directly into `RegattaDatabase`
- **Database:** `db/Database.kt` — `RegattaDatabase` singleton using Jetbrains Exposed ORM with suspend transactions against PostgreSQL 16. Table objects live in `db/` alongside it. Connection settings come from JVM system properties `jdbcurl`, `user`, `password` (defaults match `database/docker-compose.yaml`; pass via `JAVA_OPTS=-Djdbcurl=...`).
- **Schema changes:** on startup, `Migration.runMigration` (`db/Migration.kt`) runs hand-written versioned SQL upgrades (tracked in a `metadata` table, `currentDbVersion`), then `SchemaUtils.create` + `addMissingColumnsStatements` auto-create tables/columns. New tables must be added to the `tables` array in `RegattaDatabase.init()`. Anything Exposed can't do automatically (drops, renames, data backfills, type changes) needs a new `dbVersionNToN+1Upgrade` step and a bump of `currentDbVersion`.
- **Auth:** `auth/Token.kt` — stateless SHA-512 salted bearer tokens with 30-day expiry. All admin mutations require a bearer token. **Bootstrap behavior:** if the `auth` table is empty, `validateAdminToken` returns admin unconditionally so the first admin can be created.
- **Scoring:** `results/RaceResultReporter.kt` — corrected time calculations and standings generation
- **SPA serving:** The compiled web frontend is embedded as classpath resources under `static/`. The `StatusPages` unhandled handler returns `static/index.html` for all unmatched routes, enabling client-side routing.

### `web` — Compose HTML SPA (Kotlin/JS)
- **Entry point:** `Main.js.kt` — mounts the `Router` composable
- **Routing:** `Router.kt` — dispatches to route composables via `RouteViewModel` state
- **Route screens:** `components/routes/` — one file per screen (Home, Races, RaceEdit, RaceResults, Series, SeriesStandings, People, Boats, Classes, Course, Rc, Admin)
- **ViewModels:** `viewmodel/` — MVVM pattern using `BaseViewModel<T>` backed by `MutableStateFlow` + coroutines. Concrete VMs per feature area.
- **API client:** `utils/Api.kt` (typed) → `utils/Network.kt` (fetch API interop)
- **Auth:** Token stored in browser `localStorage` via `utils/Auth.kt` / `utils/LocalStorage.kt`
- **Maps:** `utils/MapLibre.kt` — MapLibre GL JS interop for course display. maplibre-gl is **not bundled**: `webpack.config.d/externals.js` maps it to the `window.maplibregl` global, and the JS + CSS load from jsDelivr tags in `common/src/commonMain/resources/static/index.html`. Keep that version in step with `npm("maplibre-gl", ...)` in `web/build.gradle.kts` (the npm dep remains only for local typing/resolution).
- **Styles:** Tailwind CSS v4, entry point `src/jsMain/resources/app.css` (theme tokens, Reboot-like base layer, and all `.rg-*` component classes), run through PostCSS by `webpack.config.d/tailwind.js`. Components keep their look in `.rg-*` classes; Kotlin uses Tailwind utilities only for one-off layout. Tailwind finds classes by scanning the `.kt` sources, so **class names must be literal strings** — never build them with templates (`"table-${x}"`); runtime-built spacing from `RgDiv`'s `Customizer` is covered by an `@source inline(...)` safelist in `app.css`. `styles/AppStyle.kt` (CSS-in-Kotlin margins) is still used alongside it. Modals are native `<dialog>`s (`components/Modal.kt`); there is no Bootstrap. Background and decisions: `doc/explore_tailwind.md`.

### `buildSrc`
- `VersionPlugin` generates `VersionInfo.kt` from git hash/date at build time
- `Versions.kt` — all dependency version constants (Kotlin 2.3.0, Ktor 3.4.0, Compose 1.10.0, Exposed 1.0.0)

### How the web build integrates with the server
The root `build.gradle.kts` wires `jsBrowserProductionWebpack` output into `jvmProcessResources` under `static/`. During production webpack, `build.gradle.kts` temporarily renames `dev_server_config.js` to exclude it from the production bundle, then restores it. (That rename is now belt-and-braces: `dev_server_config.js` is wrapped in `if (config.devServer)`, so plain webpack builds and Karma tests skip it anyway. `tailwind.js` and `externals.js` must stay active in every build.)

The `database/` directory is **not** a Gradle subproject — it only contains Docker Compose files for local dev and backup/restore scripts.

## Key Technologies

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 25, JS IR) |
| Build | Gradle Kotlin DSL, Kotlin Multiplatform |
| Server | Ktor 3.4.0 (Netty) |
| Database ORM | Jetbrains Exposed 1.0.0 |
| Database | PostgreSQL 16 |
| Serialization | kotlinx.serialization (JSON) |
| Frontend | Compose HTML (Jetbrains Compose for Web) |
| Bundler | webpack 5 via Kotlin/JS plugin |
| UI | Tailwind CSS 4 (+ `@tailwindcss/forms`) |
| Maps | MapLibre GL JS 4.7.1 |
| Drag-and-drop | SortableJS 1.15.2 |
| Deployment | Kubernetes (`k8s_deploy/`) or `.deb` package |

## ORC Scoring

`OrcCertificate.kt` in `common` is large and complex — it maps every US ORC scoring option across 3-band and 5-band wind conditions (~100 fields per certificate). The `virtualPhrf()` function converts an ORC TOT number to an equivalent PHRF rating for bracket sorting purposes.

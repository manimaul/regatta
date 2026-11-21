// MapLibre GL JS's worker, started from this origin. MapLibre cannot start a worker module straight
// from another origin: it goes through a blob: URL that it revokes before Chromium has loaded it.
// The shell points MapLibre here with setWorkerUrl (MAPLIBRE_MODULE_SCRIPT in crates/app/src/lib.rs).
// The version must be the one that script imports; a test in crates/app/tests checks it.
import "https://cdn.jsdelivr.net/npm/maplibre-gl@6.19.0/dist/maplibre-gl-worker.mjs";

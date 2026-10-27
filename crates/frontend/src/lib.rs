//! regatta-frontend: the hydrate entry point (Phase 4). cargo-leptos builds this crate for
//! wasm32 with the `hydrate` feature and serves it from `/pkg`; `hydrate()` is what the
//! `<HydrationScripts>` bootstrap calls once the wasm module has loaded.

#[cfg(feature = "hydrate")]
#[wasm_bindgen::prelude::wasm_bindgen]
pub fn hydrate() {
    console_error_panic_hook::set_once();
    leptos::mount::hydrate_body(regatta_app::App);
}

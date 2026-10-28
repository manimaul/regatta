//! The browser half of the UI: the wasm hydrate entry point for `regatta_app::App`.
//!
//! cargo-leptos builds this crate for wasm32 with the `hydrate` feature and serves it from `/pkg` as `regatta.wasm`.
//! The server and this bundle must render the same markup (and agree on `--cfg erase_components`), or hydration
//! panics.

#[cfg(feature = "hydrate")]
#[wasm_bindgen::prelude::wasm_bindgen]
/// Hydrates the server-rendered `<body>` with the app.
///
/// The `<HydrationScripts>` bootstrap calls it once the wasm module has loaded. It installs the panic hook first, so
/// a panic (a hydration mismatch, say) reaches the browser console.
pub fn hydrate() {
    console_error_panic_hook::set_once();
    leptos::mount::hydrate_body(regatta_app::App);
}

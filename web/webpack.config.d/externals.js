// maplibre-gl is loaded from CDN <script>/<link> tags in common/src/commonMain/resources/static/index.html
// rather than bundled - it was about a third of web.js. Same approach as mxmariner's webApp.
//
// The value is the global the UMD build defines: maplibre-gl sets window.maplibregl, not the
// `maplibre` that MapLibre.kt's @JsName suggests. Getting this wrong fails only at runtime on the
// course map, never at build time.
//
// "window ..." makes it `window["maplibregl"]` rather than a bare `maplibregl` reference, so the Karma
// test bundle (no CDN tag) still loads - it's just undefined there - instead of throwing a ReferenceError.
//
// Keep the version in index.html in step with npm("maplibre-gl", ...) in web/build.gradle.kts -
// nothing here checks it.
config.externals = Object.assign({}, config.externals, {
    "maplibre-gl": "window maplibregl",
});

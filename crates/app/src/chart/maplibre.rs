//! = `MapLibre.kt`: wasm-bindgen externs to the MapLibre GL JS 4.7.1 UMD global
//! (`window.maplibregl`, loaded by a CDN tag in the shell; never bundled). The subset the chart
//! controller uses.

use wasm_bindgen::prelude::*;

#[wasm_bindgen]
extern "C" {
    #[wasm_bindgen(js_namespace = maplibregl, js_name = LngLat)]
    pub type LngLat;
    #[wasm_bindgen(constructor, js_namespace = maplibregl, js_class = "LngLat")]
    pub fn new(lng: f64, lat: f64) -> LngLat;
    #[wasm_bindgen(method, getter)]
    pub fn lng(this: &LngLat) -> f64;
    #[wasm_bindgen(method, getter)]
    pub fn lat(this: &LngLat) -> f64;

    #[wasm_bindgen(js_namespace = maplibregl, js_name = LngLatBounds)]
    pub type LngLatBounds;
    #[wasm_bindgen(method, js_name = getCenter)]
    pub fn get_center(this: &LngLatBounds) -> LngLat;
    #[wasm_bindgen(method, js_name = getNorth)]
    pub fn get_north(this: &LngLatBounds) -> f64;
    #[wasm_bindgen(method, js_name = getSouth)]
    pub fn get_south(this: &LngLatBounds) -> f64;
    #[wasm_bindgen(method, js_name = getWest)]
    pub fn get_west(this: &LngLatBounds) -> f64;
    #[wasm_bindgen(method, js_name = getEast)]
    pub fn get_east(this: &LngLatBounds) -> f64;
    #[wasm_bindgen(method, js_name = getSouthWest)]
    pub fn get_south_west(this: &LngLatBounds) -> LngLat;
    #[wasm_bindgen(method, js_name = getSouthEast)]
    pub fn get_south_east(this: &LngLatBounds) -> LngLat;
    #[wasm_bindgen(method, js_name = getNorthEast)]
    pub fn get_north_east(this: &LngLatBounds) -> LngLat;
    #[wasm_bindgen(method, js_name = getNorthWest)]
    pub fn get_north_west(this: &LngLatBounds) -> LngLat;

    // https://maplibre.org/maplibre-gl-js/docs/API/classes/Marker/
    #[wasm_bindgen(js_namespace = maplibregl, js_name = Marker)]
    pub type Marker;
    #[wasm_bindgen(constructor, js_namespace = maplibregl, js_class = "Marker")]
    pub fn new(options: &JsValue) -> Marker;
    #[wasm_bindgen(method, js_name = addTo)]
    pub fn add_to(this: &Marker, map: &Map) -> Marker;
    #[wasm_bindgen(method)]
    pub fn remove(this: &Marker) -> Marker;
    #[wasm_bindgen(method, js_name = getLngLat)]
    pub fn get_lng_lat(this: &Marker) -> LngLat;
    #[wasm_bindgen(method, js_name = setLngLat)]
    pub fn set_lng_lat(this: &Marker, lng_lat: &LngLat) -> Marker;
    #[wasm_bindgen(method, js_name = setPopup)]
    pub fn set_popup(this: &Marker, popup: &Popup) -> Marker;

    // https://maplibre.org/maplibre-gl-js/docs/API/classes/Popup/
    #[wasm_bindgen(js_namespace = maplibregl, js_name = Popup)]
    pub type Popup;
    #[wasm_bindgen(constructor, js_namespace = maplibregl, js_class = "Popup")]
    pub fn new(options: &JsValue) -> Popup;
    #[wasm_bindgen(method, js_name = addTo)]
    pub fn add_to(this: &Popup, map: &Map) -> Popup;
    #[wasm_bindgen(method, js_name = isOpen)]
    pub fn is_open(this: &Popup) -> bool;
    #[wasm_bindgen(method)]
    pub fn remove(this: &Popup) -> Popup;
    #[wasm_bindgen(method, js_name = getLngLat)]
    pub fn get_lng_lat(this: &Popup) -> LngLat;
    #[wasm_bindgen(method, js_name = setLngLat)]
    pub fn set_lng_lat(this: &Popup, lng_lat: &LngLat) -> Popup;
    #[wasm_bindgen(method, js_name = trackPointer)]
    pub fn track_pointer(this: &Popup) -> Popup;
    #[wasm_bindgen(method, js_name = getElement)]
    pub fn get_element(this: &Popup) -> web_sys::HtmlElement;
    #[wasm_bindgen(method, js_name = setText)]
    pub fn set_text(this: &Popup, text: &str) -> Popup;
    #[wasm_bindgen(method, js_name = setHTML)]
    pub fn set_html(this: &Popup, html: &str) -> Popup;
    #[wasm_bindgen(method)]
    pub fn on(this: &Popup, type_: &str, listener: &js_sys::Function);

    // https://maplibre.org/maplibre-gl-js/docs/API/classes/Map/
    #[wasm_bindgen(js_namespace = maplibregl, js_name = Map)]
    pub type Map;
    #[wasm_bindgen(constructor, js_namespace = maplibregl, js_class = "Map")]
    pub fn new(args: &JsValue) -> Map;
    #[wasm_bindgen(method)]
    pub fn on(this: &Map, type_: &str, listener: &js_sys::Function);
    #[wasm_bindgen(method, js_name = queryRenderedFeatures)]
    pub fn query_rendered_features(this: &Map, box_: &JsValue) -> JsValue;
    #[wasm_bindgen(method, js_name = setStyle)]
    pub fn set_style(this: &Map, url: &str);
    #[wasm_bindgen(method)]
    pub fn remove(this: &Map);
    #[wasm_bindgen(method, js_name = jumpTo)]
    pub fn jump_to(this: &Map, options: &JsValue);
    #[wasm_bindgen(method, js_name = fitBounds)]
    pub fn fit_bounds(this: &Map, bounds: &JsValue);
    #[wasm_bindgen(method, js_name = getBounds)]
    pub fn get_bounds(this: &Map) -> LngLatBounds;
    #[wasm_bindgen(method, js_name = getCenter)]
    pub fn get_center(this: &Map) -> LngLat;
    #[wasm_bindgen(method, js_name = getZoom)]
    pub fn get_zoom(this: &Map) -> f64;
    #[wasm_bindgen(method, js_name = addSource)]
    pub fn add_source(this: &Map, id: &str, source: &JsValue);
    #[wasm_bindgen(method)]
    pub fn project(this: &Map, lng_lat: &JsValue) -> JsValue;
    #[wasm_bindgen(method)]
    pub fn unproject(this: &Map, point: &JsValue) -> LngLat;
    #[wasm_bindgen(method, js_name = loadImage)]
    pub fn load_image(this: &Map, url: &str) -> js_sys::Promise;
    #[wasm_bindgen(method, js_name = addImage)]
    pub fn add_image(this: &Map, id: &str, image: &JsValue);
    #[wasm_bindgen(method, js_name = hasImage)]
    pub fn has_image(this: &Map, id: &str) -> bool;
    #[wasm_bindgen(method, js_name = isStyleLoaded)]
    pub fn is_style_loaded(this: &Map) -> bool;
    #[wasm_bindgen(method, js_name = removeImage)]
    pub fn remove_image(this: &Map, id: &str);
    #[wasm_bindgen(method, js_name = listImages)]
    pub fn list_images(this: &Map) -> js_sys::Array;
    #[wasm_bindgen(method, js_name = addLayer)]
    pub fn add_layer(this: &Map, options: &JsValue);
}

/// `{ closeButton, closeOnClick }` (the `PopupOptions` the controller sets).
pub fn popup_options(close_button: bool, close_on_click: bool) -> JsValue {
    let o = js_sys::Object::new();
    let _ = js_sys::Reflect::set(
        &o,
        &JsValue::from_str("closeButton"),
        &JsValue::from_bool(close_button),
    );
    let _ = js_sys::Reflect::set(
        &o,
        &JsValue::from_str("closeOnClick"),
        &JsValue::from_bool(close_on_click),
    );
    o.into()
}

/// `{ element }` (`MarkerOptions`).
pub fn marker_options(element: &web_sys::HtmlElement) -> JsValue {
    let o = js_sys::Object::new();
    let _ = js_sys::Reflect::set(&o, &JsValue::from_str("element"), element);
    o.into()
}

pub fn lng_lat_array(lng: f64, lat: f64) -> JsValue {
    let a = js_sys::Array::new();
    a.push(&JsValue::from_f64(lng));
    a.push(&JsValue::from_f64(lat));
    a.into()
}

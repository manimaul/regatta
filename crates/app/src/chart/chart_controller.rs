//! = `ChartController.js.kt`: owns the MapLibre map, its markers and popups, and answers
//! `styleimagemissing` with the light sector SVGs in the theme's palette.

use super::chart_style_url;
use super::chart_view_model::{MapLocation, MapPoint, Mark};
use super::maplibre::{lng_lat_array, marker_options, popup_options, LngLat, Map, Marker, Popup};
use crate::state::theme::Theme;
use crate::utils::browser::encode_uri_component;
use crate::utils::light_sector::{parse_sector_image_id, SECTOR_IMAGE_PREFIX};
use leptos::prelude::document;
use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;
use wasm_bindgen::prelude::*;
use wasm_bindgen::JsCast;

type Listener = Closure<dyn FnMut(JsValue)>;

pub struct ChartController {
    pub map_view: Option<Rc<Map>>,
    pub on_move_end: Option<Rc<dyn Fn(MapLocation)>>,
    pub on_click: Option<Rc<dyn Fn(MapPoint)>>,
    pub on_click_location: Option<Rc<dyn Fn(LngLat)>>,
    pub on_load: Option<Rc<dyn Fn()>>,
    /// The current site theme (set by the view model).
    pub theme: Rc<dyn Fn() -> Theme>,
    mark_popup_map: HashMap<&'static str, Popup>,
    markers: Vec<Marker>,
    listeners: Vec<Listener>,
}

impl Default for ChartController {
    fn default() -> Self {
        ChartController::new()
    }
}

impl ChartController {
    pub fn new() -> ChartController {
        ChartController {
            map_view: None,
            on_move_end: None,
            on_click: None,
            on_click_location: None,
            on_load: None,
            theme: Rc::new(|| Theme::Light),
            mark_popup_map: HashMap::new(),
            markers: Vec::new(),
            listeners: Vec::new(),
        }
    }

    pub fn move_to(&self, location: MapLocation) {
        if let Some(map) = &self.map_view {
            let options = js_sys::Object::new();
            let _ = js_sys::Reflect::set(
                &options,
                &JsValue::from_str("center"),
                &lng_lat_array(location.longitude, location.latitude),
            );
            let _ = js_sys::Reflect::set(
                &options,
                &JsValue::from_str("zoom"),
                &JsValue::from_f64(location.zoom),
            );
            map.jump_to(&options);
        }
    }

    /// Animates the map to `mark` at the current zoom (`flyTo`); a no-op before the map exists.
    pub fn fly_to_mark(&self, mark: &Mark) {
        if let Some(map) = &self.map_view {
            let options = js_sys::Object::new();
            let _ = js_sys::Reflect::set(
                &options,
                &JsValue::from_str("center"),
                &lng_lat_array(mark.lng, mark.lat),
            );
            map.fly_to(&options);
        }
    }

    pub fn show_popup(&self, mark: &Mark) {
        if let (Some(popup), Some(map)) = (self.mark_popup_map.get(mark.letter), &self.map_view) {
            popup.add_to(map);
        }
    }

    pub fn hide_popup(&self, mark: &Mark) {
        if let Some(popup) = self.mark_popup_map.get(mark.letter) {
            popup.remove();
        }
    }

    pub fn add_mark_popup(
        this: &Rc<RefCell<Self>>,
        mark: Mark,
        on_select: impl Fn(Option<Mark>) + 'static,
        on_close: impl Fn(Mark) + 'static,
    ) {
        let mut ctl = this.borrow_mut();
        let Some(map) = ctl.map_view.clone() else {
            return;
        };
        let position = LngLat::new(mark.lng, mark.lat);
        let popup = Popup::new(&popup_options(true, false)).set_lng_lat(&position);
        popup.set_html(&format!("<h4>{}</h4>\n<h6>{}</h6>", mark.name, mark.desc));
        let close_mark = mark.clone();
        let close = Closure::<dyn FnMut(JsValue)>::new(move |_: JsValue| {
            leptos::logging::log!("popup {} closed", close_mark.letter);
            on_close(close_mark.clone());
        });
        popup.on("close", close.as_ref().unchecked_ref());
        ctl.listeners.push(close);
        ctl.mark_popup_map.insert(mark.letter, popup);

        let Ok(el) = document().create_element("div") else {
            return;
        };
        let Ok(el) = el.dyn_into::<web_sys::HtmlElement>() else {
            return;
        };
        el.set_inner_html(&format!("<span>{}</span>", mark.letter));
        let _ = el.style().set_property("color", "red");
        let _ = el.style().set_property("font-size", "40px");
        let select_mark = mark.clone();
        let click = Closure::<dyn FnMut(JsValue)>::new(move |_: JsValue| {
            on_select(Some(select_mark.clone()))
        });
        el.set_onclick(Some(click.as_ref().unchecked_ref()));
        ctl.listeners.push(click);
        let marker = Marker::new(&marker_options(&el))
            .set_lng_lat(&position)
            .add_to(&map);
        ctl.markers.push(marker);
    }

    pub fn get_bounds(&self) -> Vec<MapLocation> {
        leptos::logging::log!("get bounds called");
        if let Some(bounds) = self.map_view.as_ref().map(|m| m.get_bounds()) {
            leptos::logging::log!("bounds north = {}", bounds.get_north());
            leptos::logging::log!("bounds south = {}", bounds.get_south());
            leptos::logging::log!("bounds west = {}", bounds.get_west());
            leptos::logging::log!("bounds east = {}", bounds.get_east());
        }
        Vec::new()
    }

    pub fn create_map_view(this: &Rc<RefCell<Self>>, container: &web_sys::HtmlElement) {
        let args = {
            let ctl = this.borrow();
            ctl.map_libre_args(container)
        };
        let map = Rc::new(Map::new(&args));
        let weak = Rc::downgrade(this);
        let mut listeners = Vec::new();

        let w = weak.clone();
        let move_end = Closure::<dyn FnMut(JsValue)>::new(move |event: JsValue| {
            let Some(this) = w.upgrade() else { return };
            let Ok(target) = js_sys::Reflect::get(&event, &JsValue::from_str("target")) else {
                return;
            };
            let target: Map = target.unchecked_into();
            let center = target.get_center();
            let location = MapLocation {
                longitude: center.lng(),
                latitude: center.lat(),
                zoom: target.get_zoom(),
            };
            let cb = this.borrow().on_move_end.clone();
            if let Some(cb) = cb {
                cb(location);
            }
        });
        map.on("moveend", move_end.as_ref().unchecked_ref());
        listeners.push(move_end);

        let w = weak.clone();
        let click = Closure::<dyn FnMut(JsValue)>::new(move |event: JsValue| {
            let Some(this) = w.upgrade() else { return };
            let Ok(point) = js_sys::Reflect::get(&event, &JsValue::from_str("point")) else {
                return;
            };
            let x = js_sys::Reflect::get(&point, &JsValue::from_str("x"))
                .ok()
                .and_then(|v| v.as_f64())
                .unwrap_or(0.0) as i32;
            let y = js_sys::Reflect::get(&point, &JsValue::from_str("y"))
                .ok()
                .and_then(|v| v.as_f64())
                .unwrap_or(0.0) as i32;
            let pt = MapPoint { x, y };
            let (on_click, on_click_location, lng_lat) = {
                let ctl = this.borrow();
                (
                    ctl.on_click.clone(),
                    ctl.on_click_location.clone(),
                    ctl.unproject(pt),
                )
            };
            if let Some(cb) = on_click {
                cb(pt);
            }
            if let (Some(cb), Some(ll)) = (on_click_location, lng_lat) {
                cb(ll);
            }
        });
        map.on("click", click.as_ref().unchecked_ref());
        listeners.push(click);

        let w = weak.clone();
        let load = Closure::<dyn FnMut(JsValue)>::new(move |_: JsValue| {
            let Some(this) = w.upgrade() else { return };
            let cb = this.borrow().on_load.clone();
            if let Some(cb) = cb {
                cb();
            }
        });
        map.on("load", load.as_ref().unchecked_ref());
        listeners.push(load);

        let w = weak;
        let missing = Closure::<dyn FnMut(JsValue)>::new(move |event: JsValue| {
            let Some(this) = w.upgrade() else { return };
            if let Some(id) = js_sys::Reflect::get(&event, &JsValue::from_str("id"))
                .ok()
                .and_then(|v| v.as_string())
            {
                let (map, theme) = {
                    let ctl = this.borrow();
                    (ctl.map_view.clone(), (ctl.theme)())
                };
                if let Some(map) = map {
                    add_sector_image(map, &id, theme);
                }
            }
        });
        map.on("styleimagemissing", missing.as_ref().unchecked_ref());
        listeners.push(missing);

        let mut ctl = this.borrow_mut();
        ctl.listeners.extend(listeners);
        ctl.map_view = Some(map);
    }

    /// Swaps the chart style in place; markers and popups are DOM elements and survive it. The
    /// light sector images are drawn in the theme's palette under theme-independent ids, so they
    /// are dropped first and `styleimagemissing` redraws them for the new theme.
    pub fn set_theme(&mut self, theme: Theme) {
        if let Some(map) = &self.map_view {
            // listImages throws "Style is not done loading" mid-load; there is nothing to drop then
            if map.is_style_loaded() {
                for id in map.list_images().iter().filter_map(|v| v.as_string()) {
                    if id.starts_with(SECTOR_IMAGE_PREFIX) {
                        map.remove_image(&id);
                    }
                }
            }
            map.set_style(chart_style_url(theme));
        }
    }

    pub fn dispose_map_view(&mut self) {
        self.mark_popup_map.clear();
        self.markers.clear();
        if let Some(map) = self.map_view.take() {
            map.remove();
        }
        self.listeners.clear();
    }

    fn map_libre_args(&self, container: &web_sys::HtmlElement) -> JsValue {
        let obj = js_sys::Object::new();
        let set = |k: &str, v: &JsValue| {
            let _ = js_sys::Reflect::set(&obj, &JsValue::from_str(k), v);
        };
        set("style", &JsValue::from_str(chart_style_url((self.theme)())));
        set("container", container);
        set(
            "center",
            &lng_lat_array(-122.44511255700473, 47.28327102060851),
        );
        set("zoom", &JsValue::from_f64(10.5));
        set("attributionControl", &JsValue::from_bool(false));
        let bounds = js_sys::Array::new();
        bounds.push(&lng_lat_array(-123.66528922827865, 47.098629350626396)); // southwest
        bounds.push(&lng_lat_array(-121.34828434446568, 47.57621259351356)); // northeast
        set("maxBounds", &bounds);
        obj.into()
    }

    pub fn unproject(&self, map_point: MapPoint) -> Option<LngLat> {
        let map = self.map_view.as_ref()?;
        let point = js_sys::Array::new();
        point.push(&JsValue::from_f64(f64::from(map_point.x)));
        point.push(&JsValue::from_f64(f64::from(map_point.y)));
        Some(map.unproject(&point))
    }

    pub fn project(&self, map_location: MapLocation) -> Option<MapPoint> {
        let map = self.map_view.as_ref()?;
        let p = map.project(&lng_lat_array(
            map_location.longitude,
            map_location.latitude,
        ));
        let x = js_sys::Reflect::get(&p, &JsValue::from_str("x"))
            .ok()
            .and_then(|v| v.as_f64())? as i32;
        let y = js_sys::Reflect::get(&p, &JsValue::from_str("y"))
            .ok()
            .and_then(|v| v.as_f64())? as i32;
        Some(MapPoint { x, y })
    }
}

/// Rasterizes a `LIGHTS_sector` icon (see `SectorImage`) and registers it under its id.
fn add_sector_image(map: Rc<Map>, id: &str, theme: Theme) {
    let Some(sector) = parse_sector_image_id(id) else {
        return;
    };
    let svg = sector.svg(theme);
    let Ok(img) = web_sys::HtmlImageElement::new_with_width_and_height(200, 200) else {
        return;
    };
    let id = id.to_string();
    let image = img.clone();
    let onload = Closure::<dyn FnMut()>::new(move || {
        if !map.has_image(&id) {
            map.add_image(&id, &image);
        }
    });
    img.set_onload(Some(onload.as_ref().unchecked_ref()));
    onload.forget();
    img.set_src(&format!(
        "data:image/svg+xml;charset=utf-8,{}",
        encode_uri_component(&svg)
    ));
}

//! = `ChartViewModel.kt`: the course marks, the remembered map location and the selected mark.

use leptos::prelude::*;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct MapLocation {
    pub longitude: f64,
    pub latitude: f64,
    pub zoom: f64,
}

impl Default for MapLocation {
    fn default() -> Self {
        MapLocation {
            longitude: -118.512,
            latitude: 33.96832,
            zoom: 12.0,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MapPoint {
    pub x: i32,
    pub y: i32,
}

/// A course mark; `lng` / `lat` are the `MapLibre.LngLat` of the Kotlin side.
#[derive(Debug, Clone, PartialEq)]
pub struct Mark {
    pub name: &'static str,
    pub desc: &'static str,
    pub letter: &'static str,
    pub lng: f64,
    pub lat: f64,
}

const fn mark(
    name: &'static str,
    desc: &'static str,
    letter: &'static str,
    lng: f64,
    lat: f64,
) -> Mark {
    Mark {
        name,
        desc,
        letter,
        lng,
        lat,
    }
}

pub const MARKS: [Mark; 18] = [
    mark(
        "TTPYC Mark 'A'",
        "White Column",
        "A",
        -122.33305114731311,
        47.39704811163327,
    ),
    mark(
        "Blair Waterway",
        "Yellow Perm. Can",
        "B",
        -122.42167447164488,
        47.27810246077564,
    ),
    mark(
        "Chinese Reconciliation Park (Pagoda)",
        "Temp",
        "C",
        -122.460895,
        47.276678,
    ),
    mark(
        "Dashpoint Temp",
        "Temp",
        "D",
        -122.43615006110504,
        47.32174845199978,
    ),
    mark(
        "Poverty Bay",
        "Temp",
        "E",
        -122.34450922624008,
        47.345379233271075,
    ),
    mark(
        "Foss Waterway",
        "Temp",
        "F",
        -122.4379856307234,
        47.26191956366969,
    ),
    mark(
        "TTPYC Mark 'I'",
        "White Column",
        "I",
        -122.33392193748179,
        47.386890766454115,
    ),
    mark(
        "TTPYC Mark 'J'",
        "White Column",
        "J",
        -122.37664522730665,
        47.383257011839646,
    ),
    mark(
        "Lighthouse at Brown's Pt.",
        "Temp",
        "L",
        -122.44710740743537,
        47.306778710579266,
    ),
    mark(
        "Madronas",
        "Temp",
        "M",
        -122.44721754644462,
        47.29943077416439,
    ),
    mark(
        "Neil Point",
        "Temp",
        "N",
        -122.48771988020714,
        47.329219485017916,
    ),
    mark(
        "Outfall Buoy",
        "Yellow Perm. Mark",
        "O",
        -122.48416224770311,
        47.28729287090877,
    ),
    mark(
        "Piner Point",
        "Temp",
        "P",
        -122.45365267698233,
        47.34233563126003,
    ),
    mark(
        "Arrrmy Dock",
        "Temp",
        "R",
        -122.4158178975889,
        47.28649587180013,
    ),
    mark(
        "Quartermaster Harbor",
        "Temp",
        "Q",
        -122.47008809457841,
        47.382987204303305,
    ),
    mark(
        "TYC White Buoy",
        "Perm. Mark",
        "T",
        -122.50489938041565,
        47.30572288135093,
    ),
    mark(
        "Mauri Island",
        "Temp",
        "U",
        -122.42235618134569,
        47.36942826972913,
    ),
    mark(
        "Manzzzzanita",
        "Red Perm. Can",
        "Z",
        -122.4794968763011,
        47.34899776773085,
    ),
];

pub fn marks() -> Vec<Mark> {
    MARKS.to_vec()
}

/// `localStorage["MapLocation"]`, the key the Kotlin `localStoreSet(item)` used (the class name).
pub const MAP_LOCATION_STORAGE_KEY: &str = "MapLocation";

fn saved_location() -> Option<MapLocation> {
    serde_json::from_str(&crate::utils::browser::local_storage_get(
        MAP_LOCATION_STORAGE_KEY,
    )?)
    .ok()
}

/// = `ChartState` + the controller (browser only).
#[derive(Clone, Copy)]
pub struct ChartState {
    pub location: RwSignal<MapLocation>,
    pub mark: RwSignal<Option<Mark>>,
    #[cfg(feature = "hydrate")]
    controller: StoredValue<
        std::rc::Rc<std::cell::RefCell<super::chart_controller::ChartController>>,
        LocalStorage,
    >,
}

pub fn provide_chart() -> ChartState {
    let state = ChartState {
        location: RwSignal::new(saved_location().unwrap_or_default()),
        mark: RwSignal::new(None),
        #[cfg(feature = "hydrate")]
        controller: StoredValue::new_local(std::rc::Rc::new(std::cell::RefCell::new(
            super::chart_controller::ChartController::new(),
        ))),
    };
    provide_context(state);
    state
}

pub fn use_chart() -> ChartState {
    use_context::<ChartState>().unwrap_or_else(provide_chart)
}

impl ChartState {
    pub fn reload(&self) {
        self.location.set(MapLocation::default());
        self.mark.set(None);
    }

    /// Creates the map in `container` once it is in the DOM, wires the marks, follows the theme,
    /// remembers the location, and disposes the map when the screen goes away. Nothing on the server.
    pub fn mount(&self, container: NodeRef<leptos::html::Div>) {
        #[cfg(feature = "hydrate")]
        {
            use super::chart_controller::ChartController;
            use crate::state::theme::use_theme;
            let state = *self;
            let controller = self.controller;
            let theme = use_theme();
            Effect::new(move |_| {
                if let Some(div) = container.get() {
                    let el: web_sys::HtmlElement = div.into();
                    controller.with_value(|c| {
                        let mut ctl = c.borrow_mut();
                        ctl.theme = std::rc::Rc::new(move || theme.0.get_untracked());
                        let c2 = c.clone();
                        ctl.on_load = Some(std::rc::Rc::new(move || {
                            for mark in marks() {
                                ChartController::add_mark_popup(
                                    &c2,
                                    mark,
                                    move |m| state.select_mark(m),
                                    move |m| {
                                        if state.mark.get_untracked().as_ref() == Some(&m) {
                                            state.mark.set(None);
                                        }
                                    },
                                );
                            }
                        }));
                        ctl.on_move_end = Some(std::rc::Rc::new(move |location| {
                            leptos::logging::log!("moveend location: {location:?}");
                            state.location.set(location);
                        }));
                        ctl.on_click = Some(std::rc::Rc::new(|point| {
                            leptos::logging::log!("click point: {point:?}")
                        }));
                        ctl.on_click_location = None;
                        drop(ctl);
                        ChartController::create_map_view(c, &el);
                    });
                }
            });
            // remember the location
            Effect::new(move |_| {
                let location = state.location.get();
                if let Ok(json) = serde_json::to_string(&location) {
                    crate::utils::browser::local_storage_set(MAP_LOCATION_STORAGE_KEY, &json);
                }
            });
            // follow the light/dark toggle while the chart is open. The current value is skipped: createMapView
            // already picks the style for a fresh map, and touching the map before its style has loaded throws.
            Effect::new(move |previous: Option<crate::state::theme::Theme>| {
                let current = theme.0.get();
                if let Some(previous) = previous {
                    if previous != current {
                        controller.with_value(|c| c.borrow_mut().set_theme(current));
                    }
                }
                current
            });
            on_cleanup(move || {
                controller.try_with_value(|c| c.borrow_mut().dispose_map_view());
            });
        }
        #[cfg(not(feature = "hydrate"))]
        let _ = container;
    }

    /// The dropdown's choice: selects the mark and flies the map to it. Tapping a mark on the map
    /// goes through [`ChartState::select_mark`] instead, so the map does not move under the user.
    pub fn choose_mark(&self, mark: Option<Mark>) {
        self.select_mark(mark.clone());
        #[cfg(feature = "hydrate")]
        if let Some(m) = &mark {
            self.controller.with_value(|c| c.borrow().fly_to_mark(m));
        }
    }

    /// Selects `mark` (popup shown, the previous one hidden) without moving the map.
    pub fn select_mark(&self, mark: Option<Mark>) {
        #[cfg(feature = "hydrate")]
        {
            let previous = self.mark.get_untracked();
            self.controller.with_value(|c| {
                let ctl = c.borrow();
                if let Some(p) = &previous {
                    ctl.hide_popup(p);
                }
                if let Some(m) = &mark {
                    ctl.show_popup(m);
                }
            });
        }
        self.mark.set(mark);
    }
}

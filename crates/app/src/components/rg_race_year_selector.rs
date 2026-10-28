//! The year picker above the race lists.
//!
//! The list of years is server data ([`crate::server_fns::years`]); the selected year is shared app state
//! ([`selected_year`]), so every screen with a picker starts at the same year.

use crate::app_style;
use crate::cb::Cb;
use crate::routes::async_view_loading;
use crate::routes::race_results::RgYearSelect;
use crate::server_fns;
use crate::utils::async_state::Loaded;
use crate::utils::date_util::current_year;
use leptos::prelude::*;

/// The selected race year (the current year at first) and the years that have races.
#[derive(Clone, Copy)]
pub struct RgRaceYearState {
    pub year: RwSignal<Option<i32>>,
    pub years: Loaded<Vec<String>>,
}

fn provide(years: Loaded<Vec<String>>) -> RgRaceYearState {
    let state = RgRaceYearState {
        year: RwSignal::new(current_year().parse().ok()),
        years,
    };
    provide_context(state);
    state
}

/// Provides the app's year state, loading the years from the server.
pub fn provide_race_year_loaded() -> RgRaceYearState {
    provide(Loaded::new(server_fns::years))
}

/// Provides a year state that never loads, for components rendered outside the app.
pub fn provide_race_year() -> RgRaceYearState {
    provide(Loaded::inert())
}

pub fn use_race_year() -> RgRaceYearState {
    use_context::<RgRaceYearState>().unwrap_or_else(provide_race_year)
}

/// Returns the selected race year without tracking it.
pub fn selected_year() -> Option<i32> {
    use_race_year().year.get_untracked()
}

impl RgRaceYearState {
    pub fn select_year(&self, year: Option<&str>) -> Option<i32> {
        let y = year?.parse::<i32>().ok()?;
        self.year.set(Some(y));
        Some(y)
    }

    pub fn reload(&self) {
        self.years.reload();
    }
}

/// The year picker; `on_year_select` gets the newly selected year.
#[component]
pub fn RgRaceYearSelector(
    #[prop(optional, into)] on_year_select: Option<Cb<Option<i32>>>,
) -> impl IntoView {
    let state = use_race_year();
    view! {
        <div class=app_style::MARGIN_BOT style="width: fit-content;">
            {async_view_loading(state.years.signal(), move || state.reload(), || "...", move |year_list: Vec<String>| {
                (!year_list.is_empty()).then(|| {
                    let year = Signal::derive(move || state.year.get().map(|y| y.to_string()).unwrap_or_else(|| "null".to_string()));
                    view! {
                        <RgYearSelect year=year years=year_list handler={move |y: Option<String>| {
                            let selected = state.select_year(y.as_deref());
                            if let Some(cb) = &on_year_select {
                                cb.run(selected);
                            }
                        }}/>
                    }
                })
            })}
        </div>
    }
}

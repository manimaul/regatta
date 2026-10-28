//! The Series screen (admin): the series list, its order, and the add/edit series modal.

use crate::app_style;
use crate::components::join_classes;
use crate::components::modal::{RgModalBody, RgModalButton, RgModalFooterRow};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_form::{RgForm, RgInput};
use crate::components::rg_sortable::RgSortable;
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgTh, RgThead, RgTr};
use crate::routes::async_view;
use crate::server_fns;
use crate::state::alerts::use_alerts;
use crate::utils::async_state::{call, Loaded};
use leptos::either::Either;
use leptos::prelude::*;
use regatta_common::Series as SeriesModel;

pub const ADD_EDIT_SERIES_MODAL_ID: &str = "add-edit-series";

/// The Series screen state: the loaded series, the series being edited and the sort mode.
#[derive(Clone, Copy)]
pub struct SeriesState {
    pub series: Loaded<Vec<SeriesModel>>,
    pub add_edit_series: RwSignal<SeriesModel>,
    pub sort_mode: RwSignal<bool>,
}

impl SeriesState {
    pub fn new() -> SeriesState {
        SeriesState {
            series: Loaded::new(server_fns::all_series),
            add_edit_series: RwSignal::new(SeriesModel::default()),
            sort_mode: RwSignal::new(false),
        }
    }

    pub fn reload(&self) {
        self.series.reload();
    }

    /// Returns one past the highest sort value, for a new series.
    pub fn next_sort(&self) -> i32 {
        self.series
            .value()
            .and_then(|list| list.iter().map(|s| s.sort).max())
            .map(|s| s + 1)
            .unwrap_or(0)
    }

    pub fn edit_series(&self, series: SeriesModel) {
        self.add_edit_series.set(series);
    }

    pub fn edit_series_name(&self, name: String) {
        self.add_edit_series.update(|s| s.name = name);
    }

    /// Saves the series in their new order, then reloads and leaves sort mode.
    pub fn save_order(&self, order: Vec<SeriesModel>) {
        self.sort_mode.set(false);
        let series = self.series;
        call(server_fns::post_series(order), move |r| {
            series.after(r.map(drop))
        });
    }

    /// Saves `series`, a new one taking the next sort value, then reloads.
    pub fn upsert(&self, series: SeriesModel) {
        let s = if series.id > 0 {
            series
        } else {
            SeriesModel {
                sort: self.next_sort(),
                ..series
            }
        };
        let list = self.series;
        call(server_fns::post_series(vec![s]), move |r| {
            list.after(r.map(drop))
        });
    }

    /// Deletes `series`, then reloads.
    pub fn delete(&self, series: SeriesModel) {
        let list = self.series;
        call(server_fns::delete_series(series.id), move |r| list.after(r));
    }
}

impl Default for SeriesState {
    fn default() -> Self {
        SeriesState::new()
    }
}

#[component]
pub fn Series() -> impl IntoView {
    let state = SeriesState::new();
    async_view(
        state.series.signal(),
        move || state.reload(),
        move |series| {
            if state.sort_mode.get() {
                Either::Left(view! { <SortSeries all_series=series state=state/> })
            } else {
                Either::Right(view! { <AllSeries all_series=series state=state/> })
            }
        },
    )
}

#[component]
pub fn SortSeries(all_series: Vec<SeriesModel>, state: SeriesState) -> impl IntoView {
    let order = RwSignal::new(all_series.clone());
    let classes = join_classes(&[app_style::MARGIN_START, app_style::MARGIN_TOP]);
    view! {
        <h1>"Series Sort Order"</h1>
        <RgSortable
            items=all_series
            name=|s: &SeriesModel| s.name.clone()
            on_sort={move |sorted: Vec<SeriesModel>| order.set(sorted.into_iter().enumerate().map(|(i, mut s)| { s.sort = i as i32; s }).collect())}
        />
        <RgButton label="Cancel" custom_classes=classes.clone() click=move |()| state.sort_mode.set(false)/>
        <RgButton label="Save" style=RgButtonStyle::Success custom_classes=classes click=move |()| state.save_order(order.get_untracked())/>
    }
}

#[component]
pub fn AllSeries(all_series: Vec<SeriesModel>, state: SeriesState) -> impl IntoView {
    view! {
        <h1>"Series"</h1>
        <div class=app_style::MARGIN_BOT>
            <RgModalButton
                id=ADD_EDIT_SERIES_MODAL_ID
                style=RgButtonStyle::SuccessOutline
                button_label="Add Series"
                open_action=move |()| state.edit_series(SeriesModel::default())
            />
            <AddEditSeries state=state/>
        </div>
        <RgTable>
            <RgThead>
                <RgTr>
                    <RgTh>"Series Name"</RgTh>
                    <RgTh>"Total Races In Series"</RgTh>
                    <RgTh>"Action"</RgTh>
                </RgTr>
            </RgThead>
            <RgTbody>
                {all_series.into_iter().map(|series| {
                    let edit = series.clone();
                    view! {
                        <RgTr>
                            <RgTd>{series.name.clone()}</RgTd>
                            <RgTd>{series.race_count.to_string()}</RgTd>
                            <RgTd>
                                <RgModalButton
                                    id=ADD_EDIT_SERIES_MODAL_ID
                                    style=RgButtonStyle::PrimaryOutline
                                    button_label="Edit Series"
                                    open_action=move |()| state.edit_series(edit.clone())
                                />
                            </RgTd>
                        </RgTr>
                    }
                }).collect_view()}
            </RgTbody>
        </RgTable>
        <div class="flex-wrap">
            <RgButton label="Change Sort Order" click=move |()| state.sort_mode.set(true)/>
        </div>
    }
}

#[component]
pub fn AddEditSeries(state: SeriesState) -> impl IntoView {
    let alerts = use_alerts();
    let title = move || {
        if state.add_edit_series.with(|s| s.id == 0) {
            "Add Series "
        } else {
            "Edit Series"
        }
    };
    let name = Signal::derive(move || state.add_edit_series.with(|s| s.name.clone()));
    view! {
        <RgModalBody
            id=ADD_EDIT_SERIES_MODAL_ID
            modal_title=title
            footer=Box::new(move || view! {
                <RgModalFooterRow>
                    {move || state.add_edit_series.with(|s| s.id != 0).then(|| view! {
                        <RgButton
                            label="Delete"
                            style=RgButtonStyle::Danger
                            disabled=Signal::derive(move || state.add_edit_series.with(|s| s.race_count > 0))
                            closes_modal=true
                            click=move |()| {
                                let series = state.add_edit_series.get_untracked();
                                alerts.show_alert(format!("{} deleted!", series.name));
                                state.delete(series);
                            }
                        />
                    })}
                    <RgButton
                        label="Save"
                        style=RgButtonStyle::Success
                        disabled=Signal::derive(move || state.add_edit_series.with(|s| s.name.trim().is_empty()))
                        closes_modal=true
                        click=move |()| {
                            let series = state.add_edit_series.get_untracked();
                            alerts.show_alert(format!("{} saved!", series.name));
                            state.upsert(series);
                        }
                    />
                </RgModalFooterRow>
            }.into_any())
        >
            <RgForm>
                <fieldset>
                    <p><RgInput label="Series Name" value=name place_holder=true listener=move |v: String| state.edit_series_name(v)/></p>
                </fieldset>
            </RgForm>
        </RgModalBody>
    }
}

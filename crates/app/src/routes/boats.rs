//! The Boats screen (admin) and the boat and skipper state it shares with the People screen.
//!
//! Boats and people are loaded together as one [`BoatPeopleComposite`].

use crate::app_style;
use crate::components::add_edit_boat_modal::{
    AddEditBoatModal, AddEditBoatModalButton, AddEditBoatState,
};
use crate::components::join_classes;
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgThead, RgTr};
use crate::routes::async_view;
use crate::server_fns;
use crate::state::alerts::use_alerts;
use crate::utils::async_state::{call, Loaded};
use crate::utils::item_name_contains_filter;
use leptos::prelude::*;
use regatta_common::{Boat, BoatSkipper, Person};
use serde::{Deserialize, Serialize};

/// Every boat with its skipper and every person, loaded together.
#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
pub struct BoatPeopleComposite {
    pub boats: Vec<BoatSkipper>,
    pub people: Vec<Person>,
}

impl BoatPeopleComposite {
    /// Returns the name of the boat `person` skippers, or `"-"`.
    pub fn find_boat_name(&self, person: &Person) -> String {
        self.boats
            .iter()
            .find(|bs| bs.skipper.as_ref().map(|s| s.id) == Some(person.id))
            .and_then(|bs| bs.boat.as_ref().map(|b| b.name.clone()))
            .unwrap_or_else(|| "-".to_string())
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum AddEditBoatMode {
    Editing,
    #[default]
    Adding,
}

impl AddEditBoatMode {
    pub const fn label(self) -> &'static str {
        match self {
            AddEditBoatMode::Editing => "Edit Boat",
            AddEditBoatMode::Adding => "Add Boat",
        }
    }
}

/// The boat and skipper editing state and the loaded boats and people, shared by the Boats and People screens.
#[derive(Clone, Copy)]
pub struct BoatState {
    pub add_edit_state: AddEditBoatState,
    pub add_edit_person: RwSignal<Person>,
    pub show_confirm_delete_person: RwSignal<bool>,
    pub add_edit_mode: RwSignal<AddEditBoatMode>,
    pub response: Loaded<BoatPeopleComposite>,
}

impl BoatState {
    pub fn new() -> BoatState {
        BoatState {
            add_edit_state: AddEditBoatState::new(),
            add_edit_person: RwSignal::new(Person::default()),
            show_confirm_delete_person: RwSignal::new(false),
            add_edit_mode: RwSignal::new(AddEditBoatMode::Adding),
            response: Loaded::new(|| async {
                let boats = server_fns::all_boats().await?;
                let people = server_fns::all_people().await?;
                Ok(BoatPeopleComposite { boats, people })
            }),
        }
    }

    pub fn reload(&self) {
        self.response.reload();
    }

    pub fn set_add_edit_mode(&self, mode: AddEditBoatMode, boat: Option<BoatSkipper>) {
        self.add_edit_mode.set(mode);
        match boat {
            Some(bs) => self
                .add_edit_state
                .set(bs.boat.unwrap_or_default(), bs.skipper),
            None => self.add_edit_state.clear(),
        }
    }

    pub fn set_edit_person(&self, person: Person) {
        self.add_edit_person.set(person);
        self.show_confirm_delete_person.set(false);
    }

    pub fn confirm_delete_person(&self, show: bool) {
        self.show_confirm_delete_person.set(show);
    }

    /// Deletes the skipper being edited, then reloads.
    pub fn delete_person(&self) {
        let id = self.add_edit_person.with_untracked(|p| p.id);
        self.set_edit_person(Person::default());
        let response = self.response;
        call(server_fns::delete_person(id), move |r| response.after(r));
    }

    /// Inserts or updates the skipper being edited, then reloads.
    pub fn upsert_person(&self) {
        let person = self.add_edit_person.get_untracked();
        self.set_edit_person(Person::default());
        let response = self.response;
        call(server_fns::post_person(person), move |r| {
            response.after(r.map(drop))
        });
    }

    /// Inserts or updates `boat`, clears the editor, then reloads.
    pub fn save_edited_boat(&self, boat: Boat) {
        self.add_edit_state.clear();
        let response = self.response;
        call(server_fns::post_boat(boat), move |r| {
            response.after(r.map(drop))
        });
    }

    /// Deletes `boat`, then reloads.
    pub fn delete_boat(&self, boat: Boat) {
        let response = self.response;
        call(server_fns::delete_boat(boat.id), move |r| response.after(r));
    }
}

impl Default for BoatState {
    fn default() -> Self {
        BoatState::new()
    }
}

#[component]
pub fn Boats() -> impl IntoView {
    let state = BoatState::new();
    async_view(
        state.response.signal(),
        move || state.reload(),
        move |composite| {
            view! { <BoatList state=state boats_and_people=composite/> }
        },
    )
}

#[component]
pub fn BoatList(state: BoatState, boats_and_people: BoatPeopleComposite) -> impl IntoView {
    let alerts = use_alerts();
    let filter = RwSignal::new(String::new());
    let filter_group = join_classes(&["rg-input-group rg-input-group-sm", app_style::MARGIN_BOT]);
    let people = boats_and_people.people.clone();
    let boats = boats_and_people.boats;
    let modal_title = move || state.add_edit_mode.get().label();
    view! {
        <div>
            <h1>"Boats"</h1>
            <div class=app_style::MARGIN_BOT>
                <AddEditBoatModalButton
                    style=RgButtonStyle::SuccessOutline
                    button_label=AddEditBoatMode::Adding.label()
                    open_action=move |()| state.set_add_edit_mode(AddEditBoatMode::Adding, None)
                />
                <AddEditBoatModal people=people modal_title=modal_title state=state.add_edit_state save=move |boat: Boat| state.save_edited_boat(boat)/>
            </div>
            <div class=filter_group>
                <RgButton label="X" style=RgButtonStyle::SecondaryOutline click=move |()| filter.set(String::new())/>
                <input type="text" class="rg-input" placeholder="Type to filter" value=move || filter.get() prop:value=move || filter.get() on:input=move |ev| filter.set(event_target_value(&ev))/>
            </div>
            <RgTable>
                <RgThead>
                    <RgTr>
                        <th>"Boat Name"</th>
                        <th>"Skipper"</th>
                        <th>"Sail Number"</th>
                        <th>"Type"</th>
                        <th>"Rating"</th>
                        <th>"Number of races"</th>
                        <th>"Action"</th>
                    </RgTr>
                </RgThead>
                <RgTbody>
                    {move || boats.iter().filter(|bs| {
                        let searchable = [
                            bs.boat.as_ref().map(|b| b.name.clone()),
                            bs.skipper.as_ref().map(Person::full_name),
                            bs.boat.as_ref().map(|b| b.sail_number.clone()),
                            bs.boat.as_ref().map(|b| b.boat_type.clone()),
                            bs.boat.as_ref().map(|b| b.rating_type.rated_label(b.phrf_rating, b.orc_info().as_deref())),
                        ].into_iter().flatten().collect::<Vec<_>>().join(" ");
                        item_name_contains_filter(&searchable, &filter.get())
                    }).map(|boat| {
                        let edit = boat.clone();
                        let number_of_races = boat.boat.as_ref().map(|b| b.number_of_races);
                        let deletable = boat.boat.clone().filter(|b| b.number_of_races == 0);
                        let name = boat.boat.as_ref().map(|b| b.name.clone()).unwrap_or_default();
                        let skipper = boat.skipper.as_ref().map(Person::full_name);
                        let sail = boat.boat.as_ref().map(|b| b.sail_number.clone()).unwrap_or_default();
                        let boat_type = boat.boat.as_ref().map(|b| b.boat_type.clone()).unwrap_or_default();
                        let rating = boat.boat.as_ref().map(|b| b.rating_type.rated_label(b.phrf_rating, b.orc_info().as_deref())).unwrap_or_default();
                        view! {
                            <RgTr>
                                <RgTd>{name}</RgTd>
                                <RgTd>{skipper}</RgTd>
                                <RgTd>{sail}</RgTd>
                                <RgTd>{boat_type}</RgTd>
                                <RgTd>{rating}</RgTd>
                                <RgTd>{number_of_races.map(|n| n.to_string()).unwrap_or_default()}</RgTd>
                                <RgTd>
                                    <AddEditBoatModalButton
                                        style=RgButtonStyle::PrimaryOutline
                                        button_label=AddEditBoatMode::Editing.label()
                                        custom_classes=app_style::MARGIN_ALL
                                        open_action=move |()| state.set_add_edit_mode(AddEditBoatMode::Editing, Some(edit.clone()))
                                    />
                                    {deletable.map(|delete_boat| view! {
                                        <RgButton
                                            label="Delete Boat"
                                            style=RgButtonStyle::Danger
                                            custom_classes=app_style::MARGIN_ALL
                                            click=move |()| {
                                                let b = delete_boat.clone();
                                                alerts.confirm("Are you sure?", Some(format!("Delete {}?", b.name)), move |yes: bool| {
                                                    if yes {
                                                        state.delete_boat(b.clone());
                                                        alerts.show_alert(format!("{} deleted!", b.name));
                                                    }
                                                });
                                            }
                                        />
                                    })}
                                </RgTd>
                            </RgTr>
                        }
                    }).collect_view()}
                </RgTbody>
            </RgTable>
        </div>
    }
}

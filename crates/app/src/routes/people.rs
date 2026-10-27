//! = `routes/People.kt` (skippers), with the `BoatViewModel` person-editing state.

use crate::app_style;
use crate::components::join_classes;
use crate::components::modal::{RgModalBody, RgModalButton, RgModalFooterRow};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_form::{RgCheck, RgForm, RgInput};
use crate::components::rg_spinner::RgSpinner;
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgTh, RgThead, RgTr};
use crate::routes::async_view_loading;
use crate::routes::boats::{BoatPeopleComposite, BoatState};
use crate::state::alerts::use_alerts;
use crate::utils::item_name_contains_filter;
use leptos::prelude::*;
use regatta_common::Person;

pub const ADD_EDIT_SKIPPER_MODAL_ID: &str = "add-edit-skipper";

#[component]
pub fn People() -> impl IntoView {
    let state = BoatState::new();
    view! {
        <div>
            {async_view_loading(
                state.response.signal(),
                move || state.reload(),
                move || view! { <RgSpinner/><PeopleLoaded composite=None state=state/> },
                move |composite| view! { <PeopleLoaded composite=Some(composite) state=state/> },
            )}
        </div>
    }
}

#[component]
pub fn PeopleLoaded(composite: Option<BoatPeopleComposite>, state: BoatState) -> impl IntoView {
    let filter = RwSignal::new(String::new());
    let filter_group = join_classes(&["rg-input-group rg-input-group-sm", app_style::MARGIN_BOT]);
    let people = composite
        .as_ref()
        .map(|c| c.people.clone())
        .unwrap_or_default();
    view! {
        <div>
            <h1>"Skippers"</h1>
            <div class=app_style::MARGIN_BOT>
                <RgModalButton
                    id=ADD_EDIT_SKIPPER_MODAL_ID
                    style=RgButtonStyle::SuccessOutline
                    button_label="Add Skipper"
                    open_action=move |()| state.set_edit_person(Person::default())
                />
                <AddEditPerson state=state/>
            </div>
            <div class=filter_group>
                <RgButton label="X" style=RgButtonStyle::SecondaryOutline click=move |()| filter.set(String::new())/>
                <input type="text" class="rg-input" placeholder="Type to filter" value=move || filter.get() prop:value=move || filter.get() on:input=move |ev| filter.set(event_target_value(&ev))/>
            </div>
            <RgTable>
                <RgThead>
                    <RgTr>
                        <RgTh>"First"</RgTh>
                        <RgTh>"Last"</RgTh>
                        <RgTh>"Boat"</RgTh>
                        <RgTh>"Member"</RgTh>
                        <RgTh>"Action"</RgTh>
                    </RgTr>
                </RgThead>
                <RgTbody>
                    {move || {
                        let composite = composite.clone();
                        people.iter().filter(|person| {
                            let boat_name = composite.as_ref().map(|c| c.find_boat_name(person)).unwrap_or_default();
                            let searchable = format!("{} {} {}", person.first, person.last, boat_name);
                            item_name_contains_filter(&searchable, &filter.get())
                        }).cloned().map(|person| {
                            let boat_name = composite.as_ref().map(|c| c.find_boat_name(&person)).unwrap_or_default();
                            let edit = person.clone();
                            view! {
                                <RgTr>
                                    <RgTd>{person.first.clone()}</RgTd>
                                    <RgTd>{person.last.clone()}</RgTd>
                                    <RgTd>{boat_name}</RgTd>
                                    <RgTd>{if person.club_member { "Yes" } else { "No" }}</RgTd>
                                    <RgTd>
                                        <RgModalButton
                                            id=ADD_EDIT_SKIPPER_MODAL_ID
                                            style=RgButtonStyle::PrimaryOutline
                                            button_label="Edit Skipper"
                                            open_action=move |()| state.set_edit_person(edit.clone())
                                        />
                                    </RgTd>
                                </RgTr>
                            }
                        }).collect_view()
                    }}
                </RgTbody>
            </RgTable>
        </div>
    }
}

#[component]
pub fn AddEditPerson(state: BoatState) -> impl IntoView {
    let alerts = use_alerts();
    let person = state.add_edit_person;
    let confirm_delete = state.show_confirm_delete_person;
    let title = move || {
        let p = person.get();
        if confirm_delete.get() {
            format!("Delete '{} {}'?", p.first, p.last)
        } else if p.id == 0 {
            "Add Skipper".to_string()
        } else {
            "Edit Skipper".to_string()
        }
    };
    let first = Signal::derive(move || person.with(|p| p.first.clone()));
    let last = Signal::derive(move || person.with(|p| p.last.clone()));
    let member = Signal::derive(move || person.with(|p| p.club_member));
    view! {
        <RgModalBody
            id=ADD_EDIT_SKIPPER_MODAL_ID
            modal_title=title
            footer=Box::new(move || view! {
                <RgModalFooterRow>
                    {move || if confirm_delete.get() {
                        leptos::either::Either::Left(view! {
                            <RgButton label="No" style=RgButtonStyle::PrimaryOutline click=move |()| state.confirm_delete_person(false)/>
                            <RgButton
                                label="Yes"
                                style=RgButtonStyle::Danger
                                closes_modal=true
                                click=move |()| {
                                    let p = person.get_untracked();
                                    state.delete_person();
                                    alerts.show_alert(format!("{} {} deleted!", p.first, p.last));
                                }
                            />
                        })
                    } else {
                        leptos::either::Either::Right(view! {
                            {move || if person.with(|p| p.id == 0) {
                                leptos::either::Either::Left(view! {
                                    <RgButton label="Clear" style=RgButtonStyle::PrimaryOutline click=move |()| state.set_edit_person(Person::default())/>
                                })
                            } else {
                                leptos::either::Either::Right(view! {
                                    <RgButton label="Delete" style=RgButtonStyle::Danger click=move |()| state.confirm_delete_person(true)/>
                                })
                            }}
                            <RgButton
                                label="Save"
                                style=RgButtonStyle::Success
                                disabled=Signal::derive(move || person.with(|p| p.first.trim().is_empty() || p.last.trim().is_empty()))
                                closes_modal=true
                                click=move |()| {
                                    let p = person.get_untracked();
                                    state.upsert_person();
                                    alerts.show_alert(format!("{} {} updated!", p.first, p.last));
                                }
                            />
                        })
                    }}
                </RgModalFooterRow>
            }.into_any())
        >
            {move || (!confirm_delete.get()).then(|| view! {
                <RgForm>
                    <fieldset>
                        <p><RgInput label="First" value=first place_holder=true listener=move |v: String| person.update(|p| p.first = v)/></p>
                        <p><RgInput label="Last" value=last place_holder=true listener=move |v: String| person.update(|p| p.last = v)/></p>
                        <div class="mb-4"><RgCheck label="Club member" checked=member bold=false listener=move |v: bool| person.update(|p| p.club_member = v)/></div>
                    </fieldset>
                </RgForm>
            })}
        </RgModalBody>
    }
}

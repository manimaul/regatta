//! = `AddEditBoatModal.kt`. The draft boat is the `BoatViewModel.addEditState` of the Kotlin side,
//! here an `AddEditBoatState` of signals owned by the Boats screen, which also saves (`save`).

use crate::cb::Cb;
use crate::components::modal::{RgModalBody, RgModalButton, RgModalFooterRow};
use crate::components::rating_selections::{Action, RatingSelectionsBoat};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_dropdown::RgSkipperDropdown;
use crate::components::rg_form::{RgForm, RgInput};
use crate::components::Cls;
use crate::state::alerts::use_alerts;
use leptos::prelude::*;
use regatta_common::{Boat, OrcCertificate, Person, RatingType};

pub const ADD_EDIT_BOAT_MODAL_ID: &str = "add-edit-boat";

/// = `AddEditBoatState`.
#[derive(Clone, Copy)]
pub struct AddEditBoatState {
    pub add_boat: RwSignal<Boat>,
    pub add_skipper: RwSignal<Option<Person>>,
    /// Why the draft cannot be saved: a blank name, or what its rating type is missing.
    pub invalid: Signal<Option<String>>,
    pub is_valid: Signal<bool>,
}

impl AddEditBoatState {
    pub fn new() -> AddEditBoatState {
        let add_boat = RwSignal::new(Boat::default());
        let invalid = Signal::derive(move || {
            add_boat.with(|b| {
                if b.name.trim().is_empty() {
                    Some("A boat needs a name".to_string())
                } else {
                    b.missing_rating()
                }
            })
        });
        AddEditBoatState {
            add_boat,
            add_skipper: RwSignal::new(None),
            invalid,
            is_valid: Signal::derive(move || invalid.with(Option::is_none)),
        }
    }

    pub fn set(&self, boat: Boat, skipper: Option<Person>) {
        self.add_boat.set(boat);
        self.add_skipper.set(skipper);
    }

    pub fn clear(&self) {
        self.set(Boat::default(), None);
    }

    pub fn set_name(&self, name: String) {
        self.add_boat.update(|b| b.name = name);
    }

    pub fn set_skipper(&self, skipper: Option<Person>) {
        self.add_boat
            .update(|b| b.skipper_id = skipper.as_ref().map(|s| s.id));
        self.add_skipper.set(skipper);
    }

    pub fn set_sail_number(&self, sail: String) {
        self.add_boat.update(|b| b.sail_number = sail);
    }

    pub fn set_boat_type(&self, boat_type: String) {
        self.add_boat.update(|b| b.boat_type = boat_type);
    }

    pub fn set_rating_type(&self, rating_type: RatingType, phrf_rating: i32) {
        self.add_boat.update(|b| {
            b.rating_type = rating_type;
            b.phrf_rating = if rating_type.is_phrf() {
                Some(phrf_rating)
            } else {
                None
            };
        });
    }

    pub fn clear_phrf_rating(&self) {
        self.add_boat.update(|b| b.phrf_rating = None);
    }

    pub fn add_orc_certificate(&self, cert: OrcCertificate) {
        self.add_boat.update(|b| {
            b.orc_certs.retain(|c| c.ref_no != cert.ref_no);
            b.orc_certs.push(cert);
        });
    }

    pub fn delete_orc_certificate(&self, cert: OrcCertificate) {
        self.add_boat
            .update(|b| b.orc_certs.retain(|c| c.ref_no != cert.ref_no));
    }
}

impl Default for AddEditBoatState {
    fn default() -> Self {
        AddEditBoatState::new()
    }
}

#[component]
pub fn AddEditBoatModalButton(
    style: RgButtonStyle,
    #[prop(into)] button_label: String,
    #[prop(optional, into)] custom_classes: Cls,
    #[prop(optional, into)] open_action: Option<Cb<()>>,
) -> impl IntoView {
    view! {
        <RgModalButton id=ADD_EDIT_BOAT_MODAL_ID style=style custom_classes=custom_classes button_label=button_label open_action=open_action.unwrap_or_else(|| Cb::new(|()| {}))/>
    }
}

#[component]
pub fn AddEditBoatModal(
    #[prop(into)] people: Signal<Vec<Person>>,
    #[prop(into)] modal_title: TextProp,
    state: AddEditBoatState,
    /// `saveEditedBoat`: the Boats screen upserts the boat (`POST boat`) and reloads the list.
    #[prop(into)]
    save: Cb<Boat>,
) -> impl IntoView {
    let alerts = use_alerts();
    let name = Signal::derive(move || state.add_boat.with(|b| b.name.clone()));
    let sail = Signal::derive(move || state.add_boat.with(|b| b.sail_number.clone()));
    let boat_type = Signal::derive(move || state.add_boat.with(|b| b.boat_type.clone()));
    let editing = Signal::derive(move || state.add_boat.with(|b| b.id != 0));
    view! {
        <RgModalBody
            id=ADD_EDIT_BOAT_MODAL_ID
            modal_title=modal_title
            footer=Box::new(move || view! {
                <RgModalFooterRow>
                    <RgButton label="Clear" style=RgButtonStyle::PrimaryOutline disabled=editing click=move |()| state.clear()/>
                    <RgButton
                        label="Save"
                        style=RgButtonStyle::Success
                        disabled=Signal::derive(move || !state.is_valid.get())
                        closes_modal=true
                        click=move |()| {
                            let boat = state.add_boat.get_untracked();
                            let message = if boat.id != 0 { format!("{} updated!", boat.name) } else { format!("{} added!", boat.name) };
                            save.run(boat);
                            alerts.show_alert(message);
                        }
                    />
                </RgModalFooterRow>
            }.into_any())
        >
            <RgForm>
                <fieldset>
                    <p><RgInput label="Boat Name" value=name place_holder=true listener=move |v: String| state.set_name(v)/></p>
                    <div class="mb-4"><RgSkipperDropdown people=people person=state.add_skipper handler={move |p: Option<Person>| state.set_skipper(p)}/></div>
                    <p><RgInput label="Sail Number" value=sail place_holder=true listener=move |v: String| state.set_sail_number(v)/></p>
                    <p><RgInput label="Boat Type" value=boat_type place_holder=true listener=move |v: String| state.set_boat_type(v)/></p>
                    <div class="mb-4">
                        <RatingSelectionsBoat
                            boat=state.add_boat
                            on_orc=move |(a, cert): (Action, OrcCertificate)| match a {
                                Action::Add => state.add_orc_certificate(cert),
                                Action::Delete => state.delete_orc_certificate(cert),
                            }
                            type_change=move |(t, r): (RatingType, i32)| state.set_rating_type(t, r)
                            phrf_clear=move |()| state.clear_phrf_rating()
                        />
                    </div>
                    {move || state.invalid.get().map(|message| view! { <p class="text-danger-fg">{message}</p> })}
                </fieldset>
            </RgForm>
        </RgModalBody>
    }
}

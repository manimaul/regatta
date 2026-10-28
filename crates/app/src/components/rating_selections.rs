//! The rating type picker with its PHRF number and ORC certificate fields.
//!
//! Edit mode on the boat form adds and removes certificates; select mode on the result form picks one of the
//! boat's certificates.

use crate::app_style;
use crate::cb::Cb;
use crate::components::error_display::ErrorDisplay;
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_dropdown::RgDropdown;
use crate::components::rg_form::{RgInputWithButton, RgNumberInput};
use crate::components::rg_spinner::RgSpinner;
use crate::utils::async_state::{call, Async};
use crate::utils::date_util::display;
use leptos::either::{Either, EitherOf3, EitherOf4};
use leptos::prelude::*;
use regatta_common::{find_preferred, Boat, OrcCertificate, RatingType, RATING_DEFAULT};

/// What an `on_orc` callback is asked to do with a certificate.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Action {
    Add,
    Delete,
}

/// The boat form's variant: the boat's rating type, PHRF rating and certificates.
#[component]
pub fn RatingSelectionsBoat(
    #[prop(optional)] select_mode: bool,
    #[prop(into)] boat: Signal<Boat>,
    #[prop(optional, into)] on_orc: Option<Cb<(Action, OrcCertificate)>>,
    #[prop(into)] type_change: Cb<(RatingType, i32)>,
    /// The PHRF box was emptied.
    #[prop(optional, into)]
    phrf_clear: Option<Cb<()>>,
) -> impl IntoView {
    let boat_type = Signal::derive(move || boat.with(|b| b.rating_type));
    let phrf_rating = Signal::derive(move || boat.with(|b| b.phrf_rating));
    let orc_certificate = Signal::derive(move || {
        if select_mode {
            boat.with(|b| find_preferred(&b.orc_certs).cloned())
        } else {
            None
        }
    });
    let certs = Signal::derive(move || boat.with(|b| b.orc_certs.clone()));
    view! {
        <RatingSelections
            select_mode=select_mode
            boat_type=boat_type
            phrf_rating=phrf_rating
            orc_certificate=orc_certificate
            certs=certs
            on_orc=on_orc.unwrap_or_else(|| Cb::new(|_: (Action, OrcCertificate)| {}))
            type_change=type_change
            phrf_clear=phrf_clear.unwrap_or_else(|| Cb::new(|()| {}))
        />
    }
}

/// The rating type dropdown with the PHRF rating box and the ORC certificate fields the type needs.
///
/// `type_change` gets the new type with the PHRF rating. In `select_mode` the ORC types are offered only when `certs`
/// is not empty; `hide_cruising` leaves out the cruising types.
#[component]
pub fn RatingSelections(
    #[prop(optional)] select_mode: bool,
    #[prop(into)] boat_type: Signal<RatingType>,
    #[prop(into)] phrf_rating: Signal<Option<i32>>,
    #[prop(optional)] hide_rating_selector: bool,
    #[prop(optional)] hide_cruising: bool,
    #[prop(optional, into)] orc_certificate: Signal<Option<OrcCertificate>>,
    #[prop(optional, into)] certs: Signal<Vec<OrcCertificate>>,
    #[prop(optional, into)] on_orc: Option<Cb<(Action, OrcCertificate)>>,
    #[prop(into)] type_change: Cb<(RatingType, i32)>,
    /// The PHRF box was emptied; without it an empty box keeps the last rating.
    #[prop(optional, into)]
    phrf_clear: Option<Cb<()>>,
) -> impl IntoView {
    let orc = Cb::new(move |(action, cert): (Action, OrcCertificate)| {
        if let Some(cb) = &on_orc {
            cb.run((action, cert));
        }
    });
    let types = Signal::derive(move || {
        RatingType::ENTRIES
            .into_iter()
            .filter(|t| {
                if t.is_orc() {
                    certs.with(|c| !c.is_empty()) || !select_mode
                } else {
                    !hide_cruising || *t == RatingType::Phrf
                }
            })
            .collect::<Vec<_>>()
    });
    let phrf_value = Signal::derive(move || phrf_rating.get().map(f64::from));
    let phrf_input = move || {
        view! {
            <p>
                <RgNumberInput label="PHRF Rating" value=phrf_value listener=move |n: f64| type_change.run((boat_type.get_untracked(), n as i32)) on_clear=phrf_clear.unwrap_or_else(|| Cb::new(|()| {}))/>
            </p>
        }
    };
    let orc_info = move || view! { <div class="mb-4"><OrcInfo select_mode=select_mode selected=orc_certificate certs=certs on_orc=orc/></div> };
    view! {
        {(!hide_rating_selector).then(|| view! {
            <div class="mb-4">
                <label class=app_style::MARGIN_END><b>"Rating Type"</b></label>
                <RgDropdown
                    items=types
                    selected_item=boat_type
                    name=|t: &RatingType| t.label().to_string()
                    handler=move |t: RatingType| type_change.run((t, phrf_rating.get_untracked().unwrap_or(RATING_DEFAULT)))
                />
            </div>
        })}
        {move || match boat_type.get() {
            RatingType::Orc => EitherOf4::A(orc_info()),
            RatingType::OrcPhrf => EitherOf4::B(view! { {phrf_input()}{orc_info()} }),
            RatingType::Phrf => EitherOf4::C(phrf_input()),
            _ => EitherOf4::D(()),
        }}
    }
}

/// The ORC certificate fields: a dropdown over `certs` in `select_mode`, else the list with remove and add.
#[component]
pub fn OrcInfo(
    select_mode: bool,
    #[prop(optional, into)] selected: Signal<Option<OrcCertificate>>,
    #[prop(optional, into)] certs: Signal<Vec<OrcCertificate>>,
    #[prop(into)] on_orc: Cb<(Action, OrcCertificate)>,
) -> impl IntoView {
    if select_mode {
        let selected_or_first = Signal::derive(move || {
            selected
                .get()
                .or_else(|| certs.with(|c| c.first().cloned()))
                .unwrap_or_default()
        });
        Either::Left(view! {
            <b>"ORC Certificate"</b>
            {move || if certs.with(|c| c.is_empty()) {
                Either::Left(view! { <p>"Something went wrong - no certificates available"</p> })
            } else {
                Either::Right(view! {
                    <RgDropdown
                        items=certs
                        selected_item=selected_or_first
                        name=|c: &OrcCertificate| c.label()
                        handler=move |c: OrcCertificate| on_orc.run((Action::Add, c))
                    />
                })
            }}
        })
    } else {
        Either::Right(view! { <OrcViewAndAdd certs=certs on_orc=on_orc/> })
    }
}

/// The ORC certificate lookup state: the reference number being typed and the lookup's outcome.
#[derive(Clone, Copy)]
pub struct OrcState {
    pub ref_number: RwSignal<String>,
    pub cert: RwSignal<Async<OrcCertificate>>,
    pub ready_to_add: Signal<bool>,
}

impl OrcState {
    pub fn new() -> OrcState {
        let ref_number = RwSignal::new(String::new());
        OrcState {
            ref_number,
            cert: RwSignal::new(Async::Uninitialized),
            ready_to_add: Signal::derive(move || !ref_number.with(|r| r.trim().is_empty())),
        }
    }

    /// Looks up the typed reference number on data.orc.org and adds the certificate found.
    ///
    /// The server makes the lookup ([`crate::server_fns::orc_cert`]), then `on_orc` gets `(Add, cert)`; a missing
    /// certificate or a failure becomes the error state. The reference number field clears either way.
    pub fn confirm_ref(&self, on_orc: Cb<(Action, OrcCertificate)>) {
        let (cert, ref_number) = (self.cert, self.ref_number);
        let ref_no = ref_number.get_untracked().trim().to_string();
        cert.set(Async::Loading(None));
        call(crate::server_fns::orc_cert(ref_no.clone()), move |result| {
            ref_number.set(String::new());
            match result {
                Ok(Some(certificate)) => {
                    on_orc.run((Action::Add, certificate.clone()));
                    cert.set(Async::Complete(certificate));
                }
                Ok(None) => cert.set(Async::error(format!(
                    "No active ORC certificate with reference number {ref_no}"
                ))),
                Err(message) => cert.set(Async::error(message)),
            }
        });
    }

    pub fn reload(&self) {
        self.cert.set(Async::Uninitialized);
    }
}

impl Default for OrcState {
    fn default() -> Self {
        OrcState::new()
    }
}

/// The certificates in `certs`, each with a remove button, and the reference number field to add one.
#[component]
pub fn OrcViewAndAdd(
    #[prop(optional, into)] certs: Signal<Vec<OrcCertificate>>,
    #[prop(into)] on_orc: Cb<(Action, OrcCertificate)>,
) -> impl IntoView {
    let state = OrcState::new();
    view! {
        <b>"ORC Certificates"</b>
        {move || certs.get().into_iter().map(|c| view! { <OrcDisplay certificate=c on_orc=on_orc/> }).collect_view()}
        {move || match state.cert.get() {
            Async::Complete(_) | Async::Uninitialized => EitherOf3::A(view! { <OrcFetch on_orc=on_orc state=state/> }),
            Async::Error { message, .. } => EitherOf3::B(view! { <ErrorDisplay message=message handler=move |()| state.reload()/> }),
            Async::Loading(_) => EitherOf3::C(view! { <RgSpinner/> }),
        }}
    }
}

/// The ORC reference number field with its "Add Cert" button; `state` defaults to a fresh lookup.
#[component]
pub fn OrcFetch(
    #[prop(into)] on_orc: Cb<(Action, OrcCertificate)>,
    #[prop(optional)] state: Option<OrcState>,
) -> impl IntoView {
    let state = state.unwrap_or_default();
    view! {
        <RgInputWithButton
            label="ORC Reference Number"
            btn_label="Add Cert"
            value=state.ref_number
            btn_disabled=Signal::derive(move || !state.ready_to_add.get())
            listener=move |(change, clicked): (String, bool)| {
                if clicked {
                    state.confirm_ref(on_orc);
                } else {
                    state.ref_number.set(change);
                }
            }
        />
    }
}

/// One certificate's details with a remove button that sends `(Delete, certificate)` to `on_orc`.
#[component]
pub fn OrcDisplay(
    certificate: OrcCertificate,
    #[prop(into)] on_orc: Cb<(Action, OrcCertificate)>,
) -> impl IntoView {
    let remove = certificate.clone();
    view! {
        <p>
            {format!("Reference Number: {} ", certificate.ref_no)}
            <RgButton label="Remove" style=RgButtonStyle::SecondaryOutline click=move |()| on_orc.run((Action::Delete, remove.clone()))/>
            <br/>
            {format!("{} {}", certificate.yacht_name, certificate.sail_no)}
            <br/>
            {format!("Certificate Number: {}", certificate.cert_no)}
            <br/>
            {format!("Issue Date: {}", display(certificate.issue_date))}
            <br/>
            {format!("Type: {} Division: {}", certificate.c_type, certificate.division)}
        </p>
    }
}

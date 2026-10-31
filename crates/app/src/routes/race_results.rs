//! The race results screens: the results list ([`RaceResults`]), the results editor ([`RaceResultsEdit`]) with its
//! Add Result and Config modals, and the shared [`AddResult`] form, also used by the RC finish line.
//!
//! The placement helpers ([`rating_of`], [`available_classes`], [`available_brackets`]) decide which class and
//! bracket a boat's result is entered in.

use crate::app_style;
use crate::cb::Cb;
use crate::components::modal::{ModalClose, RgModalBody, RgModalButton, RgModalFooterRow};
use crate::components::race_config_form::RaceConfigForm;
use crate::components::rated_label::RatedLabel;
use crate::components::rating_selections::{Action, RatingSelections};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_dropdown::{RgDropdown, RgDropdownNone};
use crate::components::rg_race_year_selector::use_race_year;
use crate::components::rg_race_year_selector::RgRaceYearSelector;
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgTh, RgThead, RgTr};
use crate::components::time_row::TimeRow;
use crate::components::Cls;
use crate::routes::async_view;
use crate::server_fns;
use crate::state::alerts::use_alerts;
use crate::state::login::use_login;
use crate::utils::async_state::{call, Loaded};
use crate::utils::date_util::{display, display_opt, now, year_opt};
use crate::utils::race_config::{with_class_start, with_orc_option, with_phrf_b_factor};
use crate::utils::race_report::finish_code_text;
use jiff::Timestamp;
use leptos::either::Either;
use leptos::prelude::*;
use leptos_router::hooks::use_navigate;
use leptos_router::NavigateOptions;
use regatta_common::{
    find_preferred, Boat, BoatSkipper, Bracket, ClassSchedule, FinishCode, Orc3Band, Orc5Band,
    OrcCertificate, OrcScoringOption, Race, RaceClass, RaceResult, RaceResultFull, RaceSchedule,
    RaceScheduleResults, RatingType, Series, RATING_DEFAULT,
};

pub const RACE_RESULT_ADD_EDIT_MODAL_ID: &str = "race-result-add-edit";
pub const RACE_CONFIG_MODAL_ID: &str = "race-config";

/// The results editor state: the result being added or edited, and the race day config draft.
///
/// The race's schedule and results ([`crate::server_fns::schedule_results`]) and the boats
/// ([`crate::server_fns::all_boats`]) are resources; the available boats, classes and brackets follow
/// [`available_classes`], [`available_brackets`] and [`rating_of`].
#[derive(Clone, Copy)]
pub struct RaceResultAddState {
    pub race_id: i64,
    pub race_result: RwSignal<RaceResultFull>,
    pub results: Loaded<RaceScheduleResults>,
    pub boat_skippers: Loaded<Vec<BoatSkipper>>,
    /// The boats without a result in this race.
    pub boats: Signal<Vec<BoatSkipper>>,
    /// The highest HOC position recorded (at least 1).
    pub max_hoc: Signal<i32>,
    pub allow_boat_change: RwSignal<bool>,
    pub classes: RwSignal<Vec<RaceClass>>,
    pub brackets: RwSignal<Vec<Bracket>>,
    pub orc_certs: RwSignal<Vec<OrcCertificate>>,
    pub selected_class: RwSignal<Option<RaceClass>>,
    pub selected_class_start: RwSignal<Option<Timestamp>>,
    pub selected_bracket: RwSignal<Option<Bracket>>,
    /// The Config modal's draft (R4a, `orc_series_standings.md`), `None` while closed.
    pub config_draft: RwSignal<Option<RaceSchedule>>,
    /// The server's reason for rejecting the last config save.
    pub config_error: RwSignal<Option<String>>,
    /// Bumped on open so the form re-keys.
    pub config_version: RwSignal<u32>,
    /// Why the result cannot be saved: no class, no bracket in the class for the rating, a rating
    /// the type needs, or a timed finish without a time.
    pub invalid: Signal<Option<String>>,
    pub is_valid: Signal<bool>,
}

/// Returns the PHRF-equivalent rating a result or boat is placed in a bracket by.
///
/// That is the PHRF rating, else the certificate's virtual PHRF (an ORC boat, also in a PHRF class), else the
/// default rating.
pub fn rating_of(phrf_rating: Option<i32>, orc_certificate: Option<&OrcCertificate>) -> i32 {
    phrf_rating
        .or_else(|| orc_certificate.map(OrcCertificate::virtual_phrf))
        .unwrap_or(RATING_DEFAULT)
}

/// Returns the PHRF-equivalent rating of `boat` (see [`rating_of`]); the default rating for `None`.
pub fn boat_rating(boat: Option<&Boat>) -> i32 {
    rating_of(
        boat.and_then(|b| b.phrf_rating),
        boat.and_then(|b| find_preferred(&b.orc_certs)),
    )
}

fn in_bracket(bracket: &Bracket, rating: i32) -> bool {
    let rating = rating as f32;
    rating <= bracket.max_rating && rating >= bracket.min_rating
}

/// Returns the class's brackets whose rating range holds `rating`.
pub fn available_brackets(
    schedule: Option<&RaceSchedule>,
    class_id: Option<i64>,
    rating: i32,
) -> Vec<Bracket> {
    schedule
        .and_then(|s| {
            s.schedule
                .iter()
                .find(|cs| Some(cs.race_class.id) == class_id)
        })
        .map(|cs| {
            cs.brackets
                .iter()
                .filter(|b| in_bracket(b, rating))
                .cloned()
                .collect()
        })
        .unwrap_or_default()
}

/// Returns the race's classes a boat can enter: an ORC class only for a boat with a certificate.
///
/// A class without a bracket for the rating is listed; [`RaceResultAddState::invalid`] says so.
pub fn available_classes(boat: Option<&Boat>, schedule: Option<&RaceSchedule>) -> Vec<RaceClass> {
    let can_orc = boat.is_some_and(|b| !b.orc_certs.is_empty());
    schedule
        .map(|s| {
            s.schedule
                .iter()
                .filter(|cs| cs.race_class.rating_type != RatingType::Orc || can_orc)
                .map(|cs| cs.race_class.clone())
                .collect()
        })
        .unwrap_or_default()
}

/// Returns why `result` cannot be saved in `class`, or `None` if it can.
pub fn result_invalid(
    result: &RaceResultFull,
    class: Option<&RaceClass>,
    brackets: &[Bracket],
) -> Option<String> {
    if !result.boat.boat.as_ref().is_some_and(|b| b.id != 0) {
        return None;
    }
    let Some(class) = class else {
        return Some("Select a class".to_string());
    };
    // an ORC-only boat is placed by its certificate's virtual PHRF, in a PHRF class too
    let orc_only = result
        .boat
        .boat
        .as_ref()
        .is_some_and(|b| b.phrf_rating.is_none())
        && result.orc_certificate.is_some();
    if result.rating_type.is_phrf() && result.phrf_rating.is_none() && !orc_only {
        return Some(format!(
            "A {} result needs a PHRF rating",
            result.rating_type.label()
        ));
    }
    if brackets.is_empty() {
        let rating = rating_of(result.phrf_rating, result.orc_certificate.as_ref());
        let kind = if result.phrf_rating.is_none() && orc_only {
            "virtual PHRF"
        } else {
            "PHRF"
        };
        return Some(format!(
            "{} has no bracket for a {kind} rating of {rating}",
            class.name
        ));
    }
    if result.rating_type.is_phrf() && result.phrf_rating.is_none() {
        return Some(format!(
            "A {} result needs a PHRF rating",
            result.rating_type.label()
        ));
    }
    if result.rating_type.is_orc() && result.orc_certificate.is_none() {
        return Some(format!(
            "An {} result needs an ORC certificate",
            result.rating_type.label()
        ));
    }
    if result.finish_code == FinishCode::Time && result.finish.is_none() {
        return Some("A timed finish needs a finish time".to_string());
    }
    None
}

fn class_start(schedule: Option<&RaceSchedule>, class_id: Option<i64>) -> Option<Timestamp> {
    schedule
        .and_then(|s| {
            s.schedule
                .iter()
                .find(|cs| Some(cs.race_class.id) == class_id)
        })
        .and_then(ClassSchedule::race_start)
}

/// Converts a full result to the [`RaceResult`] that [`crate::server_fns::post_result`] takes.
pub fn as_sparse(result: &RaceResultFull) -> RaceResult {
    RaceResult {
        id: result.id,
        race_id: result.race_id,
        boat_id: result.boat.boat.as_ref().map(|b| b.id).unwrap_or(0),
        finish: result.finish,
        phrf_rating: result.phrf_rating,
        orc_ref: result.orc_certificate.as_ref().map(|c| c.ref_no.clone()),
        hoc_position: result.hoc_position,
        bracket_id: result.bracket_id,
        race_class_id: result.race_class_id,
        penalty: result.penalty,
        rating_type: result.rating_type,
        finish_code: result.finish_code,
    }
}

/// Returns the highest HOC position among `results`, at least 1.
pub fn find_max_hoc(results: Option<&[RaceResultFull]>) -> i32 {
    results
        .and_then(|r| r.iter().map(|x| x.hoc_position.unwrap_or(1)).max())
        .unwrap_or(1)
}

impl RaceResultAddState {
    pub fn new(race_id: i64) -> RaceResultAddState {
        let race_result = RwSignal::new(RaceResultFull {
            race_id,
            ..RaceResultFull::default()
        });
        let selected_class = RwSignal::new(None::<RaceClass>);
        let selected_bracket = RwSignal::new(None::<Bracket>);
        let brackets = RwSignal::new(Vec::<Bracket>::new());
        let invalid = Signal::derive(move || {
            race_result.with(|r| {
                selected_class.with(|c| brackets.with(|b| result_invalid(r, c.as_ref(), b)))
            })
        });
        let results: Loaded<RaceScheduleResults> = Loaded::new(move || async move {
            server_fns::schedule_results(race_id)
                .await?
                .ok_or_else(|| ServerFnError::ServerError(format!("No race with id {race_id}")))
        });
        let boat_skippers: Loaded<Vec<BoatSkipper>> = Loaded::new(server_fns::all_boats);
        let boats = Signal::derive(move || {
            let results = results.get();
            let skippers = boat_skippers.get();
            let taken: Vec<Option<i64>> = results
                .value()
                .map(|r| {
                    r.results
                        .iter()
                        .map(|x| x.boat.boat.as_ref().map(|b| b.id))
                        .collect()
                })
                .unwrap_or_default();
            skippers
                .value()
                .map(|bs| {
                    bs.iter()
                        .filter(|bs| !taken.contains(&bs.boat.as_ref().map(|b| b.id)))
                        .cloned()
                        .collect()
                })
                .unwrap_or_default()
        });
        let max_hoc = Signal::derive(move || {
            results
                .with_value(|r| find_max_hoc(Some(&r.results)))
                .unwrap_or(1)
        });
        RaceResultAddState {
            race_id,
            race_result,
            results,
            boat_skippers,
            boats,
            max_hoc,
            allow_boat_change: RwSignal::new(true),
            classes: RwSignal::new(Vec::new()),
            brackets,
            orc_certs: RwSignal::new(Vec::new()),
            selected_class,
            selected_class_start: RwSignal::new(None),
            selected_bracket,
            config_draft: RwSignal::new(None),
            config_error: RwSignal::new(None),
            config_version: RwSignal::new(0),
            invalid,
            // valid: a boat, a class, a bracket, the rating the type needs, and a time for a timed finish
            is_valid: Signal::derive(move || {
                invalid.with(Option::is_none)
                    && race_result.with(|r| {
                        r.boat.boat.as_ref().is_some_and(|b| b.id != 0)
                            && r.race_class_id != 0
                            && r.bracket_id != 0
                    })
                    && selected_class.with(|c| c.is_some())
                    && selected_bracket.with(|b| b.is_some())
            }),
        }
    }

    /// Reloads the schedule with its results, and the boats.
    pub fn reload(&self) {
        self.results.reload();
        self.boat_skippers.reload();
    }

    /// Returns the loaded race schedule, untracked.
    pub fn schedule(&self) -> Option<RaceSchedule> {
        self.results.value().map(|r| r.race_schedule)
    }

    /// Returns a fresh timed result, finishing at the race end (now when the race has none).
    fn fresh_result(&self) -> RaceResultFull {
        let end = self.schedule().as_ref().and_then(RaceSchedule::end_time);
        RaceResultFull {
            race_id: self.race_id,
            finish: Some(end.unwrap_or_else(now)),
            finish_code: FinishCode::Time,
            ..RaceResultFull::default()
        }
    }

    /// Starts a fresh timed result, finishing at the race end.
    pub fn clear_result_for_boat_selection(&self) {
        self.race_result.set(self.fresh_result());
        self.allow_boat_change.set(true);
        self.selected_class.set(None);
        self.selected_bracket.set(None);
        self.selected_class_start.set(None);
        self.classes.set(Vec::new());
        self.brackets.set(Vec::new());
    }

    /// Loads `result` into the editor.
    ///
    /// The editor gets the class and bracket its rating places it in, its boat's certificates (plus the one on the
    /// result), and the preferred certificate when the result has none.
    pub fn focus_result_for_edit(&self, result: &RaceResultFull) {
        let schedule = self.schedule();
        let rating = rating_of(result.phrf_rating, result.orc_certificate.as_ref());
        let classes = available_classes(result.boat.boat.as_ref(), schedule.as_ref());
        let selected_class = classes
            .iter()
            .find(|c| c.id == result.race_class_id)
            .cloned();
        let start = class_start(schedule.as_ref(), selected_class.as_ref().map(|c| c.id));
        let brackets = available_brackets(
            schedule.as_ref(),
            selected_class.as_ref().map(|c| c.id),
            rating,
        );
        let selected_bracket = brackets.iter().find(|b| b.id == result.bracket_id).cloned();
        let mut certs: Vec<OrcCertificate> = result
            .boat
            .boat
            .as_ref()
            .map(|b| b.orc_certs.clone())
            .unwrap_or_default();
        if let Some(c) = &result.orc_certificate {
            if !certs.iter().any(|x| x.ref_no == c.ref_no) {
                certs.push(c.clone());
            }
        }
        let mut focused = result.clone();
        if focused.orc_certificate.is_none() && !certs.is_empty() {
            focused.orc_certificate = find_preferred(&certs).cloned();
        }
        self.race_result.set(focused);
        self.allow_boat_change.set(false);
        self.classes.set(classes);
        self.brackets.set(brackets);
        self.selected_class.set(selected_class);
        self.selected_class_start.set(start);
        self.selected_bracket.set(selected_bracket);
        self.orc_certs.set(certs);
    }

    /// Selects the boat of the result being entered.
    ///
    /// The boat's rating picks the class (its own rating type, else a compatible one), the class start, the first
    /// fitting bracket and the preferred certificate. The finish fields already entered are kept.
    pub fn select_boat(&self, boat: Option<BoatSkipper>) {
        let schedule = self.schedule();
        let b = boat.as_ref().and_then(|bs| bs.boat.as_ref());
        let rating_type = b.map(|b| b.rating_type).unwrap_or(RatingType::Phrf);
        let rating = boat_rating(b);
        let classes = available_classes(b, schedule.as_ref());
        let select_class = classes
            .iter()
            .find(|c| c.rating_type == rating_type)
            .or_else(|| {
                classes
                    .iter()
                    .find(|c| c.rating_type.is_compatible(rating_type))
            })
            .or_else(|| classes.first())
            .cloned();
        let start = class_start(schedule.as_ref(), select_class.as_ref().map(|c| c.id));
        let brackets = available_brackets(
            schedule.as_ref(),
            select_class.as_ref().map(|c| c.id),
            rating,
        );
        let select_bracket = brackets.first().cloned();
        let certs = b.map(|b| b.orc_certs.clone()).unwrap_or_default();
        self.race_result.update(|r| {
            r.race_id = self.race_id;
            r.boat = boat.clone().unwrap_or_default();
            r.phrf_rating = b.and_then(|b| b.phrf_rating);
            r.orc_certificate = find_preferred(&certs).cloned();
            r.rating_type = rating_type;
            r.race_class_id = select_class.as_ref().map(|c| c.id).unwrap_or(0);
            r.bracket_id = select_bracket.as_ref().map(|b| b.id).unwrap_or(0);
        });
        self.classes.set(classes);
        self.brackets.set(brackets);
        self.orc_certs.set(certs);
        self.selected_class.set(select_class);
        self.selected_class_start.set(start);
        self.selected_bracket.set(select_bracket);
    }

    /// Selects the class of the result being entered, with its start and the first bracket for the rating.
    ///
    /// An ORC class for a boat without a certificate is scored PHRF.
    pub fn select_class(&self, class: RaceClass) {
        let schedule = self.schedule();
        let (rating, boat, can_orc) = self.race_result.with_untracked(|r| {
            (
                rating_of(r.phrf_rating, r.orc_certificate.as_ref()),
                r.boat.boat.clone(),
                r.boat
                    .boat
                    .as_ref()
                    .is_some_and(|b| !b.orc_certs.is_empty()),
            )
        });
        let classes = available_classes(boat.as_ref(), schedule.as_ref());
        let brackets = available_brackets(schedule.as_ref(), Some(class.id), rating);
        let start = class_start(schedule.as_ref(), Some(class.id));
        let rating_type = if class.rating_type.is_orc() && !can_orc {
            RatingType::Phrf
        } else {
            class.rating_type
        };
        let select_bracket = brackets.first().cloned();
        self.race_result.update(|r| {
            r.race_class_id = class.id;
            r.bracket_id = select_bracket.as_ref().map(|b| b.id).unwrap_or(0);
            r.rating_type = rating_type;
        });
        self.classes.set(classes);
        self.brackets.set(brackets);
        self.selected_class.set(Some(class));
        self.selected_class_start.set(start);
        self.selected_bracket.set(select_bracket);
    }

    /// Refreshes the selected class's brackets for the current rating.
    ///
    /// Keeps the selected bracket while it still fits, else selects the first that does, else none.
    fn refresh_brackets(&self) {
        let schedule = self.schedule();
        let class_id = self
            .selected_class
            .with_untracked(|c| c.as_ref().map(|c| c.id));
        let rating = self
            .race_result
            .with_untracked(|r| rating_of(r.phrf_rating, r.orc_certificate.as_ref()));
        let brackets = available_brackets(schedule.as_ref(), class_id, rating);
        let selected = self
            .selected_bracket
            .with_untracked(|s| {
                s.as_ref()
                    .and_then(|s| brackets.iter().find(|b| b.id == s.id))
            })
            .or_else(|| brackets.first())
            .cloned();
        self.race_result
            .update(|r| r.bracket_id = selected.as_ref().map(|b| b.id).unwrap_or(0));
        self.brackets.set(brackets);
        self.selected_bracket.set(selected);
    }

    pub fn select_bracket(&self, bracket: Bracket) {
        self.race_result.update(|r| r.bracket_id = bracket.id);
        self.selected_bracket.set(Some(bracket));
    }

    pub fn set_finish(&self, code: FinishCode, finish: Option<Timestamp>, hoc: Option<i32>) {
        self.race_result.update(|r| {
            r.finish_code = code;
            r.finish = finish;
            r.hoc_position = hoc;
            if !code.is_finisher() {
                r.penalty = None;
            }
        });
    }

    pub fn penalty(&self, penalty: Option<i32>) {
        self.race_result
            .update(|r| r.penalty = penalty.filter(|p| *p > 0));
    }

    pub fn select_orc(&self, action: Action, cert: OrcCertificate) {
        self.race_result.update(|r| {
            r.orc_certificate = match action {
                Action::Add => Some(cert),
                Action::Delete => None,
            }
        });
        self.refresh_brackets();
    }

    pub fn select_rating(&self, rating_type: RatingType, phrf_rating: i32) {
        self.race_result.update(|r| {
            r.rating_type = rating_type;
            r.phrf_rating = if rating_type.is_phrf() {
                Some(phrf_rating)
            } else {
                None
            };
        });
        self.refresh_brackets();
    }

    /// Clears the PHRF rating after the PHRF box was emptied.
    pub fn clear_phrf_rating(&self) {
        self.race_result.update(|r| r.phrf_rating = None);
        self.refresh_brackets();
    }

    /// Saves the result being entered through [`crate::server_fns::post_result`].
    ///
    /// On success it runs `on_success`, starts a fresh result and reloads; otherwise the server's text (the
    /// `validate_result` message) replaces the list.
    pub fn post_result(&self, on_success: impl FnOnce() + 'static) {
        if !self.is_valid.get_untracked() {
            self.results
                .fail(crate::utils::async_state::DEFAULT_ERROR_MESSAGE);
            return;
        }
        let this = *self;
        let sparse = self.race_result.with_untracked(as_sparse);
        call(server_fns::post_result(sparse), move |r| match r {
            Ok(_) => {
                on_success();
                this.race_result.set(this.fresh_result());
                this.results.reload();
            }
            Err(message) => this.results.fail(message),
        });
    }

    /// Deletes the result `id`, then reloads.
    pub fn delete_result(&self, id: i64) {
        let results = self.results;
        call(server_fns::delete_result(id), move |r| results.after(r));
    }

    pub fn open_config(&self) {
        self.config_draft.set(self.schedule());
        self.config_error.set(None);
        self.config_version.update(|v| *v += 1);
    }

    pub fn close_config(&self) {
        self.config_draft.set(None);
        self.config_error.set(None);
    }

    /// Saves the Config modal's draft through [`crate::server_fns::post_race_schedule`].
    ///
    /// On success `on_saved` closes the modal and the page reloads; a rejection (the `validate_race` text) lands in
    /// `config_error` and keeps the modal open.
    pub fn save_config(&self, on_saved: impl FnOnce() + 'static) {
        let Some(draft) = self.config_draft.get_untracked() else {
            return;
        };
        let this = *self;
        call(server_fns::post_race_schedule(draft), move |r| match r {
            Ok(_) => {
                on_saved();
                this.close_config();
                this.reload();
            }
            Err(message) => this.config_error.set(Some(if message.is_empty() {
                "Could not save".to_string()
            } else {
                message
            })),
        });
    }

    pub fn config_class_start(&self, cs: ClassSchedule, start: Timestamp) {
        self.config_draft.update(|d| {
            if let Some(s) = d {
                *s = with_class_start(s, &cs, start);
            }
        });
    }

    pub fn config_phrf_b_factor(&self, cf: Option<i32>) {
        self.config_draft.update(|d| {
            if let Some(s) = d {
                *s = with_phrf_b_factor(s, cf);
            }
        });
    }

    pub fn config_orc_option(&self, option: OrcScoringOption, b3: Orc3Band, b5: Orc5Band) {
        self.config_draft.update(|d| {
            if let Some(s) = d {
                *s = with_orc_option(s, option, b3, b5);
            }
        });
    }
}

/// The results editor of `race_id` (admin): the results, the Add Result modal and the Config modal.
#[component]
pub fn RaceResultsEdit(race_id: Option<i64>) -> impl IntoView {
    let state = RaceResultAddState::new(race_id.unwrap_or(0));
    let alerts = use_alerts();
    let navigate = use_navigate();
    async_view(
        state.results.signal(),
        move || state.reload(),
        move |results: RaceScheduleResults| {
            let navigate = navigate.clone();
            let has_results = !results.results.is_empty();
            let schedule = results.race_schedule.clone();
            let rows = results
            .results
            .clone()
            .into_iter()
            .map(|result| {
                let cls = schedule.schedule.iter().find(|s| s.race_class.id == result.race_class_id);
                let class_name = cls.map(|c| c.race_class.name.clone()).unwrap_or_default();
                let bracket = cls.and_then(|c| c.brackets.iter().find(|b| b.id == result.bracket_id)).map(|b| b.label()).unwrap_or_default();
                let boat_name = result.boat.boat.as_ref().map(|b| b.name.clone()).unwrap_or_default();
                let confirm_name = boat_name.clone();
                let delete_id = result.id;
                let finish = result.finish.map(display).unwrap_or_else(|| finish_code_text(result.finish_code, result.hoc_position));
                let penalty = result.penalty.map(|p| p.to_string()).unwrap_or_else(|| "-".to_string());
                let orc_ref = result.orc_certificate.as_ref().map(|c| c.ref_no.clone());
                let orc_url = result.orc_certificate.as_ref().and_then(|c| c.certificate_url());
                let rating_type = result.rating_type;
                let phrf_rating = result.phrf_rating;
                let edit = result;
                view! {
                    <RgTr>
                        <RgTd>{boat_name}</RgTd>
                        <RgTd>{class_name}</RgTd>
                        <RgTd><RatedLabel rating_type=rating_type phrf_rating=phrf_rating orc_ref=orc_ref orc_url=orc_url/></RgTd>
                        <RgTd>{bracket}</RgTd>
                        <RgTd>{finish}</RgTd>
                        <RgTd>{penalty}</RgTd>
                        <RgTd>
                            <RgModalButton id=RACE_RESULT_ADD_EDIT_MODAL_ID style=RgButtonStyle::PrimaryOutline button_label="Edit" open_action=move |()| state.focus_result_for_edit(&edit)/>
                            <RgButton
                                label="Delete"
                                style=RgButtonStyle::Danger
                                custom_classes=app_style::MARGIN_START
                                click=move |()| alerts.confirm("Are you sure?", Some(format!("Delete {confirm_name}'s result?")), move |yes: bool| if yes { state.delete_result(delete_id) })
                            />
                        </RgTd>
                    </RgTr>
                }
            })
            .collect_view();
            view! {
                <h1>{format!("{} - {} - Results Editor", year_opt(results.race_schedule.start_time()), results.race_schedule.race.name)}</h1>

                // the page actions side by side under the title, like the races page
                <div class="flex flex-wrap items-end gap-4">
                    {has_results.then(|| view! {
                        <RgButton label="Viewer" custom_classes=app_style::MARGIN_BOT click=move |()| navigate(&format!("/races/results/view/{}", race_id.unwrap_or(0)), NavigateOptions::default())/>
                    })}
                    <RgModalButton id=RACE_RESULT_ADD_EDIT_MODAL_ID button_label="Add Result" custom_classes=app_style::MARGIN_BOT open_action=move |()| state.clear_result_for_boat_selection()/>
                    <RgModalButton id=RACE_CONFIG_MODAL_ID style=RgButtonStyle::PrimaryOutline button_label="Config" custom_classes=app_style::MARGIN_BOT open_action=move |()| state.open_config()/>
                </div>

                <RgModalBody
                    id=RACE_RESULT_ADD_EDIT_MODAL_ID
                    modal_title="Race Result"
                    footer=Box::new(move || view! {
                        <RgModalFooterRow>
                            <RgButton label="Cancel" style=RgButtonStyle::PrimaryOutline closes_modal=true click=|()| {}/>
                            <RgButton label="Save" style=RgButtonStyle::Success disabled=Signal::derive(move || !state.is_valid.get()) closes_modal=true click=move |()| state.post_result(|| {})/>
                        </RgModalFooterRow>
                    }.into_any())
                >
                    <AddResult state=state/>
                </RgModalBody>
                <RaceConfigModal state=state/>

                <RgTable>
                    <RgThead>
                        <RgTr>
                            <RgTh>"Boat Name"</RgTh>
                            <RgTh>"Class"</RgTh>
                            <RgTh>"Rating"</RgTh>
                            <RgTh>"Bracket"</RgTh>
                            <RgTh>"Finish Time"</RgTh>
                            <RgTh>"Penalty"</RgTh>
                            <RgTh>"Action"</RgTh>
                        </RgTr>
                    </RgThead>
                    <RgTbody>
                        {rows}
                    </RgTbody>
                </RgTable>
            }
        },
    )
}

/// The Config modal: the race day config (class starts, PHRF B factor, ORC option and wind band) as a draft.
///
/// The draft is saved whole (R4a, `docs/explore_implemented/orc_series_standings.md`). A rejected save keeps the
/// modal open and shows the server's reason.
#[component]
fn RaceConfigModal(state: RaceResultAddState) -> impl IntoView {
    // the form is rebuilt when a draft opens or closes, not on an edit to it
    let has_draft = Memo::new(move |_| state.config_draft.with(Option::is_some));
    view! {
        <RgModalBody
            id=RACE_CONFIG_MODAL_ID
            modal_title="Race Config"
            footer=Box::new(move || {
                let close = use_context::<ModalClose>();
                view! {
                    <RgModalFooterRow>
                        <RgButton label="Cancel" style=RgButtonStyle::PrimaryOutline closes_modal=true click=move |()| state.close_config()/>
                        <RgButton
                            label="Save"
                            style=RgButtonStyle::Success
                            disabled=Signal::derive(move || state.config_draft.with(|d| d.is_none()))
                            click=move |()| { let close = close; state.save_config(move || if let Some(c) = close { c.run(()) }) }
                        />
                    </RgModalFooterRow>
                }.into_any()
            })
        >
            {move || {
                // re-keyed by config_version: a fresh form per open
                let _version = state.config_version.get();
                has_draft.get().then(|| {
                    let draft = Signal::derive(move || state.config_draft.get().unwrap_or_default());
                    view! {
                        <RaceConfigForm
                            schedule=draft
                            on_class_start=move |(cs, t): (ClassSchedule, Timestamp)| state.config_class_start(cs, t)
                            on_phrf_b_factor={move |cf: Option<i32>| state.config_phrf_b_factor(cf)}
                            on_orc_option=move |(o, b3, b5): (OrcScoringOption, Orc3Band, Orc5Band)| state.config_orc_option(o, b3, b5)
                        />
                    }
                })
            }}
            {move || state.config_error.get().map(|message| view! { <p class="text-danger-fg">{message}</p> })}
        </RgModalBody>
    }
}

/// The result form: boat, finish time, rating, class and bracket.
#[component]
pub fn AddResult(state: RaceResultAddState) -> impl IntoView {
    let result = state.race_result;
    let boat = Signal::derive(move || result.with(|r| Some(r.boat.clone())));
    let only_boat = Signal::derive(move || vec![result.with(|r| r.boat.clone())]);
    let selected_boat = Signal::derive(move || result.with(|r| r.boat.clone()));
    let finish = Signal::derive(move || result.with(|r| r.finish));
    let finish_code = Signal::derive(move || result.with(|r| r.finish_code));
    let hoc = Signal::derive(move || result.with(|r| r.hoc_position));
    let penalty = Signal::derive(move || result.with(|r| r.penalty));
    let rating_type = Signal::derive(move || result.with(|r| r.rating_type));
    let phrf_rating = Signal::derive(move || result.with(|r| r.phrf_rating));
    let orc_certificate = Signal::derive(move || result.with(|r| r.orc_certificate.clone()));
    let cruising = result.with_untracked(|r| r.rating_type.is_cruising());
    view! {
        <div>
            <span><b>"Boat"</b></span>
            {move || if state.allow_boat_change.get() {
                Either::Left(view! { <RgBoatDropdown boats=state.boats selected_boat=boat handler={move |b: Option<BoatSkipper>| state.select_boat(b)}/> })
            } else {
                Either::Right(view! { <RgBoatDropdownFixed boats=only_boat selected_boat=selected_boat/> })
            }}

            <br/>
            <span><b>"Finish Time"</b></span>
            <TimeRow
                max_hoc=state.max_hoc.get_untracked()
                finish=finish
                finish_code=finish_code
                show_hoc_option=cruising
                hoc_position=hoc
                penalty=penalty
                start=state.selected_class_start
                on_finish={move |(code, time, hoc): (FinishCode, Option<Timestamp>, Option<i32>)| state.set_finish(code, time, hoc)}
                on_penalty={move |p: Option<i32>| state.penalty(p)}
            />

            <br/>
            <RatingSelections
                select_mode=true
                boat_type=rating_type
                phrf_rating=phrf_rating
                orc_certificate=orc_certificate
                hide_rating_selector=cruising
                hide_cruising=true
                certs=state.orc_certs
                on_orc=move |(a, c): (Action, OrcCertificate)| state.select_orc(a, c)
                type_change=move |(t, r): (RatingType, i32)| state.select_rating(t, r)
                phrf_clear=move |()| state.clear_phrf_rating()
            />

            {move || state.selected_class.get().map(|selected| view! {
                <span><b>"Class"</b></span>
                <RgDropdown items=state.classes selected_item=selected name=|c: &RaceClass| c.name.clone() handler=move |c: RaceClass| state.select_class(c)/>
            })}

            {move || state.selected_bracket.get().map(|selected| view! {
                <br/>
                <span><b>"Bracket"</b></span>
                <RgDropdown items=state.brackets selected_item=selected name=|b: &Bracket| b.label() handler=move |b: Bracket| state.select_bracket(b)/>
            })}

            {move || state.invalid.get().map(|message| view! { <p class="text-danger-fg">{message}</p> })}
        </div>
    }
}

/// A filterable boat picker with a "None" entry.
#[component]
pub fn RgBoatDropdown(
    #[prop(into)] boats: Signal<Vec<BoatSkipper>>,
    #[prop(into)] selected_boat: Signal<Option<BoatSkipper>>,
    #[prop(into)] handler: Cb<Option<BoatSkipper>>,
) -> impl IntoView {
    view! {
        <RgDropdownNone
            items=boats
            selected_item=selected_boat
            name=|b: &BoatSkipper| b.drop_label()
            show_filter=true
            shor_name=crate::components::rg_dropdown::Namer::from(|b: &BoatSkipper| b.short_label())
            handler=handler
        />
    }
}

/// The fixed boat of a result being edited, shown as a dropdown whose selection is ignored.
#[component]
pub fn RgBoatDropdownFixed(
    #[prop(into)] boats: Signal<Vec<BoatSkipper>>,
    #[prop(into)] selected_boat: Signal<BoatSkipper>,
) -> impl IntoView {
    view! { <RgDropdown items=boats selected_item=selected_boat name=|b: &BoatSkipper| b.short_label() handler=|_: BoatSkipper| {}/> }
}

/// The races of a year grouped by series, in the order of each series' first race.
pub type SeriesRaces = Vec<(Series, Vec<RaceSchedule>)>;

/// Loads the races of `year` sorted by start and grouped by series; empty for `None`.
///
/// A race without a series is its own group, named after the race. Groups keep first-seen order.
///
/// # Errors
///
/// Returns the `Err` of [`crate::server_fns::all_races`].
pub async fn races_by_series(year: Option<i32>) -> Result<SeriesRaces, ServerFnError> {
    let Some(year) = year else {
        return Ok(Vec::new());
    };
    let mut races = server_fns::all_races(year).await?;
    races.sort_by_key(|r| r.start_time());
    let mut groups: SeriesRaces = Vec::new();
    for race in races {
        let series = race.series.clone().unwrap_or_else(|| Series {
            name: race.race.name.clone(),
            ..Series::default()
        });
        match groups.iter_mut().find(|(s, _)| *s == series) {
            Some((_, list)) => list.push(race),
            None => groups.push((series, vec![race])),
        }
    }
    Ok(groups)
}

/// The results list state: the year's races by series, and whether the Action column shows.
#[derive(Clone, Copy)]
pub struct ResultsState {
    pub logged_in: Signal<bool>,
    pub races: Loaded<SeriesRaces>,
}

impl ResultsState {
    pub fn new() -> ResultsState {
        let login = use_login();
        let year = use_race_year().year;
        ResultsState {
            logged_in: Signal::derive(move || login.logged_in()),
            races: Loaded::with_source(move || year.get(), races_by_series),
        }
    }

    pub fn reload(&self) {
        self.races.reload();
    }

    /// The list follows the shared year (the resource's source), so the pick itself reloads it.
    pub fn select_year(&self, _year: Option<i32>) {}
}

impl Default for ResultsState {
    fn default() -> Self {
        ResultsState::new()
    }
}

/// Returns the total number of results recorded across `schedules`.
pub fn result_count(schedules: &[RaceSchedule]) -> i64 {
    schedules.iter().map(|r| r.result_count).sum()
}

#[component]
pub fn RaceResults() -> impl IntoView {
    let state = ResultsState::new();
    let navigate = use_navigate();
    view! {
        <h1>"Race Results"</h1>
        <b>"Year"</b>
        <RgRaceYearSelector on_year_select={move |y: Option<i32>| state.select_year(y)}/>
        <br/>
        {async_view(state.races.signal(), move || state.reload(), move |groups| {
            let navigate = navigate.clone();
            view! { <RaceResultsTable groups=groups logged_in=state.logged_in navigate=Cb::new(move |path: String| navigate(&path, NavigateOptions::default()))/> }
        })}
    }
}

/// The races list grouped by series, each group with its standings link.
///
/// The Action column shows while `logged_in`; `navigate` gets the path a row's button opens.
#[component]
pub fn RaceResultsTable(
    groups: SeriesRaces,
    #[prop(into)] logged_in: Signal<bool>,
    #[prop(into)] navigate: Cb<String>,
) -> impl IntoView {
    view! {
        <RgTable>
            <RgThead>
                <RgTr>
                    <RgTh>"Name"</RgTh>
                    <RgTh>"Date"</RgTh>
                    <RgTh>"Results"</RgTh>
                    {move || logged_in.get().then(|| view! { <RgTh>"Action"</RgTh> })}
                </RgTr>
            </RgThead>
            {groups.into_iter().map(|(series, race_schedules)| {
                let series_id = series.id;
                let series_name = series.name.clone();
                let standings_name = series.name.clone();
                let count = result_count(&race_schedules);
                view! {
                    <RgTbody>
                        <tr>
                            <RgTd col_span=if logged_in.get_untracked() { 4 } else { 3 }>
                                <h4>{series_name}</h4>
                            </RgTd>
                        </tr>
                        {race_schedules.into_iter().map(|schedule| {
                            let race: Race = schedule.race.clone();
                            let view_id = race.id;
                            let edit_id = race.id;
                            let race_name = schedule.race.name.clone();
                            let start = display_opt(schedule.start_time());
                            let has_results = schedule.result_count > 0;
                            view! {
                                <RgTr>
                                    <RgTd>{race_name}</RgTd>
                                    <RgTd>{start}</RgTd>
                                    <RgTd>
                                        {if has_results {
                                            Either::Left(view! { <RgButton label="View Results" click=move |()| navigate.run(format!("/races/results/view/{view_id}"))/> })
                                        } else {
                                            Either::Right("Results not posted")
                                        }}
                                    </RgTd>
                                    {move || logged_in.get().then(|| view! {
                                        <td><RgButton label="Edit Results" click=move |()| navigate.run(format!("/races/results/{edit_id}"))/></td>
                                    })}
                                </RgTr>
                            }
                        }).collect_view()}
                        {(series_id != 0).then(|| view! {
                            <RgTr>
                                <RgTd col_span=2>{format!("{standings_name} Standings")}</RgTd>
                                <RgTd>
                                    {if count > 0 {
                                        Either::Left(view! {
                                            <RgButton label="View Standings" click=move |()| navigate.run(format!("/series/standings/view/{series_id}/{}", crate::components::rg_race_year_selector::selected_year().unwrap_or(0)))/>
                                        })
                                    } else {
                                        Either::Right("Results not posted")
                                    }}
                                </RgTd>
                                {move || logged_in.get().then(|| view! { <td></td> })}
                            </RgTr>
                        })}
                    </RgTbody>
                }
            }).collect_view()}
        </RgTable>
    }
}

/// A native year `<select>` over `years`; `handler` gets the chosen year.
#[component]
pub fn RgYearSelect(
    #[prop(into)] year: Signal<String>,
    years: Vec<String>,
    #[prop(optional, into)] custom_classes: Cls,
    #[prop(into)] handler: Cb<Option<String>>,
) -> impl IntoView {
    let class = crate::components::join_classes(&["rg-select", custom_classes.as_str()]);
    view! {
        <select class=class on:change=move |ev| handler.run(Some(event_target_value(&ev)))>
            {years.into_iter().map(|y| {
                let selected_year = y.clone();
                let value = y.clone();
                view! { <option value=value selected=move || year.get() == selected_year>{y}</option> }
            }).collect_view()}
        </select>
    }
}

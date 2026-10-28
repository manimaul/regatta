//! = `routes/Rc.kt`: race committee day (config, check-in, finish line). State = `RcViewModel.kt`.

use crate::components::modal::{RgModalBody, RgModalButton};
use crate::components::race_config_form::RaceConfigForm;
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_dropdown::RgDropdown;
use crate::components::rg_grid::{Customizer, RgDiv, RgSide, RgSpace, RgSz};
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgTh, RgThead, RgTr};
use crate::routes::async_view;
use crate::routes::race_results::{AddResult, RaceResultAddState};
use crate::server_fns;
use crate::state::login::use_login;
use crate::utils::async_state::{call, Async, Loaded};
use crate::utils::browser;
use crate::utils::date_util::{current_year, display, now, ts_date_str};
use crate::utils::item_name_contains_filter;
use crate::utils::race_config::{with_class_start, with_orc_option, with_phrf_b_factor};
use crate::utils::race_report::full_finish_text;
use jiff::{SignedDuration, Timestamp};
use leptos::either::{Either, EitherOf3};
use leptos::prelude::*;
use regatta_common::{
    BoatSkipper, ClassSchedule, FinishCode, Orc3Band, Orc5Band, OrcScoringOption, RaceResultFull,
    RaceSchedule,
};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RcTab {
    #[default]
    RaceConfig,
    Checkin,
    FinishLine,
}

impl RcTab {
    pub const ENTRIES: [RcTab; 3] = [RcTab::RaceConfig, RcTab::Checkin, RcTab::FinishLine];

    pub const fn title(self) -> &'static str {
        match self {
            RcTab::RaceConfig => "Race Config",
            RcTab::Checkin => "Check-In",
            RcTab::FinishLine => "Finish Line",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum SyncState {
    Dirty,
    Working,
    #[default]
    Synced,
}

/// = `CheckIn`: a boat on race day, its check-in state and its result so far.
#[derive(Debug, Clone, PartialEq)]
pub struct CheckIn {
    pub bs: BoatSkipper,
    pub checked_in: bool,
    pub result: Option<RaceResultFull>,
    pub start_time: Option<Timestamp>,
}

/// = `RcState` / `RcViewModel`. The year's races (`GET allRaces`) are a resource, so the server
/// renders the day with its race config; the selected race is the dropdown pick, else the first
/// race starting after yesterday. The boats, their results and the check-ins are loaded in the
/// browser once a race is selected (`refreshResults`), as the Kotlin view model did; check-ins are
/// kept in `localStorage["checkin_<raceId>"]`, not on the server.
#[derive(Clone, Copy)]
pub struct RcState {
    pub races: Loaded<Vec<RaceSchedule>>,
    /// The dropdown pick.
    manual_race: RwSignal<Option<RaceSchedule>>,
    /// The selected race with the config edits not yet (or just) saved.
    edited_race: RwSignal<Option<RaceSchedule>>,
    /// Schedules saved on this page, newer than their copy in `races` (`checkSchedule` replaced
    /// the race in the list).
    saved_races: RwSignal<Vec<RaceSchedule>>,
    pub selected_race: Signal<Option<RaceSchedule>>,
    pub boats: RwSignal<Async<Vec<CheckIn>>>,
    pub add_state: RwSignal<Option<RaceResultAddState>>,
    pub checkin_ids: RwSignal<Vec<i64>>,
    pub tab: RwSignal<RcTab>,
    pub sync_state: RwSignal<SyncState>,
}

/// `RaceSchedule.findClassSchedule(boatSkipper)`: the class of the boat's rating type, else a
/// compatible one.
pub fn find_class_schedule<'a>(
    schedule: &'a RaceSchedule,
    bs: &BoatSkipper,
) -> Option<&'a ClassSchedule> {
    let boat = bs.boat.as_ref()?;
    schedule
        .schedule
        .iter()
        .find(|cs| cs.race_class.rating_type == boat.rating_type)
        .or_else(|| {
            schedule.schedule.iter().find(|cs| {
                (boat.rating_type.is_phrf() && cs.race_class.rating_type.is_phrf())
                    || (boat.rating_type.is_orc() && cs.race_class.rating_type.is_orc())
            })
        })
}

/// `getCheckins`: every boat sorted by name, checked in when its id is in the list or it has a
/// result, with its result and its class start.
pub fn check_ins(
    ids: &[i64],
    boats: &[BoatSkipper],
    results: &[RaceResultFull],
    schedule: Option<&RaceSchedule>,
) -> Vec<CheckIn> {
    let mut sorted: Vec<&BoatSkipper> = boats.iter().collect();
    sorted.sort_by_key(|bs| bs.boat.as_ref().map(|b| b.name.clone()));
    sorted
        .into_iter()
        .map(|bs| {
            let boat_id = bs.boat.as_ref().map(|b| b.id);
            let result = results
                .iter()
                .find(|r| r.boat.boat.as_ref().map(|b| b.id) == boat_id && boat_id.is_some())
                .cloned();
            CheckIn {
                bs: bs.clone(),
                checked_in: boat_id.is_some_and(|id| ids.contains(&id)) || result.is_some(),
                result,
                start_time: schedule
                    .and_then(|s| find_class_schedule(s, bs))
                    .map(|cs| cs.start_date),
            }
        })
        .collect()
}

fn checkin_key(race_id: i64) -> String {
    format!("checkin_{race_id}")
}

impl RcState {
    pub fn new() -> RcState {
        let year: i32 = current_year().parse().unwrap_or(0);
        let races: Loaded<Vec<RaceSchedule>> = Loaded::new(move || server_fns::all_races(year));
        let manual_race = RwSignal::new(None::<RaceSchedule>);
        let edited_race = RwSignal::new(None::<RaceSchedule>);
        let saved_races = RwSignal::new(Vec::<RaceSchedule>::new());
        // `fetchRaces`: the first race starting after yesterday is selected
        let selected_race = Signal::derive(move || {
            if let Some(edited) = edited_race.get() {
                return Some(edited);
            }
            if let Some(manual) = manual_race.get() {
                return Some(manual);
            }
            let yesterday = now()
                .checked_sub(SignedDuration::from_hours(24))
                .unwrap_or_else(|_| now());
            races
                .with_value(|list| {
                    list.iter()
                        .find(|r| r.start_time().is_some_and(|t| yesterday < t))
                        .cloned()
                })
                .flatten()
                .map(|r| {
                    saved_races
                        .with(|saved| saved.iter().find(|s| s.race.id == r.race.id).cloned())
                        .unwrap_or(r)
                })
        });
        RcState {
            races,
            manual_race,
            edited_race,
            saved_races,
            selected_race,
            boats: RwSignal::new(Async::Loading(None)),
            add_state: RwSignal::new(None),
            checkin_ids: RwSignal::new(Vec::new()),
            tab: RwSignal::new(RcTab::RaceConfig),
            sync_state: RwSignal::new(SyncState::Synced),
        }
    }

    /// `reload()`: the races again (the browser then refreshes the boats of the selection).
    pub fn reload(&self) {
        self.edited_race.set(None);
        self.saved_races.set(Vec::new());
        self.races.reload();
    }

    pub fn select_race(&self, race: RaceSchedule) {
        let race = self
            .saved_races
            .with_untracked(|saved| saved.iter().find(|s| s.race.id == race.race.id).cloned())
            .unwrap_or(race);
        self.edited_race.set(None);
        self.manual_race.set(Some(race));
    }

    /// The browser's half of `selectRace`: the race's saved check-ins, a fresh result editor for
    /// it and its boats and results. Runs when the selected race id changes, not on config edits
    /// (those keep the race id, and resetting the sync state here would drop the pending save).
    pub fn race_selected(&self, race: Option<&RaceSchedule>) {
        let Some(race) = race else {
            self.boats.set(Async::Loading(None));
            self.add_state.set(None);
            return;
        };
        let race_id = race.race.id;
        let ids: Vec<i64> = browser::local_storage_get(&checkin_key(race_id))
            .and_then(|json| serde_json::from_str(&json).ok())
            .unwrap_or_default();
        self.checkin_ids.set(ids);
        self.sync_state.set(SyncState::Synced);
        if self
            .add_state
            .with_untracked(|a| a.map(|a| a.race_id) != Some(race_id))
        {
            self.add_state.set(Some(RaceResultAddState::new(race_id)));
        }
        self.refresh_results();
    }

    /// `refreshResults`: the race's results and every boat, combined into the check-in rows.
    pub fn refresh_results(&self) {
        let this = *self;
        let Some(race) = self.selected_race.get_untracked() else {
            return;
        };
        let race_id = race.race.id;
        self.boats.set(Async::Loading(None));
        call(
            async move {
                let results = server_fns::schedule_results(race_id)
                    .await?
                    .map(|r| r.results)
                    .unwrap_or_default();
                let boats = server_fns::all_boats().await?;
                Ok((results, boats))
            },
            move |r| match r {
                Ok((results, boats)) => {
                    let ids = this.checkin_ids.get_untracked();
                    let schedule = this.selected_race.get_untracked();
                    this.boats.set(Async::Complete(check_ins(
                        &ids,
                        &boats,
                        &results,
                        schedule.as_ref(),
                    )));
                }
                Err(message) => this.boats.set(Async::error(message)),
            },
        );
    }

    pub fn select_tab(&self, tab: RcTab) {
        self.tab.set(tab);
    }

    /// A config edit: the race is dirty and saves itself 3 s later (`checkSchedule`).
    fn edit_race(&self, f: impl FnOnce(&RaceSchedule) -> RaceSchedule) {
        if let Some(race) = self.selected_race.get_untracked() {
            self.edited_race.set(Some(f(&race)));
            self.sync_state.set(SyncState::Dirty);
            let this = *self;
            browser::set_timeout(move || this.check_schedule(), 3000);
        }
    }

    /// `checkSchedule`: posts a dirty race (`POST raceSchedule`); `Synced` with the stored
    /// schedule on success, `Dirty` again otherwise and retried 3 s later. An edit made while the
    /// post is in flight stays dirty and is posted by its own timeout.
    fn check_schedule(&self) {
        if self.sync_state.get_untracked() != SyncState::Dirty {
            return;
        }
        let Some(race) = self.selected_race.get_untracked() else {
            return;
        };
        let this = *self;
        self.sync_state.set(SyncState::Working);
        call(server_fns::post_race_schedule(race), move |r| match r {
            Ok(saved) => {
                this.saved_races.update(|list| {
                    list.retain(|s| s.race.id != saved.race.id);
                    list.push(saved.clone());
                });
                if this.sync_state.get_untracked() == SyncState::Working {
                    this.edited_race.set(Some(saved));
                    this.sync_state.set(SyncState::Synced);
                }
            }
            Err(_) => {
                this.sync_state.set(SyncState::Dirty);
                browser::set_timeout(move || this.check_schedule(), 3000);
            }
        });
    }

    pub fn class_start(&self, cs: ClassSchedule, start: Timestamp) {
        self.edit_race(|r| with_class_start(r, &cs, start));
    }

    pub fn select_phrf_b_factor(&self, cf: Option<i32>) {
        self.edit_race(|r| with_phrf_b_factor(r, cf));
    }

    pub fn select_orc_option(&self, option: OrcScoringOption, b3: Orc3Band, b5: Orc5Band) {
        self.edit_race(|r| with_orc_option(r, option, b3, b5));
    }

    fn set_checkins(&self, ids: Vec<i64>) {
        if let Some(race) = self.selected_race.get_untracked() {
            if let Ok(json) = serde_json::to_string(&ids) {
                browser::local_storage_set(&checkin_key(race.race.id), &json);
            }
        }
        self.checkin_ids.set(ids.clone());
        let schedule = self.selected_race.get_untracked();
        self.boats.update(|b| {
            if let Async::Complete(list) = b {
                let boats: Vec<BoatSkipper> = list.iter().map(|c| c.bs.clone()).collect();
                let results: Vec<RaceResultFull> =
                    list.iter().filter_map(|c| c.result.clone()).collect();
                *list = check_ins(&ids, &boats, &results, schedule.as_ref());
            }
        });
    }

    /// `checkIn`: remembers the boat for this race (`localStorage`).
    pub fn check_in(&self, bs: BoatSkipper) {
        let Some(id) = bs.boat.as_ref().map(|b| b.id) else {
            return;
        };
        let mut ids = vec![id];
        ids.extend(
            self.checkin_ids
                .get_untracked()
                .into_iter()
                .filter(|x| *x != id),
        );
        self.set_checkins(ids);
    }

    /// `checkOut`.
    pub fn check_out(&self, bs: BoatSkipper) {
        let Some(id) = bs.boat.as_ref().map(|b| b.id) else {
            return;
        };
        let ids = self
            .checkin_ids
            .get_untracked()
            .into_iter()
            .filter(|x| *x != id)
            .collect();
        self.set_checkins(ids);
    }

    /// `focusBoatFinish`: the boat's result for editing, or a new timed finish at now.
    pub fn focus_boat_finish(&self, check_in: &CheckIn) {
        let Some(add) = self.add_state.get_untracked() else {
            return;
        };
        match &check_in.result {
            Some(result) => add.focus_result_for_edit(result),
            None => {
                add.select_boat(Some(check_in.bs.clone()));
                add.allow_boat_change.set(false);
                add.set_finish(FinishCode::Time, Some(now()), None);
            }
        }
    }

    /// `postResult`: through the result editor, then the boats refresh.
    pub fn post_result(&self) {
        let this = *self;
        if let Some(add) = self.add_state.get_untracked() {
            add.post_result(move || this.refresh_results());
        }
    }

    /// `delete(result)`: the boat stays checked in; then the boats refresh.
    pub fn delete(&self, result: RaceResultFull) {
        let this = *self;
        if let Some(id) = result.boat.boat.as_ref().map(|b| b.id) {
            let mut ids = self.checkin_ids.get_untracked();
            if !ids.contains(&id) {
                ids.push(id);
                self.set_checkins(ids);
            }
        }
        call(server_fns::delete_result(result.id), move |r| match r {
            Ok(()) => this.refresh_results(),
            Err(message) => this.boats.set(Async::error(message)),
        });
    }
}

impl Default for RcState {
    fn default() -> Self {
        RcState::new()
    }
}

#[component]
pub fn Rc() -> impl IntoView {
    let login = use_login();
    view! {
        {move || if login.logged_in() {
            Either::Left(view! { <RcLoggedIn/> })
        } else {
            Either::Right(view! { <h1>"Unauthorized - Login Required"</h1> })
        }}
    }
}

#[component]
pub fn RcLoggedIn() -> impl IntoView {
    let state = RcState::new();
    let selected = state.selected_race;
    async_view(
        state.races.signal(),
        move || state.reload(),
        move |races: Vec<RaceSchedule>| {
            // the browser's half of `selectRace`: check-ins, result editor, boats (effects never run on the server)
            let selected_id = Memo::new(move |_| selected.with(|r| r.as_ref().map(|r| r.race.id)));
            Effect::new(move |_| {
                selected_id.track();
                state.race_selected(selected.get_untracked().as_ref());
            });
            let first = races.first().cloned().unwrap_or_default();
            let current = Signal::derive(move || selected.get().unwrap_or_else(|| first.clone()));
            view! {
                // race name and date
                {move || match selected.get().and_then(|r| r.start_time()) {
                    Some(start) => Either::Left(view! { <h1>{format!("RC Race Day - {}", ts_date_str(start))}</h1> }),
                    None => Either::Right(view! { <h1>"RC Race Day"</h1> }),
                }}
                <RgDropdown items=races selected_item=current name=|r: &RaceSchedule| r.race.name.clone() handler=move |r: RaceSchedule| state.select_race(r)/>
                <RgDiv customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::Y), Some(RgSz::S4))>
                    <ul class="rg-pills">
                        {RcTab::ENTRIES.into_iter().map(|tab| view! {
                            <li>
                                <a
                                    href="#"
                                    class="rg-pill"
                                    aria-current=move || (state.tab.get() == tab).then_some("page")
                                    on:click=move |ev| { ev.prevent_default(); state.select_tab(tab); }
                                >
                                    {tab.title()}
                                </a>
                            </li>
                        }).collect_view()}
                    </ul>
                </RgDiv>
                // rebuilt when the race or tab changes, not on a config edit (that would rebuild
                // the config form and drop the focused field)
                {move || selected_id.get().and_then(|_| selected.get_untracked()).map(|selected_race| match state.tab.get() {
                    RcTab::RaceConfig => EitherOf3::A(view! { <RcRaceConfig state=state selected_race=selected_race/> }),
                    RcTab::Checkin => EitherOf3::B(view! { <RcCheckin state=state/> }),
                    RcTab::FinishLine => EitherOf3::C(view! { <RcFinish state=state/> }),
                })}
            }
        },
    )
}

#[component]
pub fn RcRaceConfig(state: RcState, selected_race: RaceSchedule) -> impl IntoView {
    let schedule = Signal::derive(move || {
        state
            .selected_race
            .get()
            .unwrap_or_else(|| selected_race.clone())
    });
    view! {
        {move || match state.sync_state.get() {
            SyncState::Dirty => view! { <p>"Status: Unsaved"</p> },
            SyncState::Working => view! { <p>"Status: Saving..."</p> },
            SyncState::Synced => view! { <p>"Status: Saved"</p> },
        }}
        <RaceConfigForm
            schedule=schedule
            on_class_start=move |(cs, t): (ClassSchedule, Timestamp)| state.class_start(cs, t)
            on_phrf_b_factor={move |cf: Option<i32>| state.select_phrf_b_factor(cf)}
            on_orc_option=move |(o, b3, b5): (OrcScoringOption, Orc3Band, Orc5Band)| state.select_orc_option(o, b3, b5)
        />
    }
}

#[component]
pub fn RcCheckin(state: RcState) -> impl IntoView {
    let filter = RwSignal::new(String::new());
    view! {
        <div class="rg-input-group rg-input-group-sm">
            <RgButton label="X" style=RgButtonStyle::SecondaryOutline click=move |()| filter.set(String::new())/>
            <input type="text" class="rg-input" placeholder="Filter" value=move || filter.get() prop:value=move || filter.get() on:input=move |ev| filter.set(event_target_value(&ev))/>
        </div>
        <br/>
        {async_view(state.boats.into(), move || state.refresh_results(), move |boats: Vec<CheckIn>| view! {
            <RgTable>
                <RgThead>
                    <RgTr>
                        <RgTh>"Boat Name"</RgTh>
                        <RgTh>"Checked In"</RgTh>
                    </RgTr>
                </RgThead>
                <RgTbody>
                    {move || boats.iter().filter(|ea| item_name_contains_filter(&ea.bs.drop_label(), &filter.get())).cloned().map(|ea| {
                        let label = ea.bs.drop_label();
                        let bs_out = ea.bs.clone();
                        let bs_in = ea.bs.clone();
                        let has_result = ea.result.is_some();
                        view! {
                            <RgTr>
                                <RgTd>
                                    {if ea.checked_in { Either::Left(view! { <b>{label}</b> }) } else { Either::Right(label) }}
                                </RgTd>
                                <RgTd>
                                    {if ea.checked_in {
                                        Either::Left(view! { <RgButton label="Checkout" style=RgButtonStyle::Danger disabled=has_result click=move |()| state.check_out(bs_out.clone())/> })
                                    } else {
                                        Either::Right(view! { <RgButton label="Checkin" style=RgButtonStyle::Success click=move |()| state.check_in(bs_in.clone())/> })
                                    }}
                                </RgTd>
                            </RgTr>
                        }
                    }).collect_view()}
                </RgTbody>
            </RgTable>
        })}
    }
}

pub const RC_FINISH_TIME_MODAL_ID: &str = "rc-finish-time";
pub const FINISH_TIME_DELETE_CONFIRM_MODAL_ID: &str = "finish-time-delete-confirm";

#[component]
pub fn RcFinish(state: RcState) -> impl IntoView {
    let confirm_delete_result: RwSignal<Option<CheckIn>> = RwSignal::new(None);
    let title = move || {
        state
            .add_state
            .get()
            .map(|a| a.race_result.with(|r| r.boat.short_label()))
            .unwrap_or_default()
    };
    view! {
        <h4>"Race Finish Line"</h4>

        <RgModalBody
            id=RC_FINISH_TIME_MODAL_ID
            modal_title=title
            footer=Box::new(move || view! {
                <RgButton label="Cancel" style=RgButtonStyle::PrimaryOutline closes_modal=true click=|()| {}/>
                <RgButton
                    label="Save"
                    style=RgButtonStyle::Success
                    disabled=Signal::derive(move || state.add_state.get().map(|a| !a.is_valid.get()).unwrap_or(true))
                    closes_modal=true
                    click=move |()| state.post_result()
                />
            }.into_any())
        >
            {move || state.add_state.get().map(|focus| view! { <AddResult state=focus/> })}
        </RgModalBody>

        {async_view(state.boats.into(), move || state.refresh_results(), move |boats: Vec<CheckIn>| view! {
            <RgTable>
                <RgThead>
                    <RgTr>
                        <RgTh>"Boat Name"</RgTh>
                        <RgTh>"Finish Time"</RgTh>
                        <RgTh>"Action"</RgTh>
                    </RgTr>
                </RgThead>
                <RgTbody>
                    {boats.into_iter().filter(|ea| ea.checked_in).map(|ea| {
                        let txt = ea.result.as_ref().map(|r| full_finish_text(r, ea.start_time));
                        let style = if txt.is_some() { RgButtonStyle::PrimaryOutline } else { RgButtonStyle::SuccessOutline };
                        let focus = ea.clone();
                        let delete = ea.clone();
                        view! {
                            <RgTr>
                                <RgTd>{ea.bs.boat.as_ref().map(|b| b.name.clone()).unwrap_or_default()}</RgTd>
                                <RgTd>
                                    <RgModalButton id=RC_FINISH_TIME_MODAL_ID style=style button_label=txt.clone().unwrap_or_else(|| "Finish".to_string()) open_action=move |()| state.focus_boat_finish(&focus)/>
                                </RgTd>
                                <RgTd>
                                    {ea.result.is_some().then(|| view! {
                                        <RgModalButton id=FINISH_TIME_DELETE_CONFIRM_MODAL_ID style=RgButtonStyle::Danger button_label="Delete" open_action=move |()| confirm_delete_result.set(Some(delete.clone()))/>
                                    })}
                                </RgTd>
                            </RgTr>
                        }
                    }).collect_view()}
                </RgTbody>
            </RgTable>
        })}

        <RgModalBody
            id=FINISH_TIME_DELETE_CONFIRM_MODAL_ID
            modal_title="Are you sure?"
            footer=Box::new(move || view! {
                <RgButton label="Cancel" style=RgButtonStyle::PrimaryOutline closes_modal=true click=|()| {}/>
                <RgButton
                    label="Yes - Delete it!"
                    style=RgButtonStyle::Danger
                    closes_modal=true
                    click=move |()| {
                        if let Some(result) = confirm_delete_result.get_untracked().and_then(|c| c.result) {
                            state.delete(result);
                        }
                    }
                />
            }.into_any())
        >
            {move || confirm_delete_result.get().map(|it| view! {
                <p>{format!("Delete result for {}?", it.bs.boat.as_ref().map(|b| b.name.clone()).unwrap_or_default())}</p>
                <p>{it.result.as_ref().and_then(|r| r.finish).map(display).unwrap_or_else(|| "null".to_string())}</p>
            })}
        </RgModalBody>
    }
}

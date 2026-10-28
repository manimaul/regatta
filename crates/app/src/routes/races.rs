//! The Races screen (admin): the year's race schedules and the add/edit race modal.

use crate::app_style;
use crate::cb::Cb;
use crate::components::modal::{RgModalBody, RgModalButton, RgModalFooterRow};
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_dropdown::{RgClassDropdown, RgSeriesDropdown};
use crate::components::rg_form::{RgForm, RgInput, RgTime};
use crate::components::rg_race_year_selector::{use_race_year, RgRaceYearSelector};
use crate::components::rg_spinner::RgSpinner;
use crate::components::rg_switch::RgSwitches;
use crate::components::rg_table::{RgTable, RgTbody, RgTd, RgTh, RgThead, RgTr};
use crate::routes::async_view;
use crate::server_fns;
use crate::state::alerts::use_alerts;
use crate::utils::async_state::{call, Async, Loaded};
use crate::utils::date_util::{display_opt, now};
use jiff::{SignedDuration, Timestamp};
use leptos::either::Either;
use leptos::prelude::*;
use regatta_common::{
    Bracket, ClassSchedule, Race, RaceClass, RaceClassBrackets, RaceSchedule, Series,
};

pub const RACE_MODAL_ID: &str = "add-edit-race";

/// One class row of the race editor before it is saved.
#[derive(Debug, Clone, PartialEq)]
pub struct ClassScheduleDraft {
    pub key: u32,
    pub race_class: Option<RaceClass>,
    pub brackets: Vec<Bracket>,
    pub start_date: Timestamp,
    pub end_date: Timestamp,
}

impl ClassScheduleDraft {
    pub fn new(key: u32) -> ClassScheduleDraft {
        let start = now();
        ClassScheduleDraft {
            key,
            race_class: None,
            brackets: Vec::new(),
            start_date: start,
            end_date: start
                .checked_add(SignedDuration::from_hours(4))
                .unwrap_or(start),
        }
    }

    pub fn from_schedule(key: u32, cs: &ClassSchedule) -> ClassScheduleDraft {
        ClassScheduleDraft {
            key,
            race_class: Some(cs.race_class.clone()),
            brackets: cs.brackets.clone(),
            start_date: cs.start_date,
            end_date: cs.end_date,
        }
    }
}

/// The race editor state: the race being added or edited; `race_id == 0` is a new race.
///
/// The editor only exists once the modal is opened in the browser, so its loads (the race schedule, the series and
/// the classes) are plain calls into `Async` signals rather than resources.
#[derive(Clone, Copy)]
pub struct RacesEditState {
    pub race_id: i64,
    pub race: RwSignal<Async<RaceSchedule>>,
    pub series: RwSignal<Async<Vec<Series>>>,
    pub classes: RwSignal<Async<Vec<RaceClassBrackets>>>,
    pub drafts: RwSignal<Vec<ClassScheduleDraft>>,
    next_key: RwSignal<u32>,
    /// Runs after a save: the races list shows an alert and reloads.
    on_saved: Cb<RaceSchedule>,
    /// Runs after a delete.
    on_deleted: Cb<RaceSchedule>,
}

impl RacesEditState {
    pub fn new(
        race_id: i64,
        on_saved: impl Into<Cb<RaceSchedule>>,
        on_deleted: impl Into<Cb<RaceSchedule>>,
    ) -> RacesEditState {
        let state = RacesEditState {
            race_id,
            race: RwSignal::new(Async::Loading(None)),
            series: RwSignal::new(Async::Loading(None)),
            classes: RwSignal::new(Async::Loading(None)),
            drafts: RwSignal::new(Vec::new()),
            next_key: RwSignal::new(1),
            on_saved: on_saved.into(),
            on_deleted: on_deleted.into(),
        };
        state.reload();
        state
    }

    /// Loads the race (a default schedule for a new one) with its class times as drafts, the series and the classes.
    pub fn reload(&self) {
        let this = *self;
        this.series.set(Async::Loading(None));
        this.classes.set(Async::Loading(None));
        this.race.set(Async::Loading(None));
        if this.race_id == 0 {
            this.drafts.set(Vec::new());
            this.race.set(Async::Complete(RaceSchedule::default()));
        } else {
            let race_id = this.race_id;
            call(server_fns::race_schedule(race_id), move |r| match r {
                Ok(Some(schedule)) => {
                    let drafts = schedule
                        .schedule
                        .iter()
                        .map(|cs| ClassScheduleDraft::from_schedule(this.take_key(), cs))
                        .collect();
                    this.drafts.set(drafts);
                    this.race.set(Async::Complete(schedule));
                }
                Ok(None) => this
                    .race
                    .set(Async::error(format!("No race with id {race_id}"))),
                Err(message) => this.race.set(Async::error(message)),
            });
        }
        call(server_fns::all_series(), move |r| this.series.set(r.into()));
        call(server_fns::all_classes(), move |r| {
            this.classes.set(r.into())
        });
    }

    fn take_key(&self) -> u32 {
        let key = self.next_key.get_untracked();
        self.next_key.set(key + 1);
        key
    }

    pub fn set_race_name(&self, name: String) {
        self.race.update(|r| {
            if let Async::Complete(s) = r {
                s.race.name = name;
            }
        });
    }

    pub fn set_series(&self, series: Series) {
        self.race.update(|r| {
            if let Async::Complete(s) = r {
                s.race.series_id = Some(series.id);
                s.series = Some(series);
            }
        });
    }

    /// Adds an empty class draft starting from the previous draft's times (classes usually start close together).
    pub fn add_draft(&self) {
        let key = self.take_key();
        self.drafts.update(|d| {
            let mut draft = ClassScheduleDraft::new(key);
            if let Some(previous) = d.last() {
                draft.start_date = previous.start_date;
                draft.end_date = previous.end_date;
            }
            d.push(draft);
        });
    }

    pub fn remove_draft(&self, key: u32) {
        self.drafts.update(|d| d.retain(|x| x.key != key));
    }

    pub fn select_draft_class(&self, key: u32, race_class: Option<RaceClass>) {
        self.update_draft(key, |d| {
            d.race_class = race_class;
            d.brackets.clear();
        });
    }

    pub fn update_draft(&self, key: u32, f: impl FnOnce(&mut ClassScheduleDraft)) {
        self.drafts.update(|d| {
            if let Some(draft) = d.iter_mut().find(|x| x.key == key) {
                f(draft);
            }
        });
    }

    /// Returns the schedule to save, or `None` while a draft is incomplete.
    pub fn drafted_schedule(&self) -> Option<RaceSchedule> {
        let Async::Complete(mut schedule) = self.race.get() else {
            return None;
        };
        let drafts = self.drafts.get();
        let mut classes = Vec::new();
        for d in drafts {
            let race_class = d.race_class?;
            if d.end_date <= d.start_date {
                return None;
            }
            classes.push(ClassSchedule {
                race_class,
                brackets: d.brackets,
                start_date: d.start_date,
                end_date: d.end_date,
            });
        }
        if classes.is_empty() || schedule.race.name.trim().is_empty() {
            return None;
        }
        schedule.schedule = classes;
        Some(schedule)
    }

    /// Saves `schedule` through [`crate::server_fns::post_race_schedule`].
    ///
    /// Runs `on_saved` on success, else shows the server's text (the `validate_race` message) in the alert banner.
    pub fn save(&self, schedule: RaceSchedule) {
        let this = *self;
        let alerts = use_alerts();
        call(
            server_fns::post_race_schedule(schedule.clone()),
            move |r| match r {
                Ok(_) => this.on_saved.run(schedule),
                Err(message) => alerts.show_alert(if message.is_empty() {
                    "Could not save the race".to_string()
                } else {
                    message
                }),
            },
        );
    }

    /// Deletes the race through [`crate::server_fns::delete_race`].
    ///
    /// Runs `on_deleted` on success, else shows an alert (the server refuses a race with results).
    pub fn delete(&self) {
        let Some(schedule) = self.race.with_untracked(|r| r.value().cloned()) else {
            return;
        };
        let this = *self;
        let alerts = use_alerts();
        call(server_fns::delete_race(this.race_id), move |r| match r {
            Ok(()) => this.on_deleted.run(schedule),
            Err(_) => alerts.show_alert(format!("{} could not be deleted.", schedule.race.name)),
        });
    }
}

/// The Races screen state: the races of the selected year, sorted by start, and the editor behind the modal.
#[derive(Clone, Copy)]
pub struct RacesState {
    pub races: Loaded<Vec<RaceSchedule>>,
    pub edit: RwSignal<Option<RacesEditState>>,
}

/// Loads the race schedules of `year` sorted by start time; empty for `None`.
///
/// # Errors
///
/// Returns the `Err` of [`crate::server_fns::all_races`].
pub async fn races_of_year(year: Option<i32>) -> Result<Vec<RaceSchedule>, ServerFnError> {
    let Some(year) = year else {
        return Ok(Vec::new());
    };
    let mut races = server_fns::all_races(year).await?;
    races.sort_by_key(|r| r.start_time());
    Ok(races)
}

impl RacesState {
    pub fn new() -> RacesState {
        let year = use_race_year().year;
        RacesState {
            races: Loaded::with_source(move || year.get(), races_of_year),
            edit: RwSignal::new(None),
        }
    }

    pub fn reload(&self) {
        self.races.reload();
    }

    /// The list follows the shared year (the resource's source), so the pick itself reloads it.
    pub fn select_year(&self, _year: Option<i32>) {}

    pub fn new_race(&self) {
        self.open_editor(0);
    }

    pub fn edit_race(&self, race: &Race) {
        self.open_editor(race.id);
    }

    /// Opens a fresh editor for `race_id` each time the modal opens; saving or deleting shows an alert and reloads
    /// the list.
    fn open_editor(&self, race_id: i64) {
        let alerts = use_alerts();
        let races = self.races;
        self.edit.set(Some(RacesEditState::new(
            race_id,
            move |saved: RaceSchedule| {
                alerts.show_alert(format!(
                    "{} {}!",
                    saved.race.name,
                    if race_id == 0 { "created" } else { "updated" }
                ));
                races.reload();
            },
            move |deleted: RaceSchedule| {
                alerts.show_alert(format!("{} deleted!", deleted.race.name));
                races.reload();
            },
        )));
    }
}

impl Default for RacesState {
    fn default() -> Self {
        RacesState::new()
    }
}

#[component]
pub fn Races() -> impl IntoView {
    let state = RacesState::new();
    view! {
        <h1>"Races"</h1>
        // year picker and the create button side by side under the title, like the other list pages
        <div class="flex flex-wrap items-end gap-4">
            <div>
                <b>"Year"</b>
                <RgRaceYearSelector on_year_select={move |y: Option<i32>| state.select_year(y)}/>
            </div>
            <RgModalButton
                id=RACE_MODAL_ID
                style=RgButtonStyle::SuccessOutline
                custom_classes=app_style::MARGIN_BOT
                button_label="Create Race"
                open_action=move |()| state.new_race()
            />
        </div>
        <AddEditRaceModal edit=state.edit/>
        {async_view(state.races.signal(), move || state.reload(), move |schedules| view! { <RaceList schedules=schedules state=state/> })}
    }
}

#[component]
pub fn RaceList(schedules: Vec<RaceSchedule>, state: RacesState) -> impl IntoView {
    view! {
        <RgTable>
            <RgThead>
                <RgTr>
                    <RgTh>"Series"</RgTh>
                    <RgTh>"Name"</RgTh>
                    <RgTh>"Race start"</RgTh>
                    <RgTh>"Race finish"</RgTh>
                    <RgTh>"Race committee"</RgTh>
                    <RgTh>"Action"</RgTh>
                </RgTr>
            </RgThead>
            <RgTbody>
                {schedules.into_iter().map(|sch| {
                    let race = sch.race.clone();
                    let series_name = sch.series.as_ref().map(|s| s.name.clone()).unwrap_or_else(|| "-".to_string());
                    let race_name = sch.race.name.clone();
                    let start = display_opt(sch.start_time());
                    let end = display_opt(sch.end_time());
                    let rc = sch.rc.as_ref().map(|rc| format!("{} {}", rc.first, rc.last)).unwrap_or_else(|| "-".to_string());
                    view! {
                        <RgTr>
                            <RgTd>{series_name}</RgTd>
                            <RgTd>{race_name}</RgTd>
                            <RgTd>{start}</RgTd>
                            <RgTd>{end}</RgTd>
                            <RgTd>{rc}</RgTd>
                            <RgTd>
                                <RgModalButton
                                    id=RACE_MODAL_ID
                                    style=RgButtonStyle::PrimaryOutline
                                    custom_classes="float-start"
                                    button_label="Edit"
                                    open_action=move |()| state.edit_race(&race)
                                />
                            </RgTd>
                        </RgTr>
                    }
                }).collect_view()}
            </RgTbody>
        </RgTable>
    }
}

/// The add/edit race form: name, series, then the class start times.
///
/// The class rows are edited in place with +/- buttons like the brackets of the Add Race Class modal. The race
/// committee and correction factor are not set here: they are entered on race day
/// from the RC tab or afterwards in the race's config. `edit` is `None` until the modal is first opened.
#[component]
pub fn AddEditRaceModal(#[prop(into)] edit: Signal<Option<RacesEditState>>) -> impl IntoView {
    let alerts = use_alerts();
    let editing = move || edit.get().map(|e| e.race_id != 0).unwrap_or(false);
    // results reference the race (a foreign key), so a race with results cannot be deleted
    let result_count = move || {
        edit.get()
            .and_then(|e| e.race.with(|r| r.value().map(|s| s.result_count)))
            .unwrap_or(0)
    };
    let schedule = move || edit.get().and_then(|e| e.drafted_schedule());
    view! {
        <RgModalBody
            id=RACE_MODAL_ID
            modal_title=move || if editing() { "Edit Race" } else { "Create Race" }
            footer=Box::new(move || view! {
                <RgModalFooterRow>
                    {move || editing().then(|| view! {
                        <RgButton
                            label="Delete"
                            style=RgButtonStyle::Danger
                            disabled=Signal::derive(move || result_count() > 0)
                            closes_modal=true
                            click=move |()| {
                                if let Some(e) = edit.get_untracked() {
                                    let name = e.race.with_untracked(|r| r.value().map(|s| s.race.name.clone())).unwrap_or_else(|| "this race".to_string());
                                    alerts.confirm("Are you sure?", Some(format!("Delete {name}?")), move |yes: bool| if yes { e.delete() });
                                }
                            }
                        />
                    })}
                    <RgButton
                        label="Save"
                        style=RgButtonStyle::Success
                        disabled=Signal::derive(move || schedule().is_none())
                        closes_modal=true
                        click=move |()| {
                            if let (Some(e), Some(s)) = (edit.get_untracked(), schedule()) {
                                e.save(s);
                            }
                        }
                    />
                </RgModalFooterRow>
            }.into_any())
        >
            {move || match edit.get() {
                None => Either::Left(view! { <RgSpinner/> }),
                Some(e) => Either::Right(view! { <RaceFields edit=e/> }),
            }}
        </RgModalBody>
    }
}

#[component]
fn RaceFields(edit: RacesEditState) -> impl IntoView {
    async_view(
        edit.race.into(),
        move || edit.reload(),
        move |schedule: RaceSchedule| {
            let name = schedule.race.name.clone();
            let series = schedule.series.clone();
            let result_count = schedule.result_count;
            view! {
                <RgForm>
                    <fieldset>
                        <p><RgInput label="Race Name" place_holder=true value=name listener=move |v: String| edit.set_race_name(v)/></p>
                        <div class="mb-4">
                            <label class=app_style::MARGIN_END><b>"Series"</b></label>
                            {
                                let series = series.clone();
                                async_view(edit.series.into(), move || edit.reload(), move |all: Vec<Series>| view! {
                                    <RgSeriesDropdown series_list=all series=series.clone() handler=move |s: Series| edit.set_series(s)/>
                                })
                            }
                        </div>
                        {(result_count > 0).then(|| view! {
                            <p class="text-muted">{format!("{result_count} result(s) are recorded for this race, so it cannot be deleted.")}</p>
                        })}
                        <h6>"Race Class Start Times"</h6>
                        <hr/>
                        {async_view(edit.classes.into(), move || edit.reload(), move |classes: Vec<RaceClassBrackets>| {
                            let class_count = classes.len();
                            view! {
                                <For each=move || edit.drafts.get() key=|d| d.key let:draft>
                                    <div class="mb-4"><EditClassSchedule draft_key=draft.key classes=classes.clone() edit=edit/></div>
                                    <hr/>
                                </For>
                                <p>
                                    <RgButton label="+" disabled=Signal::derive(move || edit.drafts.with(|d| d.len() >= class_count)) click=move |()| edit.add_draft()/>
                                </p>
                            }
                        })}
                    </fieldset>
                </RgForm>
            }
        },
    )
}

/// One class row of the race form: remove button and class picker, start and end, then the brackets as switches.
///
/// The picker offers only classes no other draft has taken.
#[component]
fn EditClassSchedule(
    draft_key: u32,
    classes: Vec<RaceClassBrackets>,
    edit: RacesEditState,
) -> impl IntoView {
    let draft = Signal::derive(move || {
        edit.drafts
            .with(|d| d.iter().find(|x| x.key == draft_key).cloned())
            .unwrap_or_else(|| ClassScheduleDraft::new(draft_key))
    });
    let all_classes = classes.clone();
    let available = Signal::derive(move || {
        let taken: Vec<i64> = edit.drafts.with(|d| {
            d.iter()
                .filter(|x| x.key != draft_key)
                .filter_map(|x| x.race_class.as_ref().map(|c| c.id))
                .collect()
        });
        all_classes
            .iter()
            .filter(|c| !taken.contains(&c.race_class.id))
            .cloned()
            .collect::<Vec<_>>()
    });
    let current = Signal::derive(move || draft.with(|d| d.race_class.clone()));
    let start = Signal::derive(move || draft.with(|d| d.start_date));
    let end = Signal::derive(move || draft.with(|d| d.end_date));
    let bracket_classes = classes;
    let brackets = Signal::derive(move || {
        let id = draft.with(|d| d.race_class.as_ref().map(|c| c.id));
        bracket_classes
            .iter()
            .find(|c| Some(c.race_class.id) == id)
            .map(|c| c.brackets.clone())
    });
    view! {
        <div class="flex items-center">
            <RgButton label="-" custom_classes=app_style::MARGIN_END click=move |()| edit.remove_draft(draft_key)/>
            <RgClassDropdown items=available current=current handler={move |c: Option<RaceClassBrackets>| edit.select_draft_class(draft_key, c.map(|c| c.race_class))}/>
        </div>
        <div class=app_style::MARGIN_TOP>
            <RgTime label="Start" show_date=true date=start listener=move |t: Timestamp| edit.update_draft(draft_key, |d| d.start_date = t)/>
        </div>
        <div class=app_style::MARGIN_TOP>
            <RgTime label="End" show_date=true date=end listener=move |t: Timestamp| edit.update_draft(draft_key, |d| d.end_date = t)/>
        </div>
        {move || (end.get() <= start.get()).then(|| view! { <p class="text-danger-fg">"End must be after start"</p> })}
        {move || brackets.get().map(|brackets| view! {
            <p><b>"Brackets"</b></p>
            <RgSwitches
                items=brackets
                label=|b: Bracket| b.label()
                check=move |b: Bracket| draft.with(|d| d.brackets.iter().any(|x| x.id == b.id))
                change=move |(b, on): (Bracket, bool)| edit.update_draft(draft_key, |d| {
                    if on {
                        d.brackets.push(b);
                    } else {
                        d.brackets.retain(|x| x.id != b.id);
                    }
                })
            />
        })}
    }
}

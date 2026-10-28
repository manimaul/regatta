//! The race day config form: one start time per class, the PHRF B factor, and the ORC scoring option with its wind
//! band.
//!
//! Shared by the RC page and the results editor's Config modal (R4a,
//! `docs/explore_implemented/orc_series_standings.md`). The form only reports edits; the callers apply them with the
//! helpers in [`crate::utils::race_config`].

use crate::app_style;
use crate::cb::Cb;
use crate::components::rg_dropdown::RgIdDropdown;
use crate::components::rg_form::{RgInput, RgTime};
use crate::components::rg_grid::{Customizer, RgDiv, RgSide, RgSpace, RgSz};
use crate::utils::race_config::{time_limit, time_limit_text};
use jiff::Timestamp;
use leptos::either::EitherOf3;
use leptos::prelude::*;
use regatta_common::{
    ClassSchedule, Orc3Band, Orc5Band, OrcScoringOption, PhrfBFactor, RaceSchedule,
};

/// The race day config of `schedule`, reporting each edit through its callback.
///
/// `on_class_start` gets a class and its new start, `on_phrf_b_factor` the new B factor (`None` when cleared) and
/// `on_orc_option` the scoring option with both wind bands.
#[component]
pub fn RaceConfigForm(
    #[prop(into)] schedule: Signal<RaceSchedule>,
    #[prop(into)] on_class_start: Cb<(ClassSchedule, Timestamp)>,
    #[prop(into)] on_phrf_b_factor: Cb<Option<i32>>,
    #[prop(into)] on_orc_option: Cb<(OrcScoringOption, Orc3Band, Orc5Band)>,
) -> impl IntoView {
    let b_factor = Signal::derive(move || schedule.with(|s| s.race.phrf_b_factor.to_string()));
    let option = Signal::derive(move || schedule.with(|s| s.race.orc_scoring_option));
    let band3 = Signal::derive(move || schedule.with(|s| s.race.orc3_band));
    let band5 = Signal::derive(move || schedule.with(|s| s.race.orc5_band));
    view! {
        // keyed by class, so an edit updates the start fields in place instead of rebuilding
        // them (a rebuilt input loses focus after every keystroke)
        <For
            each=move || schedule.with(|s| s.schedule.iter().map(|cs| (cs.race_class.id, cs.race_class.name.clone())).collect::<Vec<_>>())
            key=|class| class.clone()
            children=move |(class_id, name)| {
                let class_schedule = move || schedule.with(|s| s.schedule.iter().find(|cs| cs.race_class.id == class_id).cloned());
                let start = Signal::derive(move || class_schedule().map(|cs| cs.start_date).unwrap_or_default());
                let limit = move || class_schedule().as_ref().and_then(time_limit).map(|l| format!("{} time limit", time_limit_text(l)));
                view! {
                    <h6>{name}</h6>
                    <RgDiv customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::B), Some(RgSz::S3))>
                        <RgTime label="Start time" date=start listener=move |t: Timestamp| {
                            if let Some(cs) = schedule.with_untracked(|s| s.schedule.iter().find(|cs| cs.race_class.id == class_id).cloned()) {
                                on_class_start.run((cs, t));
                            }
                        }/>
                        {move || limit().map(|text| view! { <p>{text}</p> })}
                    </RgDiv>
                }
            }
        />

        <RgInput label="PHRF B Factor" value=b_factor listener=move |v: String| on_phrf_b_factor.run(Some(v.parse::<i32>().unwrap_or(0)))/>

        <p>{move || {
            let factor = PhrfBFactor::from_b_factor(schedule.with(|s| s.race.phrf_b_factor));
            format!("{} - {}", factor.label(), factor.desc())
        }}</p>

        <RgDiv customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::T), Some(RgSz::S3))>
            <OrcScoringOptionSelect selected=option selected3_band=band3 selected5_band=band5 on_select=on_orc_option/>
        </RgDiv>
    }
}

/// The ORC scoring option dropdown with the 3-band or 5-band wind band the option needs.
#[component]
pub fn OrcScoringOptionSelect(
    #[prop(into)] selected: Signal<OrcScoringOption>,
    #[prop(into)] selected3_band: Signal<Orc3Band>,
    #[prop(into)] selected5_band: Signal<Orc5Band>,
    #[prop(into)] on_select: Cb<(OrcScoringOption, Orc3Band, Orc5Band)>,
) -> impl IntoView {
    view! {
        <div class="mb-4">
            <label for="orc_option" class=app_style::MARGIN_END><b>"ORC Scoring Option"</b></label>
            <RgIdDropdown
                id="orc_option"
                items=OrcScoringOption::ENTRIES.to_vec()
                selected_item=selected
                name=|o: &OrcScoringOption| o.label().to_string()
                handler=move |o: OrcScoringOption| on_select.run((o, selected3_band.get_untracked(), selected5_band.get_untracked()))
            />
        </div>
        {move || {
            let option = selected.get();
            if option.uses_3_band() {
                EitherOf3::A(view! {
                    <div class="mb-4">
                        <label for="orc_wind" class=app_style::MARGIN_END><b>"ORC Wind / Conditions"</b></label>
                        <RgIdDropdown
                            id="orc_wind"
                            items=Orc3Band::ENTRIES.to_vec()
                            selected_item=selected3_band
                            name=|b: &Orc3Band| format!("{} - {}", b.label(), b.desc())
                            handler=move |b: Orc3Band| on_select.run((selected.get_untracked(), b, selected5_band.get_untracked()))
                        />
                    </div>
                })
            } else if option.uses_5_band() {
                EitherOf3::B(view! {
                    <div class="mb-4">
                        <label for="orc_wind" class=app_style::MARGIN_END><b>"ORC Wind / Conditions"</b></label>
                        <RgIdDropdown
                            id="orc_wind"
                            items=Orc5Band::ENTRIES.to_vec()
                            selected_item=selected5_band
                            name=|b: &Orc5Band| format!("{} - {}", b.label(), b.desc())
                            handler=move |b: Orc5Band| on_select.run((selected.get_untracked(), selected3_band.get_untracked(), b))
                        />
                    </div>
                })
            } else {
                EitherOf3::C(())
            }
        }}
    }
}

//! The series standings screen: one standings table per class, from [`crate::server_fns::series_standings`].

use crate::components::cell_lines::{CellLines, ScoreLines};
use crate::components::rg_table::{
    RgTable, RgTbody, RgTd, RgTdColor, RgTh, RgThead, RgTr, Scope, TableColor,
};
use crate::routes::async_view;
use crate::server_fns;
use crate::utils::async_state::Loaded;
use crate::utils::standings_report::{
    bracket_place_lines, class_place_lines, score_lines, status_text, total_lines,
};
use leptos::either::Either;
use leptos::prelude::*;
use regatta_common::{StandingsBoatSkipper, StandingsClass, StandingsSeries};

const COL1: [&str; 3] = ["Boat", "Skipper", "Rating"];

/// The standings screen of series `id` in `year`; without both it shows the error view.
#[component]
pub fn SeriesStandings(id: Option<i64>, year: Option<i32>) -> impl IntoView {
    // without both the id and the year nothing loads
    let standings: Loaded<StandingsSeries> = Loaded::new(move || async move {
        let (Some(id), Some(year)) = (id, year) else {
            return Err(ServerFnError::ServerError(
                "missing series id or year".to_string(),
            ));
        };
        server_fns::series_standings(id, year)
            .await?
            .ok_or_else(|| {
                ServerFnError::ServerError(format!("No standings for series {id} in {year}"))
            })
    });
    async_view(
        standings.signal(),
        move || standings.reload(),
        move |standings_series: StandingsSeries| {
            view! {
                <h1>{format!("{} Standings {}", standings_series.series.name, standings_series.year)}</h1>
                {standings_series.standings.iter().map(|standings_class| view! {
                    <br/>
                    <h4>{standings_class.race_class.name.clone()}</h4>
                    <StandingsTable standings_series=standings_series.clone() standings_class=standings_class.clone()/>
                }).collect_view()}
            }
        },
    )
}

/// The standings table of one class, one section per bracket; the ORC columns show only in an ORC-rated class.
#[component]
pub fn StandingsTable(
    standings_series: StandingsSeries,
    standings_class: StandingsClass,
) -> impl IntoView {
    let multi_bracket = standings_class.standings.len() > 1;
    // D9 (`test_audit_and_plan.md`): the ORC side shows only in an ORC-rated class, the race view's gate
    let show_orc = standings_class.race_class.rating_type.is_orc();
    let race_count = standings_series.races.len();
    let race_headers = standings_series
        .races
        .iter()
        .map(|race| {
            let name = race.name.clone();
            view! {
                <RgTh scope=Scope::Colgroup>
                    <p>{name}</p>
                    {multi_bracket.then_some("(bracket, class)")}
                </RgTh>
            }
        })
        .collect_view();
    let rows = standings_class
        .standings
        .into_iter()
        .map(|standings_bracket| {
            let bracket_title = format!("{} {}", standings_bracket.bracket.name, standings_bracket.bracket.description.clone().unwrap_or_default());
            let boats = standings_bracket
                .standings
                .into_iter()
                .map(|standings| {
                    let skipper = standings.boat_skipper.skipper.as_ref().map(|s| s.full_name()).unwrap_or_default();
                    let rating = standings.rating_type;
                    let races = standings
                        .race_standings
                        .iter()
                        .map(|race| {
                            let status = status_text(race, rating, show_orc);
                            let lines = score_lines(race, show_orc, multi_bracket, rating);
                            match status {
                                Some(status) => Either::Left(view! { <RgTd><span class="text-muted">{status}</span></RgTd> }),
                                None => Either::Right(view! { <RgTd><ScoreLines lines=lines/></RgTd> }),
                            }
                        })
                        .collect_view();
                    let totals = total_lines(&standings, show_orc, multi_bracket);
                    let bracket_place = multi_bracket.then(|| bracket_place_lines(&standings, show_orc));
                    let class_place = class_place_lines(&standings, show_orc);
                    view! {
                        <RgTr>
                            <RgTd><StandingsBoatLabel card=standings.clone()/></RgTd>
                            <RgTd>{skipper}</RgTd>
                            <RgTd>{rating.label()}</RgTd>
                            {races}
                            <RgTd><ScoreLines lines=totals/></RgTd>
                            {bracket_place.map(|lines| view! { <RgTd><CellLines lines=lines/></RgTd> })}
                            <RgTd><CellLines lines=class_place/></RgTd>
                        </RgTr>
                    }
                })
                .collect_view();
            view! {
                <RgTr classes=TableColor::Light.cls()>
                    <RgTdColor col_span=(COL1.len() + race_count + 3) as i32 color=TableColor::Warning>
                        <h6>{bracket_title}</h6>
                    </RgTdColor>
                </RgTr>
                {boats}
            }
        })
        .collect_view();
    view! {
        <RgTable stripe_column=true color=TableColor::Light>
            <RgThead>
                <RgTr>
                    {COL1.iter().map(|c| view! { <RgTh scope=Scope::Colgroup><p>{*c}</p></RgTh> }).collect_view()}
                    {race_headers}
                    <RgTh scope=Scope::Colgroup>
                        <p>"Total Score"</p>
                        {multi_bracket.then_some("(bracket, class)")}
                    </RgTh>
                    {multi_bracket.then(|| view! { <RgTh scope=Scope::Colgroup><p>"Place In Bracket"</p></RgTh> })}
                    <RgTh scope=Scope::Colgroup><p>"Place In Class"</p></RgTh>
                </RgTr>
            </RgThead>
            <RgTbody>
                {rows}
            </RgTbody>
        </RgTable>
    }
}

/// The boat cell of a standings row: bold name, then type and sail number when present.
#[component]
pub fn StandingsBoatLabel(card: StandingsBoatSkipper) -> impl IntoView {
    let boat = card.boat_skipper.boat;
    view! {
        <span style="font-weight: bold;">{boat.as_ref().map(|b| b.name.clone()).unwrap_or_default()}</span>
        {boat.as_ref().map(|b| b.boat_type.clone()).filter(|t| !t.trim().is_empty()).map(|t| format!(" - {t}"))}
        {boat.as_ref().map(|b| b.sail_number.clone()).filter(|s| !s.trim().is_empty()).map(|s| format!(" ({s})"))}
    }
}

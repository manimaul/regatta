//! = `routes/RaceResultsView.kt`: the public race report.

use crate::app_style;
use crate::components::cell_lines::CellLines;
use crate::components::rated_label::RatedLabel;
use crate::components::rg_button::RgButton;
use crate::components::rg_table::{
    RgTable, RgTbody, RgTd, RgTdColor, RgTh, RgThead, RgTr, Scope, TableColor,
};
use crate::routes::async_view;
use crate::server_fns;
use crate::state::login::use_login;
use crate::utils::async_state::Loaded;
use crate::utils::date_util::{ts_time_str, year_opt};
use crate::utils::race_report::{
    bracket_place_lines, card_finish_text, class_place_lines, cor_time_orc_text,
    cor_time_phrf_text, elapsed_text, single_bracket_place_lines,
};
use leptos::prelude::*;
use leptos_router::hooks::use_navigate;
use leptos_router::NavigateOptions;
use regatta_common::{ClassReportCards, PhrfBFactor, RaceReport, RaceReportCard, RatingType};

#[component]
pub fn RaceResultsView(race_id: Option<i64>) -> impl IntoView {
    // = `RaceResultViewViewModel`: `GET report?raceId=`.
    let report: Loaded<RaceReport> = Loaded::new(move || async move {
        let Some(id) = race_id else {
            return Err(ServerFnError::ServerError("missing race id".to_string()));
        };
        server_fns::report(id)
            .await?
            .ok_or_else(|| ServerFnError::ServerError(format!("No race with id {id}")))
    });
    let login = use_login();
    let navigate = use_navigate();
    async_view(
        report.signal(),
        move || report.reload(),
        move |report: RaceReport| {
            let navigate = navigate.clone();
            view! {
                <h1>{format!("{} - {} - Results", year_opt(report.race_schedule.start_time()), report.race_schedule.race.name)}</h1>
                {move || {
                    let navigate = navigate.clone();
                    login.logged_in().then(|| view! {
                        // the page actions side by side under the title, like the races page and the results editor
                        <div class="flex flex-wrap items-end gap-4">
                            <RgButton label="Editor" custom_classes=app_style::MARGIN_BOT click=move |()| navigate(&format!("/races/results/{}", race_id.unwrap_or(0)), NavigateOptions::default())/>
                        </div>
                    })
                }}
                {report.class_reports.iter().enumerate().map(|(i, class_report_cards)| view! {
                    <RaceResultsClassTable add_divider={i > 0} report=report.clone() class_report_cards=class_report_cards.clone()/>
                }).collect_view()}
            }
        },
    )
}

#[component]
pub fn RaceResultsClassTable(
    add_divider: bool,
    report: RaceReport,
    class_report_cards: ClassReportCards,
) -> impl IntoView {
    let total_bracket_in_class_count = class_report_cards.bracket_report.len();
    let show_orc = class_report_cards.race_class.rating_type.is_orc();
    let orc_or_phrf = class_report_cards.race_class.rating_type.is_orc_or_phrf();
    let headers: &[&str] = if orc_or_phrf {
        &[
            "Boat",
            "Skipper",
            "Rating",
            "Finish",
            "Elapsed Time",
            "Corrected Time",
        ]
    } else {
        &["Boat", "Skipper", "Rating", "Finish", "Elapsed Time"]
    };
    let start = report
        .class_start(class_report_cards.race_class.id)
        .map(ts_time_str)
        .unwrap_or_else(|| "None".to_string());
    let rows = class_report_cards
        .bracket_report
        .clone()
        .into_iter()
        .map(|class_report| {
            let bracket_title = format!("{} {}", class_report.bracket.name, class_report.bracket.description.clone().unwrap_or_default());
            let cards = class_report
                .cards
                .into_iter()
                .map(|card| {
                    let rating = card.rating_type();
                    let skipper = card.skipper.clone();
                    let phrf_rating = card.phrf_rating;
                    let orc_ref = card.result_record.result.orc_ref.clone();
                    let orc_url = card.orc_cert_url();
                    let finish = card_finish_text(&card);
                    let elapsed = elapsed_text(&card);
                    let corrected = orc_or_phrf.then(|| {
                        let phrf = rating.is_phrf().then(|| cor_time_phrf_text(&card));
                        let orc = (show_orc && rating.is_orc()).then(|| cor_time_orc_text(&card));
                        view! {
                            <RgTd>
                                {phrf}
                                {(rating == RatingType::OrcPhrf).then(|| view! { <br/> })}
                                {orc}
                            </RgTd>
                        }
                    });
                    let places = if total_bracket_in_class_count > 1 {
                        let bracket = bracket_place_lines(&card, show_orc);
                        let class = class_place_lines(&card, show_orc);
                        leptos::either::Either::Left(view! {
                            <RgTd><CellLines lines=bracket/></RgTd>
                            <RgTd><CellLines lines=class/></RgTd>
                        })
                    } else {
                        let single = single_bracket_place_lines(&card);
                        leptos::either::Either::Right(view! { <RgTd><CellLines lines=single/></RgTd> })
                    };
                    view! {
                        <RgTr>
                            <RgTd><ReportBoatLabel card=card/></RgTd>
                            <RgTd>{skipper}</RgTd>
                            <RgTd><RatedLabel rating_type=rating phrf_rating=phrf_rating orc_ref=orc_ref orc_url=orc_url/></RgTd>
                            <RgTd>{finish}</RgTd>
                            <RgTd>{elapsed}</RgTd>
                            {corrected}
                            {places}
                        </RgTr>
                    }
                })
                .collect_view();
            view! {
                // (Bootstrap's table-borderless only works on <table>, so it was a no-op on this row)
                <RgTr classes=TableColor::Light.cls()>
                    <RgTdColor col_span=15 color=TableColor::Warning>
                        <h6>{bracket_title}</h6>
                    </RgTdColor>
                </RgTr>
                {cards}
            }
        })
        .collect_view();
    view! {
        {add_divider.then(|| view! { <div style="margin-bottom: 32px;"></div> })}
        <div class="border-2 border-line">
            <h4>{class_report_cards.race_class.label()}</h4>
            <h6>{format!(" Start time - {start}")}</h6>
            <h6>{format!("PHRF B - {} - {}", class_report_cards.phrf_b_factor, PhrfBFactor::from_b_factor(class_report_cards.phrf_b_factor).label())}</h6>
            {show_orc.then(|| view! {
                <h6>{format!("ORC {}{}", class_report_cards.orc_scoring_option.label(), class_report_cards.orc_band_label().map(|l| format!(" - {l}")).unwrap_or_default())}</h6>
            })}
        </div>
        <RgTable stripe_column=true color=TableColor::Light>
            <RgThead>
                <RgTr>
                    {headers.iter().map(|h| view! { <RgTh scope=Scope::Colgroup>{*h}</RgTh> }).collect_view()}
                    {if total_bracket_in_class_count > 1 {
                        leptos::either::Either::Left(view! {
                            <RgTh scope=Scope::Colgroup>"Place In Bracket"</RgTh>
                            <RgTh scope=Scope::Colgroup>"Place In Class"</RgTh>
                        })
                    } else {
                        leptos::either::Either::Right(view! { <RgTh scope=Scope::Colgroup>"Place In Class"</RgTh> })
                    }}
                </RgTr>
            </RgThead>
            <RgTbody>
                {rows}
            </RgTbody>
        </RgTable>
    }
}

/// The boat cell: bold name, then type and sail number when present.
#[component]
pub fn ReportBoatLabel(card: RaceReportCard) -> impl IntoView {
    view! {
        <span style="font-weight: bold;">{card.boat_name.clone()}</span>
        {(!card.boat_type.trim().is_empty()).then(|| format!(" - {}", card.boat_type))}
        {(!card.sail.trim().is_empty()).then(|| format!(" ({})", card.sail))}
    }
}

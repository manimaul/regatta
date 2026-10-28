//! = `TimeRow.kt`: the finish time editor of a result (time, penalty, finish code, HOC place).

use crate::app_style;
use crate::cb::Cb;
use crate::components::finish_code_drop::FinishCodeDrop;
use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_form::RgTime;
use crate::utils::date_util::now;
use jiff::Timestamp;
use leptos::prelude::*;
use regatta_common::{display, FinishCode};

#[component]
pub fn TimeRow(
    max_hoc: i32,
    #[prop(into)] finish: Signal<Option<Timestamp>>,
    #[prop(into)] finish_code: Signal<FinishCode>,
    show_hoc_option: bool,
    #[prop(into)] hoc_position: Signal<Option<i32>>,
    #[prop(into)] penalty: Signal<Option<i32>>,
    #[prop(into)] start: Signal<Option<Timestamp>>,
    #[prop(into)] on_finish: Cb<(FinishCode, Option<Timestamp>, Option<i32>)>,
    #[prop(into)] on_penalty: Cb<Option<i32>>,
) -> impl IntoView {
    let has_finish = Memo::new(move |_| finish.with(Option::is_some));
    let penalty_label = move || format!("Penalty {}", penalty.get().unwrap_or(0));
    let penalty_minus_classes = super::join_classes(&[
        app_style::MARGIN_START,
        app_style::MARGIN_BOT,
        app_style::MARGIN_TOP,
    ]);
    let penalty_classes = super::join_classes(&[app_style::MARGIN_TOP, app_style::MARGIN_BOT]);
    let hoc_minus_classes = super::join_classes(&[app_style::MARGIN_TOP, app_style::MARGIN_START]);
    view! {
        // rebuilt when a finish time is set or cleared, not on an edit to it (a rebuilt field
        // loses focus after every keystroke)
        {move || has_finish.get().then(|| {
            let f = finish.get_untracked().unwrap_or_default();
            view! {
                <RgTime date=Signal::derive(move || finish.get().unwrap_or(f)) show_date=true show_seconds=true listener=move |t: Timestamp| on_finish.run((FinishCode::Time, Some(t), None))/>
                {move || finish.get().zip(start.get()).map(|(f, s)| view! { <p>{format!("Elapsed time: {}", display(f.duration_since(s)))}</p> })}
            }
        })}
        // D4: a scoring penalty only applies to a finishing place, timed or HOC
        {move || finish_code.get().is_finisher().then(|| view! {
            <RgButton label=penalty_label custom_classes=penalty_classes.clone() click=move |()| on_penalty.run(Some(penalty.get_untracked().map(|p| p + 1).unwrap_or(1)))/>
            {penalty.get().map(|p| view! {
                <RgButton style=RgButtonStyle::Danger label="-" custom_classes=penalty_minus_classes.clone() click=move |()| on_penalty.run(Some(p - 1))/>
            })}
        })}
        <FinishCodeDrop
            selected=finish_code
            hoc_position=hoc_position
            show_hoc=show_hoc_option
            custom_classes=app_style::MARGIN_TOP
            handler=move |code: FinishCode| match code {
                // todo: use race end time
                FinishCode::Time => on_finish.run((FinishCode::Time, Some(finish.get_untracked().unwrap_or_else(now)), None)),
                FinishCode::Ret | FinishCode::Dnf | FinishCode::DnsRc | FinishCode::Nsc | FinishCode::Dns | FinishCode::Dsq => on_finish.run((code, None, None)),
                FinishCode::Hoc => on_finish.run((code, None, Some(max_hoc))),
            }
        />
        {move || hoc_position.get().map(|hoc| view! {
            <RgButton style=RgButtonStyle::Danger label="+" custom_classes=app_style::MARGIN_TOP click=move |()| on_finish.run((FinishCode::Hoc, None, Some(hoc + 1)))/>
            {(hoc > 1).then(|| view! {
                <RgButton style=RgButtonStyle::Danger label="-" custom_classes=hoc_minus_classes.clone() click=move |()| on_finish.run((FinishCode::Hoc, None, Some(hoc - 1)))/>
            })}
        })}
    }
}

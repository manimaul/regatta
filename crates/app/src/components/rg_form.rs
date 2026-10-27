//! = `RgForm.kt` (+ `RgTimeViewModel.kt`, folded into `RgTime`).

use crate::app_style;
use crate::cb::Cb;
use crate::components::rg_button::RgButtonStyle;
use crate::utils::date_util::{double_digit, input_str, instant, local_date_time};
use jiff::civil::DateTime;
use jiff::Timestamp;
use leptos::either::Either;
use leptos::prelude::*;

#[component]
pub fn RgForm(children: Children) -> impl IntoView {
    view! { <form on:submit=|ev| ev.prevent_default()>{children()}</form> }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum NumberType {
    #[default]
    NumberInt,
    NumberFloat,
    NumberDouble,
}

impl NumberType {
    fn format(self, value: f64) -> String {
        match self {
            NumberType::NumberInt => (value as i64).to_string(),
            // Kotlin/JS prints a whole Float/Double without the ".0"
            NumberType::NumberFloat | NumberType::NumberDouble => value.to_string(),
        }
    }

    fn parse(self, text: &str) -> Option<f64> {
        match self {
            NumberType::NumberInt => text.parse::<i64>().ok().map(|v| v as f64),
            NumberType::NumberFloat => text.parse::<f32>().ok().map(f64::from),
            NumberType::NumberDouble => text.parse::<f64>().ok(),
        }
    }
}

/// A numeric text field (`inputmode="numeric"`). Only valid numbers reach `listener`; while the
/// user types, the field keeps the raw text (`locked`) instead of the formatted value.
#[component]
pub fn RgNumberInput(
    #[prop(into)] label: String,
    #[prop(into)] value: Signal<Option<f64>>,
    #[prop(optional)] place_holder: bool,
    #[prop(optional, into)] custom_classes: super::Cls,
    #[prop(optional)] number_type: NumberType,
    #[prop(into)] listener: Cb<f64>,
) -> impl IntoView {
    let locked = RwSignal::new(false);
    let r_number = RwSignal::new(String::new());
    let shown = move || {
        if locked.get() {
            r_number.get()
        } else {
            value
                .get()
                .map(|v| number_type.format(v))
                .unwrap_or_default()
        }
    };
    let id = super::next_input_id();
    let class = super::join_classes(&[custom_classes.as_str(), "rg-input"]);
    let placeholder = place_holder.then(|| label.clone());
    view! {
        <label for=id.clone()><b>{label}</b></label>
        <input
            type="text"
            id=id
            class=class
            placeholder=placeholder
            inputmode="numeric"
            value=shown
            prop:value=shown
            on:change=move |_| locked.set(false)
            on:input=move |ev| {
                let text = event_target_value(&ev);
                locked.set(true);
                r_number.set(text.clone());
                if !text.trim().is_empty() && text != "-" {
                    if let Some(n) = number_type.parse(&text) {
                        listener.run(n);
                    }
                }
            }
        />
    }
}

#[component]
pub fn RgInput(
    #[prop(into)] label: String,
    #[prop(into)] value: Signal<String>,
    #[prop(optional)] place_holder: bool,
    #[prop(optional, into)] custom_classes: super::Cls,
    #[prop(into)] listener: Cb<String>,
) -> impl IntoView {
    let id = super::next_input_id();
    let class = super::join_classes(&[custom_classes.as_str(), "rg-input"]);
    let placeholder = place_holder.then(|| label.clone());
    view! {
        <label for=id.clone() class=app_style::MARGIN_END><b>{label}</b></label>
        <input
            type="text"
            id=id
            class=class
            placeholder=placeholder
            value=move || value.get()
            prop:value=move || value.get()
            on:input=move |ev| listener.run(event_target_value(&ev).trim_start().to_string())
        />
    }
}

/// A text field with a button beside it (the ORC reference lookup). `listener(value, clicked)`:
/// `false` while typing, `true` when the button is pressed.
#[component]
pub fn RgInputWithButton(
    #[prop(optional, into)] label: Option<String>,
    #[prop(into)] value: Signal<String>,
    #[prop(into)] btn_label: String,
    #[prop(optional, into)] disabled: Signal<bool>,
    #[prop(optional, into)] btn_disabled: Signal<bool>,
    #[prop(into)] listener: Cb<(String, bool)>,
) -> impl IntoView {
    let id = super::next_input_id();
    let placeholder = label.clone();
    view! {
        {label.map(|l| view! { <label for=id.clone() class=app_style::MARGIN_END><b>{l}</b></label> })}
        <div class="rg-input-group mb-4">
            <input
                class="rg-input"
                type="text"
                id=id
                placeholder=placeholder
                disabled=move || disabled.get()
                value=move || value.get()
                prop:value=move || value.get()
                on:input=move |ev| listener.run((event_target_value(&ev).trim().to_string(), false))
            />
            <button
                class=RgButtonStyle::PrimaryOutline.classes()
                disabled=move || btn_disabled.get()
                on:click=move |_| listener.run((value.get_untracked().trim().to_string(), true))
            >
                {btn_label}
            </button>
        </div>
    }
}

/// Seconds dropped unless the field shows them (`RgTimeViewModel`).
fn truncate(dt: DateTime, seconds: bool) -> DateTime {
    if seconds {
        dt
    } else {
        dt.with()
            .second(0)
            .subsec_nanosecond(0)
            .build()
            .unwrap_or(dt)
    }
}

/// `setInputDate`: `YYYY-MM-DD` with the time kept.
pub fn with_input_date(local: DateTime, input: &str) -> Option<Timestamp> {
    let parts: Vec<&str> = input.split('-').collect();
    if parts.len() != 3 {
        return None;
    }
    let y: i16 = parts[0].parse().unwrap_or(0);
    let m: i8 = parts[1].parse().ok()?;
    let d: i8 = parts[2].parse().ok()?;
    if !(1..=12).contains(&m) || !(1..=31).contains(&d) || y < 0 {
        return None;
    }
    let dt = DateTime::new(y, m, d, local.hour(), local.minute(), local.second(), 0).ok()?;
    Some(instant(&dt))
}

fn with_field(
    local: DateTime,
    value: &str,
    range: std::ops::RangeInclusive<i8>,
    set: impl Fn(DateTime, i8) -> Option<DateTime>,
) -> Option<Timestamp> {
    let v: i8 = value.parse::<i32>().ok()?.try_into().ok()?;
    if !range.contains(&v) {
        return None;
    }
    set(local, v).map(|dt| instant(&dt))
}

pub fn with_input_hour(local: DateTime, value: &str) -> Option<Timestamp> {
    with_field(local, value, 0..=23, |dt, h| dt.with().hour(h).build().ok())
}

pub fn with_input_minute(local: DateTime, value: &str) -> Option<Timestamp> {
    with_field(local, value, 0..=59, |dt, m| {
        dt.with().minute(m).build().ok()
    })
}

pub fn with_input_second(local: DateTime, value: &str) -> Option<Timestamp> {
    with_field(local, value, 0..=59, |dt, s| {
        dt.with().second(s).build().ok()
    })
}

/// Hour / minute (/ second) number fields in a compact input group, with a date field above when
/// `show_date`. Every valid edit reports the new instant.
#[component]
pub fn RgTime(
    #[prop(optional, into)] label: Option<String>,
    #[prop(into)] date: Signal<Timestamp>,
    #[prop(optional)] show_seconds: bool,
    #[prop(optional)] show_date: bool,
    #[prop(into)] listener: Cb<Timestamp>,
) -> impl IntoView {
    let local = Memo::new(move |_| truncate(local_date_time(date.get()), show_seconds));
    let max_width = if show_date {
        "max-width: 600px;"
    } else {
        "max-width: 400px;"
    };
    let date_label = label.clone();
    let time_label = label.filter(|_| !show_date);
    let report = move |value: Option<Timestamp>| {
        if let Some(v) = value {
            listener.run(v);
        }
    };
    view! {
        {show_date.then(|| view! {
            <div style=max_width class="rg-input-group rg-input-group-sm">
                {date_label.map(|l| view! { <span class="rg-input-addon">{l}</span> })}
                <input
                    type="date"
                    class="rg-input"
                    value=move || input_str(&local.get())
                    prop:value=move || input_str(&local.get())
                    on:input=move |ev| report(with_input_date(local.get_untracked(), &event_target_value(&ev)))
                />
            </div>
        })}
        <div style=max_width class="rg-input-group rg-input-group-sm">
            {time_label.map(|l| view! { <span class="rg-input-addon">{l}</span> })}
            <input
                type="number"
                class="rg-input"
                value=move || double_digit(i32::from(local.get().hour()))
                prop:value=move || double_digit(i32::from(local.get().hour()))
                on:input=move |ev| report(with_input_hour(local.get_untracked(), &event_target_value(&ev)))
            />
            <span class="rg-input-addon">":"</span>
            <input
                type="number"
                class="rg-input"
                value=move || double_digit(i32::from(local.get().minute()))
                prop:value=move || double_digit(i32::from(local.get().minute()))
                on:input=move |ev| report(with_input_minute(local.get_untracked(), &event_target_value(&ev)))
            />
            {show_seconds.then(|| view! {
                <span class="rg-input-addon">":"</span>
                <input
                    type="number"
                    class="rg-input"
                    value=move || double_digit(i32::from(local.get().second()))
                    prop:value=move || double_digit(i32::from(local.get().second()))
                    on:input=move |ev| report(with_input_second(local.get_untracked(), &event_target_value(&ev)))
                />
            })}
        </div>
    }
}

#[component]
pub fn RgCheck(
    #[prop(into)] label: String,
    #[prop(into)] checked: Signal<bool>,
    #[prop(default = true)] bold: bool,
    #[prop(optional, into)] custom_classes: Option<super::Cls>,
    #[prop(into)] listener: Cb<bool>,
) -> impl IntoView {
    let id = super::next_input_id();
    let class = custom_classes
        .map(|c| c.0)
        .unwrap_or_else(|| "rg-check".to_string());
    let text = if bold {
        Either::Left(view! { <b>{label}</b> })
    } else {
        Either::Right(label)
    };
    view! {
        <div class=class>
            <label for=id.clone()>{text}</label>
            <input
                type="checkbox"
                class="rg-checkbox"
                id=id
                checked=move || checked.get()
                prop:checked=move || checked.get()
                on:change=move |ev| listener.run(event_target_checked(&ev))
            />
        </div>
    }
}

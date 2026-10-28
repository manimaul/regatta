//! The labels of the race results tables (finish, elapsed, corrected time) and the lines of the place columns.
//!
//! Pure, covered by `crates/app/tests/race_report_labels_test.rs`.

use crate::utils::date_util::ts_time_str;
use jiff::{SignedDuration, Timestamp};
use regatta_common::{display, FinishCode, RaceReportCard, RaceResultFull, RatingType};

/// Formats a result's finish for the editor tables.
///
/// The local finish time (with `P+n` for a penalty) when the class has a start, else the finish code's text.
pub fn full_finish_text(result: &RaceResultFull, start_time: Option<Timestamp>) -> String {
    match (result.finish, start_time) {
        (Some(finish), Some(_)) => penalized(ts_time_str(finish), result.penalty),
        _ => finish_code_text(result.finish_code, result.hoc_position),
    }
}

/// Returns a finish code's label; HOC shows its place from `hoc_position`.
pub fn finish_code_text(code: FinishCode, hoc_position: Option<i32>) -> String {
    match code {
        FinishCode::Time
        | FinishCode::Ret
        | FinishCode::Dnf
        | FinishCode::Nsc
        | FinishCode::Dns
        | FinishCode::Dsq => code.name().to_string(),
        FinishCode::Hoc => format!(
            "HOC {}",
            hoc_position.map(|p| p.to_string()).unwrap_or_default()
        ),
        FinishCode::DnsRc => "DNS RC Volunteer".to_string(),
    }
}

fn penalized(time: String, penalty: Option<i32>) -> String {
    match penalty {
        Some(p) => format!("{time} P+{p}"),
        None => time,
    }
}

/// Formats a report card's finish cell.
pub fn card_finish_text(card: &RaceReportCard) -> String {
    match (card.finish_time, card.start_time) {
        (Some(finish), Some(_)) => penalized(ts_time_str(finish), card.penalty),
        _ => finish_code_text(card.result_record.result.finish_code, card.hoc_position),
    }
}

/// Formats a report card's elapsed time cell.
pub fn elapsed_text(card: &RaceReportCard) -> String {
    card.elapsed_time
        .map(display)
        .unwrap_or_else(|| "n/a".to_string())
}

/// Formats a time correction factor with four decimals.
fn cf_text(tcf: f64) -> String {
    format!("{tcf:.4}")
}

pub fn cor_time_phrf_text(card: &RaceReportCard) -> String {
    cor_time_text(card.phrf_tcf, card.corrected_phrf_time, "PHRF")
}

pub fn cor_time_orc_text(card: &RaceReportCard) -> String {
    cor_time_text(card.orc_tcf, card.corrected_orc_time, "ORC")
}

fn cor_time_text(tcf: f64, duration: Option<SignedDuration>, label: &str) -> String {
    duration
        .map(|d| format!("{} ({} {label})", display(d), cf_text(tcf)))
        .unwrap_or_else(|| "n/a".to_string())
}

// Place columns of the race results table. Each function returns the lines of one cell; the
// view renders them separated by <br>.

/// Returns the lines of the "Place In Bracket" cell of a class with more than one bracket.
pub fn bracket_place_lines(card: &RaceReportCard, show_orc: bool) -> Vec<String> {
    let rating = card.rating_type();
    if !show_orc {
        vec![card.place_in_bracket.to_string()]
    } else if rating == RatingType::OrcPhrf && card.place_in_bracket_orc != 0 {
        vec![
            format!("{}(PHRF), ", card.place_in_bracket),
            format!("{}(ORC)", card.place_in_bracket_orc),
        ]
    } else if rating.is_phrf() {
        vec![format!("{}(PHRF)", card.place_in_bracket)]
    } else if rating == RatingType::Orc {
        vec![format!("{}(ORC)", card.place_in_bracket_orc)]
    } else {
        Vec::new()
    }
}

/// Returns the lines of the "Place In Class" cell of a class with more than one bracket.
pub fn class_place_lines(card: &RaceReportCard, show_orc: bool) -> Vec<String> {
    let rating = card.rating_type();
    if !show_orc {
        vec![card.place_in_class.to_string()]
    } else if rating == RatingType::OrcPhrf && card.place_in_class_orc != 0 {
        vec![
            format!("{}(PHRF), ", card.place_in_class),
            format!("{}(ORC)", card.place_in_class_orc),
        ]
    } else if rating.is_phrf() {
        vec![format!("{}(PHRF)", card.place_in_class)]
    } else if rating == RatingType::Orc {
        vec![format!("{}(ORC)", card.place_in_class_orc)]
    } else {
        Vec::new()
    }
}

/// Returns the lines of the single "Place In Class" cell of a class with one bracket.
///
/// There the bracket place equals the class place.
pub fn single_bracket_place_lines(card: &RaceReportCard) -> Vec<String> {
    let rating = card.rating_type();
    if rating == RatingType::OrcPhrf && card.place_in_bracket_orc != 0 {
        vec![
            format!("{}(PHRF), ", card.place_in_bracket),
            format!("{}(ORC)", card.place_in_bracket_orc),
        ]
    } else if rating.is_phrf() {
        vec![format!("{}(PHRF)", card.place_in_bracket)]
    } else if rating == RatingType::Orc {
        vec![format!("{}(ORC)", card.place_in_bracket_orc)]
    } else {
        vec![card.place_in_bracket.to_string()]
    }
}

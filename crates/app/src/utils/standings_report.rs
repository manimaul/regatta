//! = `StandingsReport.kt`: cell text and decoration of the series standings table. Pure, covered
//! by `tests/standings_labels_test.rs`. Class names are literal strings so Tailwind's scanner
//! finds them.

use regatta_common::{
    score_text, FinishCode, RaceStatus, RatingType, StandingsBoatSkipper, StandingsRace,
};

/// One decorated piece of a standings cell line.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct ScoreSegment {
    pub text: String,
    pub classes: Vec<&'static str>,
    pub bold: bool,
}

impl ScoreSegment {
    pub fn new(text: impl Into<String>) -> ScoreSegment {
        ScoreSegment {
            text: text.into(),
            classes: Vec::new(),
            bold: false,
        }
    }

    pub fn bold(text: impl Into<String>, classes: Vec<&'static str>) -> ScoreSegment {
        ScoreSegment {
            text: text.into(),
            classes,
            bold: true,
        }
    }
}

/// One line of a standings cell: its segments, then a plain suffix (a rating label and/or a finish code).
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct ScoreLine {
    pub segments: Vec<ScoreSegment>,
    pub suffix: String,
}

impl ScoreLine {
    pub fn new(segments: Vec<ScoreSegment>, suffix: impl Into<String>) -> ScoreLine {
        ScoreLine {
            segments,
            suffix: suffix.into(),
        }
    }
}

/// D9: which sides of a row the table shows, as (PHRF, ORC). The ORC side appears only when the
/// class is ORC-rated (`show_orc`, the race view's gate) and the row is; the PHRF side appears
/// unless the class is ORC-rated and the row is ORC-only.
fn sides(rating: RatingType, show_orc: bool) -> (bool, bool) {
    (!show_orc || rating.is_phrf(), show_orc && rating.is_orc())
}

/// Text that replaces the whole cell when the race was not sailed for any group the row is scored
/// in (D2 / D9), else `None`. `status_text_phrf` is the Kotlin default (the PHRF side alone).
pub fn status_text(
    race: &StandingsRace,
    rating: RatingType,
    show_orc: bool,
) -> Option<&'static str> {
    let (phrf, orc) = sides(rating, show_orc);
    let sailed = (phrf && (race.bracket_sailed || race.class_sailed))
        || (orc && (race.bracket_sailed_orc || race.class_sailed_orc));
    match race.status {
        RaceStatus::NotYetSailed => Some("Not yet sailed"),
        RaceStatus::Cancelled => Some("Cancelled"),
        _ if !sailed => Some("Not sailed"),
        _ => None,
    }
}

pub fn status_text_phrf(race: &StandingsRace) -> Option<&'static str> {
    status_text(race, RatingType::Phrf, false)
}

/// Bracket score, shown first in a race cell ("4", "3.5"); a dash when the race was not sailed in the bracket.
pub fn bracket_score_text(race: &StandingsRace) -> String {
    if race.bracket_sailed {
        score_text(race.place_in_bracket)
    } else {
        "-".to_string()
    }
}

/// Class score, shown after the bracket score when the class has more than one bracket; a dash when not sailed in the class.
pub fn class_score_text(race: &StandingsRace) -> String {
    if race.class_sailed {
        format!(", {}", score_text(race.place_in_class))
    } else {
        ", -".to_string()
    }
}

/// Decoration of the bracket score: red when thrown out of the bracket total.
pub fn bracket_score_classes(race: &StandingsRace) -> Vec<&'static str> {
    if race.throw_out {
        vec!["text-danger-fg"]
    } else {
        Vec::new()
    }
}

/// Decoration of the class score: red when thrown out of the class total.
pub fn class_score_classes(race: &StandingsRace) -> Vec<&'static str> {
    if race.throw_out_class {
        vec!["text-danger-fg"]
    } else {
        Vec::new()
    }
}

// D9: the same four for the ORC groups

pub fn bracket_score_text_orc(race: &StandingsRace) -> String {
    if race.bracket_sailed_orc {
        score_text(race.place_in_bracket_orc)
    } else {
        "-".to_string()
    }
}

pub fn class_score_text_orc(race: &StandingsRace) -> String {
    if race.class_sailed_orc {
        format!(", {}", score_text(race.place_in_class_orc))
    } else {
        ", -".to_string()
    }
}

pub fn bracket_score_classes_orc(race: &StandingsRace) -> Vec<&'static str> {
    if race.throw_out_orc {
        vec!["text-danger-fg"]
    } else {
        Vec::new()
    }
}

pub fn class_score_classes_orc(race: &StandingsRace) -> Vec<&'static str> {
    if race.throw_out_class_orc {
        vec!["text-danger-fg"]
    } else {
        Vec::new()
    }
}

/// Finish-code suffix after the scores, e.g. " DNF", " HOC3", " DSQ", " DNS RC Volunteer"; " DNS" for no record; empty for a timed finish.
pub fn code_suffix(race: &StandingsRace) -> String {
    match race.finish_code {
        Some(FinishCode::Time) => String::new(),
        Some(
            code @ (FinishCode::Ret
            | FinishCode::Dnf
            | FinishCode::Nsc
            | FinishCode::Dns
            | FinishCode::Dsq),
        ) => {
            format!(" {}", code.name())
        }
        Some(FinishCode::Hoc) => format!(
            " HOC{}",
            race.hoc_position
                .map(|p| p.to_string())
                .unwrap_or_else(|| "null".to_string())
        ),
        Some(FinishCode::DnsRc) => " DNS RC Volunteer".to_string(),
        None => " DNS".to_string(),
    }
}

/// Bracket total, shown before the class total when the class has more than one bracket.
pub fn total_bracket_text(row: &StandingsBoatSkipper) -> String {
    format!("{}, ", score_text(row.total_score_bracket))
}

/// Class total.
pub fn total_class_text(row: &StandingsBoatSkipper) -> String {
    score_text(row.total_score_class)
}

/// The race cell: one line per side the row is scored on (D9). Each line is the bold bracket score
/// and, in a class with more than one bracket, the bold class score, each red when thrown out of
/// its total, then the rating label when the class is ORC-rated. The finish code follows the last
/// line, except that a boat with no ORC entry that day is `DNS` on the ORC line, and the PHRF line
/// then carries the boat's own code (a finish shows nothing, no record shows `DNS` too).
/// Without `show_orc` this is the single unlabeled line the table has always shown.
pub fn score_lines(
    race: &StandingsRace,
    show_orc: bool,
    multi_bracket: bool,
    rating: RatingType,
) -> Vec<ScoreLine> {
    let (phrf, orc) = sides(rating, show_orc);
    let code = code_suffix(race);
    // the ORC line says DNS itself when the boat had no ORC entry; the PHRF line then keeps its own code
    let orc_dns = orc && race.non_starter_orc;
    let line = |bracket: String,
                bracket_classes: Vec<&'static str>,
                klass: String,
                class_classes: Vec<&'static str>,
                suffix: String| {
        let mut segments = vec![ScoreSegment::bold(bracket, bracket_classes)];
        if multi_bracket {
            segments.push(ScoreSegment::bold(klass, class_classes));
        }
        ScoreLine::new(segments, suffix)
    };
    let mut lines = Vec::new();
    if phrf {
        let mut suffix = if show_orc {
            " (PHRF)".to_string()
        } else {
            String::new()
        };
        if !orc || orc_dns {
            suffix.push_str(&code);
        }
        lines.push(line(
            bracket_score_text(race),
            bracket_score_classes(race),
            class_score_text(race),
            class_score_classes(race),
            suffix,
        ));
    }
    if orc {
        let suffix = format!(
            " (ORC){}",
            if orc_dns {
                " DNS".to_string()
            } else {
                code.clone()
            }
        );
        lines.push(line(
            bracket_score_text_orc(race),
            bracket_score_classes_orc(race),
            class_score_text_orc(race),
            class_score_classes_orc(race),
            suffix,
        ));
    }
    lines
}

/// The "Total Score" cell: one line per side (D9), each the bold bracket total (in a class with more
/// than one bracket) and the class total, labeled with the rating when the class is ORC-rated.
pub fn total_lines(
    row: &StandingsBoatSkipper,
    show_orc: bool,
    multi_bracket: bool,
) -> Vec<ScoreLine> {
    let (phrf, orc) = sides(row.rating_type, show_orc);
    let line = |bracket: f64, klass: f64, label: &str| {
        let mut segments = Vec::new();
        if multi_bracket {
            segments.push(ScoreSegment::bold(
                format!("{}, ", score_text(bracket)),
                Vec::new(),
            ));
        }
        segments.push(ScoreSegment::new(score_text(klass)));
        ScoreLine::new(segments, label)
    };
    let mut lines = Vec::new();
    if phrf {
        lines.push(line(
            row.total_score_bracket,
            row.total_score_class,
            if show_orc { " (PHRF)" } else { "" },
        ));
    }
    if orc {
        lines.push(line(
            row.total_score_bracket_orc,
            row.total_score_class_orc,
            " (ORC)",
        ));
    }
    lines
}

/// A place cell in the race view's notation: `2(PHRF), ` ⏎ `1(ORC)`, `2(PHRF)`, `1(ORC)`, or the bare place outside an ORC class.
fn place_lines(phrf_place: i32, orc_place: i32, rating: RatingType, show_orc: bool) -> Vec<String> {
    if !show_orc {
        vec![phrf_place.to_string()]
    } else if rating == RatingType::OrcPhrf && orc_place != 0 {
        vec![format!("{phrf_place}(PHRF), "), format!("{orc_place}(ORC)")]
    } else if rating.is_phrf() {
        vec![format!("{phrf_place}(PHRF)")]
    } else if rating == RatingType::Orc {
        vec![format!("{orc_place}(ORC)")]
    } else {
        vec![phrf_place.to_string()]
    }
}

/// The "Place In Bracket" cell.
pub fn bracket_place_lines(row: &StandingsBoatSkipper, show_orc: bool) -> Vec<String> {
    place_lines(
        row.place_in_bracket,
        row.place_in_bracket_orc,
        row.rating_type,
        show_orc,
    )
}

/// The "Place In Class" cell.
pub fn class_place_lines(row: &StandingsBoatSkipper, show_orc: bool) -> Vec<String> {
    place_lines(
        row.place_in_class,
        row.place_in_class_orc,
        row.rating_type,
        show_orc,
    )
}

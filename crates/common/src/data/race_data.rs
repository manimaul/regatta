//! = `RaceData.kt`.
//!
//! Four Kotlin defaults depend on another field (`Race.orc3Band` / `orc5Band` and
//! `ClassReportCards.orc3Band` / `orc5Band` from `phrfBFactor`; `RaceResult.finishCode` /
//! `RaceResultFull.finishCode` from `finish`). Those structs deserialize through a private
//! `…Raw` mirror whose dependent fields are `Option`s (`#[serde(from = "…Raw")]`), and the
//! `From` impl fills the missing ones the way the Kotlin constructor does. Serialization is the
//! plain derive, so the output order and keys are unaffected.

use crate::data::{
    orc_certificate_url, FinishCode, Orc3Band, Orc5Band, OrcCertificate, OrcScoringOption,
};
use crate::serde_kotlin::{kotlin_duration, kotlin_instant, DISTANT_FUTURE, DISTANT_PAST};
use crate::utils::{kotlin_f32_to_string, CORRECTION_FACTOR_DEFAULT};
use jiff::{SignedDuration, Timestamp};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct Series {
    pub id: i64,
    pub name: String,
    pub sort: i32,
    pub race_count: i64,
    pub active: bool,
}

impl Default for Series {
    fn default() -> Self {
        Series {
            id: 0,
            name: String::new(),
            sort: 0,
            race_count: 0,
            active: true,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct Person {
    pub id: i64,
    pub first: String,
    pub last: String,
    pub club_member: bool,
    pub active: bool,
}

impl Default for Person {
    fn default() -> Self {
        Person {
            id: 0,
            first: String::new(),
            last: String::new(),
            club_member: false,
            active: true,
        }
    }
}

impl Person {
    pub fn full_name(&self) -> String {
        format!("{} {}", self.first, self.last)
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceClassBrackets {
    pub race_class: RaceClass,
    pub brackets: Vec<Bracket>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceClass {
    pub id: i64,
    pub name: String,
    pub sort: i32,
    pub rating_type: RatingType,
    pub number_of_races: i64,
    pub active: bool,
}

impl Default for RaceClass {
    fn default() -> Self {
        RaceClass {
            id: 0,
            name: String::new(),
            sort: 0,
            rating_type: RatingType::CruisingNonFlyingSails,
            number_of_races: 0,
            active: true,
        }
    }
}

impl RaceClass {
    pub fn label(&self) -> String {
        match self.rating_type {
            RatingType::Orc | RatingType::OrcPhrf | RatingType::Phrf => {
                format!("{} {}", self.rating_type.label(), self.name)
            }
            RatingType::CruisingFlyingSails | RatingType::CruisingNonFlyingSails => {
                self.name.clone()
            }
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct Bracket {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
    pub active: bool,
    pub number_of_races: i64,
    /// slowest PHRF - fastest ORC
    pub min_rating: f32,
    /// fastest PHRF - slowest ORC
    pub max_rating: f32,
    pub class_id: i64,
}

impl Default for Bracket {
    fn default() -> Self {
        Bracket {
            id: 0,
            name: String::new(),
            description: None,
            active: true,
            number_of_races: 0,
            min_rating: -1000.0,
            max_rating: 1000.0,
            class_id: 0,
        }
    }
}

impl Bracket {
    /// The name with its open or closed rating range; the ratings print as Kotlin `Float`s ("10.0").
    pub fn label(&self) -> String {
        if self.min_rating <= -1000.0 && self.max_rating >= 1000.0 {
            self.name.clone()
        } else if self.min_rating <= -1000.0 {
            format!("{} {} -", self.name, kotlin_f32_to_string(self.max_rating))
        } else if self.max_rating >= 1000.0 {
            format!("{} {} +", self.name, kotlin_f32_to_string(self.min_rating))
        } else {
            format!(
                "{} {} to {}",
                self.name,
                kotlin_f32_to_string(self.min_rating),
                kotlin_f32_to_string(self.max_rating)
            )
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct ClassSchedule {
    pub race_class: RaceClass,
    pub brackets: Vec<Bracket>,
    #[serde(with = "kotlin_instant")]
    pub start_date: Timestamp,
    #[serde(with = "kotlin_instant")]
    pub end_date: Timestamp,
}

impl Default for ClassSchedule {
    fn default() -> Self {
        ClassSchedule {
            race_class: RaceClass::default(),
            brackets: Vec::new(),
            start_date: DISTANT_PAST,
            end_date: DISTANT_FUTURE,
        }
    }
}

impl ClassSchedule {
    pub fn race_start(&self) -> Option<Timestamp> {
        (self.start_date != DISTANT_PAST).then_some(self.start_date)
    }

    pub fn race_end(&self) -> Option<Timestamp> {
        (self.end_date != DISTANT_FUTURE).then_some(self.end_date)
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceSchedule {
    pub race: Race,
    pub result_count: i64,
    pub series: Option<Series>,
    pub rc: Option<Person>,
    pub schedule: Vec<ClassSchedule>,
}

impl RaceSchedule {
    /// The earliest class start (Kotlin `startTime`; `None` where Kotlin throws on an empty schedule).
    pub fn start_time(&self) -> Option<Timestamp> {
        self.schedule.iter().map(|s| s.start_date).min()
    }

    /// The latest class end (Kotlin `endTime`; `None` where Kotlin throws on an empty schedule).
    pub fn end_time(&self) -> Option<Timestamp> {
        self.schedule.iter().map(|s| s.end_date).max()
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", from = "RaceRaw")]
pub struct Race {
    pub id: i64,
    pub name: String,
    pub series_id: Option<i64>,
    pub rc_id: Option<i64>,
    pub report_image: Option<String>,
    pub phrf_b_factor: i32,
    pub orc_scoring_option: OrcScoringOption,
    pub orc3_band: Orc3Band,
    pub orc5_band: Orc5Band,
}

impl Default for Race {
    fn default() -> Self {
        Race::from(RaceRaw::default())
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", default)]
struct RaceRaw {
    id: i64,
    name: String,
    series_id: Option<i64>,
    rc_id: Option<i64>,
    report_image: Option<String>,
    phrf_b_factor: i32,
    orc_scoring_option: OrcScoringOption,
    orc3_band: Option<Orc3Band>,
    orc5_band: Option<Orc5Band>,
}

impl Default for RaceRaw {
    fn default() -> Self {
        RaceRaw {
            id: 0,
            name: String::new(),
            series_id: None,
            rc_id: None,
            report_image: None,
            phrf_b_factor: CORRECTION_FACTOR_DEFAULT,
            orc_scoring_option: OrcScoringOption::FiveBandWindwardLeeward,
            orc3_band: None,
            orc5_band: None,
        }
    }
}

impl From<RaceRaw> for Race {
    fn from(raw: RaceRaw) -> Self {
        Race {
            id: raw.id,
            name: raw.name,
            series_id: raw.series_id,
            rc_id: raw.rc_id,
            report_image: raw.report_image,
            phrf_b_factor: raw.phrf_b_factor,
            orc_scoring_option: raw.orc_scoring_option,
            orc3_band: raw
                .orc3_band
                .unwrap_or_else(|| Orc3Band::from_phrf_b_factor(raw.phrf_b_factor)),
            orc5_band: raw
                .orc5_band
                .unwrap_or_else(|| Orc5Band::from_phrf_b_factor(raw.phrf_b_factor)),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RaceTime {
    #[serde(with = "kotlin_instant")]
    pub start_date: Timestamp,
    #[serde(with = "kotlin_instant")]
    pub end_date: Timestamp,
    pub class_id: i64,
    pub race_id: i64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum RatingType {
    #[serde(rename = "ORC")]
    Orc,
    #[serde(rename = "ORC_PHRF")]
    OrcPhrf,
    #[serde(rename = "PHRF")]
    Phrf,
    CruisingFlyingSails,
    CruisingNonFlyingSails,
}

impl RatingType {
    pub const ENTRIES: [RatingType; 5] = [
        RatingType::Orc,
        RatingType::OrcPhrf,
        RatingType::Phrf,
        RatingType::CruisingFlyingSails,
        RatingType::CruisingNonFlyingSails,
    ];

    /// Kotlin `name`, the stored and serialized form.
    pub const fn name(self) -> &'static str {
        match self {
            RatingType::Orc => "ORC",
            RatingType::OrcPhrf => "ORC_PHRF",
            RatingType::Phrf => "PHRF",
            RatingType::CruisingFlyingSails => "CruisingFlyingSails",
            RatingType::CruisingNonFlyingSails => "CruisingNonFlyingSails",
        }
    }

    pub const fn label(self) -> &'static str {
        match self {
            RatingType::Orc => "ORC",
            RatingType::OrcPhrf => "ORC, PHRF",
            RatingType::Phrf => "PHRF",
            RatingType::CruisingFlyingSails => "Not Rated (FS)",
            RatingType::CruisingNonFlyingSails => "Not Rated",
        }
    }

    pub const fn is_cruising(self) -> bool {
        matches!(
            self,
            RatingType::CruisingFlyingSails | RatingType::CruisingNonFlyingSails
        )
    }

    pub const fn is_phrf(self) -> bool {
        matches!(self, RatingType::Phrf | RatingType::OrcPhrf)
    }

    pub const fn is_orc(self) -> bool {
        matches!(self, RatingType::Orc | RatingType::OrcPhrf)
    }

    pub const fn is_orc_or_phrf(self) -> bool {
        matches!(
            self,
            RatingType::Orc | RatingType::OrcPhrf | RatingType::Phrf
        )
    }

    pub const fn is_compatible(self, other: RatingType) -> bool {
        (self.is_cruising() && other.is_cruising())
            || (self.is_phrf() && other.is_phrf())
            || (self.is_orc() && other.is_orc())
    }

    /// The label with the ratings: `PHRF (48)`, `PHRF (null)`, `ORC(~75)`, `PHRF (48), ORC (~75)`.
    pub fn rated_label(self, phrf_rating: Option<i32>, orc_info: Option<&str>) -> String {
        let phrf = match phrf_rating {
            Some(r) => r.to_string(),
            None => "null".to_string(),
        };
        match self {
            RatingType::Orc => {
                format!(
                    "{}{}",
                    self.label(),
                    orc_info.map(|i| format!("({i})")).unwrap_or_default()
                )
            }
            RatingType::OrcPhrf => format!(
                "PHRF ({phrf}), ORC{}",
                orc_info.map(|i| format!(" ({i})")).unwrap_or_default()
            ),
            RatingType::Phrf => format!("{} ({phrf})", self.label()),
            RatingType::CruisingFlyingSails | RatingType::CruisingNonFlyingSails => {
                self.label().to_string()
            }
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct Boat {
    pub id: i64,
    pub name: String,
    pub sail_number: String,
    pub boat_type: String,
    pub phrf_rating: Option<i32>,
    pub orc_certs: Vec<OrcCertificate>,
    pub skipper_id: Option<i64>,
    pub rating_type: RatingType,
    pub number_of_races: i64,
    pub active: bool,
}

impl Default for Boat {
    fn default() -> Self {
        Boat {
            id: 0,
            name: String::new(),
            sail_number: String::new(),
            boat_type: String::new(),
            phrf_rating: None,
            orc_certs: Vec::new(),
            skipper_id: None,
            rating_type: RatingType::CruisingNonFlyingSails,
            number_of_races: 0,
            active: true,
        }
    }
}

impl Boat {
    /// `~<lowest virtual PHRF of the certificates>`, or `None` without certificates.
    pub fn orc_info(&self) -> Option<String> {
        self.orc_certs
            .iter()
            .map(OrcCertificate::virtual_phrf)
            .min()
            .map(|v| format!("~{v}"))
    }

    /// What the boat's rating type is missing: a PHRF or ORC/PHRF boat needs a PHRF rating, an
    /// ORC or ORC/PHRF boat needs an ORC certificate. `None` when it has both it needs.
    pub fn missing_rating(&self) -> Option<String> {
        if self.rating_type.is_phrf() && self.phrf_rating.is_none() {
            return Some(format!(
                "A {} boat needs a PHRF rating",
                self.rating_type.label()
            ));
        }
        if self.rating_type.is_orc() && self.orc_certs.is_empty() {
            return Some(format!(
                "An {} boat needs an ORC certificate",
                self.rating_type.label()
            ));
        }
        None
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct BoatSkipper {
    pub skipper: Option<Person>,
    pub boat: Option<Boat>,
}

impl BoatSkipper {
    pub fn label(&self) -> String {
        match (&self.boat, &self.skipper) {
            (Some(boat), Some(skipper)) => format!("{} - {}", boat.name, skipper.full_name()),
            (Some(boat), None) => boat.name.clone(),
            (None, Some(skipper)) => skipper.full_name(),
            (None, None) => String::new(),
        }
    }

    pub fn drop_label(&self) -> String {
        let sail = self
            .boat
            .as_ref()
            .map(|b| format!(" ({})", b.sail_number))
            .unwrap_or_default();
        let rating = self
            .boat
            .as_ref()
            .map(|b| b.rating_type.label())
            .unwrap_or("");
        format!("{}{sail} {rating}", self.label())
    }

    pub fn short_label(&self) -> String {
        self.boat
            .as_ref()
            .map(|b| b.name.clone())
            .unwrap_or_default()
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceScheduleResults {
    pub race_schedule: RaceSchedule,
    pub results: Vec<RaceResultFull>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", from = "RaceResultFullRaw")]
pub struct RaceResultFull {
    pub id: i64,
    pub race_id: i64,
    pub boat: BoatSkipper,
    #[serde(with = "kotlin_instant::option")]
    pub finish: Option<Timestamp>,
    pub phrf_rating: Option<i32>,
    pub orc_certificate: Option<OrcCertificate>,
    pub hoc_position: Option<i32>,
    pub bracket_id: i64,
    pub race_class_id: i64,
    pub penalty: Option<i32>,
    pub rating_type: RatingType,
    pub finish_code: FinishCode,
}

impl Default for RaceResultFull {
    fn default() -> Self {
        RaceResultFull::from(RaceResultFullRaw::default())
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", default)]
struct RaceResultFullRaw {
    id: i64,
    race_id: i64,
    boat: BoatSkipper,
    #[serde(with = "kotlin_instant::option")]
    finish: Option<Timestamp>,
    phrf_rating: Option<i32>,
    orc_certificate: Option<OrcCertificate>,
    hoc_position: Option<i32>,
    bracket_id: i64,
    race_class_id: i64,
    penalty: Option<i32>,
    rating_type: RatingType,
    finish_code: Option<FinishCode>,
}

impl Default for RaceResultFullRaw {
    fn default() -> Self {
        RaceResultFullRaw {
            id: 0,
            race_id: 0,
            boat: BoatSkipper::default(),
            finish: None,
            phrf_rating: None,
            orc_certificate: None,
            hoc_position: None,
            bracket_id: 0,
            race_class_id: 0,
            penalty: None,
            rating_type: RatingType::CruisingNonFlyingSails,
            finish_code: None,
        }
    }
}

/// The Kotlin default `finish?.let { FinishCode.TIME } ?: FinishCode.RET`.
fn default_finish_code(finish: Option<Timestamp>) -> FinishCode {
    if finish.is_some() {
        FinishCode::Time
    } else {
        FinishCode::Ret
    }
}

impl From<RaceResultFullRaw> for RaceResultFull {
    fn from(raw: RaceResultFullRaw) -> Self {
        RaceResultFull {
            id: raw.id,
            race_id: raw.race_id,
            boat: raw.boat,
            finish: raw.finish,
            phrf_rating: raw.phrf_rating,
            orc_certificate: raw.orc_certificate,
            hoc_position: raw.hoc_position,
            bracket_id: raw.bracket_id,
            race_class_id: raw.race_class_id,
            penalty: raw.penalty,
            rating_type: raw.rating_type,
            finish_code: raw
                .finish_code
                .unwrap_or_else(|| default_finish_code(raw.finish)),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", from = "RaceResultRaw")]
pub struct RaceResult {
    pub id: i64,
    pub race_id: i64,
    pub boat_id: i64,
    #[serde(with = "kotlin_instant::option")]
    pub finish: Option<Timestamp>,
    pub phrf_rating: Option<i32>,
    pub orc_ref: Option<String>,
    pub hoc_position: Option<i32>,
    pub bracket_id: i64,
    pub race_class_id: i64,
    pub penalty: Option<i32>,
    pub rating_type: RatingType,
    pub finish_code: FinishCode,
}

impl Default for RaceResult {
    fn default() -> Self {
        RaceResult::from(RaceResultRaw::default())
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", default)]
struct RaceResultRaw {
    id: i64,
    race_id: i64,
    boat_id: i64,
    #[serde(with = "kotlin_instant::option")]
    finish: Option<Timestamp>,
    phrf_rating: Option<i32>,
    orc_ref: Option<String>,
    hoc_position: Option<i32>,
    bracket_id: i64,
    race_class_id: i64,
    penalty: Option<i32>,
    rating_type: RatingType,
    finish_code: Option<FinishCode>,
}

impl Default for RaceResultRaw {
    fn default() -> Self {
        RaceResultRaw {
            id: 0,
            race_id: 0,
            boat_id: 0,
            finish: None,
            phrf_rating: None,
            orc_ref: None,
            hoc_position: None,
            bracket_id: 0,
            race_class_id: 0,
            penalty: None,
            rating_type: RatingType::CruisingNonFlyingSails,
            finish_code: None,
        }
    }
}

impl From<RaceResultRaw> for RaceResult {
    fn from(raw: RaceResultRaw) -> Self {
        RaceResult {
            id: raw.id,
            race_id: raw.race_id,
            boat_id: raw.boat_id,
            finish: raw.finish,
            phrf_rating: raw.phrf_rating,
            orc_ref: raw.orc_ref,
            hoc_position: raw.hoc_position,
            bracket_id: raw.bracket_id,
            race_class_id: raw.race_class_id,
            penalty: raw.penalty,
            rating_type: raw.rating_type,
            finish_code: raw
                .finish_code
                .unwrap_or_else(|| default_finish_code(raw.finish)),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceResultBoatBracket {
    pub result: RaceResult,
    pub boat_skipper: BoatSkipper,
    pub bracket: Bracket,
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct StandingsSeries {
    pub year: i32,
    pub series: Series,
    pub standings: Vec<StandingsClass>,
    pub races: Vec<Race>,
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct StandingsClass {
    pub race_class: RaceClass,
    pub standings: Vec<StandingsBracket>,
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct StandingsBracket {
    pub bracket: Bracket,
    pub standings: Vec<StandingsBoatSkipper>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct StandingsBoatSkipper {
    pub boat_skipper: BoatSkipper,
    pub race_standings: Vec<StandingsRace>,
    /// Sum of the counted bracket scores; fractional when an RC volunteer average is in it (D1).
    pub total_score_bracket: f64,
    pub total_score_class: f64,
    /// 1-based series place in the bracket; tied boats share it and the next place skips (D5).
    pub place_in_bracket: i32,
    pub place_in_class: i32,
    /// D9: which ratings this row is scored under. In an ORC class, `ORC_PHRF` when the boat has
    /// both PHRF- and ORC-rated results in the series, else the one it has; elsewhere the rating
    /// of its most recent result. The PHRF fields are 0 for an `ORC` row and the ORC fields are 0
    /// for a `PHRF` row.
    pub rating_type: RatingType,
    /// D9: the ORC series scores, from the ORC race places, by the same rules as the PHRF ones.
    pub total_score_bracket_orc: f64,
    pub total_score_class_orc: f64,
    pub place_in_bracket_orc: i32,
    pub place_in_class_orc: i32,
}

impl Default for StandingsBoatSkipper {
    fn default() -> Self {
        StandingsBoatSkipper {
            boat_skipper: BoatSkipper::default(),
            race_standings: Vec::new(),
            total_score_bracket: 0.0,
            total_score_class: 0.0,
            place_in_bracket: 0,
            place_in_class: 0,
            rating_type: RatingType::CruisingNonFlyingSails,
            total_score_bracket_orc: 0.0,
            total_score_class_orc: 0.0,
            place_in_bracket_orc: 0,
            place_in_class_orc: 0,
        }
    }
}

/// D2: whether a race in a series standings column was sailed at all.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Default, Serialize, Deserialize)]
pub enum RaceStatus {
    #[default]
    Sailed,
    /// No results yet and the last class end time is still in the future.
    NotYetSailed,
    /// No results and the race is over.
    Cancelled,
}

impl RaceStatus {
    pub const fn name(self) -> &'static str {
        match self {
            RaceStatus::Sailed => "Sailed",
            RaceStatus::NotYetSailed => "NotYetSailed",
            RaceStatus::Cancelled => "Cancelled",
        }
    }
}

/// One boat's cell in a series standings row. A race is scored per group (D2): `place_in_bracket`
/// counts only when `bracket_sailed`, `place_in_class` only when `class_sailed`; the throw-outs are
/// chosen per group too. An RC volunteer's cell holds the average of the boat's other races (D1),
/// which is why the scores are fractional.
///
/// `finish` and `finish_code` have no Kotlin default and are required on input.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StandingsRace {
    #[serde(default)]
    pub non_starter: bool,
    #[serde(default)]
    pub hoc_position: Option<i32>,
    pub finish: bool,
    #[serde(default)]
    pub place_in_bracket: f64,
    #[serde(default)]
    pub place_in_class: f64,
    #[serde(default)]
    pub throw_out: bool,
    #[serde(default)]
    pub throw_out_class: bool,
    pub finish_code: Option<FinishCode>,
    #[serde(default = "default_true")]
    pub bracket_sailed: bool,
    #[serde(default = "default_true")]
    pub class_sailed: bool,
    #[serde(default)]
    pub status: RaceStatus,
    /// D9: the ORC groups of the race, scored like the PHRF ones among the ORC-rated entries; 0 / false outside an ORC class.
    #[serde(default)]
    pub place_in_bracket_orc: f64,
    #[serde(default)]
    pub place_in_class_orc: f64,
    #[serde(default)]
    pub throw_out_orc: bool,
    #[serde(default)]
    pub throw_out_class_orc: bool,
    #[serde(default)]
    pub bracket_sailed_orc: bool,
    #[serde(default)]
    pub class_sailed_orc: bool,
    /// D9: the boat had no ORC entry in a race sailed for its ORC group (no record, or raced PHRF-only that day), so it scores DNS there.
    #[serde(default)]
    pub non_starter_orc: bool,
}

fn default_true() -> bool {
    true
}

impl StandingsRace {
    /// A cell with the Kotlin defaults around the two required fields.
    pub fn new(finish: bool, finish_code: Option<FinishCode>) -> Self {
        StandingsRace {
            non_starter: false,
            hoc_position: None,
            finish,
            place_in_bracket: 0.0,
            place_in_class: 0.0,
            throw_out: false,
            throw_out_class: false,
            finish_code,
            bracket_sailed: true,
            class_sailed: true,
            status: RaceStatus::Sailed,
            place_in_bracket_orc: 0.0,
            place_in_class_orc: 0.0,
            throw_out_orc: false,
            throw_out_class_orc: false,
            bracket_sailed_orc: false,
            class_sailed_orc: false,
            non_starter_orc: false,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceReport {
    pub race_schedule: RaceSchedule,
    pub class_reports: Vec<ClassReportCards>,
}

impl RaceReport {
    pub fn class_start(&self, class_id: i64) -> Option<Timestamp> {
        self.race_schedule
            .schedule
            .iter()
            .find(|s| s.race_class.id == class_id)
            .map(|s| s.start_date)
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", from = "ClassReportCardsRaw")]
pub struct ClassReportCards {
    pub race_class: RaceClass,
    pub phrf_b_factor: i32,
    pub orc_scoring_option: OrcScoringOption,
    pub orc3_band: Orc3Band,
    pub orc5_band: Orc5Band,
    pub bracket_report: Vec<BracketReportCards>,
    pub has_orc_results: bool,
}

impl Default for ClassReportCards {
    fn default() -> Self {
        ClassReportCards::from(ClassReportCardsRaw::default())
    }
}

impl ClassReportCards {
    pub fn orc_band_label(&self) -> Option<String> {
        self.orc_scoring_option
            .orc_band_label(self.orc5_band, self.orc3_band)
    }
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", default)]
struct ClassReportCardsRaw {
    race_class: RaceClass,
    phrf_b_factor: i32,
    orc_scoring_option: OrcScoringOption,
    orc3_band: Option<Orc3Band>,
    orc5_band: Option<Orc5Band>,
    bracket_report: Vec<BracketReportCards>,
    has_orc_results: bool,
}

impl Default for ClassReportCardsRaw {
    fn default() -> Self {
        ClassReportCardsRaw {
            race_class: RaceClass::default(),
            phrf_b_factor: CORRECTION_FACTOR_DEFAULT,
            orc_scoring_option: OrcScoringOption::FiveBandWindwardLeeward,
            orc3_band: None,
            orc5_band: None,
            bracket_report: Vec::new(),
            has_orc_results: false,
        }
    }
}

impl From<ClassReportCardsRaw> for ClassReportCards {
    fn from(raw: ClassReportCardsRaw) -> Self {
        ClassReportCards {
            race_class: raw.race_class,
            phrf_b_factor: raw.phrf_b_factor,
            orc_scoring_option: raw.orc_scoring_option,
            orc3_band: raw
                .orc3_band
                .unwrap_or_else(|| Orc3Band::from_phrf_b_factor(raw.phrf_b_factor)),
            orc5_band: raw
                .orc5_band
                .unwrap_or_else(|| Orc5Band::from_phrf_b_factor(raw.phrf_b_factor)),
            bracket_report: raw.bracket_report,
            has_orc_results: raw.has_orc_results,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct BracketReportCards {
    pub bracket: Bracket,
    pub cards: Vec<RaceReportCard>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", default)]
pub struct RaceReportCard {
    pub result_record: RaceResultBoatBracket,
    pub boat_name: String,
    pub sail: String,
    pub skipper: String,
    pub boat_type: String,
    pub phrf_rating: Option<i32>,
    #[serde(with = "kotlin_instant::option")]
    pub start_time: Option<Timestamp>,
    #[serde(with = "kotlin_instant::option")]
    pub finish_time: Option<Timestamp>,
    #[serde(with = "kotlin_duration::option")]
    pub elapsed_time: Option<SignedDuration>,
    pub phrf_tcf: f64,
    pub orc_tcf: f64,
    #[serde(with = "kotlin_duration::option")]
    pub corrected_phrf_time: Option<SignedDuration>,
    #[serde(with = "kotlin_duration::option")]
    pub corrected_orc_time: Option<SignedDuration>,
    pub place_in_bracket: i32,
    pub place_in_bracket_orc: i32,
    pub place_in_class: i32,
    pub place_in_class_orc: i32,
    pub hoc_position: Option<i32>,
    pub penalty: Option<i32>,
    /// [`OrcCertificate::dxt_id`] of the result's certificate, for the link in the results table; never written while null.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub orc_dxt_id: Option<i64>,
}

impl Default for RaceReportCard {
    fn default() -> Self {
        RaceReportCard {
            result_record: RaceResultBoatBracket::default(),
            boat_name: String::new(),
            sail: String::new(),
            skipper: String::new(),
            boat_type: String::new(),
            phrf_rating: None,
            start_time: None,
            finish_time: None,
            elapsed_time: None,
            phrf_tcf: 1.0,
            orc_tcf: 1.0,
            corrected_phrf_time: None,
            corrected_orc_time: None,
            place_in_bracket: 0,
            place_in_bracket_orc: 0,
            place_in_class: 0,
            place_in_class_orc: 0,
            hoc_position: None,
            penalty: None,
            orc_dxt_id: None,
        }
    }
}

impl RaceReportCard {
    pub fn rating_type(&self) -> RatingType {
        self.result_record.result.rating_type
    }

    /// The public page of the result's ORC certificate, or null when its id was never resolved.
    pub fn orc_cert_url(&self) -> Option<String> {
        orc_certificate_url(self.orc_dxt_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn race_bands_default_from_phrf_b_factor() {
        let r: Race = serde_json::from_str(r#"{"phrfBFactor": 480}"#).unwrap();
        assert_eq!(r.orc3_band, Orc3Band::High);
        assert_eq!(r.orc5_band, Orc5Band::High);
        let r: Race = serde_json::from_str("{}").unwrap();
        assert_eq!(r.phrf_b_factor, 600);
        assert_eq!(r.orc3_band, Orc3Band::Low);
        assert_eq!(r.orc5_band, Orc5Band::LowMedium);
        let r: Race = serde_json::from_str(r#"{"phrfBFactor": 480, "orc5Band": "Low"}"#).unwrap();
        assert_eq!(r.orc5_band, Orc5Band::Low);
        assert_eq!(r.orc3_band, Orc3Band::High);
        assert_eq!(Race::default(), r_default());
        let c: ClassReportCards = serde_json::from_str(r#"{"phrfBFactor": 550}"#).unwrap();
        assert_eq!(
            (c.orc3_band, c.orc5_band),
            (Orc3Band::Medium, Orc5Band::Medium)
        );
    }

    fn r_default() -> Race {
        Race {
            id: 0,
            name: String::new(),
            series_id: None,
            rc_id: None,
            report_image: None,
            phrf_b_factor: 600,
            orc_scoring_option: OrcScoringOption::FiveBandWindwardLeeward,
            orc3_band: Orc3Band::Low,
            orc5_band: Orc5Band::LowMedium,
        }
    }

    #[test]
    fn finish_code_defaults_from_finish() {
        let r: RaceResult = serde_json::from_str("{}").unwrap();
        assert_eq!(r.finish_code, FinishCode::Ret);
        let r: RaceResult = serde_json::from_str(r#"{"finish": "2024-01-21T03:00:00Z"}"#).unwrap();
        assert_eq!(r.finish_code, FinishCode::Time);
        let r: RaceResult =
            serde_json::from_str(r#"{"finish": "2024-01-21T03:00:00Z", "finishCode": "DNF"}"#)
                .unwrap();
        assert_eq!(r.finish_code, FinishCode::Dnf);
        let r: RaceResultFull = serde_json::from_str(r#"{"finish": null}"#).unwrap();
        assert_eq!(r.finish_code, FinishCode::Ret);
        assert_eq!(RaceResult::default().finish_code, FinishCode::Ret);
    }

    #[test]
    fn race_json_has_kotlin_keys_in_order() {
        let json = serde_json::to_string(&Race::default()).unwrap();
        assert_eq!(
            json,
            r#"{"id":0,"name":"","seriesId":null,"rcId":null,"reportImage":null,"phrfBFactor":600,"orcScoringOption":"FiveBandWindwardLeeward","orc3Band":"Low","orc5Band":"LowMedium"}"#
        );
    }

    #[test]
    fn class_schedule_defaults_are_the_kotlin_sentinels() {
        let s: ClassSchedule = serde_json::from_str("{}").unwrap();
        assert_eq!(s.race_start(), None);
        assert_eq!(s.race_end(), None);
        let json = serde_json::to_string(&s).unwrap();
        assert!(
            json.contains(r#""startDate":"-100001-12-31T23:59:59.999999999Z""#),
            "{json}"
        );
        assert!(
            json.contains(r#""endDate":"+100000-01-01T00:00:00Z""#),
            "{json}"
        );
        let back: ClassSchedule = serde_json::from_str(&json).unwrap();
        assert_eq!(back, s);
    }

    #[test]
    fn schedule_start_and_end_time() {
        let mut rs = RaceSchedule::default();
        assert_eq!(rs.start_time(), None);
        let a: Timestamp = "2024-01-21T02:00:00Z".parse().unwrap();
        let b: Timestamp = "2024-01-21T03:00:00Z".parse().unwrap();
        let c: Timestamp = "2024-01-21T08:00:00Z".parse().unwrap();
        rs.schedule.push(ClassSchedule {
            start_date: b,
            end_date: c,
            ..Default::default()
        });
        rs.schedule.push(ClassSchedule {
            start_date: a,
            end_date: b,
            ..Default::default()
        });
        assert_eq!(rs.start_time(), Some(a));
        assert_eq!(rs.end_time(), Some(c));
        let report = RaceReport {
            race_schedule: rs,
            class_reports: vec![],
        };
        assert_eq!(report.class_start(0), Some(b));
        assert_eq!(report.class_start(99), None);
    }

    #[test]
    fn boat_skipper_labels() {
        let boat = Boat {
            name: "Waymaker".into(),
            sail_number: "59793".into(),
            rating_type: RatingType::OrcPhrf,
            ..Default::default()
        };
        let skipper = Person {
            first: "Ann".into(),
            last: "Lee".into(),
            ..Default::default()
        };
        let both = BoatSkipper {
            skipper: Some(skipper.clone()),
            boat: Some(boat.clone()),
        };
        assert_eq!(both.label(), "Waymaker - Ann Lee");
        assert_eq!(both.drop_label(), "Waymaker - Ann Lee (59793) ORC, PHRF");
        assert_eq!(both.short_label(), "Waymaker");
        assert_eq!(
            BoatSkipper {
                skipper: None,
                boat: Some(boat)
            }
            .label(),
            "Waymaker"
        );
        let only_skipper = BoatSkipper {
            skipper: Some(skipper),
            boat: None,
        };
        assert_eq!(only_skipper.label(), "Ann Lee");
        assert_eq!(only_skipper.drop_label(), "Ann Lee ");
        assert_eq!(only_skipper.short_label(), "");
        assert_eq!(BoatSkipper::default().label(), "");
    }

    #[test]
    fn class_label_and_orc_info() {
        let c = RaceClass {
            name: "Fast".into(),
            rating_type: RatingType::OrcPhrf,
            ..Default::default()
        };
        assert_eq!(c.label(), "ORC, PHRF Fast");
        let c = RaceClass {
            name: "Cruisers".into(),
            ..Default::default()
        };
        assert_eq!(c.label(), "Cruisers");
        let boat = Boat {
            orc_certs: vec![
                OrcCertificate {
                    five_band_wl_low_med_tot: 1.0,
                    ..Default::default()
                },
                OrcCertificate {
                    five_band_wl_low_med_tot: 0.8,
                    ..Default::default()
                },
            ],
            ..Default::default()
        };
        assert_eq!(boat.orc_info(), Some("~-50".into()));
        assert_eq!(Boat::default().orc_info(), None);
    }

    #[test]
    fn rating_type_predicates() {
        use RatingType::*;
        assert!(Orc.is_orc() && !Orc.is_phrf() && !Orc.is_cruising() && Orc.is_orc_or_phrf());
        assert!(OrcPhrf.is_orc() && OrcPhrf.is_phrf());
        assert!(Phrf.is_phrf() && !Phrf.is_orc());
        assert!(CruisingFlyingSails.is_cruising() && !CruisingFlyingSails.is_orc_or_phrf());
        assert!(
            Orc.is_compatible(OrcPhrf) && Phrf.is_compatible(OrcPhrf) && !Orc.is_compatible(Phrf)
        );
        assert!(CruisingFlyingSails.is_compatible(CruisingNonFlyingSails));
        assert!(!CruisingFlyingSails.is_compatible(Phrf));
        for t in RatingType::ENTRIES {
            assert_eq!(
                serde_json::to_string(&t).unwrap(),
                format!("\"{}\"", t.name())
            );
        }
    }

    #[test]
    fn standings_race_requires_finish_and_finish_code() {
        assert!(serde_json::from_str::<StandingsRace>("{}").is_err());
        let r: StandingsRace =
            serde_json::from_str(r#"{"finish": true, "finishCode": null}"#).unwrap();
        assert_eq!(r, StandingsRace::new(true, None));
        assert!(r.bracket_sailed && r.class_sailed && !r.bracket_sailed_orc);
        assert_eq!(r.status, RaceStatus::Sailed);
    }

    #[test]
    fn report_card_never_writes_null_orc_dxt_id() {
        let json = serde_json::to_string(&RaceReportCard::default()).unwrap();
        assert!(!json.contains("orcDxtId"));
        assert!(
            json.ends_with(r#""hocPosition":null,"penalty":null}"#),
            "{json}"
        );
        let card = RaceReportCard {
            orc_dxt_id: Some(264182),
            ..Default::default()
        };
        assert!(serde_json::to_string(&card)
            .unwrap()
            .ends_with(r#""orcDxtId":264182}"#));
    }
}

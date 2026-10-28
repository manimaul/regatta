//! The ORC certificate model, the ORC scoring options and wind bands, and the named PHRF B factors.
//!
//! [`OrcCertificate`] is ORC's RMS record as data.orc.org serves it (fetched by `crates/service/src/orc.rs`); its
//! serde keys are ORC's own (`RefNo`, `US_5B_LM_TOT`), and the same JSON is what the REST API and the golden files
//! carry. A race picks an [`OrcScoringOption`] and, for the banded options, an [`Orc3Band`] / [`Orc5Band`]; together
//! they select the one time-on-time factor of the certificate that scores the race (`orc_tcf` in
//! `crates/scoring/src/reporter.rs`). The bands follow the race's PHRF B factor, R4
//! (`docs/explore_implemented/orc_series_standings.md`): [`Orc3Band::from_phrf_b_factor`],
//! [`Orc5Band::from_phrf_b_factor`].

use crate::serde_kotlin::{kotlin_instant, DISTANT_FUTURE};
use jiff::Timestamp;
use serde::{Deserialize, Serialize};

/// Public page of an ORC certificate copy, by ORC's file id ([`OrcCertificate::dxt_id`]).
pub const ORC_CERTIFICATE_URL: &str = "https://data.orc.org/public/WPub.dll/CC/";

/// Returns the public page of the certificate with ORC file id `dxt_id`, or `None` without an id.
pub fn orc_certificate_url(dxt_id: Option<i64>) -> Option<String> {
    dxt_id.map(|id| format!("{ORC_CERTIFICATE_URL}{id}"))
}

/// The four named PHRF B factors, one per wind condition.
///
/// A race stores the number ([`crate::Race::phrf_b_factor`]); this names the condition it stands for, for the race
/// configuration form and the race view heading.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PhrfBFactor {
    Calm,
    Light,
    Avg,
    Hvy,
}

impl PhrfBFactor {
    /// Every B factor, calm to heavy (descending B).
    pub const ENTRIES: [PhrfBFactor; 4] = [
        PhrfBFactor::Calm,
        PhrfBFactor::Light,
        PhrfBFactor::Avg,
        PhrfBFactor::Hvy,
    ];

    /// Returns the serialized form, the variant name.
    pub const fn name(self) -> &'static str {
        match self {
            PhrfBFactor::Calm => "Calm",
            PhrfBFactor::Light => "Light",
            PhrfBFactor::Avg => "Avg",
            PhrfBFactor::Hvy => "Hvy",
        }
    }

    /// Returns the condition with its wind range, such as `Light 5 to 10 knots`.
    pub const fn label(self) -> &'static str {
        match self {
            PhrfBFactor::Calm => "Calm / Drifting 1 to 5 knots",
            PhrfBFactor::Light => "Light 5 to 10 knots",
            PhrfBFactor::Avg => "Average 10 to 15 knots",
            PhrfBFactor::Hvy => "Heavy 15 to 20+ knots",
        }
    }

    /// Returns the one-line description of when to use it, such as `Light air or all windward work`.
    pub const fn desc(self) -> &'static str {
        match self {
            PhrfBFactor::Calm => "Very light air",
            PhrfBFactor::Light => "Light air or all windward work",
            PhrfBFactor::Avg => "Medium racing conditions",
            PhrfBFactor::Hvy => "Heavy air or all off-the-wind sailing",
        }
    }

    /// Returns the B factor: 650, 600, 550 or 480.
    pub const fn cf(self) -> i32 {
        match self {
            PhrfBFactor::Calm => 650,
            PhrfBFactor::Light => 600,
            PhrfBFactor::Avg => 550,
            PhrfBFactor::Hvy => 480,
        }
    }

    /// Returns the named B factor nearest to `cf`.
    ///
    /// Each name covers from half-way to its lower neighbour up to half-way to its upper one (integer halves,
    /// an edge value going to the heavier name): `Calm` 626 and up, `Light` 576 to 625, `Avg` 516 to 575, `Hvy` 515
    /// and below.
    pub fn from_b_factor(cf: i32) -> PhrfBFactor {
        if cf <= PhrfBFactor::Hvy.cf() {
            return PhrfBFactor::Hvy;
        } else if cf >= 650 {
            return PhrfBFactor::Calm;
        }
        let array = PhrfBFactor::ENTRIES;
        let mut selected = PhrfBFactor::Light;
        for (index, factor) in array.iter().enumerate() {
            let (min, max) = if index == 0 {
                (
                    factor.cf() - (factor.cf() - array[index + 1].cf()) / 2,
                    factor.cf(),
                )
            } else if index == array.len() - 1 {
                (
                    factor.cf(),
                    factor.cf() + (array[index - 1].cf() - factor.cf()) / 2,
                )
            } else {
                (
                    factor.cf() - (factor.cf() - array[index + 1].cf()) / 2,
                    factor.cf() + (array[index - 1].cf() - factor.cf()) / 2,
                )
            };
            if cf <= max && cf >= min {
                selected = *factor;
            }
        }
        selected
    }
}

/// The wind band of the 3-band (triple number) ORC scoring options.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Orc3Band {
    /// 9 knots or less.
    Low,
    /// 9 to 14 knots.
    Medium,
    /// 14 knots or more.
    High,
}

impl Orc3Band {
    /// Every band, light to heavy.
    pub const ENTRIES: [Orc3Band; 3] = [Orc3Band::Low, Orc3Band::Medium, Orc3Band::High];

    /// Returns the serialized form, the variant name.
    pub const fn name(self) -> &'static str {
        match self {
            Orc3Band::Low => "Low",
            Orc3Band::Medium => "Medium",
            Orc3Band::High => "High",
        }
    }

    /// Returns the display name, the same as [`Orc3Band::name`].
    pub const fn label(self) -> &'static str {
        self.name()
    }

    /// Returns the wind range, such as `9 to 14 kts`.
    pub const fn desc(self) -> &'static str {
        match self {
            Orc3Band::Low => "9 kts or less",
            Orc3Band::Medium => "9 to 14 kts",
            Orc3Band::High => "14 kts or more",
        }
    }

    /// Returns the PHRF B factor that stands for this band (600, 550, 480); [`Orc3Band::from_phrf_b_factor`] maps it
    /// back.
    pub const fn phrf_b_factor(self) -> i32 {
        match self {
            Orc3Band::Low => 600,
            Orc3Band::Medium => 550,
            Orc3Band::High => 480,
        }
    }

    /// Returns the 3-band wind band a PHRF B factor maps to: 600 and up `Low`, 550 to 599 `Medium`, below 550 `High`.
    ///
    /// R4 (`docs/explore_implemented/orc_series_standings.md`): the B factor is the authority on the wind, so a race's
    /// 3-band band must be this one.
    pub fn from_phrf_b_factor(cf: i32) -> Orc3Band {
        if cf >= Orc3Band::Low.phrf_b_factor() {
            Orc3Band::Low
        } else if (Orc3Band::Medium.phrf_b_factor()..=Orc3Band::Low.phrf_b_factor()).contains(&cf) {
            Orc3Band::Medium
        } else {
            Orc3Band::High
        }
    }
}

/// The wind band of the 5-band ORC scoring options.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum Orc5Band {
    /// 7 knots or less.
    Low,
    /// 7 to 10 knots.
    LowMedium,
    /// 10 to 13 knots.
    Medium,
    /// 13 to 17 knots.
    MediumHigh,
    /// 17 knots or more.
    High,
}

impl Orc5Band {
    /// Every band, light to heavy.
    pub const ENTRIES: [Orc5Band; 5] = [
        Orc5Band::Low,
        Orc5Band::LowMedium,
        Orc5Band::Medium,
        Orc5Band::MediumHigh,
        Orc5Band::High,
    ];

    /// Returns the serialized form, the variant name.
    pub const fn name(self) -> &'static str {
        match self {
            Orc5Band::Low => "Low",
            Orc5Band::LowMedium => "LowMedium",
            Orc5Band::Medium => "Medium",
            Orc5Band::MediumHigh => "MediumHigh",
            Orc5Band::High => "High",
        }
    }

    /// Returns the display name, such as `Low/Medium`.
    pub const fn label(self) -> &'static str {
        match self {
            Orc5Band::Low => "Low",
            Orc5Band::LowMedium => "Low/Medium",
            Orc5Band::Medium => "Medium",
            Orc5Band::MediumHigh => "Medium/High",
            Orc5Band::High => "High",
        }
    }

    /// Returns the wind range, such as `7 to 10 kts`.
    pub const fn desc(self) -> &'static str {
        match self {
            Orc5Band::Low => "7 kts or less",
            Orc5Band::LowMedium => "7 to 10 kts",
            Orc5Band::Medium => "10 to 13 kts",
            Orc5Band::MediumHigh => "13 to 17 kts",
            Orc5Band::High => "17 kts or more",
        }
    }

    /// Returns the PHRF B factor that stands for this band (650, 600, 550, 510, 480);
    /// [`Orc5Band::from_phrf_b_factor`] maps it back.
    pub const fn phrf_b_factor(self) -> i32 {
        match self {
            Orc5Band::Low => 650,
            Orc5Band::LowMedium => 600,
            Orc5Band::Medium => 550,
            Orc5Band::MediumHigh => 510,
            Orc5Band::High => 480,
        }
    }

    /// Returns the 5-band wind band a PHRF B factor maps to.
    ///
    /// 650 and up `Low`, 600 to 649 `LowMedium`, 550 to 599 `Medium`, 510 to 549 `MediumHigh`, below 510 `High`. R4
    /// (`docs/explore_implemented/orc_series_standings.md`): the B factor is the authority on the wind, so a race's
    /// 5-band band must be this one.
    pub fn from_phrf_b_factor(cf: i32) -> Orc5Band {
        use Orc5Band::*;
        if cf >= Low.phrf_b_factor() {
            Low
        } else if (LowMedium.phrf_b_factor()..=Low.phrf_b_factor()).contains(&cf) {
            LowMedium
        } else if (Medium.phrf_b_factor()..=LowMedium.phrf_b_factor()).contains(&cf) {
            Medium
        } else if (MediumHigh.phrf_b_factor()..=Medium.phrf_b_factor()).contains(&cf) {
            MediumHigh
        } else {
            High
        }
    }
}

/// The ORC scoring option of a race: which of the certificate's time-on-time factors corrects the elapsed times.
///
/// The triple-number options also need an [`Orc3Band`] ([`OrcScoringOption::uses_3_band`]) and the five-band ones
/// an [`Orc5Band`] ([`OrcScoringOption::uses_5_band`]); the single-number options use one factor in all winds. New
/// races default to [`OrcScoringOption::FiveBandWindwardLeeward`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum OrcScoringOption {
    SingleNumberAllPurpose,
    SingleNumberWindwardLeeward,
    TripleNumberAllPurpose,
    TripleNumberWindwardLeeward,
    SingleNumberPredominantUpwind,
    SingleNumberPredominantReaching,
    SingleNumberPredominantDownwind,
    PredominantUpwind,
    PredominantDownwind,
    PredominantReaching,
    FiveBandWindwardLeeward,
    #[serde(rename = "WindwardLeeward60_40")]
    WindwardLeeward6040,
    FiveBandAllPurpose,
}

impl OrcScoringOption {
    /// Every option, in declaration order (the order the race configuration form lists them).
    pub const ENTRIES: [OrcScoringOption; 13] = [
        OrcScoringOption::SingleNumberAllPurpose,
        OrcScoringOption::SingleNumberWindwardLeeward,
        OrcScoringOption::TripleNumberAllPurpose,
        OrcScoringOption::TripleNumberWindwardLeeward,
        OrcScoringOption::SingleNumberPredominantUpwind,
        OrcScoringOption::SingleNumberPredominantReaching,
        OrcScoringOption::SingleNumberPredominantDownwind,
        OrcScoringOption::PredominantUpwind,
        OrcScoringOption::PredominantDownwind,
        OrcScoringOption::PredominantReaching,
        OrcScoringOption::FiveBandWindwardLeeward,
        OrcScoringOption::WindwardLeeward6040,
        OrcScoringOption::FiveBandAllPurpose,
    ];

    /// Returns the serialized form: the variant name, except `WindwardLeeward60_40`.
    pub const fn name(self) -> &'static str {
        use OrcScoringOption::*;
        match self {
            SingleNumberAllPurpose => "SingleNumberAllPurpose",
            SingleNumberWindwardLeeward => "SingleNumberWindwardLeeward",
            TripleNumberAllPurpose => "TripleNumberAllPurpose",
            TripleNumberWindwardLeeward => "TripleNumberWindwardLeeward",
            SingleNumberPredominantUpwind => "SingleNumberPredominantUpwind",
            SingleNumberPredominantReaching => "SingleNumberPredominantReaching",
            SingleNumberPredominantDownwind => "SingleNumberPredominantDownwind",
            PredominantUpwind => "PredominantUpwind",
            PredominantDownwind => "PredominantDownwind",
            PredominantReaching => "PredominantReaching",
            FiveBandWindwardLeeward => "FiveBandWindwardLeeward",
            WindwardLeeward6040 => "WindwardLeeward60_40",
            FiveBandAllPurpose => "FiveBandAllPurpose",
        }
    }

    /// Returns the display name, such as `5-Band Windward/Leeward`.
    ///
    /// `PredominantUpwind`'s label ends in a space (`Predominant Upwind `), and pages render it that way.
    pub const fn label(self) -> &'static str {
        use OrcScoringOption::*;
        match self {
            SingleNumberAllPurpose => "Single Number All Purpose",
            SingleNumberWindwardLeeward => "Single Number Windward/Leeward",
            TripleNumberAllPurpose => "Triple Number All Purpose",
            TripleNumberWindwardLeeward => "Triple Number Windward/Leeward",
            SingleNumberPredominantUpwind => "Single Number Predominant Upwind",
            SingleNumberPredominantReaching => "Single Number Predominant Reaching",
            SingleNumberPredominantDownwind => "Single Number Predominant Downwind",
            PredominantUpwind => "Predominant Upwind ",
            PredominantDownwind => "Predominant Downwind",
            PredominantReaching => "Predominant Reaching",
            FiveBandWindwardLeeward => "5-Band Windward/Leeward",
            WindwardLeeward6040 => "Windward/Leeward 60-40",
            FiveBandAllPurpose => "5-Band All Purpose",
        }
    }

    /// Returns whether the option picks its factor by [`Orc3Band`]: the two triple-number options.
    pub const fn uses_3_band(self) -> bool {
        matches!(
            self,
            OrcScoringOption::TripleNumberAllPurpose
                | OrcScoringOption::TripleNumberWindwardLeeward
        )
    }

    /// Returns whether the option picks its factor by [`Orc5Band`]: the predominant-course, 5-band and 60-40 ones.
    pub const fn uses_5_band(self) -> bool {
        use OrcScoringOption::*;
        matches!(
            self,
            PredominantUpwind
                | PredominantDownwind
                | PredominantReaching
                | FiveBandWindwardLeeward
                | WindwardLeeward6040
                | FiveBandAllPurpose
        )
    }

    /// Returns `"<label> - <desc>"` of the band the option scores with (`Low/Medium - 7 to 10 kts`), or `None` for
    /// the single-number options.
    pub fn orc_band_label(self, orc5_band: Orc5Band, orc3_band: Orc3Band) -> Option<String> {
        if self.uses_3_band() {
            Some(format!("{} - {}", orc3_band.label(), orc3_band.desc()))
        } else if self.uses_5_band() {
            Some(format!("{} - {}", orc5_band.label(), orc5_band.desc()))
        } else {
            None
        }
    }
}

/// The `rms` envelope of data.orc.org's RMS JSON; the other keys of the response are ignored.
#[derive(Debug, Clone, PartialEq, Default, Serialize, Deserialize)]
pub struct OrcResponse {
    #[serde(default)]
    pub rms: Vec<OrcCertificate>,
}

/// Returns the certificate a boat races under by default, or `None` without certificates.
///
/// A `CLUB` certificate wins over any other type; among certificates of the same type the earliest issue date
/// wins. Two certificates of different non-`CLUB` types compare equal and keep their input order.
pub fn find_preferred(certs: &[OrcCertificate]) -> Option<&OrcCertificate> {
    use std::cmp::Ordering;
    let compare = |lhs: &OrcCertificate, rhs: &OrcCertificate| -> Ordering {
        if lhs.c_type != rhs.c_type {
            if lhs.c_type == "CLUB" {
                Ordering::Less
            } else if rhs.c_type == "CLUB" {
                Ordering::Greater
            } else {
                Ordering::Equal
            }
        } else {
            lhs.issue_date.cmp(&rhs.issue_date)
        }
    };
    // The comparator is not a total order (different non-CLUB types compare equal, so it is not
    // transitive), and `sort_by` may panic on such a comparator; a stable insertion sort never does.
    let mut sorted: Vec<&OrcCertificate> = certs.iter().collect();
    for i in 1..sorted.len() {
        let mut j = i;
        while j > 0 && compare(sorted[j], sorted[j - 1]) == Ordering::Less {
            sorted.swap(j, j - 1);
            j -= 1;
        }
    }
    sorted.first().copied()
}

fn distant_future() -> Timestamp {
    DISTANT_FUTURE
}

/// An ORC certificate: ORC's RMS record with every US scoring option over 3 and 5 wind bands.
///
/// The serde keys are ORC's RMS keys verbatim, and every field may be left out on input. The `_tot` fields are
/// time-on-time factors: corrected time = elapsed time × factor, so a faster boat has a larger factor. The `_tod`
/// fields are time-on-distance allowances in seconds per nautical mile; they are stored but not used in scoring. A
/// race's [`OrcScoringOption`] and wind band pick the one `_tot` factor that scores it.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(default)]
pub struct OrcCertificate {
    /// ORC's file id of this certificate, the key of its public page (see [`OrcCertificate::certificate_url`]).
    ///
    /// Not part of the RMS data: it comes from ORC's active-certificate list, so it is `None` for certificates that
    /// were never resolved (for example ones that expired before the lookup existed). It is never written while
    /// `None`, so the JSON of such certificates matches the golden files.
    #[serde(rename = "dxtId", skip_serializing_if = "Option::is_none")]
    pub dxt_id: Option<i64>,
    /// The issuing national authority, such as `USA`.
    #[serde(rename = "NatAuth")]
    pub nat_auth: String,
    #[serde(rename = "BIN")]
    pub bin: String,
    #[serde(rename = "CertNo")]
    pub cert_no: String,
    /// ORC's reference number: the key a result stores as [`crate::RaceResult::orc_ref`] and the server fetches by
    /// (`GET /v1/api/orcCert?refNo=`).
    #[serde(rename = "RefNo")]
    pub ref_no: String,
    #[serde(rename = "SailNo")]
    pub sail_no: String,
    #[serde(rename = "YachtName")]
    pub yacht_name: String,
    #[serde(rename = "Class")]
    pub yacht_class: String,
    #[serde(rename = "Builder")]
    pub builder: String,
    #[serde(rename = "Designer")]
    pub designer: String,
    /// The certificate type, such as `CLUB` or `INTL`; [`find_preferred`] prefers `CLUB`.
    #[serde(rename = "C_Type")]
    pub c_type: String,
    #[serde(rename = "Division")]
    pub division: String,
    /// [`DISTANT_FUTURE`] when missing, so an undated certificate loses to a dated one in [`find_preferred`].
    #[serde(
        rename = "IssueDate",
        with = "kotlin_instant",
        default = "distant_future"
    )]
    pub issue_date: Timestamp,

    // Single Number All Purpose
    /// Time-on-distance allowance in seconds per mile: Single number all purpose, all winds.
    #[serde(rename = "APHD")]
    pub all_purpose_tod: f64,
    /// Time-on-time factor: Single number all purpose, all winds.
    #[serde(rename = "APHT")]
    pub all_purpose_tot: f64,

    // Single Number Windward/Leeward
    /// Time-on-distance allowance in seconds per mile: Single number windward/leeward, all winds.
    #[serde(rename = "ILCWA")]
    pub wl_single_number_tod: f64,
    /// Time-on-time factor: Single number windward/leeward, all winds.
    #[serde(rename = "TMF_Inshore")]
    pub wl_single_number_tot: f64,

    // Triple Number All Purpose (Low, Med, High)
    /// Time-on-distance allowance in seconds per mile: Triple number all purpose, Low band (9 kts or less).
    #[serde(rename = "TND_Offshore_Low")]
    pub triple_number_all_purpose_low_tod: f64,
    /// Time-on-time factor: Triple number all purpose, Low band (9 kts or less).
    #[serde(rename = "TN_Offshore_Low")]
    pub triple_number_all_purpose_low_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Triple number all purpose, Medium band (9 to 14 kts).
    #[serde(rename = "TND_Offshore_Medium")]
    pub triple_number_all_purpose_med_tod: f64,
    /// Time-on-time factor: Triple number all purpose, Medium band (9 to 14 kts).
    #[serde(rename = "TN_Offshore_Medium")]
    pub triple_number_all_purpose_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Triple number all purpose, High band (14 kts or more).
    #[serde(rename = "TND_Offshore_High")]
    pub triple_number_all_purpose_hi_tod: f64,
    /// Time-on-time factor: Triple number all purpose, High band (14 kts or more).
    #[serde(rename = "TN_Offshore_High")]
    pub triple_number_all_purpose_hi_tot: f64,

    // Triple Number Windward/Leeward (Low, Med, High)
    /// Time-on-time factor: Triple number windward/leeward, Low band (9 kts or less).
    #[serde(rename = "US_TN_Inshore_Low")]
    pub triple_number_wl_low_tot: f64,
    /// Time-on-time factor: Triple number windward/leeward, Medium band (9 to 14 kts).
    #[serde(rename = "TN_Inshore_Medium")]
    pub triple_number_wl_med_tot: f64,
    /// Time-on-time factor: Triple number windward/leeward, High band (14 kts or more).
    #[serde(rename = "TN_Inshore_High")]
    pub triple_number_wl_hi_tot: f64,

    // Single Number Predominant Upwind
    /// Time-on-time factor: Single number predominant upwind, all winds.
    #[serde(rename = "US_PREDUP_TOT")]
    pub single_number_predominant_upwind_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Single number predominant upwind, all winds.
    #[serde(rename = "US_PREDUP_TOD")]
    pub single_number_predominant_upwind_tod: f64,

    // Single Number Predominant Reaching
    /// Time-on-time factor: Single number predominant reaching, all winds.
    #[serde(rename = "US_PREDRC_TOT")]
    pub single_number_predominant_reaching_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Single number predominant reaching, all winds.
    #[serde(rename = "US_PREDRC_TOD")]
    pub single_number_predominant_reaching_tod: f64,

    // Single Number Predominant Downwind
    /// Time-on-time factor: Single number predominant downwind, all winds.
    #[serde(rename = "US_PREDDN_TOT")]
    pub single_number_predominant_downwind_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Single number predominant downwind, all winds.
    #[serde(rename = "US_PREDDN_TOD")]
    pub single_number_predominant_downwind_tod: f64,

    // Predominant Upwind (Low, Low/Med, Medium, Med/High, High)
    /// Time-on-time factor: Predominant upwind, Low band (7 kts or less).
    #[serde(rename = "US_PREDUP_L_TOT")]
    pub predominant_upwind_low_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant upwind, Low band (7 kts or less).
    #[serde(rename = "US_PREDUP_L_TOD")]
    pub predominant_upwind_low_tod: f64,
    /// Time-on-time factor: Predominant upwind, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_PREDUP_LM_TOT")]
    pub predominant_upwind_low_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant upwind, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_PREDUP_LM_TOD")]
    pub predominant_upwind_low_med_tod: f64,
    /// Time-on-time factor: Predominant upwind, Medium band (10 to 13 kts).
    #[serde(rename = "US_PREDUP_M_TOT")]
    pub predominant_upwind_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant upwind, Medium band (10 to 13 kts).
    #[serde(rename = "US_PREDUP_M_TOD")]
    pub predominant_upwind_med_tod: f64,
    /// Time-on-time factor: Predominant upwind, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_PREDUP_MH_TOT")]
    pub predominant_upwind_med_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant upwind, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_PREDUP_MH_TOD")]
    pub predominant_upwind_med_hi_tod: f64,
    /// Time-on-time factor: Predominant upwind, High band (17 kts or more).
    #[serde(rename = "US_PREDUP_H_TOT")]
    pub predominant_upwind_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant upwind, High band (17 kts or more).
    #[serde(rename = "US_PREDUP_H_TOD")]
    pub predominant_upwind_hi_tod: f64,

    // Predominant Downwind (Low, Low/Med, Medium, Med/High, High)
    /// Time-on-time factor: Predominant downwind, Low band (7 kts or less).
    #[serde(rename = "US_PREDDN_L_TOT")]
    pub predominant_downwind_low_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant downwind, Low band (7 kts or less).
    #[serde(rename = "US_PREDDN_L_TOD")]
    pub predominant_downwind_low_tod: f64,
    /// Time-on-time factor: Predominant downwind, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_PREDDN_LM_TOT")]
    pub predominant_downwind_low_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant downwind, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_PREDDN_LM_TOD")]
    pub predominant_downwind_low_med_tod: f64,
    /// Time-on-time factor: Predominant downwind, Medium band (10 to 13 kts).
    #[serde(rename = "US_PREDDN_M_TOT")]
    pub predominant_downwind_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant downwind, Medium band (10 to 13 kts).
    #[serde(rename = "US_PREDDN_M_TOD")]
    pub predominant_downwind_med_tod: f64,
    /// Time-on-time factor: Predominant downwind, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_PREDDN_MH_TOT")]
    pub predominant_downwind_med_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant downwind, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_PREDDN_MH_TOD")]
    pub predominant_downwind_med_hi_tod: f64,
    /// Time-on-time factor: Predominant downwind, High band (17 kts or more).
    #[serde(rename = "US_PREDDN_H_TOT")]
    pub predominant_downwind_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant downwind, High band (17 kts or more).
    #[serde(rename = "US_PREDDN_H_TOD")]
    pub predominant_downwind_hi_tod: f64,

    // Predominant Reaching (Low, Low/Med, Medium, Med/High, High)
    /// Time-on-time factor: Predominant reaching, Low band (7 kts or less).
    #[serde(rename = "US_PREDRC_L_TOT")]
    pub predominant_reaching_low_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant reaching, Low band (7 kts or less).
    #[serde(rename = "US_PREDRC_L_TOD")]
    pub predominant_reaching_low_tod: f64,
    /// Time-on-time factor: Predominant reaching, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_PREDRC_LM_TOT")]
    pub predominant_reaching_low_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant reaching, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_PREDRC_LM_TOD")]
    pub predominant_reaching_low_med_tod: f64,
    /// Time-on-time factor: Predominant reaching, Medium band (10 to 13 kts).
    #[serde(rename = "US_PREDRC_M_TOT")]
    pub predominant_reaching_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant reaching, Medium band (10 to 13 kts).
    #[serde(rename = "US_PREDRC_M_TOD")]
    pub predominant_reaching_med_tod: f64,
    /// Time-on-time factor: Predominant reaching, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_PREDRC_MH_TOT")]
    pub predominant_reaching_med_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant reaching, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_PREDRC_MH_TOD")]
    pub predominant_reaching_med_hi_tod: f64,
    /// Time-on-time factor: Predominant reaching, High band (17 kts or more).
    #[serde(rename = "US_PREDRC_H_TOT")]
    pub predominant_reaching_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Predominant reaching, High band (17 kts or more).
    #[serde(rename = "US_PREDRC_H_TOD")]
    pub predominant_reaching_hi_tod: f64,

    // 5-Band Windward/Leeward (Low, Low/Med, Medium, Med/High, High)
    /// Time-on-time factor: 5-band windward/leeward, Low band (7 kts or less).
    #[serde(rename = "US_5B_L_TOT")]
    pub five_band_wl_low_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band windward/leeward, Low band (7 kts or less).
    #[serde(rename = "US_5B_L_TOD")]
    pub five_band_wl_low_tod: f64,
    /// Time-on-time factor: 5-band windward/leeward, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_5B_LM_TOT")]
    pub five_band_wl_low_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band windward/leeward, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_5B_LM_TOD")]
    pub five_band_wl_low_med_tod: f64,
    /// Time-on-time factor: 5-band windward/leeward, Medium band (10 to 13 kts).
    #[serde(rename = "US_5B_M_TOT")]
    pub five_band_wl_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band windward/leeward, Medium band (10 to 13 kts).
    #[serde(rename = "US_5B_M_TOD")]
    pub five_band_wl_med_tod: f64,
    /// Time-on-time factor: 5-band windward/leeward, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_5B_MH_TOT")]
    pub five_band_wl_med_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band windward/leeward, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_5B_MH_TOD")]
    pub five_band_wl_med_hi_tod: f64,
    /// Time-on-time factor: 5-band windward/leeward, High band (17 kts or more).
    #[serde(rename = "US_5B_H_TOT")]
    pub five_band_wl_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band windward/leeward, High band (17 kts or more).
    #[serde(rename = "US_5B_H_TOD")]
    pub five_band_wl_hi_tod: f64,

    // Windward/Leeward 60-40 (Low, Low/Med, Medium, Med/High, High)
    /// Time-on-distance allowance in seconds per mile: Windward/leeward 60-40, Low band (7 kts or less).
    #[serde(rename = "US_WL6040_L_TOD")]
    pub us_wl6040_l_tod: f64,
    /// Time-on-time factor: Windward/leeward 60-40, Low band (7 kts or less).
    #[serde(rename = "US_WL6040_L_TOT")]
    pub us_wl6040_l_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Windward/leeward 60-40, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_WL6040_LM_TOD")]
    pub us_wl6040_lm_tod: f64,
    /// Time-on-time factor: Windward/leeward 60-40, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_WL6040_LM_TOT")]
    pub us_wl6040_lm_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Windward/leeward 60-40, Medium band (10 to 13 kts).
    #[serde(rename = "US_WL6040_M_TOD")]
    pub us_wl6040_m_tod: f64,
    /// Time-on-time factor: Windward/leeward 60-40, Medium band (10 to 13 kts).
    #[serde(rename = "US_WL6040_M_TOT")]
    pub us_wl6040_m_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Windward/leeward 60-40, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_WL6040_MH_TOD")]
    pub us_wl6040_mh_tod: f64,
    /// Time-on-time factor: Windward/leeward 60-40, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_WL6040_MH_TOT")]
    pub us_wl6040_mh_tot: f64,
    /// Time-on-distance allowance in seconds per mile: Windward/leeward 60-40, High band (17 kts or more).
    #[serde(rename = "US_WL6040_H_TOD")]
    pub us_wl6040_h_tod: f64,
    /// Time-on-time factor: Windward/leeward 60-40, High band (17 kts or more).
    #[serde(rename = "US_WL6040_H_TOT")]
    pub us_wl6040_h_tot: f64,

    // 5-Band All Purpose (Low, Low/Med, Medium, Med/High, High)
    /// Time-on-time factor: 5-band all purpose, Low band (7 kts or less).
    #[serde(rename = "US_AP_L_TOT")]
    pub five_band_all_purpose_low_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band all purpose, Low band (7 kts or less).
    #[serde(rename = "US_AP_L_TOD")]
    pub five_band_all_purpose_low_tod: f64,
    /// Time-on-time factor: 5-band all purpose, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_AP_LM_TOT")]
    pub five_band_all_purpose_low_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band all purpose, Low/Medium band (7 to 10 kts).
    #[serde(rename = "US_AP_LM_TOD")]
    pub five_band_all_purpose_low_med_tod: f64,
    /// Time-on-time factor: 5-band all purpose, Medium band (10 to 13 kts).
    #[serde(rename = "US_AP_M_TOT")]
    pub five_band_all_purpose_med_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band all purpose, Medium band (10 to 13 kts).
    #[serde(rename = "US_AP_M_TOD")]
    pub five_band_all_purpose_med_tod: f64,
    /// Time-on-time factor: 5-band all purpose, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_AP_MH_TOT")]
    pub five_band_all_purpose_med_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band all purpose, Medium/High band (13 to 17 kts).
    #[serde(rename = "US_AP_MH_TOD")]
    pub five_band_all_purpose_med_hi_tod: f64,
    /// Time-on-time factor: 5-band all purpose, High band (17 kts or more).
    #[serde(rename = "US_AP_H_TOT")]
    pub five_band_all_purpose_hi_tot: f64,
    /// Time-on-distance allowance in seconds per mile: 5-band all purpose, High band (17 kts or more).
    #[serde(rename = "US_AP_H_TOD")]
    pub five_band_all_purpose_hi_tod: f64,
}

impl Default for OrcCertificate {
    /// Creates an empty certificate: empty strings, `0.0` factors, no file id and an issue date of
    /// [`DISTANT_FUTURE`].
    fn default() -> Self {
        OrcCertificate {
            dxt_id: None,
            nat_auth: String::new(),
            bin: String::new(),
            cert_no: String::new(),
            ref_no: String::new(),
            sail_no: String::new(),
            yacht_name: String::new(),
            yacht_class: String::new(),
            builder: String::new(),
            designer: String::new(),
            c_type: String::new(),
            division: String::new(),
            issue_date: DISTANT_FUTURE,
            all_purpose_tod: 0.0,
            all_purpose_tot: 0.0,
            wl_single_number_tod: 0.0,
            wl_single_number_tot: 0.0,
            triple_number_all_purpose_low_tod: 0.0,
            triple_number_all_purpose_low_tot: 0.0,
            triple_number_all_purpose_med_tod: 0.0,
            triple_number_all_purpose_med_tot: 0.0,
            triple_number_all_purpose_hi_tod: 0.0,
            triple_number_all_purpose_hi_tot: 0.0,
            triple_number_wl_low_tot: 0.0,
            triple_number_wl_med_tot: 0.0,
            triple_number_wl_hi_tot: 0.0,
            single_number_predominant_upwind_tot: 0.0,
            single_number_predominant_upwind_tod: 0.0,
            single_number_predominant_reaching_tot: 0.0,
            single_number_predominant_reaching_tod: 0.0,
            single_number_predominant_downwind_tot: 0.0,
            single_number_predominant_downwind_tod: 0.0,
            predominant_upwind_low_tot: 0.0,
            predominant_upwind_low_tod: 0.0,
            predominant_upwind_low_med_tot: 0.0,
            predominant_upwind_low_med_tod: 0.0,
            predominant_upwind_med_tot: 0.0,
            predominant_upwind_med_tod: 0.0,
            predominant_upwind_med_hi_tot: 0.0,
            predominant_upwind_med_hi_tod: 0.0,
            predominant_upwind_hi_tot: 0.0,
            predominant_upwind_hi_tod: 0.0,
            predominant_downwind_low_tot: 0.0,
            predominant_downwind_low_tod: 0.0,
            predominant_downwind_low_med_tot: 0.0,
            predominant_downwind_low_med_tod: 0.0,
            predominant_downwind_med_tot: 0.0,
            predominant_downwind_med_tod: 0.0,
            predominant_downwind_med_hi_tot: 0.0,
            predominant_downwind_med_hi_tod: 0.0,
            predominant_downwind_hi_tot: 0.0,
            predominant_downwind_hi_tod: 0.0,
            predominant_reaching_low_tot: 0.0,
            predominant_reaching_low_tod: 0.0,
            predominant_reaching_low_med_tot: 0.0,
            predominant_reaching_low_med_tod: 0.0,
            predominant_reaching_med_tot: 0.0,
            predominant_reaching_med_tod: 0.0,
            predominant_reaching_med_hi_tot: 0.0,
            predominant_reaching_med_hi_tod: 0.0,
            predominant_reaching_hi_tot: 0.0,
            predominant_reaching_hi_tod: 0.0,
            five_band_wl_low_tot: 0.0,
            five_band_wl_low_tod: 0.0,
            five_band_wl_low_med_tot: 0.0,
            five_band_wl_low_med_tod: 0.0,
            five_band_wl_med_tot: 0.0,
            five_band_wl_med_tod: 0.0,
            five_band_wl_med_hi_tot: 0.0,
            five_band_wl_med_hi_tod: 0.0,
            five_band_wl_hi_tot: 0.0,
            five_band_wl_hi_tod: 0.0,
            us_wl6040_l_tod: 0.0,
            us_wl6040_l_tot: 0.0,
            us_wl6040_lm_tod: 0.0,
            us_wl6040_lm_tot: 0.0,
            us_wl6040_m_tod: 0.0,
            us_wl6040_m_tot: 0.0,
            us_wl6040_mh_tod: 0.0,
            us_wl6040_mh_tot: 0.0,
            us_wl6040_h_tod: 0.0,
            us_wl6040_h_tot: 0.0,
            five_band_all_purpose_low_tot: 0.0,
            five_band_all_purpose_low_tod: 0.0,
            five_band_all_purpose_low_med_tot: 0.0,
            five_band_all_purpose_low_med_tod: 0.0,
            five_band_all_purpose_med_tot: 0.0,
            five_band_all_purpose_med_tod: 0.0,
            five_band_all_purpose_med_hi_tot: 0.0,
            five_band_all_purpose_med_hi_tod: 0.0,
            five_band_all_purpose_hi_tot: 0.0,
            five_band_all_purpose_hi_tod: 0.0,
        }
    }
}

impl OrcCertificate {
    /// Returns `"<yacht name> <ref no> <type> <division>"`, the certificate's text in the rating pickers.
    pub fn label(&self) -> String {
        format!(
            "{} {} {} {}",
            self.yacht_name, self.ref_no, self.c_type, self.division
        )
    }

    /// Returns the public page of this certificate, or `None` when [`OrcCertificate::dxt_id`] was never resolved.
    pub fn certificate_url(&self) -> Option<String> {
        orc_certificate_url(self.dxt_id)
    }

    /// Returns an equivalent PHRF rating, in seconds per mile, for placing an ORC boat in a bracket.
    ///
    /// `500 / TOT - 550` from the 5-band windward/leeward Low/Medium factor, rounded by
    /// [`crate::utils::round_to_long`] (ties toward positive infinity) and clamped to `i32`. A certificate whose
    /// factor is 0 (the default) gives `i32::MAX`. Only bracket placement uses it; scoring uses the ORC factors
    /// themselves.
    pub fn virtual_phrf(&self) -> i32 {
        /*
        Coefficient A (The Numerator)
        Purpose: A is a constant used to scale the resulting TCF so that a "middle-of-the-fleet" boat has a multiplier near 1.000.
        Impact: Changing the A coefficient does not change the finishing order of the race; it only affects the margins between boats.
        Standard Value: The most commonly used value for A is 650.
         */
        let a = 500.0;

        /*
        Coefficient B (The Denominator)
        480: Heavy air (15+ knots) or all off-the-wind sailing.
        550: Average/Moderate conditions (10–15 knots).
        600: Light air (5–10 knots) or all windward work.
        650: Very light air/Calm (1–5 knots).
         */
        let b = 550.0;

        /*
        Low("Low", "7 kts or less"),
        LowMedium("Low/Medium", "7 to 10 kts"),
        Medium("Medium", "10 to 13 kts"),
        MediumHigh("Medium/High", "13 to 17 kts"),
        High("High", "17 kts or more");
         */
        let tcf = self.five_band_wl_low_med_tot;

        /*
        PHRF formula "tcf = a / ( b + phrf)"
         */
        let phrf = (a / tcf) - b;

        crate::utils::round_to_long(phrf).clamp(i32::MIN as i64, i32::MAX as i64) as i32
    }
}

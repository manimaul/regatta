//! How a boat's race ended, and what each finish code scores.

use serde::{Deserialize, Serialize};

/// How a boat's race ended, stored and serialized by [`FinishCode::name`] in `raceresults.finish_code`.
///
/// Scores follow the low-point system of GSI 14.1 with the definitions of D3
/// (`docs/explore_implemented/test_audit_and_plan.md`): *finishers* are `TIME` and `HOC` entries, *starters* every
/// entry except `DNS` and `DNS_RC`, and a boat's own score counts the *other* starters. Only `TIME` and `HOC`
/// entries can take a penalty, D4 (same document).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum FinishCode {
    /// Finished with a time: placed by corrected time.
    #[serde(rename = "TIME")]
    Time,
    /// A finisher without a usable time, ranked after every timed finisher by its
    /// [`crate::RaceResult::hoc_position`], its position on the course.
    #[serde(rename = "HOC")]
    Hoc,
    /// Retired: finishers + 1 (GSI 14.1.1).
    #[serde(rename = "RET")]
    Ret,
    /// Did not finish: finishers + 1 (GSI 14.1.1).
    #[serde(rename = "DNF")]
    Dnf,
    /// Started but did not sail the course: other starters + 1, the same as DNS (GSI 14.1.4, D3).
    #[serde(rename = "NSC")]
    Nsc,
    /// Did not start (GSI 1.2 late entry, 8.6 started too late): starters + 1, the same as no record (D6).
    #[serde(rename = "DNS")]
    Dns,
    /// Disqualified (GSI 4.3-4.5, 6.4, 6.5, 8.9, 12.3.4): other starters + 2 (GSI 14.1.5, D6).
    #[serde(rename = "DSQ")]
    Dsq,
    /// RC volunteer (GSI 1.4.1.2): scored in the series standings as the average of the boat's other races, D1
    /// (`docs/explore_implemented/test_audit_and_plan.md`); not a starter.
    #[serde(rename = "DNS_RC")]
    DnsRc,
}

impl FinishCode {
    /// Every finish code, in declaration order.
    pub const ENTRIES: [FinishCode; 8] = [
        FinishCode::Time,
        FinishCode::Hoc,
        FinishCode::Ret,
        FinishCode::Dnf,
        FinishCode::Nsc,
        FinishCode::Dns,
        FinishCode::Dsq,
        FinishCode::DnsRc,
    ];

    /// Returns the stored and serialized form (`TIME`, `DNS_RC`).
    pub const fn name(self) -> &'static str {
        match self {
            FinishCode::Time => "TIME",
            FinishCode::Hoc => "HOC",
            FinishCode::Ret => "RET",
            FinishCode::Dnf => "DNF",
            FinishCode::Nsc => "NSC",
            FinishCode::Dns => "DNS",
            FinishCode::Dsq => "DSQ",
            FinishCode::DnsRc => "DNS_RC",
        }
    }

    /// Returns the sort weight that orders the non-finishers in a race report, lowest first.
    ///
    /// D6 (`docs/explore_implemented/test_audit_and_plan.md`): `TIME` 0, `HOC` / `RET` / `DNF` 1, `NSC` 2, `DNS` 3,
    /// `DSQ` 4, `DNS_RC` 5.
    pub const fn weight(self) -> i32 {
        match self {
            FinishCode::Time => 0,
            FinishCode::Hoc | FinishCode::Ret | FinishCode::Dnf => 1,
            FinishCode::Nsc => 2,
            FinishCode::Dns => 3,
            FinishCode::Dsq => 4,
            FinishCode::DnsRc => 5,
        }
    }

    /// Returns whether the entry started the race: every code except `DNS_RC` and `DNS`.
    ///
    /// GSI 14.1 / D3 (`docs/explore_implemented/test_audit_and_plan.md`); the starter count sets the DNS, NSC and DSQ
    /// scores.
    pub const fn is_starter(self) -> bool {
        !matches!(self, FinishCode::DnsRc | FinishCode::Dns)
    }

    /// Returns whether the entry finished the race: `TIME` and `HOC`.
    ///
    /// GSI 14.1 / D3 (`docs/explore_implemented/test_audit_and_plan.md`); the finisher count sets the DNF and RET
    /// scores.
    pub const fn is_finisher(self) -> bool {
        matches!(self, FinishCode::Time | FinishCode::Hoc)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn names_round_trip_through_json() {
        for code in FinishCode::ENTRIES {
            let json = serde_json::to_string(&code).unwrap();
            assert_eq!(json, format!("\"{}\"", code.name()));
            assert_eq!(serde_json::from_str::<FinishCode>(&json).unwrap(), code);
        }
        assert_eq!(
            serde_json::to_string(&FinishCode::DnsRc).unwrap(),
            "\"DNS_RC\""
        );
    }

    #[test]
    fn weights_starters_finishers() {
        assert_eq!(FinishCode::Time.weight(), 0);
        assert_eq!(FinishCode::DnsRc.weight(), 5);
        assert!(FinishCode::Hoc.is_starter() && FinishCode::Hoc.is_finisher());
        assert!(FinishCode::Dnf.is_starter() && !FinishCode::Dnf.is_finisher());
        assert!(!FinishCode::Dns.is_starter() && !FinishCode::DnsRc.is_starter());
    }
}

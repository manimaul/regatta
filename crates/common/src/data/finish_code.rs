//! = `FinishCode.kt`.

use serde::{Deserialize, Serialize};

/// How a boat's race ended. The weight orders the non-finishers in a race report (D6).
/// Stored by name in `raceresults.finish_code`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum FinishCode {
    #[serde(rename = "TIME")]
    Time,
    #[serde(rename = "HOC")]
    Hoc,
    #[serde(rename = "RET")]
    Ret,
    #[serde(rename = "DNF")]
    Dnf,
    #[serde(rename = "NSC")]
    Nsc,
    /// Did not start (GSI 1.2 late entry, 8.6 started too late): starters + 1, the same as no record.
    #[serde(rename = "DNS")]
    Dns,
    /// Disqualified (GSI 4.3-4.5, 6.4, 6.5, 8.9, 12.3.4): starters + 2.
    #[serde(rename = "DSQ")]
    Dsq,
    /// RC volunteer (GSI 1.4.1.2): scored in the series standings as the average of the boat's other races.
    #[serde(rename = "DNS_RC")]
    DnsRc,
}

impl FinishCode {
    /// Kotlin `entries`, in declaration order.
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

    /// Kotlin `name`, the stored and serialized form.
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

    /// The weight orders the non-finishers in a race report (D6).
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

    /// GSI 14.1 / D3: every entry except an RC volunteer or a DNS started the race.
    pub const fn is_starter(self) -> bool {
        !matches!(self, FinishCode::DnsRc | FinishCode::Dns)
    }

    /// GSI 14.1 / D3: timed finishers and HOC boats are finishers.
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

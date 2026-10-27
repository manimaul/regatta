//! = the Kotlin `utils/` package (the pure parts) plus the browser shims the components need.

pub mod async_state;
pub mod browser;
pub mod date_util;
pub mod light_sector;
pub mod race_config;
pub mod race_report;
pub mod route_matcher;
pub mod standings_report;
pub mod string_util;

/// `itemNameContainsFilter` of `RgDropdown.kt`: every whitespace-separated word of the filter is
/// tried case-insensitively; a blank filter matches everything.
pub fn item_name_contains_filter(item_name: &str, filter: &str) -> bool {
    let filter = filter.trim();
    if filter.is_empty() {
        return true;
    }
    let lower = item_name.to_lowercase();
    filter
        .split(' ')
        .any(|word| lower.contains(&word.to_lowercase()))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn filter_words() {
        assert!(item_name_contains_filter("Zulu Tango", "zulu"));
        assert!(item_name_contains_filter("Zulu Tango", "  "));
        assert!(item_name_contains_filter("Grace E", "kahuna grace"));
        assert!(!item_name_contains_filter("Kahuna", "zulu"));
    }
}

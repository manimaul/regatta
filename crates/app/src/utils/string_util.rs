//! = `StringUtil.kt`.

/// Keeps the digits of a string (at most `max_len` of them), with a leading minus sign when the
/// string started with one.
pub fn digits(s: &str, max_len: usize) -> String {
    let negative = s.starts_with('-');
    let digits: String = s.chars().filter(|c| c.is_ascii_digit()).collect();
    let digits = if digits.len() > max_len {
        digits[..max_len].to_string()
    } else {
        digits
    };
    if negative {
        format!("-{digits}")
    } else {
        digits
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn keeps_digits_and_sign() {
        assert_eq!(digits("12a3", 4), "123");
        assert_eq!(digits("-12a3456", 4), "-1234");
        assert_eq!(digits("-", 3), "-");
        assert_eq!(digits("", 3), "");
    }
}

package utils

val notDigit = Regex("[^0-9]")

fun String.digits(maxLen: Int): String {
    val negative = if (length > 0 && get(0) == '-') {
        true
    } else {
        false
    }
    return replace(notDigit, "").let{ digits ->
        when {
            negative && (digits.length > maxLen) -> "-${digits.substring(0, maxLen)}"
            digits.length > maxLen -> digits.substring(0, maxLen)
            negative -> "-$digits"
            else -> digits
        }
    }
}
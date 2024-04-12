package utils

import kotlinx.datetime.*

fun Int.doubleDigit(): String {
    return "$this".padStart(2, '0')
}

fun Int.quadDigit(): String {
    return "$this".padStart(4, '0')
}

fun LocalDateTime.timeStr() = "${hour.doubleDigit()}:${minute.doubleDigit()}:${second.doubleDigit()}"

fun LocalDateTime.dateStr() = "${monthNumber}/${dayOfMonth}/${year}"

fun LocalDateTime.inputStr() = "${year.quadDigit()}-${monthNumber.doubleDigit()}-${dayOfMonth.doubleDigit()}"

fun LocalDateTime.instant() = toInstant(TimeZone.currentSystemDefault())

fun Instant.localDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())

fun getClockValue(): String {
    return now().localDateTime().timeStr()
}

fun now(): Instant {
    return Clock.System.now()
}

fun currentYear(): String {
    return now().year()
}

fun Instant.dateStr(): String {
    return localDateTime().dateStr()
}

fun Instant.timeStr(): String {
    return localDateTime().timeStr()
}

fun Instant.display(): String {
    return localDateTime().let {
        "${it.dateStr()} ${it.timeStr()} "
    }
}

fun Instant.year(): String {
    return localDateTime().let {
        "${it.year}"
    }
}

fun String.datePickerInstant(): LocalDateTime? {
    return split("/").takeIf { it.size == 2 }?.let {
        val month = it[0].toInt()
        val day = it[1].toInt()
        val year = it[2].toInt()
        LocalDateTime(
            year,
            month,
            day,
            0,
            0,
            0,
        )
    }
}

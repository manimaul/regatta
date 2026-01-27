package utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.*
import kotlin.time.Instant

fun Int.doubleDigit(): String {
    return "$this".padStart(2, '0')
}

fun Int.quadDigit(): String {
    return "$this".padStart(4, '0')
}

fun LocalDateTime.timeStr() = "${hour.doubleDigit()}:${minute.doubleDigit()}:${second.doubleDigit()}"

fun LocalDateTime.dateStr() = "${month.number}/${day}/${year}"

fun LocalDateTime.inputStr() = "${year.quadDigit()}-${month.number.doubleDigit()}-${day.doubleDigit()}"

fun LocalDateTime.instant() = Instant.fromEpochSeconds(toInstant(TimeZone.currentSystemDefault()).epochSeconds)

fun Instant.localDateTime(): LocalDateTime = toLocalDateTime(TimeZone.currentSystemDefault())

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

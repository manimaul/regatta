package utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.internal.JSJoda.DateTimeFormatter
import kotlinx.datetime.toJSDate
import kotlinx.datetime.toKotlinInstant
import kotlin.js.Date

//https://js-joda.github.io/js-joda/manual/formatting.html
fun Instant.formattedDateString(addTime: Boolean): String {
    val date = kotlinx.datetime.internal.JSJoda.LocalDateTime.ofInstant(
        kotlinx.datetime.internal.JSJoda.Instant.ofEpochMilli(toEpochMilliseconds())
    )
    return if (addTime) {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)
    } else {
        DateTimeFormatter.ISO_LOCAL_DATE.format(date)
    }
}

fun getClockValue(): String {
    val now = Clock.System.now()
    return now.toJSDate().toLocaleTimeString()
}
fun Instant.display(): String {
    return toJSDate().let {
        "${it.toLocaleDateString()} ${it.toLocaleTimeString()}"
    }
}

fun String.datePickerInstant() : Instant {
    return Date(this).toKotlinInstant()
}
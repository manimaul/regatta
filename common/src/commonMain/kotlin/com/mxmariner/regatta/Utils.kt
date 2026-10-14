package com.mxmariner.regatta

import kotlin.math.roundToLong
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun String.versionedApi(): String {
    return "/v1/api/$this"
}

const val correctionFactorDefault = 600
const val ratingDefault = 200

fun String.versionedApi(version: Int = 1, params: Map<String, String>? = null): String {
    val paramString = params?.let {
        StringBuilder().apply {
            it.onEachIndexed { index, entry ->
                if (index == 0) {
                    append('?')
                } else {
                    append('&')
                }
                append(entry.key)
                append('=')
                append(entry.value)
            }
        }.toString()
    } ?: ""
    return "/v$version/api/$this$paramString"
}

/** A series score as a label: whole numbers without a decimal ("4"), others with one or two ("3.5", "3.33"). */
fun Double.scoreText(): String {
    val rounded = (this * 100).roundToLong() / 100.0
    val whole = rounded.roundToLong()
    if (rounded == whole.toDouble()) {
        return whole.toString()
    }
    return rounded.toString().trimEnd('0').trimEnd('.')
}

/** "1h:2m:3s", rounded to the nearest second (D7). Places still use millisecond precision. */
fun kotlin.time.Duration.display(): String {
    val totalSeconds = (inWholeMilliseconds / 1000.0).roundToLong()
    val hrs = totalSeconds / 3600
    val min = totalSeconds % 3600 / 60
    val sec = totalSeconds % 60
    return "${hrs}h:${min}m:${sec}s"
}
inline fun <T> List<T>.moveItem(up: Boolean = false, predicate: (T) -> Boolean): List<T> {
    val i = indexOfFirst { predicate(it) }
    val lst = toMutableList()
    if (up) {
        if (i > 0) {
            lst[i - 1] = this[i]
            lst[i] = this[i - 1]
        }
    } else {
        if (i >= 0 && i < size - 1) {
            lst[i + 1] = this[i]
            lst[i] = this[i + 1]
        }
    }
    return lst
}

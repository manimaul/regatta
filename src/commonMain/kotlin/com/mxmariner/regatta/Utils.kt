package com.mxmariner.regatta

import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun String.versionedApi(): String {
    return "/v1/api/$this"
}

const val correctionFactorDefault = 600
const val ratingDefault = 200.0F

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
fun kotlin.time.Duration.display(): String {
    var t = this
    val hrs = t.inWholeHours
    t -= hrs.toDuration(DurationUnit.HOURS)
    val min = t.inWholeMinutes
    t -= min.toDuration(DurationUnit.MINUTES)
    val sec = t.inWholeSeconds
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

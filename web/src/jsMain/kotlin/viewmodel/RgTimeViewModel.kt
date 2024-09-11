package viewmodel

import kotlinx.datetime.*
import utils.instant
import utils.localDateTime
import utils.timeStr
import utils.now


data class RgTimeState(
    val localTime: LocalDateTime = now().localDateTime(),
    val timeStr: String = localTime.timeStr(),
) : VmState

class RgTimeViewModel(
    instant: Instant,
    val seconds: Boolean
) : BaseViewModel<RgTimeState>(RgTimeState(instant.localDateTime().let {
    if (seconds) it else LocalDateTime(it.year, it.month, it.dayOfMonth, it.hour, it.minute, 0)

})) {
    override fun reload() {}

    fun setInstant(instant: Instant) {
        setState {
            copy(
                localTime = instant.localDateTime().let {
                    if (seconds) it else LocalDateTime(it.year, it.month, it.dayOfMonth, it.hour, it.minute, 0)
                }
            )
        }
    }

    fun setInputDate(inputStr: String): Instant? {
        inputStr.split("-").takeIf { it.size == 3 }?.let {
            val y = it[0].toIntOrNull() ?: 0
            val m = it[1].toIntOrNull()
            val d = it[2].toIntOrNull()
            println("y: $y, m: $m, d: $d")
            if (m != null && d != null) {
                return setInputDate(m, d, y)
            }
        }
        return null
    }


    fun setInputDate(month: Int, day: Int, year: Int): Instant? {
        if (month in 1..12 && day in 1..31 && year >= 0) {
            return withState { state ->
                val localTime = LocalDateTime(
                    year, month, day,
                    state.localTime.hour,
                    state.localTime.minute,
                    state.localTime.second,
                )
                val instant = localTime.instant()
                setState {
                    copy(
                        timeStr = localTime.timeStr(),
                        localTime = localTime,
                    )
                }
                instant
            }
        } else {
            return null
        }
    }

    fun setInputHour(value: Number?): Instant? {
        return value?.toInt()?.takeIf { it in 0..23 }?.let { hour ->
            withState { state ->
                val localTime = LocalDateTime(
                    state.localTime.year,
                    state.localTime.monthNumber,
                    state.localTime.dayOfMonth,
                    hour,
                    state.localTime.minute,
                    state.localTime.second,
                )
                val instant = localTime.instant()
                setState {
                    copy(
                        timeStr = localTime.timeStr(),
                        localTime = localTime,
                    )
                }
                instant
            }
        }
    }

    fun setInputMinute(value: Number?): Instant? {
        return value?.toInt()?.takeIf { it in 0..59 }?.let { minute ->
            withState { state ->
                val localTime = LocalDateTime(
                    state.localTime.year,
                    state.localTime.monthNumber,
                    state.localTime.dayOfMonth,
                    state.localTime.hour,
                    minute,
                    state.localTime.second,
                )
                val instant = localTime.instant()
                setState {
                    copy(
                        timeStr = localTime.timeStr(),
                        localTime = localTime,
                    )
                }
                instant
            }
        }
    }

    fun setInputSecond(value: Number?): Instant? {
        return value?.toInt()?.takeIf { it in 0..59 }?.let { second ->
            withState { state ->
                val localTime = LocalDateTime(
                    state.localTime.year,
                    state.localTime.monthNumber,
                    state.localTime.dayOfMonth,
                    state.localTime.hour,
                    state.localTime.minute,
                    second,
                )
                val instant = localTime.instant()
                setState {
                    copy(
                        timeStr = localTime.timeStr(),
                        localTime = localTime,
                    )
                }
                instant
            }
        }
    }
}

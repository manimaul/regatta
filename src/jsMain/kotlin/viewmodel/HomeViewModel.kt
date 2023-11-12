package viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


data class HomeState(
    val clock: String = getClockValue(),
    val expires: String = loginExpires(),
) : VmState

private fun getClockValue(): String {
    val now = Clock.System.now()
    return now.toJSDate().toLocaleTimeString()
}

private fun loginExpires(): String {
    return loginViewModel.flow.value.login?.expires?.let {
        var remaining = it.minus(Clock.System.now())
        val days = remaining.inWholeDays
        remaining = remaining.minus(days.days)
        val hours = remaining.inWholeHours
        remaining = remaining.minus(hours.hours)
        val minutes = remaining.inWholeMinutes
        remaining = remaining.minus(minutes.minutes)
        val seconds = remaining.inWholeSeconds
        "$days days, $hours hours, $minutes minutes, $seconds seconds"
    } ?: ""
}

class HomeViewModel(
    val loginVm: LoginViewModel = loginViewModel
) : BaseViewModel<HomeState>(HomeState()) {

    init {
        launch(Dispatchers.Unconfined) {
            while (true) {
                delay(250)
                setState {
                    HomeState(
                        clock = getClockValue(),
                        expires = loginExpires()
                    )
                }
            }
        }
    }
}
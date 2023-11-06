package viewmodel

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import utils.Scopes.mainScope
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


data class HomeState(
    val clock: String,
    val expires: String,
)

class HomeViewModel(
    val loginVm: LoginViewModel = loginViewModel,
    val routeVm: RouteViewModel = routeViewModel
) {
    private val homeState = mutableStateOf(
        HomeState(
            clock = getClockValue(),
            expires = loginExpires()
        )
    )

    val state: HomeState
        get() = homeState.value

    private fun getClockValue(): String {
        val now = Clock.System.now()
        return now.toJSDate().toLocaleTimeString()
    }

    private fun loginExpires(): String {
        return loginVm.loginResponse?.expires?.let {
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

    init {
        mainScope.launch(Dispatchers.Unconfined) {
            while (true) {
                delay(250)
                homeState.value = HomeState(
                    clock = getClockValue(),
                    expires = loginExpires()
                )
            }
        }
    }
}
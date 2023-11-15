package viewmodel

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.LoginResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import utils.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class LoginStatus {
    Loading,
    Ready,
    Complete,
    LoggedIn,
    Failed,
}

private fun initialState(): LoginState {
    val savedLogin = localStoreGet<LoginResponse>()?.takeIf { !it.isExpired() }
    return LoginState(
        login = savedLogin,
        loginStatus = savedLogin?.let { LoginStatus.LoggedIn } ?: LoginStatus.Ready
    )
}

data class LoginState(
    val auth: AuthRecord = AuthRecord(hash = "", userName = localStoreGet("username") ?: ""),
    val pass: String = "",
    val loginStatus: LoginStatus = LoginStatus.Ready,
    val errorMessage: String? = null,
    val login: LoginResponse? = localStoreGet<LoginResponse>()
) : VmState

private fun getClockValue(): String {
    val now = Clock.System.now()
    return now.toJSDate().toLocaleTimeString()
}

private fun loginExpires(login: LoginResponse?): String {
    return login?.expires?.let {
        var remaining = it.minus(Clock.System.now())
        val days = remaining.inWholeDays
        remaining = remaining.minus(days.days)
        val hours = remaining.inWholeHours
        remaining = remaining.minus(hours.hours)
        val minutes = remaining.inWholeMinutes
        remaining = remaining.minus(minutes.minutes)
        val seconds = remaining.inWholeSeconds
        if (remaining.isNegative()) {
            loginViewModel.logout(3000)
            "EXPIRED"
        } else {
            "$days days, $hours hours, $minutes minutes, $seconds seconds"
        }
    } ?: ""
}

data class ClockState(
    val display: String = "",
    val expiresDisplay: String = "",
) : VmState

val loginViewModel = LoginViewModel()

class LoginViewModel : BaseViewModel<LoginState>(initialState()) {
    private val clockState = MutableStateFlow(ClockState())
    val clockFlow: StateFlow<ClockState>
        get() = clockState

    init {
        launch {
            while (true) {
                delay(250)
                clockState.setState {
                    ClockState(getClockValue(), loginExpires(flow.value.login))
                }
            }
        }
    }
    fun setPassword(value: String) {
        setState {
            copy(
                auth = AuthRecord(
                    hash = value.takeIf { it.isNotEmpty() }?.let { hash(value) } ?: "",
                    userName = flow.value.auth.userName
                ),
                pass = value
            )
        }
    }

    fun submitNew() {
        setState {
            copy(loginStatus = LoginStatus.Loading)
        }
        launch {
            val response = Api.postAuth(flow.value.auth)
            response.body?.let {
                setState {
                    copy(
                        auth = it,
                        loginStatus = LoginStatus.Complete
                    )
                }
            } ?: setState {
                copy(
                    loginStatus = LoginStatus.Failed,
                    errorMessage = "${response.status} ${response.statusText}",
                )
            }
            delay(3000)
            setState {
                initialState()
            }
        }
    }

    fun logout(pause: Long? = null) {
        setState {
            pause?.let { delay(it) }
            localStoreSet("username", "")
            localStoreSet<LoginResponse>(null)
            val state = initialState()
            routeViewModel.setRoute(Route.Home)
            state
        }
    }

    fun login() {
        setState {
            copy(loginStatus = LoginStatus.Loading)
        }
        launch {
            val time = Clock.System.now()
            val salt = salt()
            val login = Login(
                userName = flow.value.auth.userName,
                hashOfHash = hash(salt, "${time.epochSeconds}", flow.value.auth.hash),
                salt = salt,
                time = time,
            )
            val response = Api.login(login)
            response.body?.let {
                localStoreSet(it)
                localStoreSet("username", login.userName)
                println("storing login $it")
                setState {
                    copy(
                        loginStatus = LoginStatus.LoggedIn,
                        login = it,
                    )
                }
                routeViewModel.setRoute(Route.Home)
            } ?: setState {
                copy(
                    loginStatus = LoginStatus.Failed,
                    errorMessage = "${response.status} ${response.statusText}"
                )
            }
        }
    }

    fun reload() {
        launch {
            delay(3000)
            setState {
                copy(loginStatus = LoginStatus.Ready)
            }
        }
    }

    fun setUserName(value: String) {
        setState {
            copy(auth = auth.copy(userName = value))
        }
    }
}

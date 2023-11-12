package viewmodel

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.LoginResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import utils.*


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
        state = savedLogin?.let { LoginStatus.LoggedIn } ?: LoginStatus.Ready
    )
}

data class LoginState(
    val auth: AuthRecord = AuthRecord(hash = "", userName = localStoreGet("username") ?: ""),
    val pass: String = "",
    val state: LoginStatus = LoginStatus.Ready,
    val errorMessage: String? = null,
    val login: LoginResponse? = localStoreGet<LoginResponse>()
) : VmState

val loginViewModel = LoginViewModel()

class LoginViewModel : BaseViewModel<LoginState>(initialState()) {

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
            copy(state = LoginStatus.Loading)
        }
        launch {
            val response = Api.postAuth(flow.value.auth)
            response.body?.let {
                setState {
                    copy(
                        auth = it,
                        state = LoginStatus.Complete
                    )
                }
            } ?: setState {
                copy(
                    state = LoginStatus.Failed,
                    errorMessage = "${response.status} ${response.statusText}",
                )
            }
            delay(3000)
            setState {
                initialState()
            }
        }
    }

    fun logout() {
        localStoreSet("username", "")
        localStoreSet<LoginResponse>(null)
        setState {
            initialState()
        }
        routeViewModel.setRoute(Route.Home)
    }

    fun login() {
        setState {
            copy(state = LoginStatus.Loading)
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
                        state = LoginStatus.LoggedIn,
                        login = it,
                    )
                }
                routeViewModel.setRoute(Route.Home)
            } ?: setState {
                copy(
                    state = LoginStatus.Failed,
                    errorMessage = "${response.status} ${response.statusText}"
                )
            }
        }
    }

    fun reload() {
        launch {
            delay(3000)
            setState {
                copy(state = LoginStatus.Ready)
            }
        }
    }

    fun setUserName(value: String) {
        setState {
            copy(auth = auth.copy(userName = value))
        }
    }
}

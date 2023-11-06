package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.LoginResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import utils.*
import utils.Scopes.mainScope


enum class State {
    Loading,
    Ready,
    Complete,
    LoggedIn,
    Failed,
}

private fun initialState(): LoginVmState {
    val savedLogin = localStoreGet<LoginResponse>()
    return LoginVmState(
        login = savedLogin,
        state = savedLogin?.takeIf { it.expires.minus(Clock.System.now()).isPositive() }?.let { State.LoggedIn }
            ?: State.Ready
    )
}

data class LoginVmState(
    val auth: AuthRecord = AuthRecord(hash = "", userName = localStoreGet("username") ?: ""),
    val pass: String = "",
    val state: State = State.Ready,
    val login: LoginResponse? = localStoreGet<LoginResponse>()
)

class LoginViewModel {
    private val loginState = mutableStateOf(initialState())

    val hash: String
        get() = loginState.value.auth.hash

    val state: State
        get() = loginState.value.state

    val loginResponse: LoginResponse?
        get() = loginState.value.login

    var userName: String
        get() = loginState.value.auth.userName
        set(value) {
            loginState.value = loginState.value.copy(
                auth =
                AuthRecord(
                    hash = hash,
                    userName = value
                )
            )
        }

    var password: String
        get() = loginState.value.pass
        set(value) {
            mainScope.launch {
                loginState.value = loginState.value.copy(
                    auth = AuthRecord(
                        hash = value.takeIf { it.isNotEmpty() }?.let { hash(value) } ?: "",
                        userName = userName
                    ),
                    pass = value
                )
            }
        }

    fun isValid() = password.length > 4

    fun submitNew() {
        loginState.value = loginState.value.copy(state = State.Loading)
        mainScope.launch {
            val record: AuthRecord? = Network.post("auth", loginState.value.auth)
            record?.let {
                loginState.value = loginState.value.copy(
                    auth = it,
                    state = State.Complete
                )
            } ?: run {
                loginState.value = loginState.value.copy(state = State.Failed)
            }
        }
    }

    fun logout() {
        localStoreSet("username", "")
        localStoreSet<LoginResponse>(null)
        loginState.value = initialState()
    }

    fun login() {
        mainScope.launch {
            loginState.value = loginState.value.copy(state = State.Loading)
            val time = Clock.System.now()
            val salt = salt()
            val login = Login(
                userName = userName,
                hashOfHash = hash(salt, "${time.epochSeconds}", hash),
                salt = salt,
                time = time,
            )
            val response: LoginResponse? = Network.post("login", login)
            response?.let {
                localStoreSet(it)
                localStoreSet("username", login.userName)
                loginState.value = loginState.value.copy(state = State.LoggedIn)
            } ?: run {
                loginState.value = loginState.value.copy(state = State.Failed)
            }
        }
    }

    fun reload() {
        mainScope.launch {
            delay(3000)
            loginState.value = loginState.value.copy(state = State.Ready)
        }
    }
}
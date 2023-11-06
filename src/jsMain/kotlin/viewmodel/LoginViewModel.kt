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
)

val loginViewModel = LoginViewModel()

class LoginViewModel {
    private val loginState = mutableStateOf(initialState())

    val hash: String
        get() = loginState.value.auth.hash

    val loginStatus: LoginStatus
        get() = loginState.value.state

    val loginResponse: LoginResponse?
        get() = loginState.value.login?.takeIf { !it.isExpired() }

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

    val state: LoginState
        get() = loginState.value

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
        loginState.value = loginState.value.copy(state = LoginStatus.Loading)
        mainScope.launch {
            val response  = Network.post<AuthRecord, AuthRecord>("auth", loginState.value.auth)
            response.body?.let {
                loginState.value = loginState.value.copy(
                    auth = it,
                    state = LoginStatus.Complete
                )
            } ?: run {
                loginState.value = loginState.value.copy(
                    state = LoginStatus.Failed,
                    errorMessage = "${response.status} ${response.statusText}",
                )
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
            loginState.value = loginState.value.copy(state = LoginStatus.Loading)
            val time = Clock.System.now()
            val salt = salt()
            val login = Login(
                userName = userName,
                hashOfHash = hash(salt, "${time.epochSeconds}", hash),
                salt = salt,
                time = time,
            )
            val response  = Network.post<Login, LoginResponse>("login", login)
            response.body?.let {
                localStoreSet(it)
                localStoreSet("username", login.userName)
                println("storing login $it")
                loginState.value = loginState.value.copy(
                    state = LoginStatus.LoggedIn,
                    login = it,
                )
            } ?: run {
                loginState.value = loginState.value.copy(
                    state = LoginStatus.Failed,
                    errorMessage = "${response.status} ${response.statusText}"
                )
            }
        }
    }

    fun reload() {
        mainScope.launch {
            delay(3000)
            loginState.value = loginState.value.copy(state = LoginStatus.Ready)
        }
    }
}

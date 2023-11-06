package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.AuthRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.Network
import utils.Scopes.mainScope
import utils.hash


enum class State {
    Loading,
    Ready,
    Complete,
    Failed,
}

data class LoginVmState(
    val auth: AuthRecord = AuthRecord(hash = "", userName = ""),
    val pass: String = "",
    val state: State = State.Ready
)

class LoginViewModel {
    private val loginState = mutableStateOf(LoginVmState())

    val hash: String
        get() = loginState.value.auth.hash

    val state: State
        get() = loginState.value.state

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
            val record: AuthRecord? = Network.post<AuthRecord?>("auth", loginState.value.auth)
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

    fun login() {
        mainScope.launch {
            loginState.value = loginState.value.copy(state = State.Loading)
            delay(1000)
            loginState.value = loginState.value.copy(state = State.Ready)
//            val record: AuthRecord? = Network.post<AuthRecord?>("auth".versionedApi(), loginState.value.auth)
//            record?.let {
//                loginState.value = loginState.value.copy(
//                    auth = it,
//                    state = State.Complete
//                )
//            } ?: run {
//                loginState.value = loginState.value.copy(state = State.Failed)
//            }
        }
    }
}
package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.Person
import components.ClockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.Scopes.mainScope
import utils.hashInternal
import kotlin.js.Date

data class LoginState(
    val login: Login? = null,
    val person: Person? = null,
)

class HomeViewModel : ClockTime {
    private val state = mutableStateOf(LoginState())
    private val auth = mutableStateOf(AuthRecord(admin = false, hash = "", userName = ""))
    private val clockState = mutableStateOf("")

    override val readOut: String
        get() = clockState.value

    var admin: Boolean
        get() = auth.value.admin
        set(value) {
            auth.value = AuthRecord(
                admin = value,
                hash = auth.value.hash,
                userName = auth.value.userName
            )
        }
    val hash: String
        get() = auth.value.hash

    private fun getClockValue(): String {
        return Date().toLocaleTimeString()
    }

    fun userName(name: String) {
        auth.value = AuthRecord(
            admin = auth.value.admin,
            hash = auth.value.hash,
            userName = name
        )
    }

    fun password(password: String) {
        mainScope.launch {
            auth.value = AuthRecord(
                admin = auth.value.admin,
                hash = password.takeIf { it.isNotEmpty() }?.let { hashInternal(password) } ?: "",
                userName = auth.value.userName
            )
        }
    }

    val loggedInPerson: Person?
        get() = state.value.person

    init {
        mainScope.launch(Dispatchers.Unconfined) {
            while (true) {
                delay(100)
                clockState.value = getClockValue()
            }
        }
    }
}
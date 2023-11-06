package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.Login
import com.mxmariner.regatta.data.Person
import components.ClockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.Scopes.mainScope
import kotlin.js.Date

data class LoginState(
    val login: Login? = null,
    val person: Person? = null,
)

class HomeViewModel : ClockTime {
    private val state = mutableStateOf(LoginState())
    private val clockState = mutableStateOf("")

    override val readOut: String
        get() = clockState.value


    private fun getClockValue(): String {
        return Date().toLocaleTimeString()
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
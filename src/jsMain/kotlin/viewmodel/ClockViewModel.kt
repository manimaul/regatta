package viewmodel

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.Scopes.mainScope
import kotlin.js.Date

class ClockViewModel {
    private val clockState = mutableStateOf("")

    val readOut: String
        get() = clockState.value
    private fun getClockValue(): String {
        return Date().toLocaleTimeString()
    }
    init {
        mainScope.launch(Dispatchers.Unconfined) {
            while(true) {
                delay(100)
                clockState.value = getClockValue()
            }
        }
    }
}
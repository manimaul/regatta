package viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AlertState(
    val message: String? = null
) : VmState

val alertsViewModel = AlertsViewModel()

class AlertsViewModel : BaseViewModel<AlertState>(AlertState()){
    override fun reload() {
    }
    private var job: Job? = null

    fun showAlert(message: String) {
        setState { copy(message = message) }
        job?.cancel()
        job = launch {
            delay(3000)
            setState { copy(message = null) }
        }
    }
}
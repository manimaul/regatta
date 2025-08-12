package viewmodel

import kotlinx.browser.document
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.events.EventListener
import utils.Bootstrap

data class Confirmation(
    val msg: String,
    val subTitle: String? = null,
    val handler: (Boolean) -> Unit
)

data class AlertState(
    val message: String? = null,
    val confirmation: Confirmation? = null,
    val confirmationModal: Bootstrap.Modal? = null
) : VmState

val alertsViewModel = AlertsViewModel()
const val confirmationModalId = "confirmation"
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

    fun confirm(
        msg: String,
        subTitle: String? = null,
        handler: (Boolean) -> Unit
    ) {
        setState {
            copy(confirmation = Confirmation(msg, subTitle, handler))
        }
        withState { it.confirmationModal?.show() }
    }

    fun noConfirmation() {
        withState {
            it.confirmation?.handler(false)
            it.confirmationModal?.hide()
        }
    }

    fun yesConfirmation() {
        withState {
            it.confirmation?.handler(true)
            it.confirmationModal?.hide()
        }
        setState { copy(confirmation = null) }
    }

    fun confirmationModalCreated() {
        document.querySelector("#$confirmationModalId")?.let { element ->
            element.addEventListener("hidden.bs.modal", EventListener {
                println("$confirmationModalId modal hidden")
            })
            element.addEventListener("shown.bs.modal", EventListener {
                println("$confirmationModalId modal shown")
            })
            Bootstrap.Modal.getOrCreateInstance(element)?.let { modal ->
                println("$confirmationModalId modal created")
                setState { copy(confirmationModal = modal) }
            }
        }
    }

    fun confirmationModalDestroyed() {
        println("$confirmationModalId modal destroyed")
        setState {
            copy(confirmationModal = null)
        }
    }
}

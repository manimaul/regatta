package viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLDialogElement
import org.w3c.dom.events.EventListener

data class Confirmation(
    val msg: String,
    val subTitle: String? = null,
    val handler: (Boolean) -> Unit
)

data class AlertState(
    val message: String? = null,
    val confirmation: Confirmation? = null,
    val confirmationModal: HTMLDialogElement? = null
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
        withState { it.confirmationModal?.takeIf { m -> !m.open }?.showModal() }
    }

    fun noConfirmation() {
        withState {
            it.confirmation?.handler(false)
            it.confirmationModal?.close()
        }
    }

    fun yesConfirmation() {
        withState {
            it.confirmation?.handler(true)
            it.confirmationModal?.close()
        }
        setState { copy(confirmation = null) }
    }

    fun confirmationModalCreated(dialog: HTMLDialogElement) {
        // Esc / backdrop click close the dialog without answering (as with Bootstrap); drop the stale question.
        dialog.addEventListener("close", EventListener {
            println("$confirmationModalId modal closed")
            setState { copy(confirmation = null) }
        })
        println("$confirmationModalId modal created")
        setState { copy(confirmationModal = dialog) }
    }

    fun confirmationModalDestroyed() {
        println("$confirmationModalId modal destroyed")
        setState {
            copy(confirmationModal = null)
        }
    }
}

package viewmodel

import com.mxmariner.regatta.data.OrcCertificate
import components.Action
import utils.Api
import utils.Async
import utils.Uninitialized
import utils.toAsync


data class OrcState(
    val refNumber: String = "",
    val cert: Async<OrcCertificate> = Uninitialized,
    val certs: List<OrcCertificate> = emptyList(),
    val readyToAdd: Boolean = false,
) : VmState

class OrcViewModel : BaseViewModel<OrcState>(OrcState()) {

    override fun reload() {
        setState { OrcState() }
    }

    fun refNumber(ref: String) {
        setState {
            val ready = certs.count { it.refNo.startsWith(ref) } == 0 && ref.isNotBlank()
            copy(
                refNumber = ref,
                readyToAdd = ready
            )
        }
    }

    fun confirmRef(
        onComplete: (Action, OrcCertificate) -> Unit,
    ) {
        setState {
            copy(cert = cert.loading())
        }
        setState {
            // the server fetches the RMS data and the file id of the certificate's public page (data.orc.org has no CORS)
            val value = Api.getOrcCertificate(refNumber.trim()).toAsync()
            value.value?.let { onComplete(Action.Add, it) }
            copy(
                refNumber = "",
                cert = value
            )
        }
    }

    fun setCerts(certList: List<OrcCertificate>) {
        withState { state ->
            if (certList.map { it.refNo }.toSet() != state.certs.map { it.refNo }.toSet()) {
                setState {
                    copy(certs = certList)
                }
            }
        }
    }
}

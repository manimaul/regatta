package viewmodel

import OrcCertificate
import OrcResponse
import components.Action
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit
import utils.Async
import utils.Network.networkResponse
import utils.Uninitialized
import utils.mapNotNull
import utils.toAsync
import kotlin.js.json


data class OrcState(
    val refNumber: String = "",
    val cert: Async<OrcCertificate> = Uninitialized,
    val certs: List<OrcCertificate> = emptyList(),
    val readyToAdd: Boolean = false,
) : VmState

private val withUnknownKeys = Json { ignoreUnknownKeys = true }

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
            val response = window.fetch(
                "https://data.orc.org/public/WPub.dll?action=DownBoatRMS&RefNo=${refNumber}&ext=json",
                RequestInit(
                    method = "GET",
                    headers = json(
                        "Accept" to "application/json",
                    ),
                )
            ).await()
            val value = response.networkResponse<OrcResponse>(json = withUnknownKeys).toAsync().mapNotNull{
                it.rms.firstOrNull()
            }
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

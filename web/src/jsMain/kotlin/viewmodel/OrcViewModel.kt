package viewmodel

import OrcCertificate
import OrcResponse
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit
import utils.Async
import utils.Complete
import utils.Network.networkResponse
import utils.Uninitialized
import utils.map
import utils.toAsync
import kotlin.js.json


data class OrcState(
    val refNumber: String = "",
    val cert: Async<OrcCertificate> = Uninitialized
) : VmState

private val withUnknownKeys = Json { ignoreUnknownKeys = true }

class OrcViewModel : BaseViewModel<OrcState>(OrcState()) {

    override fun reload() {
        setState { OrcState() }
    }

    fun refNumber(ref: String) {
        setState { copy(refNumber = ref) }
    }

    fun confirmRef() {
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
            val value = response.networkResponse<OrcResponse>(json = withUnknownKeys).toAsync().map {
                it.rms.first()
            }
            copy(cert = value)
        }
    }

    fun setCert(c: OrcCertificate?) {
        setState {
            copy(
                cert = c?.let { Complete(it) } ?: Uninitialized
            )
        }
    }

}

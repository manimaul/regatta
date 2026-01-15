package viewmodel

import OrcCertificate
import com.mxmariner.regatta.data.Boat
import utils.Async
import utils.Uninitialized


data class OrcState(
    val refNumber: String = "",
    val cert: Async<OrcCertificate> = Uninitialized
) : VmState

class OrcViewModel : BaseViewModel<OrcState>(OrcState()){

    override fun reload() {
        setState { OrcState() }
    }

    fun refNumber(ref: String) {
        setState { copy(refNumber = ref) }
    }

    fun lookup(boat: Boat) {

    }

}

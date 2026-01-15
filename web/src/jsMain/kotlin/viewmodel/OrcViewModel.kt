package viewmodel

import OrcCertificate
import com.mxmariner.regatta.data.Boat
import kotlinx.coroutines.Job
import utils.Async
import utils.Uninitialized


data class OrcState(
    val cert: Async<OrcCertificate> = Uninitialized
) : VmState

class OrcViewModel : BaseViewModel<OrcState>(OrcState()){

    override fun reload() {
        setState { OrcState() }
    }

    fun lookup(boat: Boat) {

    }

}

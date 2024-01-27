package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.RaceReport
import utils.*

data class RaceResultEditState(
    val report: Async<RaceReport> = Loading(),
    val boats: Async<List<Boat>> = Loading(),
) : VmState

class RaceResultEditViewModel(
    private val raceId: Long,
    val addViewModel: RaceResultAddViewModel = RaceResultAddViewModel(raceId),
) : BaseViewModel<RaceResultEditState>(RaceResultEditState()) {
    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(
                report = Api.getReport(raceId).toAsync(),
                boats = Api.getAllBoats().toAsync(),
            )
        }
    }

    fun addResult(value: RaceResultAddState) {
        addViewModel.setCard()
        setState {
            value.asPost()?.let { post ->
                copy(
                    report = Api.postResult(post).toAsync().flatMap { Api.getReport(raceId).toAsync() }
                )
            } ?: this
        }
    }

    fun delete(id: Long) {
        setState {
            copy(
                report = Api.deleteResult(id).toAsync().flatMap { Api.getReport(raceId).toAsync() }
            )
        }
        addViewModel.setCard()
    }
}
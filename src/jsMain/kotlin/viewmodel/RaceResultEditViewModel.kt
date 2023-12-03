package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.RaceFull
import com.mxmariner.regatta.data.RaceResultFull
import utils.Api
import utils.Async
import utils.Loading
import utils.toAsync

data class RaceResultAddState(
    val race: Async<RaceFull> = Loading(),
    val result: Async<List<RaceResultFull>> = Loading(),
    val boats: Async<List<Boat>> = Loading(),
) : VmState

class RaceResultEditViewModel(
    private val raceId: Long
) : BaseViewModel<RaceResultAddState>(RaceResultAddState()) {
    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(
                race = Api.getRace(raceId).toAsync(),
                result = Api.getResults(raceId).toAsync(),
                boats = Api.getAllBoats().toAsync()
            )
        }
    }
}
package viewmodel

import com.mxmariner.regatta.data.RaceFull
import com.mxmariner.regatta.data.RaceResultFull
import utils.Api
import utils.Async
import utils.Loading
import utils.Error
import utils.toAsync

data class RaceResultAddState(
    val race: Async<RaceFull> = Loading(),
    val result: Async<List<RaceResultFull>> = Loading(),
) : VmState

class RaceResultEditViewModel(
    raceId: Long?
) : BaseViewModel<RaceResultAddState>(RaceResultAddState()) {
    fun reload() {
    }

    init {
        setState {
            copy(
                race = raceId?.let { Api.getRace(it).toAsync() } ?: Error(),
                result = raceId?.let { Api.getResults(it).toAsync() } ?: Error()
            )
        }
    }
}
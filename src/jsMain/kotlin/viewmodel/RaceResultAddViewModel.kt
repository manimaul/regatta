package viewmodel

import com.mxmariner.regatta.data.RaceFull
import utils.Api
import utils.Async
import utils.Loading
import utils.Error
import utils.toAsync

data class RaceResultAddState(
    val race: Async<RaceFull> = Loading()
) : VmState

class RaceResultAddViewModel(
    raceId: Long?
) : BaseViewModel<RaceResultAddState>(RaceResultAddState()) {
    fun reload() {
    }

    init {
        setState {
            copy(
                race = raceId?.let { Api.getRace(it).toAsync() } ?: Error()
            )
        }
    }
}
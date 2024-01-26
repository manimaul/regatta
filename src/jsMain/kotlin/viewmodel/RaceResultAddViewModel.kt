package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.RaceFull
import com.mxmariner.regatta.data.RaceTime
import kotlinx.datetime.Instant

data class RaceResultAddState(
    val race: RaceFull? = null,
    override val boat: Boat? = null,
    override val raceTime: RaceTime? = null,
) : VmState, RaceResultComputed

class RaceResultAddViewModel(
) : BaseViewModel<RaceResultAddState>(RaceResultAddState()) {
    override fun reload() {
        setState { RaceResultAddState() }
    }

    fun addBoat(boat: Boat?) {
        setState {
            copy(
                boat = boat,
                raceTime = race?.let { boatRaceTime(it, boat) }
            )
        }
    }

    fun setRace(value: RaceFull?) {
        setState {
            copy(
                race = value,
                raceTime = value?.let { boatRaceTime(it, boat) }
            )
        }
    }

    fun setFinish(it: Instant) {
        setState { copy(raceTime = raceTime?.copy(endDate = it)) }
    }
}

package viewmodel

import com.mxmariner.regatta.data.*
import kotlinx.datetime.Instant
import utils.Api
import utils.toAsync
import kotlin.math.max


fun findBoatBracket(race: RaceFull?, boat: Boat?) : Long? {
   TODO()
}

data class RaceResultAddState(
    val id: Long? = null,
    val race: RaceFull? = null,
    val boat: Boat? = null,
    val raceClassId: Long? = null,
    val start: Instant? = null,
    val finish: Instant? = null,
    val hocPosition: Int? = null,
) : VmState {
    fun asPost(): RaceResultPost? {
        val bracketId = findBoatBracket(race, boat)
        return if (boat?.id != null && race?.id != null && bracketId != null) {
            RaceResultPost(
                id = id,
                raceId = race.id,
                boatId = boat.id,
                bracketId = bracketId,
                start = start,
                finish = finish,
                phrfRating = boat.phrfRating,
                hocPosition = hocPosition,
            )
        } else {
            null
        }
    }
}

class RaceResultAddViewModel(
    val raceId: Long
) : BaseViewModel<RaceResultAddState>(RaceResultAddState()) {
    override fun reload() {
        setState {
            val race = Api.getRace(raceId).toAsync()
            RaceResultAddState(
                boat = null,
                race = race.value,
                start = race.value?.startTime,
                finish = race.value?.endTime
            )
        }
    }

    init {
        reload()
    }

    fun addBoat(boat: Boat?) {
        setState { copy(boat = boat) }
    }

    fun setFinish(it: Instant?) {
        setState { copy(finish = it) }
    }

    fun setStart(it: Instant?) {
        setState { copy(start = it) }
    }

    fun setCard(card: RaceReportCard? = null) {
        setState {
            copy(
                id = card?.resultRecord?.id,
                boat = card?.resultRecord?.boat,
                raceClassId = card?.resultRecord?.bracketId,
                race = if (card != null) card.resultRecord.race else race,
                start = if (card != null) card.startTime else race?.startTime,
                finish = if (card != null) card.finishTime else race?.endTime,
                hocPosition = card?.hocPosition,
            )
        }
    }

    fun hoc(i: Int) {
        setState {
            copy(
                hocPosition = max(0, i),
                finish = null,
            )
        }
    }
}

package viewmodel

import com.mxmariner.regatta.data.*
import kotlinx.datetime.Instant
import utils.Api
import utils.toAsync
import kotlin.math.max



data class RaceResultAddState(
    val id: Long = 0,
    val raceSchedule: RaceSchedule? = null,
    val boat: Boat? = null,
    val skipper: Person? = null,
    val raceClassId: Long? = null,
    val start: Instant? = null,
    val finish: Instant? = null,
    val hocPosition: Int? = null,
) : VmState {
    fun asPost(): RaceResult? {
        return if (boat?.id != null && raceSchedule?.race?.id != null) {
            RaceResult(
                id = id,
                raceId = raceSchedule.race.id,
                boatId = boat.id,
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
            val race = Api.getRaceSchedule(raceId).toAsync()
            RaceResultAddState(
                boat = null,
                raceSchedule = race.value,
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
                id = card?.resultRecord?.result?.id ?: 0L,
                boat = card?.resultRecord?.boatSkipper?.boat,
                raceClassId = card?.resultRecord?.bracket?.id,
                raceSchedule = if (card != null) card.resultRecord.raceSchedule else raceSchedule,
                start = if (card != null) card.startTime else raceSchedule?.startTime,
                finish = if (card != null) card.finishTime else raceSchedule?.endTime,
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

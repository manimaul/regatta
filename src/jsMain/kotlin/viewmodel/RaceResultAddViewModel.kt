package viewmodel

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.ratingDefault
import kotlinx.datetime.Instant
import utils.Api
import utils.toAsync
import kotlin.math.max

enum class BoatType {
    PHRF,
    Windseeker
}

data class RaceResultAddState(
    val id: Long = 0,
    val raceSchedule: RaceSchedule? = null,
    val boatSkipper: BoatSkipper? = null,
    val phrfRating: String = ratingDefault.toInt().toString(),
    val wsRating: String = ratingDefault.toInt().toString(),
    val wsFlying: Boolean = false,
    val boatType: BoatType = BoatType.PHRF,
    val raceClassId: Long? = null,
    val start: Instant? = null,
    val finish: Instant? = null,
    val hocPosition: Int? = null,
) : VmState {
    fun asPost(): RaceResult? {
        var phrfRating = phrfRating.toIntOrNull()
        var windseeker = wsRating.toIntOrNull()?.let {
            Windseeker(it, wsFlying)
        }
        val valid = when (boatType) {
            BoatType.PHRF -> {
                windseeker = null
                phrfRating != null
            }
            BoatType.Windseeker -> {
                phrfRating = null
                windseeker != null
            }
        }
        return if (boatSkipper?.boat?.id != null && raceSchedule?.race?.id != null && valid) {
            RaceResult(
                id = id,
                raceId = raceSchedule.race.id,
                boatId = boatSkipper.boat.id,
                start = start,
                finish = finish,
                phrfRating = phrfRating,
                windseeker = windseeker,
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
                boatSkipper = null,
                raceSchedule = race.value,
                start = race.value?.startTime,
                finish = race.value?.endTime
            )
        }
    }

    init {
        reload()
    }

    fun addBoat(boatSkipper: BoatSkipper?) {
        setState {
            copy(
                boatSkipper = boatSkipper,
                phrfRating = boatSkipper?.boat?.phrfRating?.toString() ?: "",
                wsRating = boatSkipper?.boat?.windseeker?.rating?.toString() ?: "",
                wsFlying = boatSkipper?.boat?.windseeker?.flyingSails == true,
            )
        }
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
                boatSkipper = card?.resultRecord?.boatSkipper,
                phrfRating = card?.resultRecord?.result?.phrfRating?.toString() ?: "",
                wsRating = card?.resultRecord?.result?.windseeker?.rating?.toString() ?: "",
                wsFlying = card?.resultRecord?.result?.windseeker?.flyingSails == true,
                raceSchedule = if (card != null) card.resultRecord.raceSchedule else raceSchedule,
                start = if (card != null) card.startTime else raceSchedule?.startTime,
                finish = if (card != null) card.finishTime else raceSchedule?.endTime,
                hocPosition = card?.hocPosition,
            )
        }
    }

    fun hoc(i: Int?) {
        setState {
            copy(
                hocPosition = i?.let { max(0, i) },
                finish = if (i != null) null else finish,
            )
        }
        println(flow.value.finish)
    }

    fun setPhrfRating(rating: String) {
        setState {
            copy(
                phrfRating = rating,
            )
        }
    }

    fun setType(type: BoatType) {
       setState { copy(boatType = type) }
    }

    fun setWsRating(rating: String) {
        setState {
            copy(
                wsRating = rating,
            )
        }
    }

    fun setWsFlying(flying: Boolean) {
        setState {
            copy(
                phrfRating = "",
                wsFlying = flying,
            )
        }
    }
}

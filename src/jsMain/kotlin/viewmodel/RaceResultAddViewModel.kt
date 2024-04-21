package viewmodel

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.ratingDefault
import kotlinx.datetime.Instant
import utils.Api
import utils.toAsync
import kotlin.math.max


data class RaceResultAddState(
    val id: Long = 0,
    val raceSchedule: RaceSchedule? = null,
    val boatSkipper: BoatSkipper? = null,
    val phrfRating: String = ratingDefault.toInt().toString(),
    val wsRating: String = ratingDefault.toInt().toString(),
    val wsFlying: Boolean = false,
    val ratingType: RatingType = RatingType.PHRF,
    val raceClassId: Long? = null,
    val finish: Instant? = null,
    val hocPosition: Int? = null,
    val penalty: Int? = null,
) : VmState {
    fun asPost(): RaceResult? {
        var phrfRating = phrfRating.toIntOrNull()
        var windseeker = wsRating.toIntOrNull()?.let {
            Windseeker(it, wsFlying)
        }
        val valid = when (ratingType) {
            RatingType.PHRF -> {
                windseeker = null
                phrfRating != null
            }

            RatingType.Windseeker -> {
                phrfRating = null
                windseeker != null
            }
        }
        return if (boatSkipper?.boat?.id != null && raceSchedule?.race?.id != null && valid) {
            RaceResult(
                id = id,
                raceId = raceSchedule.race.id,
                boatId = boatSkipper.boat.id,
                finish = finish,
                phrfRating = phrfRating,
                windseeker = windseeker,
                hocPosition = hocPosition,
                penalty = penalty,
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
                finish = race.value?.endTime
            )
        }
    }

    init {
        reload()
    }

    fun addBoat(boatSkipper: BoatSkipper?) {

        val type = boatSkipper?.boat?.ratingType() ?: RatingType.PHRF
        setState {
            copy(
                boatSkipper = boatSkipper,
                phrfRating = boatSkipper?.boat?.phrfRating?.toString() ?: "",
                wsRating = boatSkipper?.boat?.windseeker?.rating?.toString() ?: "",
                wsFlying = boatSkipper?.boat?.windseeker?.flyingSails == true,
                ratingType = type,
            )
        }
    }

    fun setFinish(it: Instant?) {
        setState { copy(finish = it) }
    }

    fun setCard(card: RaceReportCard? = null) {
        val type = card?.ratingType() ?: RatingType.Windseeker
        setState {
            copy(
                id = card?.resultRecord?.result?.id ?: 0L,
                boatSkipper = card?.resultRecord?.boatSkipper,
                phrfRating = card?.resultRecord?.result?.phrfRating?.toString() ?: "",
                wsRating = card?.resultRecord?.result?.windseeker?.rating?.toString() ?: "",
                wsFlying = card?.resultRecord?.result?.windseeker?.flyingSails == true,
                ratingType = type,
                raceSchedule = if (card != null) card.resultRecord.raceSchedule else raceSchedule,
                finish = if (card != null) card.finishTime else raceSchedule?.endTime,
                hocPosition = card?.hocPosition,
                penalty = card?.penalty,
            )
        }
    }

    fun penalty(i: Int?) {
        setState {
            copy(penalty = i?.takeIf { it > 0 })
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

    fun setType(type: RatingType) {
        setState { copy(ratingType = type) }
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

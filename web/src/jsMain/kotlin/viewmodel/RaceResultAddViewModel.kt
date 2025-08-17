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
    val bracketId: Long? = null,
    val finish: Instant? = null,
    val finishCode: FinishCode = FinishCode.TIME,
    val hocPosition: Int? = null,
    val penalty: Int? = null,
) : VmState {

    val isValid = when (ratingType) {
        RatingType.PHRF -> {
            phrfRating.toIntOrNull() != null
        }

        RatingType.Windseeker -> {
            wsRating.toIntOrNull() != null
        }
    }

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
                boatId = boatSkipper.boat?.id ?: 0L,
                finish = finish,
                phrfRating = phrfRating,
                windseeker = windseeker,
                hocPosition = hocPosition,
                penalty = penalty,
                finishCode = finishCode,
                bracketId = bracketId,
                raceClassId = raceClassId,
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
            val ft = race.value?.endTime
            RaceResultAddState(
                boatSkipper = null,
                raceSchedule = race.value,
                finish = ft,
                finishCode = ft?.let { FinishCode.TIME } ?: FinishCode.RET,
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


    fun setFinish(code: FinishCode, value: Instant?, clearPenalty: Boolean = false) {
        print("setting finish $code, $value")
        setState {
            copy(
                hocPosition = null,
                finishCode = code,
                finish = value,
                penalty = if (clearPenalty) null else penalty
            )
        }
    }

    fun setCard(
        card: RaceReportCard? = null,
        autoRaceClassId: Long? = null,
        autoBracketId: Long? = null
    ) {
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
                finishCode = card?.resultRecord?.result?.finishCode ?: FinishCode.TIME,
                raceClassId = card?.resultRecord?.result?.raceClassId ?: autoRaceClassId,
                bracketId = card?.resultRecord?.result?.bracketId ?: autoBracketId
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
                finishCode = FinishCode.HOC,
                penalty = null,
                hocPosition = i?.let { max(0, i) },
                finish = if (i != null) null else finish,
            )
        }
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

    fun addResultSelectedClass(cs: ClassSchedule?) {
        setState {
            copy(
                raceClassId = cs?.raceClass?.id
            )
        }
    }

    fun addResultSelectedBracket(bracket: Bracket?) {
        setState {
            copy(
                bracketId = bracket?.id
            )
        }
    }
}

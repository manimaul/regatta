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
//        RatingType.ORC -> {
//            false
//        }
        RatingType.PHRF -> {
            phrfRating.toIntOrNull() != null
        }

        RatingType.CruisingFlyingSails -> {
            wsRating.toIntOrNull() != null
        }

        RatingType.CruisingNonFlyingSails -> {
            wsRating.toIntOrNull() != null
        }
    }

    fun asPost(): RaceResult? {
        var phrfRating: Int?
        var windseeker: Windseeker?
        val valid = when (ratingType) {
//            RatingType.ORC -> false
            RatingType.PHRF -> {
                windseeker = null
                phrfRating = this.phrfRating.toIntOrNull()

                phrfRating != null
            }

            RatingType.CruisingFlyingSails -> {
                phrfRating = null
                windseeker = wsRating.toIntOrNull()?.let {
                    Windseeker(it, true)
                }

                windseeker != null
            }
            RatingType.CruisingNonFlyingSails -> {
                phrfRating = null
                windseeker = wsRating.toIntOrNull()?.let {
                    Windseeker(it, false)
                }

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
        val type = card?.ratingType() ?: RatingType.CruisingNonFlyingSails
        setState {
            copy(
                id = card?.resultRecord?.result?.id ?: 0L,
                boatSkipper = card?.resultRecord?.boatSkipper,
                phrfRating = card?.resultRecord?.result?.phrfRating?.toString() ?: "",
                wsRating = card?.resultRecord?.result?.windseeker?.rating?.toString() ?: "",
                wsFlying = card?.resultRecord?.result?.windseeker?.flyingSails == true,
                ratingType = type,
                raceSchedule = card?.resultRecord?.raceSchedule ?: raceSchedule,
                finish = card?.finishTime ?: raceSchedule?.endTime,
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

    fun setType(type: RatingType, rating: Int) {
        setState {
            copy(
                ratingType = type,
                raceClassId = null,
                bracketId = null,
                wsFlying = type == RatingType.CruisingFlyingSails,
                wsRating = rating.toString(),
                phrfRating = rating.toString()
            )
        }
    }

    fun addResultSelectedClass(cs: ClassSchedule?) {
        setState {
            copy(
                raceClassId = cs?.raceClass?.id,
                bracketId = cs?.brackets?.firstOrNull { bracket ->
                    when (boatSkipper?.boat?.ratingType()) {
//                        RatingType.ORC -> false
                        RatingType.PHRF -> boatSkipper.boat?.phrfRating?.let {
                            it >= bracket.minRating && it <= bracket.maxRating
                        } ?: false

                        RatingType.CruisingFlyingSails -> boatSkipper.boat?.windseeker?.rating?.let {
                            it >= bracket.minRating && it <= bracket.maxRating
                        } ?: false

                        RatingType.CruisingNonFlyingSails -> boatSkipper.boat?.windseeker?.rating?.let {
                            it >= bracket.minRating && it <= bracket.maxRating
                        } ?: false
                        null -> false
                    }
                }?.id
            )
        }
    }

    fun availableBrackets(selectedRaceClass: ClassSchedule?): List<Bracket>? {
        return withState { addState ->
            val rating: Float = when (addState.ratingType) {
//                RatingType.ORC -> 1.0f //addState.phrfRating.toInt()
                RatingType.PHRF -> addState.phrfRating.toFloat()
                RatingType.CruisingFlyingSails -> addState.wsRating.toFloat()
                RatingType.CruisingNonFlyingSails -> addState.wsRating.toFloat()
            }
            selectedRaceClass?.brackets?.filter { bracket ->
                rating >= bracket.minRating && rating <= bracket.maxRating
            }
        }
    }

    fun availableClasses(schedule: List<ClassSchedule>): List<ClassSchedule> {
        return withState { addState ->
            schedule.filter { classSchedule ->
                when (addState.ratingType) {
                    RatingType.PHRF -> {
                        val rating = addState.phrfRating.toInt()
                        val brackets = classSchedule.brackets.count {
                            rating >= it.minRating && rating <= it.maxRating
                        }
                        classSchedule.raceClass.isPHRF && brackets > 0
                    }

                    RatingType.CruisingFlyingSails,
                    RatingType.CruisingNonFlyingSails -> {
                        val rating = addState.wsRating.toInt()
                        val brackets = classSchedule.brackets.count {
                            rating >= it.minRating && rating <= it.maxRating
                        }
                        !classSchedule.raceClass.isPHRF
                                && brackets > 0
                                && classSchedule.raceClass.wsFlying == addState.wsFlying
                    }
                }
            }
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


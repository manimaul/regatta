package viewmodel

import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.ratingDefault
import components.Action
import kotlinx.coroutines.launch
import kotlin.time.Instant
import utils.Api
import utils.Async
import utils.Complete
import utils.Loading
import utils.flatMap
import utils.toAsync
import kotlin.math.max


data class RaceResultAddState(
    val raceResult: RaceResultFull = RaceResultFull(),
    val results: Async<RaceScheduleResults> = Loading(),
    val boatSkippers: Async<List<BoatSkipper>> = Loading(),
    val boats: List<BoatSkipper> = emptyList(),
    val maxHoc: Int = 1,
    val allowBoatChange: Boolean = true,
    val classes: List<RaceClass> = emptyList(),
    val brackets: List<Bracket> = emptyList(),
    val orcCerts: List<OrcCertificate> = emptyList(),
    val selectedClass: RaceClass? = null,
    val selectedClassStart: Instant? = null,
    val selectedBracket: Bracket? = null,
) : VmState {

    val isValid: Boolean
        get() {
            return raceResult.boat.boat?.id != null &&
                    raceResult.boat.boat?.id != 0L &&
                    raceResult.raceClassId != 0L &&
                    raceResult.bracketId != 0L &&
                    when (raceResult.ratingType) {
                        RatingType.ORC_PHRF -> {
                            raceResult.phrfRating != null && raceResult.orcCertificate != null
                        }

                        RatingType.ORC -> {
                            raceResult.orcCertificate != null
                        }

                        RatingType.PHRF -> {
                            raceResult.phrfRating != null
                        }

                        RatingType.CruisingFlyingSails,
                        RatingType.CruisingNonFlyingSails -> true

                    }
        }
}

class RaceResultAddViewModel(
    rId: Long
) : BaseViewModel<RaceResultAddState>(RaceResultAddState()) {

    var raceId = rId
        set(value) {
            field = value
            setState { copy(results = Loading()) }
            reload()
        }

    override fun reload() {
        println("reloading")
        val raceId = raceId
        setState {
            val results = when {
                raceId == 0L -> Loading()
                results is Complete -> results
                else -> Api.getRaceScheduleResults(raceId).toAsync()
            }
            val ft = results.value?.raceSchedule?.endTime
            val bs = boatSkippers as? Complete ?: Api.getAllBoats().toAsync()

            RaceResultAddState(
                raceResult = RaceResultFull(
                    raceId = raceId,
                    finish = ft,
                    finishCode = ft?.let { FinishCode.TIME } ?: FinishCode.RET,
                ),
                results = results,
                boatSkippers = bs,
                boats = availableBoats(results.value, bs.value),
            )
        }
    }

    init {
        raceId = rId
        launch {
            flow.collect { state ->
                println("state change")
                println("-- rating ${state.raceResult.ratingType}")
                println("-- hoc ${state.raceResult.finishCode}")
                println("-- maxhoc ${state.maxHoc}")
            }
        }
    }

    private fun getRating(addState: RaceResultAddState): Int {
        return getRating(
            addState.raceResult.ratingType,
            addState.raceResult.phrfRating ?: ratingDefault,
            addState.raceResult.orcCertificate
        )

    }

    private fun getRating(boat: Boat?): Int {
        return getRating(
            boat?.ratingType ?: RatingType.CruisingNonFlyingSails,
            boat?.phrfRating ?: ratingDefault,
            boat?.orcCerts?.findPreferred()
        )
    }

    private fun getRating(ratingType: RatingType, phrfRating: Int?, orcCertificate: OrcCertificate?): Int {
        val rating = when (ratingType) {
            RatingType.ORC -> phrfRating ?: run { orcCertificate?.virtualPhrf() } ?: ratingDefault
            RatingType.ORC_PHRF,
            RatingType.CruisingFlyingSails,
            RatingType.CruisingNonFlyingSails,
            RatingType.PHRF -> phrfRating ?: ratingDefault

        }
        println("rating determined $ratingType $rating")
        return rating
    }

    private fun availableBrackets(raceSchedule: RaceSchedule?, classId: Long?, rating: Int): List<Bracket> {
        return raceSchedule?.schedule?.firstOrNull {
            it.raceClass.id == classId
        }?.brackets?.filter { b ->
            rating <= b.maxRating && rating >= b.minRating
        } ?: emptyList()
    }

    /**
     * List of available classes the result can be placed in based on the boat being orc capable, the rating and
     * brackets in the race schedule
     */
    fun getAvailableClasses(boat: Boat?, raceResultsSchedule: RaceSchedule?, rating: Int): List<RaceClass> {
        val canOrc = boat?.orcCerts?.isNotEmpty() == true
        return raceResultsSchedule?.schedule?.filter { cs ->
            if (cs.raceClass.ratingType == RatingType.ORC && !canOrc) {
                false
            } else {
                cs.brackets.any { b ->
                    rating <= b.maxRating && rating >= b.minRating
                }
            }
        }?.map {
            it.raceClass
        } ?: emptyList()
    }

    fun focusResultForEdit(result: RaceResultFull) {
        println("+++++ ${result.boat.boat?.name}")
        setState {
            val resultRating =
                getRating(result.ratingType, result.phrfRating, result.orcCertificate)
            val classes = getAvailableClasses(result.boat.boat, results.value?.raceSchedule, resultRating)
            val selectedClass = classes.firstOrNull { it.id == result.raceClassId }
            val selectedClassStart =
                results.value?.raceSchedule?.schedule?.firstOrNull { it.raceClass.id == selectedClass?.id }?.raceStart()
            val brackets = availableBrackets(results.value?.raceSchedule, selectedClass?.id, resultRating)
            val selectedBracket = brackets.firstOrNull { it.id == result.bracketId }
            val certs = result.orcCertificate?.let { c ->
                ((result.boat.boat?.orcCerts ?: emptyList()) + c).distinctBy { it.refNo }
            } ?: result.boat.boat?.orcCerts ?: emptyList()
            copy(
                raceResult = if (result.orcCertificate == null && certs.isNotEmpty()) {
                    result.copy(orcCertificate = certs.findPreferred())
                } else {
                    result
                },
                allowBoatChange = false,
                classes = classes,
                brackets = brackets,
                selectedClass = selectedClass,
                selectedClassStart = selectedClassStart,
                selectedBracket = selectedBracket,
                orcCerts = certs,
            )
        }
    }

    fun selectBracket(bracket: Bracket) {
        setState {
            copy(
                raceResult = raceResult.copy(
                    bracketId = bracket.id
                ),
                selectedBracket = bracket,
            )
        }
    }

    fun selectClass(raceClass: RaceClass) {
        setState {
            val rating = getRating(this)
            val classes = getAvailableClasses(raceResult.boat.boat, results.value?.raceSchedule, rating)
            val brackets = availableBrackets(results.value?.raceSchedule, raceClass.id, rating)
            val selectedClassStart =
                results.value?.raceSchedule?.schedule?.firstOrNull { it.raceClass.id == raceClass.id }?.raceStart()

            val canOrc = raceResult.boat.boat?.orcCerts?.isNotEmpty() == true
            val ratingType = if (raceClass.ratingType.isORC && !canOrc) {
                RatingType.PHRF
            } else {
                raceClass.ratingType
            }
            val selectBracket = brackets.firstOrNull()
            copy(
                raceResult = raceResult.copy(
                    raceClassId = raceClass.id,
                    bracketId = selectBracket?.id ?: 0L,
                    ratingType = ratingType,
                ),
                classes = classes,
                brackets = brackets,
                selectedClass = raceClass,
                selectedClassStart = selectedClassStart,
                selectedBracket = selectBracket,
            )
        }
    }

    fun clearResultForBoatSelection() {
        setState {
            copy(
                raceResult = RaceResultFull(
                    raceId = raceId,
                    finish = results.value?.raceSchedule?.endTime,
                ),
                allowBoatChange = true,
                selectedClass = null,
                selectedClassStart = null,
                selectedBracket = null,
                classes = emptyList(),
                brackets = emptyList(),
            )
        }
    }

    fun selectBoat(boatSkipper: BoatSkipper?, allowBoatChange: Boolean = true) {
        setState {
            val ratingType = boatSkipper?.boat?.ratingType ?: RatingType.PHRF
            val rating = getRating(boatSkipper?.boat)
            val classes = getAvailableClasses(boatSkipper?.boat, results.value?.raceSchedule, rating)
            val selectClass = classes.firstOrNull { it.ratingType == ratingType } ?: run {
                classes.firstOrNull { it.ratingType.isCompatible(ratingType) }
            }
            val selectedClassStart =
                results.value?.raceSchedule?.schedule?.firstOrNull { it.raceClass.id == selectClass?.id }?.raceStart()
            val brackets = availableBrackets(results.value?.raceSchedule, selectClass?.id, rating)
            val selectBracket = brackets.firstOrNull()
            println("selected boat bracket $selectBracket $ratingType")

            val result = RaceResultFull(
                raceId = raceId,
                boat = boatSkipper ?: BoatSkipper(),
                phrfRating = boatSkipper?.boat?.phrfRating,
                orcCertificate = boatSkipper?.boat?.orcCerts?.findPreferred(),
                ratingType = ratingType,
                raceClassId = selectClass?.id ?: 0L,
                bracketId = selectBracket?.id ?: 0L,
            )

            copy(
                raceResult = result,
                classes = classes,
                brackets = brackets,
                orcCerts = result.boat.boat?.orcCerts ?: emptyList(),
                selectedClass = selectClass,
                selectedClassStart = selectedClassStart,
                selectedBracket = selectBracket,
                allowBoatChange = allowBoatChange,
            )
        }
    }


    fun setFinish(code: FinishCode, finish: Instant?, hoc: Int?) {
        setState {
            val maxHoc = max(hoc ?: 1, findMaxHoc(results.value?.results))
            val updateResult = raceResult.copy(
                hocPosition = hoc,
                finishCode = code,
                finish = finish,
                penalty = if (code == FinishCode.NSC) null else raceResult.penalty,
            )
            copy(
                raceResult = updateResult,
                maxHoc = maxHoc,
            )
        }
    }

    fun penalty(i: Int?) {
        setState {
            copy(
                raceResult = raceResult.copy(
                    penalty = i?.takeIf { it > 0 }
                )
            )
        }
    }

    fun selectOrc(action: Action, certificate: OrcCertificate) {
        setState {
            copy(
                raceResult = raceResult.copy(
                    orcCertificate = when (action) {
                        Action.Add -> certificate
                        Action.Delete -> null
                    }
                )
            )
        }
    }

    fun selectRating(ratingType: RatingType, rating: Int) {
        setState {
            copy(raceResult = raceResult.copy(phrfRating = rating, ratingType = ratingType))
        }
    }

    private fun RaceResultFull.asRaceResultSparse(): RaceResult {
        return RaceResult(
            id = id,
            raceId = raceId,
            boatId = boat.boat?.id ?: 0L,
            finish = finish,
            phrfRating = phrfRating,
            orcRef = orcCertificate?.refNo,
            hocPosition = hocPosition,
            bracketId = bracketId,
            raceClassId = raceClassId,
            penalty = penalty,
            ratingType = ratingType,
            finishCode = finishCode,
        )
    }

    private fun availableBoats(results: RaceScheduleResults?, boatSkippers: List<BoatSkipper>?): List<BoatSkipper> {
        return boatSkippers?.filter { bs ->
            val match = results?.results?.any { result ->
                result.boat.boat?.id == bs.boat?.id
            } ?: false
            !match
        } ?: emptyList()
    }

    fun postResult(onSuccess: (() -> Unit)? = null) {
        launch {
            withStateAsync { state ->
                if (state.isValid) {
                    setState { copy(results = Loading()) }
                    val results = Api.postResult(state.raceResult.asRaceResultSparse()).toAsync().flatMap {
                        onSuccess?.invoke()
                        Api.getRaceScheduleResults(raceId).toAsync()
                    }
                    val ft = state.results.value?.raceSchedule?.endTime
                    setState {
                        copy(
                            raceResult = RaceResultFull(
                                raceId = raceId,
                                finish = ft,
                                finishCode = ft?.let { FinishCode.TIME } ?: FinishCode.RET),
                            results = results,
                            boats = availableBoats(results.value, state.boatSkippers.value),
                            maxHoc = findMaxHoc(results.value?.results)
                        )
                    }
                } else {
                    setState { copy(results = results.error()) }
                }
            }
        }
    }

    fun findMaxHoc(): Int {
        return withState {
            findMaxHoc(it.results.value?.results)
        }
    }

    fun findMaxHoc(results: List<RaceResultFull>?): Int {
        return results?.maxOfOrNull {
            it.hocPosition ?: 1
        } ?: 1
    }

    fun deleteResult(id: Long) {
        setState {
            copy(results = Loading())
        }
        launch {
            Api.deleteResult(id)
            reload()
        }
    }

}


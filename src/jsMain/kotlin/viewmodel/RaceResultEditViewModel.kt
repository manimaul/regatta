package viewmodel

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.display
import kotlinx.datetime.Instant
import utils.*
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun correctionFactor(factor: Int?, boat: Boat?): Double {
    return factor?.let { cf ->
        boat?.phrfRating?.let { rating ->
            650.0 / (cf.toDouble() + rating.toDouble())
        }
    } ?: 1.0
}

fun boatRaceTime(race: RaceFull, boat: Boat?): RaceTime? {
    return boat?.raceClass?.let { brc ->
        race.raceTimes.firstOrNull { raceTime ->
            raceTime.raceClassCategory.id == brc.category
        }
    }
}

fun elapsedTime(start: Instant?, end: Instant?): String {
    return end?.let {
        start?.let {
            (end - start).display()
        }
    } ?: ""
}

fun elapsedTimeSec(start: Instant?, end: Instant?): String {
    return end?.let {
        start?.let {
            val t = end - start
            "${t.inWholeSeconds}s"
        }
    } ?: ""
}

fun boatCorrectedRaceTime(factor: Int?, start: Instant?, finish: Instant?, boat: Boat?): String {
    return boatCorrectedTime(factor, start, finish, boat)?.display() ?: ""
}

fun boatCorrectedTime(factor: Int?, start: Instant?, finish: Instant?, boat: Boat?): Duration? {
    if (start != null && finish != null) {
        val cf = correctionFactor(factor, boat)
        val seconds = ((finish - start).inWholeSeconds.toDouble() * cf).roundToLong()
        return seconds.toDuration(DurationUnit.SECONDS)
    }
    return null
}

interface RaceResultComputed {
    val boat: Boat?
    val raceTime: RaceTime?
    val finish: Instant?
    val boatName: String get() = boat?.name ?: ""
    val sail: String get() = boat?.sailNumber ?: ""
    val skipper: String get() = boat?.skipper?.fullName() ?: ""
    val boatType: String get() = boat?.boatType ?: ""
    val phrfRating: String get() = boat?.phrfRating?.toString() ?: "-"
    val startTime: String get() = raceTime?.startDate?.display() ?: ""
    val finishTime: String get() = finish?.display() ?: ""
    val elapsedTime: String get() = elapsedTime(raceTime?.startDate, finish)
    val elapsedTimeSec: String get() = elapsedTimeSec(raceTime?.startDate, finish)
    private val correctionFactor: Double get() = correctionFactor(raceTime?.correctionFactor, boat)
    val correctionFactorDisplay: String get() = "${correctionFactor.asDynamic().toFixed(3)}"
    val correctedTime: String
        get() = boatCorrectedRaceTime(
            raceTime?.correctionFactor,
            raceTime?.startDate,
            finish,
            boat
        )
    val correctedTimeSeconds: Duration?
        get() = boatCorrectedTime(
            raceTime?.correctionFactor,
            raceTime?.startDate,
            finish,
            boat
        )
}

data class RaceResultRecordComputed(
    val record: RaceResultFull
) : VmState, RaceResultComputed {
    override val boat: Boat get() = record.boat
    override val raceTime: RaceTime? get() = boatRaceTime(record.race, boat)
    override val finish: Instant? get() = record.finish ?: raceTime?.endDate
}


data class RaceResultEditState(
    val race: Async<RaceFull> = Loading(),
    val result: Async<Map<RaceClassCategory?, List<RaceResultRecordComputed>>> = Loading(),
    val boats: Async<List<Boat>> = Loading(),
    val categories: Async<List<RaceClassCategory>> = Loading(),
) : VmState

class RaceResultEditViewModel(
    private val raceId: Long,
    val addViewModel: RaceResultAddViewModel = RaceResultAddViewModel(),
) : BaseViewModel<RaceResultEditState>(RaceResultEditState()) {
    init {
        reload()
    }

    override fun reload() {
        setState {
//            combineAsync(
//                Api.getRace(raceId),
//                Api.getAllCategories(),
//                Api.getAllBoats(),
//                Api.getResults(raceId),
//            ) { r, c, b, e ->
//                ""
//            }
            val race = Api.getRace(raceId).toAsync()
            addViewModel.setRace(race.value)
            val categories = Api.getAllCategories().toAsync()
            copy(
                race = race,
                result = Api.getResults(raceId).toAsync().flatMap { xx -> categories.map { xx.reduce(it) } },
                boats = Api.getAllBoats().toAsync().map { it.filter { it.raceClass != null } },
                categories = categories
            )
        }
    }

    private fun List<RaceResultFull>.reduce(categories: List<RaceClassCategory>): Map<RaceClassCategory?, List<RaceResultRecordComputed>> {
        return this.map {
            RaceResultRecordComputed(it)
        }.sortedBy { it.correctedTimeSeconds }.groupBy { ea ->
            categories.find { it.id == ea.boat.raceClass?.category }
        }
    }

    fun addResult(value: RaceResultAddState) {
        value.asPost()?.let { post ->
            setState {
                copy(result = Loading(result.value))
            }
            addViewModel.reload()
            setState {
                copy(
                    result = Api.postResult(post).toAsync().map {
                        val list =
                            result.value?.values?.flatten()?.map { it.record }?.toMutableList()?.apply { add(it) }
                                ?: listOf(it)
                        list.reduce(categories.value ?: emptyList())
                    }
                )
            }
        }
    }
}
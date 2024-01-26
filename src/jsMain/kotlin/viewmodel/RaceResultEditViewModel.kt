package viewmodel

import com.mxmariner.regatta.data.*
import kotlinx.datetime.Instant
import utils.*
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun correctionFactor(raceTime: RaceTime?, boat: Boat?): Double {
    return raceTime?.correctionFactor?.let { cf ->
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

fun boatCorrectedRaceTime(raceTime: RaceTime?, boat: Boat?): String {
    return boatCorrectedTime(raceTime, boat)?.display() ?: ""
}

fun boatCorrectedTime(raceTime: RaceTime?, boat: Boat?): Duration? {
    return raceTime?.let {
        val cf = correctionFactor(it, boat)
        val seconds = ((raceTime.endDate - raceTime.startDate).inWholeSeconds.toDouble() * cf).roundToLong()
        seconds.toDuration(DurationUnit.SECONDS)
    }
}

interface RaceResultComputed {
    val boat: Boat?
    val raceTime: RaceTime?
    val boatName: String get() = boat?.name ?: ""
    val sail: String get() = boat?.sailNumber ?: ""
    val skipper: String get() = boat?.skipper?.fullName() ?: ""
    val boatType: String get() = boat?.boatType ?: ""
    val phrfRating: String get() = boat?.phrfRating?.toString() ?: "-"
    val startTime: String get() = raceTime?.startDate?.display() ?: ""
    val finishTime: String get() = raceTime?.endDate?.display() ?: ""
    val elapsedTime: String get() = elapsedTime(raceTime?.startDate, raceTime?.endDate)
    val elapsedTimeSec: String get() = elapsedTimeSec(raceTime?.startDate, raceTime?.endDate)
    private val correctionFactor: Double get() = correctionFactor(raceTime, boat)
    val correctionFactorDisplay: String get() = "${correctionFactor.asDynamic().toFixed(3)}"
    val correctedTime: String get() = boatCorrectedRaceTime(raceTime, boat)
    val correctedTimeSeconds: Duration? get() = boatCorrectedTime(raceTime, boat)
}

data class RaceResultRecordComputed(
    val record: RaceResultFull
) : VmState, RaceResultComputed {
    override val boat: Boat get() = record.boat
    override val raceTime: RaceTime? get() = boatRaceTime(record.race, boat)
}

data class RaceResultEditState(
    val race: Async<RaceFull> = Loading(),
    val result: Async<Map<RaceClass?, List<RaceResultComputed>>> = Loading(),
    val boats: Async<List<Boat>> = Loading(),
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
            val race = Api.getRace(raceId).toAsync()
            addViewModel.setRace(race.value)
            copy(
                race = race,
                result = Api.getResults(raceId).toAsync().map {
                    it.map {
                        RaceResultRecordComputed(it)
                    }.sortedBy { it.correctedTimeSeconds }.groupBy { it.record.boat.raceClass }
                },
                boats = Api.getAllBoats().toAsync()
            )
        }
    }

    fun saveResult() {

    }
}
package viewmodel

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.ClassSchedule
import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceResultFull
import com.mxmariner.regatta.data.RaceSchedule
import com.mxmariner.regatta.data.RatingType
import components.Action
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import utils.*
import kotlin.time.Duration.Companion.days

enum class RcTab(val title: String) {
    //start time - change
    //CF change

    //tabs
    // finish line tab
    // add time (now button, h-m-s) allow unknown boat

    // check in tab
    // easy add boat
    // paper report tab
    // future course tab
    RaceConfig("Race Config"),
    Checkin("Check-In"),
    FinishLine("Finish Line"),
}

enum class SyncState {
    Dirty,
    Working,
    Synced,
}

data class CheckIn(
    val bs: BoatSkipper,
    val checkedIn: Boolean,
    val result: RaceResultFull? = null,
    val startTime: Instant?
)

data class RcState(
    val races: Async<List<RaceSchedule>> = Loading(),
    val boats: Async<List<CheckIn>> = Loading(),
    val results: Async<Map<Long, RaceResultFull>> = Uninitialized,
    val addState: RaceResultAddState? = null,
    val checkinIds: List<Long> = emptyList(),
    val selectedRace: RaceSchedule? = null,
    val tab: RcTab = RcTab.RaceConfig,
    val syncState: SyncState = SyncState.Synced,
) : VmState

class RcViewModel : BaseViewModel<RcState>(RcState()) {

    val addVm = RaceResultAddViewModel(0)

    init {
        fetchRaces()
        launch {
            addVm.flow.collect { state ->
                setState {
                    copy(
                        addState = state
                    )
                }
            }
        }
        launch {
            while (true) {
                delay(3000)
                checkSchedule()
            }
        }
    }

    override fun reload() {
        setState { RcState() }
        fetchRaces()
    }

    private fun getCheckinIds(raceId: Long): List<Long> {
        return localStoreGetById<List<Long>>("checkin_${raceId}") ?: emptyList()
    }

    private fun getCheckins(
        ids: List<Long>,
        boats: List<BoatSkipper>,
        results: Map<Long, RaceResultFull>?,
        raceSchedule: RaceSchedule?,
    ): List<CheckIn> {
        return boats.sortedBy { it.boat?.name }.map {
            CheckIn(
                bs = it,
                checkedIn = ids.contains(it.boat?.id) || results?.contains(it.boat?.id) == true,
                result = results?.get(it.boat?.id),
                startTime = raceSchedule?.findClassSchedule(it)?.startDate
            )
        }
    }

    private fun fetchRaces() {
        setState {
            val t = now() - 1.days
            val races = Api.getAllRaces(currentYear().toInt()).toAsync()
            races.value?.firstOrNull { t < it.startTime }?.let { selectRace(it) }
            copy(
                races = races,
            )
        }
    }

    fun refreshResults(race: RaceSchedule?) {
        launch {
            addVm.flow.filter { it.results.complete }.take(1).collect { state ->
                setState {
                    val results = state.results.map { it.results.associateBy { it.boat.boat?.id ?: 0 } }
                    val boats = Api.getAllBoats().toAsync().map { getCheckins(checkinIds, it, results.value, race) }
                    copy(
                        results = results,
                        boats = boats
                    )
                }
            }
        }
    }

    fun selectRace(race: RaceSchedule?) {
        race?.race?.id?.let {
            if (it != addVm.raceId) {
                addVm.raceId = it

            }
        }
        setState { copy(boats = Loading(), checkinIds = emptyList(), results = Loading(), selectedRace = null) }
        refreshResults(race)
        setState {
            val checkinIds = race?.race?.id?.let { getCheckinIds(it) } ?: emptyList()
            copy(
                selectedRace = race,
                syncState = SyncState.Synced,
                checkinIds = checkinIds,
            )
        }
    }

    fun selectTab(tab: RcTab) {
        setState { copy(tab = tab) }
    }

    fun checkOut(bs: BoatSkipper) {
        setState {
            val c = bs.boat?.id?.let {
                (checkinIds.asSequence()).filter { it != bs.boat?.id }.toList().apply {
                    localStoreSetById("checkin_${selectedRace?.race?.id}", this)
                }
            } ?: checkinIds
            copy(checkinIds = c, boats = boats.map { getCheckins(c, it.map { it.bs }, results.value, selectedRace) })
        }
    }

    fun checkIn(bs: BoatSkipper) {
        setState {
            val c = bs.boat?.id?.let {
                (sequenceOf(it) + checkinIds.asSequence()).distinct().toList().apply {
                    localStoreSetById("checkin_${selectedRace?.race?.id}", this)
                }
            } ?: checkinIds
            copy(checkinIds = c, boats = boats.map { getCheckins(c, it.map { it.bs }, results.value, selectedRace) })
        }
    }

    fun RaceSchedule.findClassSchedule(boatSkipper: BoatSkipper): ClassSchedule? {
        return boatSkipper.boat?.let { boat ->
            schedule.firstOrNull {
                boat.ratingType == it.raceClass.ratingType
            } ?: run {
                schedule.firstOrNull {
                    boat.ratingType.isPHRF && it.raceClass.ratingType.isPHRF || boat.ratingType.isORC && it.raceClass.ratingType.isORC
                }
            }
        }
    }

    fun focus(checkin: CheckIn) {
        val t = withState {
            it.selectedRace?.endTime
        }
        checkin.result?.let {
            println("focusing result $checkin")
            addVm.focusResultForEdit(it)
        } ?: run {
            addVm.selectBoat(checkin.bs, false)
            addVm.setFinish(FinishCode.TIME, t, null)
        }
    }

    fun selectRating(ratingType: RatingType, rating: Int) {
        addVm.selectRating(ratingType, rating)

    }

    fun selectOrc(action: Action, certificate: OrcCertificate) {
        addVm.selectOrc(action, certificate)
    }

    fun postResult() {
        val race = withState { it.selectedRace }
        addVm.postResult {
            refreshResults(race)
        }
    }

    fun delete(result: RaceResultFull) {
        addVm.deleteResult(result.id)
        val race = withState { it.selectedRace }
        setState {
            val ids = result.boat.boat?.id?.takeIf { !checkinIds.contains(it) }?.let {
                checkinIds.toMutableList().apply { add(it) }
            } ?: checkinIds
            copy(checkinIds = ids)
        }
        refreshResults(race)
    }

    fun penalty(value: Int?) {
        addVm.penalty(value)
    }

    fun selectBracket(bracket: Bracket) {
        addVm.selectBracket(bracket)
    }

    fun selectClass(raceClass: RaceClass) {
        addVm.selectClass(raceClass)
    }

    fun selectBoat(boatSkipper: BoatSkipper?) {
        addVm.selectBoat(boatSkipper)
    }

    fun setFinish(code: FinishCode, finish: Instant?, hoc: Int?) {
        addVm.setFinish(code, finish, hoc)
    }

    fun selectPhrfBFactor(cf: Int?) {
        setState {
            val sr = selectedRace?.let {
                val factor = cf ?: correctionFactorDefault
                it.copy(
                    race = it.race.copy(
                        phrfBFactor = cf ?: correctionFactorDefault,
                        orc3Band = Orc3Band.fromPhrfBFactor(factor),
                        orc5Band = Orc5Band.fromPhrfBFactor(factor),
                    )
                )
            }
            copy(
                syncState = SyncState.Dirty,
                selectedRace = sr
            )
        }
    }

    fun classStart(classSchedule: ClassSchedule, start: Instant) {
        setState {
            val sr = selectedRace?.let {
                val sch = it.schedule.map { cs ->
                    if (cs.raceClass.id == classSchedule.raceClass.id) {
                        classSchedule.copy(startDate = start)
                    } else {
                        cs
                    }
                }
                it.copy(schedule = sch)
            }
            copy(
                selectedRace = sr,
                syncState = SyncState.Dirty
            )
        }
    }

    private fun checkSchedule() {
        val isDirty = withState { state ->
            state.syncState == SyncState.Dirty
        }

        if (isDirty) {
            setState { copy(syncState = SyncState.Working) }
            setState {
                val ps = selectedRace?.let {
                    Api.postSchedule(it).toAsync()
                }?.value
                val rs = ps?.let {
                    races.map {
                        it.map {
                            if (it.race.id == ps.race.id) {
                                ps
                            } else {
                                it
                            }
                        }
                    }
                } ?: races
                copy(
                    syncState = ps?.let { SyncState.Synced } ?: SyncState.Dirty,
                    selectedRace = ps ?: selectedRace,
                    races = rs
                )
            }
        }
    }

}

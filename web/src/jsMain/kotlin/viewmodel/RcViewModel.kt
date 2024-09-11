package viewmodel

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.ClassSchedule
import com.mxmariner.regatta.data.RaceResult
import com.mxmariner.regatta.data.RaceSchedule
import com.mxmariner.regatta.display
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import utils.*
import kotlin.math.max
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
    val result: RaceResult? = null,
    val startTime: Instant?
)

data class RcFocus(
    val bs: BoatSkipper,
    val finish: Instant?,
    val raceStart: Instant?,
    val penalty: Int?,
    val hocPosition: Int?,
    val maxHoc: Int
) {

    fun isValid() : Boolean {
        return raceStart?.let { s ->
           finish?.let { f ->
              f > s
           }
        } ?: false
    }
    fun elapsedTime() : String? {
        return raceStart?.let { s ->
            finish?.let { f ->
                (f - s).display()
            }
        }
    }

    fun asResult(raceId: Long?): RaceResult? {
        return raceId?.let {
            bs.boat?.id?.let {
                RaceResult(
                    raceId = raceId,
                    boatId = it,
                    finish = finish,
                    phrfRating = bs.boat?.phrfRating,
                    windseeker = bs.boat?.windseeker,
                    penalty = penalty,
                    hocPosition = hocPosition,
                )
            }
        }
    }
}

data class RcState(
    val races: Async<List<RaceSchedule>> = Loading(),
    val boats: Async<List<CheckIn>> = Loading(),
    val results: Async<Map<Long, RaceResult>> = Uninitialized,
    val focus: RcFocus? = null,
    val checkinIds: List<Long> = emptyList(),
    val selectedRace: RaceSchedule? = null,
    val tab: RcTab = RcTab.RaceConfig,
    val syncState: SyncState = SyncState.Synced,
) : VmState

class RcViewModel : BaseViewModel<RcState>(RcState()) {
    init {
        fetchRaces()
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
        results: Map<Long, RaceResult>?,
        raceSchedule: RaceSchedule?,
    ): List<CheckIn> {
        return boats.sortedBy { it.boat?.name }.map {
            CheckIn(
                bs = it,
                checkedIn = ids.contains(it.boat?.id) || results?.contains(it.boat?.id) == true,
                result = results?.get(it.boat!!.id),
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

    fun selectRace(race: RaceSchedule?) {
        setState { copy(boats = Loading(), checkinIds = emptyList(), results = Loading()) }
        setState {
            val checkinIds = race?.race?.id?.let { getCheckinIds(it) } ?: emptyList()
            val results = race?.race?.id?.let { raceId ->
                Api.getResults(raceId).toAsync().map { it.associateBy { it.boatId } }
            }
            val boats = Api.getAllBoats().toAsync().map { getCheckins(checkinIds, it, results?.value, race) }
            copy(
                selectedRace = race,
                syncState = SyncState.Synced,
                checkinIds = checkinIds,
                boats = boats,
                results = results ?: Uninitialized,
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
            val isPhrf = boat.phrfRating != null
            schedule.filter {
                (isPhrf && it.raceClass.isPHRF) ||
                        (it.raceClass.wsFlying == boat.windseeker?.flyingSails)
            }.map {
                it
            }.firstOrNull()
        }
    }

    fun raceDayFinish(race: RaceSchedule?, bs: BoatSkipper, finish: Instant): Instant {
        race?.findClassSchedule(bs)?.startDate?.let { start ->
            val l = start.localDateTime()
            val f = finish.localDateTime()
            LocalDateTime(
                l.year,
                l.monthNumber,
                l.dayOfMonth,
                f.hour,
                f.minute,
                f.second
            ).instant()
        } ?: finish

        return finish
    }

    fun focus(bs: BoatSkipper?, finish: Instant?) {
        setState {
            val result = results.value?.get(bs?.boat?.id)
            val f = bs?.let {
                finish?.let {
                    RcFocus(
                        bs = bs,
                        finish = raceDayFinish(selectedRace, bs, it),
                        penalty = result?.penalty,
                        hocPosition = result?.hocPosition,
                        raceStart = selectedRace?.findClassSchedule(bs)?.startDate,
                        maxHoc = findMaxHoc()
                    )
                }
            }
            copy(focus = f)
        }
    }

    private fun findMaxHoc(): Int {
        return withState {
            it.results.value?.values?.fold(0) { acc, raceResult ->
                max(
                    acc,
                    raceResult.hocPosition ?: 0
                )
            } ?: 0
        }
    }

    fun saveFocus() {
        withState { state ->
            val result = state.focus?.asResult(state.selectedRace?.race?.id)
            setState { copy(focus = null) }

            setState {
                result?.let { Api.postResult(it).toAsync().value }?.let { result ->
                    val results = results.map { it.toMutableMap().also { it.put(result.boatId, result) } }
                    copy(
                        boats = boats.map { getCheckins(checkinIds, it.map { it.bs }, results.value, selectedRace) },
                        results = results
                    )
                } ?: this
            }
        }
    }

    fun delete(result: RaceResult) {
        setState {
            val results = Api.deleteResult(result.id).toAsync().flatMap {
                results.map { it.toMutableMap().also { it.remove(result.boatId) } }
            }
            val ids = if (!checkinIds.contains(result.boatId)) {
                (sequenceOf(result.boatId) + checkinIds.asSequence()).distinct().toList().apply {
                    localStoreSetById("checkin_${selectedRace?.race?.id}", this)
                }
            } else {
                checkinIds
            }
            copy(
                checkinIds = ids,
                boats = boats.map { getCheckins(ids, it.map { it.bs }, results.value, selectedRace) },
                results = results
            )
        }
    }


    fun penalty(value: Int?) {
        setState {
            copy(
                focus = focus?.copy(
                    penalty = value?.takeIf { it > 0 },
                )
            )
        }
    }

    fun hoc(value: Int?) {
        setState { copy(focus = focus?.copy(hocPosition = value?.let { max(1, it) }, finish = null)) }
    }

    fun setFinish(value: Instant?) {
        setState { copy(focus = focus?.copy(finish = value, hocPosition = null)) }
    }

    fun setCf(cf: Int?) {
        setState {
            val sr = selectedRace?.let {
                it.copy(race = it.race.copy(correctionFactor = cf ?: correctionFactorDefault))
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
        val sync = withState { state ->
            state.syncState == SyncState.Dirty
        }

        if (sync) {
            setState { copy(syncState = SyncState.Working) }
            setState {
                val ps = selectedRace?.let {
                    Api.postSchedule(it).toAsync()
                }?.value
                copy(
                    syncState = ps?.let { SyncState.Synced } ?: SyncState.Dirty,
                    selectedRace = ps ?: selectedRace
                )
            }
        }
    }

}

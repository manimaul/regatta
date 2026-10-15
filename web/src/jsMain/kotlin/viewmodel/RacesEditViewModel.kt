package viewmodel

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.*
import kotlinx.coroutines.launch
import utils.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * One class's start/end times and brackets while it is being filled in on the add/edit race modal. Becomes a
 * [ClassSchedule] once a class and at least one bracket are picked and the end time follows the start.
 */
data class ClassScheduleDraft(
    /** Stable identity for the composition while drafts are added and removed. */
    val key: Int,
    val raceClass: RaceClass? = null,
    val brackets: List<Bracket> = emptyList(),
    val startDate: Instant,
    val endDate: Instant,
) {
    fun asSchedule(): ClassSchedule? =
        if (raceClass != null && brackets.isNotEmpty() && endDate > startDate) {
            ClassSchedule(raceClass, brackets, startDate, endDate)
        } else {
            null
        }
}

data class RacesEditState(
    val race: Async<RaceSchedule> = Loading(),
    val skippers: Async<List<Person>> = Loading(),
    val series: Async<List<Series>> = Loading(),
    val classes: Async<List<RaceClassBrackets>> = Loading(),
    val drafts: List<ClassScheduleDraft> = emptyList(),
) : VmState {

    /** The race with the drafts as its schedule, or null while a draft is incomplete or there is none. */
    fun draftedSchedule(): RaceSchedule? {
        val schedules = drafts.map { it.asSchedule() ?: return null }
        return race.value?.copy(schedule = schedules)?.takeIf { it.validate() }
    }
}

fun RaceSchedule.validate(): Boolean {
    return race.name.isNotBlank() && schedule.isNotEmpty()
}

/**
 * Backs the add/edit race modal: a new race when [raceId] is 0, otherwise the existing race with its class times
 * loaded as editable drafts.
 */
class RacesEditViewModel(
    val raceId: Long,
    private val onSaved: (RaceSchedule) -> Unit,
    private val onDeleted: (RaceSchedule) -> Unit = {},
) : BaseViewModel<RacesEditState>(RacesEditState()) {

    private var draftKeys = 0

    init {
        reload()
    }

    override fun reload() {
        setState {
            val loaded = if (raceId == 0L) Complete(RaceSchedule()) else Api.getRaceSchedule(raceId).toAsync()
            copy(
                race = loaded,
                drafts = loaded.value?.schedule?.map { it.toDraft() } ?: emptyList(),
                skippers = Api.getAllPeople().toAsync(),
                series = Api.allSeries().toAsync(),
                classes = Api.getAllClasses().toAsync(),
            )
        }
    }

    private fun ClassSchedule.toDraft() =
        ClassScheduleDraft(key = ++draftKeys, raceClass = raceClass, brackets = brackets, startDate = startDate, endDate = endDate)

    /** Adds an empty draft, starting from the previous draft's times (classes usually start close together). */
    fun addDraft() {
        setState {
            val previous = drafts.lastOrNull()
            val start = previous?.startDate ?: now()
            copy(
                drafts = drafts + ClassScheduleDraft(
                    key = ++draftKeys,
                    startDate = start,
                    endDate = previous?.endDate ?: start.plus(4.hours),
                )
            )
        }
    }

    fun removeDraft(key: Int) {
        setState { copy(drafts = drafts.filterNot { it.key == key }) }
    }

    fun updateDraft(key: Int, change: ClassScheduleDraft.() -> ClassScheduleDraft) {
        setState { copy(drafts = drafts.map { if (it.key == key) it.change() else it }) }
    }

    /** Picking a class clears the brackets, since they belong to the class. */
    fun selectDraftClass(key: Int, raceClass: RaceClass?) {
        updateDraft(key) { copy(raceClass = raceClass, brackets = emptyList()) }
    }

    fun save(schedule: RaceSchedule) {
        launch {
            Api.postSchedule(schedule)
            onSaved(schedule)
        }
    }

    fun setRaceName(name: String) {
        setState {
            copy(race = race.map { it.copy(race = it.race.copy(name = name)) })
        }
    }

    fun setSeries(series: Series) {
        setState {
            copy(
                race = race.map {
                    it.copy(
                        series = series,
                        race = it.race.copy(
                            seriesId = series.id,
                        )
                    )
                }
            )
        }
    }

    fun setRC(rc: Person?) {
        setState {
            copy(
                race = race.map {
                    it.copy(
                        race = it.race.copy(rcId = rc?.id),
                        rc = rc,
                    )
                }
            )
        }
    }

    fun setCF(cf: Int?) {
        setState {
            copy(race = race.map { it.copy(race = it.race.copy(phrfBFactor = cf ?: correctionFactorDefault)) })
        }
    }

    fun delete() {
        val schedule = flow.value.race.value ?: return
        launch {
            if (Api.deleteRace(raceId).ok) {
                onDeleted(schedule)
            } else {
                alertsViewModel.showAlert("${schedule.race.name} could not be deleted.")
            }
        }
    }
}

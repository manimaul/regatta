package viewmodel

import com.mxmariner.regatta.data.*
import kotlinx.datetime.Instant
import utils.*
import kotlin.time.Duration.Companion.hours

data class RgAddTimeState(
    val classes: Async<List<RaceClassBrackets>> = Loading(),
    val raceClass: RaceClass? = null,
    val availableBrackets: List<Bracket>? = null,
    val brackets: List<Bracket> = emptyList(),
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val focus: ClassSchedule? = null,
) : VmState {

    fun isValid(): Boolean {
        return if (raceClass != null && brackets.isNotEmpty() && startDate != null && endDate != null) {
            endDate > startDate
        } else {
            false
        }
    }

    fun asSchedule() = if (isValid()) ClassSchedule(
        raceClass!!, brackets, startDate!!, endDate!!
    ) else null

}

class RgAddTimeViewModel : BaseViewModel<RgAddTimeState>(RgAddTimeState()) {

    private val removed = mutableListOf<RaceClassBrackets>()

    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(
                classes = getRcb()
            )
        }
    }

    private suspend fun RgAddTimeState.getRcb(): Async<List<RaceClassBrackets>> {
        val ids = classes.value?.map { it.raceClass.id } ?: emptySet()
        return Api.getAllClasses().toAsync().map { all ->
            all.filter { rcb -> !ids.contains(rcb.raceClass.id) }
        }
    }

    fun selectClass(raceClass: RaceClass?) {
        setState {
            copy(
                raceClass = raceClass,
                brackets = emptyList(),
                availableBrackets = classes.value?.firstOrNull { it.raceClass.id == raceClass?.id }?.brackets
            )
        }
    }

    fun startTime(t: Instant?) {
        setState {
            copy(
                startDate = t,
                endDate = endDate ?: t?.plus(4.hours)
            )
        }
    }

    fun endTime(t: Instant) {
        setState { copy(endDate = t) }
    }

    fun addBracket(b: Bracket) {
        setState {
            copy(
                brackets = brackets.toMutableList().apply { add(b) },
            )
        }
    }

    fun removeBracket(b: Bracket) {
        setState {
            copy(
                brackets = brackets.toMutableList().apply { remove(b) },
            )
        }
    }

    fun resetOption(classId: Long) {
        removed.indexOfFirst { it.raceClass.id == classId }.takeIf { it >= 0 }?.let { i ->
            val c = removed.removeAt(i)
            setState {
                copy(classes = classes.map { it.toMutableList().apply { add(c) }.sortedBy { it.raceClass.sort } })
            }
        }
    }

    fun removeOption(classId: Long) {
        setState {
            copy(classes = classes.removeOption(classId).also {
                it.value?.firstOrNull()?.let { selectClass(it.raceClass) }
            })
        }
    }

    private suspend fun Async<List<RaceClassBrackets>>.removeOption(classId: Long?) = this.map {
        it.toMutableList().apply {
            indexOfFirst { it.raceClass.id == classId }.takeIf { it >= 0 }?.let { i ->
                removed.add(removeAt(i))
            }
        }
    }

    fun focusSchedule(schedule: ClassSchedule) {
        resetOption(schedule.raceClass.id)
        setState {
            copy(
                focus = schedule,
                startDate = schedule.startDate,
                endDate = schedule.endDate
            )
        }
        println("focusing schedule ${schedule.raceClass.name} ${schedule.brackets.size}")
        selectClass(schedule.raceClass)
        schedule.brackets.forEach {
            println("adding bracket for focus ${it.name}")
            addBracket(it)
        }
    }

    fun removeFocus() {
        setState {
            copy(
                classes = classes.removeOption(focus?.raceClass?.id),
                focus = null,
            )
        }
        selectClass(null)
    }
}
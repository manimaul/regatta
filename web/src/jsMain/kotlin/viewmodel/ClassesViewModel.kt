package viewmodel

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import kotlinx.coroutines.launch
import utils.*

data class ClassesState(
    val editClass: RaceClassBrackets = RaceClassBrackets(brackets = listOf(Bracket())),
    val classList: Async<List<RaceClassBrackets>> = Loading(),
    val sortMode: Boolean = false,
) : VmState

class ClassesViewModel() : BaseViewModel<ClassesState>(ClassesState()) {

    init {
        reload()
    }

    override fun reload() {
        launch {
            setState { ClassesState() }
            setState {
                copy(classList = Api.getAllClasses().toAsync())
            }
        }
    }

    fun nextSort(): Int {
        return flow.value.classList.value?.lastOrNull()?.raceClass?.sort?.let { it + 1 } ?: 0
    }

    fun isUpsertClassBracketsValid(rcb: RaceClassBrackets): Boolean {
        val nameValid = rcb.raceClass.name.isNotBlank()
        if (!nameValid) {
           return false
        }
        val bracketFull = rcb.brackets.isNotEmpty()
        if (!bracketFull) {
            return false
        }
        return rcb.brackets.fold(true) { l, r ->
            l && r.name.isNotBlank() && r.minRating <= r.maxRating
        }
    }

    fun upsertClassBrackets(rcb: RaceClassBrackets) {
        setState {
            val list = Api.postClass(
                if (rcb.raceClass.id == 0L) {
                    rcb.copy(raceClass = rcb.raceClass.copy(sort = nextSort()))
                } else {
                    rcb
                }
            ).toAsync().flatMap {
                Api.getAllClasses().toAsync()
            }
            copy(
                editClass = RaceClassBrackets(
                    raceClass = RaceClass(
                        sort = nextSort()
                    ),
                    brackets = listOf(Bracket())
                ),
                classList = list
            )
        }
    }

    fun editClass(raceClass: RaceClassBrackets) {
        setState { copy(editClass = raceClass) }
    }

    fun delete(raceClass: RaceClass) {
        setState {
            copy(
                classList = Api.deleteClass(raceClass.id).toAsync().flatMap { Api.getAllClasses().toAsync() }
            )
        }
    }

    fun delete(raceClassBrackets: RaceClassBrackets) {
        setState { copy(classList = Loading()) }
        launch {
            if (raceClassBrackets.brackets.mapNotNull {
                    Api.deleteBracket(it.id).toAsync().takeIf { it is Complete }
                }.size == raceClassBrackets.brackets.size) {

                setState {
                    copy(
                        classList = Api.deleteClass(raceClassBrackets.raceClass.id).toAsync()
                            .flatMap { Api.getAllClasses().toAsync() }
                    )
                }
            }
        }
    }

    fun delete(bracket: Bracket) {
        setState {
            copy(
                classList = Api.deleteBracket(bracket.id).toAsync().flatMap { Api.getAllClasses().toAsync() }
            )
        }
    }

    fun sortMode(b: Boolean) {
        setState { copy(sortMode = b) }
    }

    fun saveClassOrder(order: List<RaceClass>) {
        setState {
            copy(
                classList = classList.loading(),
                sortMode = false,
            )
        }
        setState {
            copy(
                classList = Api.postClassList(order).toAsync()
            )
        }
    }

    fun addBracket() {
        setState {
            val list = editClass.brackets.toMutableList()
            list.add(Bracket())
            copy(editClass = editClass.copy(brackets = list))
        }
    }

    fun removeBracket(index: Int) {
        setState {
            val list = editClass.brackets.toMutableList()
            list.removeAt(index)
            copy(editClass = editClass.copy(brackets = list))
        }
    }

    fun updateBracket(index: Int, bracket: Bracket) {
        setState {
            val list = editClass.brackets.toMutableList()
            list[index] = bracket
            copy(editClass = editClass.copy(brackets = list))
        }
    }
}


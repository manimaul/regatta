package viewmodel

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.moveItem
import kotlinx.coroutines.launch
import utils.*

data class ClassesState(
    val editBracketId: Long? = 0L,
    val editClassId: Long? = 0L,
    val classList: Async<List<RaceClassBrackets>> = Loading(),
) : VmState

class ClassesViewModel(
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<ClassesState>(ClassesState()) {

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

    fun editBracket(bracket: Bracket?) {
        setState { copy(editBracketId = bracket?.id ?: 0L) }
    }

    fun nextSort(): Int {
        return flow.value.classList.value?.lastOrNull()?.raceClass?.sort?.let { it + 1 } ?: 0
    }

    fun upsertClass(raceClass: RaceClass) {
        val rc = if (raceClass.id == 0L) {
            raceClass.copy(sort = nextSort())
        } else {
            raceClass
        }

        setState {
            val list =
                Api.postClass(rc).toAsync().flatMap { Api.getAllClasses().toAsync() }
            copy(
                editClassId = 0L,
                classList = list,
            )
        }
    }

    fun upsertBracket(bracket: Bracket) {
        setState {
            val list = Api.postBracket(bracket).toAsync().flatMap { Api.getAllClasses().toAsync() }
            copy(
                editBracketId = 0L,
                classList = list,
            )
        }
    }

    fun editClass(raceClass: RaceClass?) {
        setState { copy(editClassId = raceClass?.id) }
    }

    fun delete(raceClass: RaceClass) {
        setState {
            copy(
                classList = Api.deleteClass(raceClass.id).toAsync().flatMap { Api.getAllClasses().toAsync() }
            )
        }
    }

    fun delete(bracket: Bracket) {
        setState {
            copy(
                classList = Api.deleteBracket(bracket.id).toAsync().flatMap { Api.getAllClasses().toAsync() }
            )
        }
    }

    fun moveUp(raceClass: RaceClass) {
        flow.value.classList.value
            ?.map { it.raceClass }
            ?.moveItem(up = true) { it.id == raceClass.id }
            ?.let { lst ->
                setState {
                    lst.forEachIndexed { index, raceClass ->
                        Api.postClass(raceClass.copy(sort = index))
                    }
                    copy(
                        editClassId = null,
                        classList = Api.getAllClasses().toAsync(),
                    )
                }
            }
    }

    fun moveDown(raceClass: RaceClass) {
        flow.value.classList.value
            ?.map { it.raceClass }
            ?.moveItem(up = false) { it.id == raceClass.id }
            ?.let { lst ->
                setState {
                    lst.forEachIndexed { index, raceClass ->
                        Api.postClass(raceClass.copy(sort = index))
                    }
                    copy(
                        editClassId = null,
                        classList = Api.getAllClasses().toAsync(),
                    )
                }
            }
    }


}

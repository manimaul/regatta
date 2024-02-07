package viewmodel

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import kotlinx.coroutines.launch
import utils.*

data class ClassesState(
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

    fun editBracket(rc: Bracket?) {
        routeVm.pushRoute("/bracket/${rc?.id}")
    }

    fun upsertCategory(raceClass: RaceClass) {
        setState {
            val list = Api.postCategory(raceClass).toAsync().flatMap { Api.getAllClasses().toAsync() }
            copy(
                classList = list
            )
        }
    }

    fun upsertBracket(bracket: Bracket) {
        setState {
            val list = Api.postBracket(bracket).toAsync().flatMap { Api.getAllClasses().toAsync() }
            copy(
                classList = list
            )
        }
    }

    fun editClass(raceClass: RaceClass) {
        routeVm.pushRoute("/class/${raceClass.id}")
    }
}

package viewmodel

import com.mxmariner.regatta.data.RaceClass
import components.routes.ClassEdit
import kotlinx.coroutines.launch
import utils.*

data class ClassEditState(
    val raceClass: Async<RaceClass> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class ClassEditViewModel(
    val id: Long?,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<ClassEditState>(ClassEditState()) {
    init {
        launch {
            id?.let {
                setState {
                    ClassEditState(
                        raceClass = Api.getClass(id).toAsync(),
                        operation = Operation.Fetched
                    )
                }
            }
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    fun upsert(newClass: RaceClass) {
        setState {
            copy(
                raceClass = Api.postClass(newClass).toAsync(),
                operation = Operation.Updated
            )
        }
    }

    fun delete(raceClass: RaceClass) {
        raceClass.id?.let { id ->
            setState {
                copy(
                    raceClass = Api.deleteClass(id).toAsync().map { raceClass },
                    operation = Operation.Deleted
                )
            }
        }
    }
}
package viewmodel

import com.mxmariner.regatta.data.RaceClass
import utils.*

data class ClassEditState(
    val series: Async<RaceClass> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class ClassEditViewModel(
    val id: Long,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<ClassEditState>(ClassEditState()) {
    init {
        reload()
    }

    override fun reload() {
        setState {
            ClassEditState(
                series = Api.getClass(id).toAsync(),
                operation = Operation.Fetched
            )
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    fun upsert(newClass: RaceClass) {
        setState {
            copy(
                series = Api.postClass(newClass).toAsync(),
                operation = Operation.Updated
            )
        }
    }

    fun delete(raceClass: RaceClass) {
        raceClass.id?.let { id ->
            setState {
                copy(
                    series = Api.deleteClass(id).toAsync().map { raceClass },
                    operation = Operation.Deleted
                )
            }
        }
    }
}
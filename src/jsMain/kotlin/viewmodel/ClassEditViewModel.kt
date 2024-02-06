package viewmodel

import com.mxmariner.regatta.data.Bracket
import utils.*

data class ClassEditState(
    val series: Async<Bracket> = Uninitialized,
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

    fun upsert(newClass: Bracket) {
        setState {
            copy(
                series = Api.postClass(newClass).toAsync(),
                operation = Operation.Updated
            )
        }
    }

    fun delete(bracket: Bracket) {
        bracket.id?.let { id ->
            setState {
                copy(
                    series = Api.deleteClass(id).toAsync().map { bracket },
                    operation = Operation.Deleted
                )
            }
        }
    }
}
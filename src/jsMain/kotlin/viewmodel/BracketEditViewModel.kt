package viewmodel

import com.mxmariner.regatta.data.Bracket
import utils.*

data class BracketEditState(
    val series: Async<Bracket> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class BracketEditViewModel(
    val id: Long,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<BracketEditState>(BracketEditState()) {
    init {
        reload()
    }

    override fun reload() {
        setState {
            BracketEditState(
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
                series = Api.postBracket(newClass).toAsync(),
                operation = Operation.Updated
            )
        }
    }

    fun delete(bracket: Bracket) {
        setState {
            copy(
                series = Api.deleteBracket(bracket.id).toAsync().map { bracket },
                operation = Operation.Deleted
            )
        }
    }
}
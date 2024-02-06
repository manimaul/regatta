package viewmodel

import com.mxmariner.regatta.data.RaceClass
import utils.*

data class CategoryEditState(
    val category: Async<RaceClass> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class CategoryEditViewModel(
    private val id: Long,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<CategoryEditState>(CategoryEditState()) {

    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(
                category = Api.getCategory(id).toAsync().mapErrorMessage { "foobar" },
                operation = Operation.Fetched,
            )
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    fun delete(cat: RaceClass) {
        cat.id?.let {
            setState {
                copy(
                    category = category.loading(),
                )
            }
            setState {
                copy(
                    category = Api.deleteCategory(cat.id).toAsync().map { cat }
                        .mapErrorMessage { "failed to delete '${cat.name}'!" },
                    operation = Operation.Deleted,
                )
            }
        }
    }

    fun upsert(cat: RaceClass) {
        setState {
            copy(
                category = category.loading(),
            )
        }
        setState {
            copy(
                category = Api.postCategory(cat).toAsync()
                    .map { it.toRaceClass() }
                    .mapErrorMessage { "failed to update'${cat.name}'!" },
                operation = Operation.Updated,
            )
        }
    }
}
package viewmodel

import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.RaceClassCat
import com.mxmariner.regatta.data.RaceClassCategory
import utils.*

data class CategoryEditState(
    val category: Async<RaceCategory> = Uninitialized,
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

    fun delete(cat: RaceCategory) {
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

    fun upsert(cat: RaceCategory) {
        setState {
            copy(
                category = category.loading(),
            )
        }
        setState {
            copy(
                category = Api.postCategory(cat).toAsync()
                    .map { it.toCategory() }
                    .mapErrorMessage { "failed to update'${cat.name}'!" },
                operation = Operation.Updated,
            )
        }
    }
}
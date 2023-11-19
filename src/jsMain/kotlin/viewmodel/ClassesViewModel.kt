package viewmodel

import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.*

data class ClassesState(
    val classList: Async<List<RaceClassCategory>> = Uninitialized,
    val deleteCat: RaceClassCategory? = null,
    val deletedCat: Async<RaceClassCategory> = Uninitialized,
) : VmState

class ClassesViewModel(
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<ClassesState>(ClassesState()) {

    init {
        reload()
    }

    fun reload(pause: Long? = null) {
        launch {
            setState {
                copy(deleteCat = null, deletedCat = Uninitialized)
            }
            pause?.let { delay(it) }
            setState {
                copy(classList = Api.getAllCategories().toAsync())
            }
        }
    }

    fun setEditClass(rc: RaceClass?) {
        routeVm.pushRoute("/class/${rc?.id}")
    }

    fun upsertCategory(category: RaceCategory) {
        setState {
            val list = Api.postCategory(category).toAsync().flatMap { Api.getAllCategories().toAsync() }
            copy(
                classList = list
            )
        }
    }

    fun upsertClass(raceClass: RaceClass) {
        setState {
            val list = Api.postClass(raceClass).toAsync().flatMap { Api.getAllCategories().toAsync() }
            copy(
                classList = list
            )
        }
    }

    fun setDeleteCategory(cat: RaceClassCategory?) {
        setState {
            copy(deleteCat = cat)
        }
    }

    fun delete(cat: RaceClassCategory) {
        cat.id?.let {
            setState {
                copy(
                    deletedCat = Api.deleteCategory(cat.id).toAsync().map { cat }
                        .mapErrorMessage { "failed to delete '${cat.name}'!" }
                )
            }
        }
    }
}

package viewmodel

import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import kotlinx.coroutines.launch
import utils.*

data class ClassesState(
    val classList: Async<List<RaceClassCategory>> = Loading(),
) : VmState

class ClassesViewModel(
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<ClassesState>(ClassesState()) {

    init {
        reload()
    }

    fun reload() {
        launch {
            setState { ClassesState() }
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

    fun editCategory(cat: RaceClassCategory) {
        routeVm.pushRoute("/category/${cat.id}")
    }
}

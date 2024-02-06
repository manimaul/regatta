package viewmodel

import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.Bracket
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

    override fun reload() {
        launch {
            setState { ClassesState() }
            setState {
                copy(classList = Api.getAllCategories().toAsync())
            }
        }
    }

    fun setEditClass(rc: Bracket?) {
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

    fun upsertClass(bracket: Bracket) {
        setState {
            val list = Api.postClass(bracket).toAsync().flatMap { Api.getAllCategories().toAsync() }
            copy(
                classList = list
            )
        }
    }

    fun editCategory(cat: RaceClassCategory) {
        routeVm.pushRoute("/category/${cat.id}")
    }
}

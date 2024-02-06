package viewmodel

import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClassFull
import kotlinx.coroutines.launch
import utils.*

data class ClassesState(
    val classList: Async<List<RaceClassFull>> = Loading(),
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

    fun upsertCategory(category: RaceClass) {
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

    fun editCategory(cat: RaceClassFull) {
        routeVm.pushRoute("/category/${cat.id}")
    }
}

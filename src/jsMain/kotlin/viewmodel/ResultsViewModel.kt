package viewmodel

import com.mxmariner.regatta.data.RaceResultFull
import utils.Async
import utils.Uninitialized

data class ResultState(
    val results: Async<List<RaceResultFull>> = Uninitialized
) : VmState
class ResultsViewModel(
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<ResultState>(ResultState()){

    fun addResult() {
        routeVm.pushRoute(Route.RaceResultCreate)
    }
}

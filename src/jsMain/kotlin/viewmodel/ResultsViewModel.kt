package viewmodel

import com.mxmariner.regatta.data.Race
import com.mxmariner.regatta.data.RaceFull
import com.mxmariner.regatta.data.RaceResultFull
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch
import utils.*

data class ResultState(
    val loggedIn: Boolean = loginViewModel.flow.value.login?.isExpired() == false,
    val results: Async<List<RaceResultFull>> = Loading(),
    val races: Async<Map<Series, List<RaceFull>>> = Loading(),
    val year: Int? = currentYear().toInt(),
) : VmState

class ResultsViewModel(
    val routeVm: RouteViewModel = routeViewModel,
    val loginVm: LoginViewModel = loginViewModel,
) : BaseViewModel<ResultState>(ResultState()) {

    init {
        launch {
            loginVm.flow.collect {
                val isLoggedIn = it.login?.isExpired() == false
                setState { copy(loggedIn = isLoggedIn) }
            }
        }
        setState {
            copy(
                races = year?.let { fetchRaces(it) } ?: races
            )
        }
    }

    private val none = Series(name = "No series")
    private suspend fun fetchRaces(year: Int) = Api.getAllRaces(year).toAsync().map { lst ->
        lst.sortedBy { it.startTime }.groupBy { it.series ?: none }
    }

    override fun reload() {
        setState { ResultState(year = year) }

        setState {
            year?.let {
                copy( races = fetchRaces(year))
            } ?: this
        }
    }

    fun addResult(race: Race) {
        routeVm.pushRoute("/races/results/${race.id}")
    }

    fun viewResult(race: Race) {
        routeVm.pushRoute("/races/results/view/${race.id}")
    }

    fun selectYear(year: Int?) {
        year?.let { y ->
            setState { copy(races = Loading()) }
            setState {
                copy(
                    year = y,
                    races = fetchRaces(y)
                )
            }
        }
    }
}

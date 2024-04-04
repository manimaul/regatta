package viewmodel

import com.mxmariner.regatta.data.*
import components.selectedYear
import kotlinx.coroutines.launch
import utils.*

data class ResultState(
    val loggedIn: Boolean = loginViewModel.flow.value.login?.isExpired() == false,
    val results: Async<List<RaceResult>> = Loading(),
    val races: Async<Map<Series, List<RaceSchedule>>> = Loading(),
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
        setState { copy( races = selectedYear()?.let { fetchRaces(it) } ?: races ) }
    }

    private suspend fun fetchRaces(year: Int) = Api.getAllRaces(year).toAsync().map { lst ->
        lst.sortedBy { it.startTime }.groupBy { it.series ?: Series(name = it.race.name)}
    }

    override fun reload() {
        setState { ResultState() }
        setState { selectedYear()?.let { copy(races = fetchRaces(it)) } ?: this }
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
                copy(races = fetchRaces(y))
            }
        }
    }

    fun viewStandings(series: Series) {
        routeVm.pushRoute("/series/standings/view/${series.id}/${selectedYear()}")
    }
}

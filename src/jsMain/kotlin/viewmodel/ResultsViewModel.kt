package viewmodel

import com.mxmariner.regatta.data.Race
import com.mxmariner.regatta.data.RaceFull
import com.mxmariner.regatta.data.RaceResultFull
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.toJSDate
import utils.*

data class ResultState(
    val loggedIn: Boolean = loginViewModel.flow.value.login?.isExpired() == false,
    val results: Async<List<RaceResultFull>> = Uninitialized,
    val races: Async<List<RaceFull>> = Uninitialized,
    val year: String? = currentYear()
) : VmState {

    fun raceBySeries(): Map<Series, List<RaceFull>> {
        val none = Series(name = "No series")
        return racesByYear().groupBy { it.series ?: none }
    }

    private fun racesByYear(): List<RaceFull> {
        return emptyList()
//        return races.value?.sortedBy { it.startDate }?.filter { it.startDate?.year() == year } ?: emptyList()
    }

    fun years(): List<String> {
        return emptyList()
//        return races.value?.sortedByDescending { it.startDate }?.mapNotNull { it.startDate?.year() }?.distinct()
//            ?: emptyList()
    }
}

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
                races = Api.getAllRaces().toAsync()
            )
        }
    }

    override fun reload() {
        setState { ResultState() }
        setState {
            copy(
                races = Api.getAllRaces().toAsync()
            )
        }
    }

    fun addResult(race: Race) {
        routeVm.pushRoute("/races/results/${race.id}")
    }

    fun viewResult(race: Race) {
        routeVm.pushRoute("/races/results/view/${race.id}")
    }

    fun selectYear(year: String?) {
        setState { copy(year = year) }
    }
}

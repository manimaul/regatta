package viewmodel

import com.mxmariner.regatta.data.Race
import com.mxmariner.regatta.data.RaceSchedule
import components.selectedYear
import utils.*

data class RacesState(
    val races: Async<List<RaceSchedule>> = Loading(),
) : VmState

class RacesViewModel(
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<RacesState>(RacesState()) {

    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(races = selectedYear()?.let { getAllRaces(it) } ?: races)
        }
    }

    private suspend fun getAllRaces(year: Int) = Api.getAllRaces(year).toAsync().map { it.sortedBy { it.startTime } }

    fun createRace() {
        routeVm.pushRoute(Route.RaceCreate)
    }

    fun editRace(race: Race) {
        routeVm.pushRoute("/race/${race.id}")
    }

    fun selectYear(year: Int?) {
        setState { copy(races = Loading()) }
        year?.let {
            setState { copy(races = getAllRaces(it)) }
        }
    }
}

package viewmodel

import com.mxmariner.regatta.data.Race
import com.mxmariner.regatta.data.RaceSchedule
import components.selectedYear
import utils.*

data class RacesState(
    val races: Async<List<RaceSchedule>> = Loading(),
    /** Backs the add/edit race modal; a fresh one each time the modal opens so nothing from the last race lingers. */
    val edit: RacesEditViewModel? = null,
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

    fun newRace() = openEditor(0)

    fun editRace(race: Race) = openEditor(race.id)

    private fun openEditor(raceId: Long) {
        setState {
            copy(
                edit = RacesEditViewModel(
                    raceId = raceId,
                    onSaved = { saved ->
                        alertsViewModel.showAlert("${saved.race.name} ${if (raceId == 0L) "created" else "updated"}!")
                        reload()
                    },
                    onDeleted = { deleted ->
                        alertsViewModel.showAlert("${deleted.race.name} deleted!")
                        reload()
                    },
                )
            )
        }
    }

    fun selectYear(year: Int?) {
        setState { copy(races = Loading()) }
        year?.let {
            setState { copy(races = getAllRaces(it)) }
        }
    }
}

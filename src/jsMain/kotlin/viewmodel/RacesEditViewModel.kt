package viewmodel

import com.mxmariner.regatta.data.*
import components.selectedYear
import utils.*

data class RacesEditState(
    val race: Async<RaceSchedule> = Loading(),
    val skippers: Async<List<Person>> = Loading(),
    val series: Async<List<Series>> = Loading(),
    val operation: Operation = Operation.None,
) : VmState

class RacesEditViewModel(
    val raceId: Long = 0,
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<RacesEditState>(RacesEditState()) {

    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(
                race = race.loading(),
                skippers = skippers.loading(),
                series = series.loading(),
                operation = Operation.Fetched
            )
        }
        setState {
            if (raceId == 0L) {
                copy(
                    race = Complete(RaceSchedule()),
                    skippers = Api.getAllPeople().toAsync(),
                    series = Api.allSeries().toAsync(),
                )
            } else {
                copy(
                    race = Api.getRaceSchedule(raceId).toAsync(),
                    skippers = Api.getAllPeople().toAsync(),
                    series = Api.allSeries().toAsync(),
                )
            }
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
//        setState { copy(races = Loading()) }
//        year?.let {
//            setState { copy(races = getAllRaces(it)) }
//        }
    }

    fun cancelCreate() {
        routeVm.goBackOrHome()
    }

    fun addSchedule(schedule: ClassSchedule) {
        setState {
            copy(
                race = race.map {
                    it.copy(schedule = it.schedule.toMutableList().apply {
                        removeAll { it.raceClass.id == schedule.raceClass.id }
                        add(schedule)
                    })
                },
//                classes = classes.map {
//                    it.toMutableList().apply {
//                        removeAll { it.raceClass.id == schedule.raceClass.id }
//                    }
//                }
            )
        }
//        if (raceId != 0L) {
//            setState { copy(race = Api.postSchedule(raceId, schedule).toAsync()) }
//        }
    }
}

package viewmodel

import com.mxmariner.regatta.data.*
import utils.*

data class EditRace(
    val race: Async<Race> = Uninitialized,
    val operation: Operation = Operation.None,
)

data class RacesState(
    val races: Async<List<RaceFull>> = Loading(),
    val people: Async<List<Person>> = Loading(),
    val series: Async<List<Series>> = Loading(),
    val categories: Async<List<RaceClassCategory>> = Loading(),
    val editRace: EditRace = EditRace()
) : VmState

class RacesViewModel(
    val fetchRaces: Boolean = true,
    val fetchPeople: Boolean = false,
    val fetchSeries: Boolean = false,
    val fetchCategories: Boolean = false,
    val editRaceId: Long? = null,
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<RacesState>(RacesState()) {

    init {
        reload()
    }

    override fun reload() {
        setState {
            val allRaces = if (fetchRaces) Api.getAllRaces().toAsync()
//                .map {
//                it.sortedBy { it.startDate }
//            }
            else races
            val editRace: Async<Race> = allRaces.value?.firstOrNull { it.id == editRaceId }?.let { Complete(it) }
                ?: editRaceId?.let { Api.getRace(it).toAsync() } ?: Complete(RacePost())
            copy(
                races = allRaces,
                people = if (fetchPeople) Api.getAllPeople().toAsync() else people,
                series = if (fetchSeries) Api.allSeries().toAsync() else series,
                categories = if(fetchCategories) Api.getAllCategories().toAsync() else categories,
                editRace = EditRace(editRace, Operation.Fetched)
            )
        }
    }

    fun reloadRaces() {
        setState {
            copy(
                races = Api.getAllRaces().toAsync(),
            )
        }
    }

    fun reloadSeries() {
        setState {
            copy(
                series = Api.allSeries().toAsync(),
            )
        }
    }

    fun reloadPeople() {
        setState {
            copy(people = Api.getAllPeople().toAsync())
        }
    }

    fun createRace() {
        routeVm.pushRoute(Route.RaceCreate)
    }

    fun editRace(race: Race) {
        race.id?.let {
            routeVm.pushRoute("/race/${race.id}")
        }
    }

    fun cancelCreate() {
        routeVm.goBackOrHome()
    }

    fun deleteRace(race: Race) {
        race.id?.let { id ->
            setState {
                copy(editRace = editRace.copy(race = Loading(), operation = Operation.Deleted))
            }
            setState {
                val del = Api.deleteRace(id).toAsync()
                println("del race $del")
                copy(editRace = editRace.copy(race = del.flatMap { Complete(race) }))
            }

        }
    }

    fun saveRace(race: Race) {
        setState {
            copy(editRace = editRace.copy(race = Loading(), operation = Operation.Updated))
        }
        setState {
            copy(editRace = editRace.copy(race = Api.postRace(race).toAsync()))
        }
    }
}

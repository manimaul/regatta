package viewmodel

import com.mxmariner.regatta.data.*
import components.rgRaceYearViewModel
import kotlinx.datetime.Instant
import utils.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class EditRace(
    val race: Async<Race> = Uninitialized,
    val operation: Operation = Operation.None,
)

data class RacesState(
    val races: Async<List<RaceFull>> = Loading(),
    val people: Async<List<Person>> = Loading(),
    val series: Async<List<Series>> = Loading(),
    val categories: Async<List<RaceClassCategory>> = Loading(),
    val editRace: EditRace = EditRace(),
    val year: Int? = currentYear().toInt(),
    val years: Async<List<String>> = Loading()
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
            val allRaces =  rgRaceYearViewModel.selectedYear()?.takeIf { fetchRaces }?.let { getAllRaces(it) } ?: races
            println("reload year = ${rgRaceYearViewModel.selectedYear()}")
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

    private suspend fun getAllRaces(year: Int) = Api.getAllRaces(year).toAsync().map { it.sortedBy { it.startTime } }

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

    fun selectYear(year: Int?) {
       setState { copy(races = Loading()) }
       year?.let {
           setState { copy(races = getAllRaces(it)) }
       }
    }
}

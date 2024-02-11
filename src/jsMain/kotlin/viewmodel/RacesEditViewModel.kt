package viewmodel

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.*
import components.selectedYear
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import utils.*

data class RacesEditState(
    val race: Async<RaceSchedule> = Loading(),
    val skippers: Async<List<Person>> = Loading(),
    val series: Async<List<Series>> = Loading(),
    val operation: Operation = Operation.None,
) : VmState

fun RaceSchedule.validate(): Boolean {
    return race.name.isNotBlank() && schedule.isNotEmpty()
}

class RacesEditViewModel(
    val raceId: Long = 0,
    val routeVm: RouteViewModel = routeViewModel,
    val timeVm: RgAddTimeViewModel = RgAddTimeViewModel()
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
                    race = Api.getRaceSchedule(raceId).toAsync().map {
                        it.schedule.forEach {
                            timeVm.removeOption(it.raceClass.id, false)
                        }
                        it
                    },
                    skippers = Api.getAllPeople().toAsync(),
                    series = Api.allSeries().toAsync(),
                )
            }
        }
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
                    }.sortedBy { it.raceClass.sort })
                },
            )
        }
    }

    fun removeSchedule(schedule: ClassSchedule) {
        setState {
            copy(
                race = race.map {
                    it.copy(schedule = it.schedule.toMutableList().apply {
                        removeAll { it.raceClass.id == schedule.raceClass.id }
                    })
                },
            )
        }
    }


    fun save(schedule: RaceSchedule) {
        setState {
            RacesEditState()
        }
        launch {
            Api.postSchedule(schedule)
            routeViewModel.pushRoute(Route.Races)
        }
    }

    fun setRaceName(name: String) {
        setState {
            copy(race = race.map { it.copy(race = it.race.copy(name = name)) })
        }
    }

    fun setSeries(series: Series) {
        setState {
            copy(
                race = race.map {
                    it.copy(
                        series = series,
                        race = it.race.copy(
                            seriesId = series.id,
                        )
                    )
                }
            )
        }
    }

    fun setRC(rc: Person?) {
        setState {
            copy(
                race = race.map {
                    it.copy(
                        race = it.race.copy(rcId = rc?.id),
                        rc = rc,
                    )
                }
            )
        }
    }

    fun setCF(cf: Int?) {
        setState {
            copy(race = race.map { it.copy(race = it.race.copy(correctionFactor = cf ?: correctionFactorDefault)) })
        }
    }

}

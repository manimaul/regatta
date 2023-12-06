package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.*
import components.*
import kotlinx.datetime.Instant
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.Operation
import viewmodel.RacesViewModel
import viewmodel.complete
import kotlin.time.Duration.Companion.hours

@Composable
fun Races(
    viewModel: RacesViewModel = remember { RacesViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H1 {
        Text("Races")
    }
    when (val races = state.value.races) {
        is Complete -> RaceList(races, viewModel)
        is Error -> ErrorDisplay(races) {
            viewModel.reloadRaces()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }

}

@Composable
fun RaceList(races: Complete<List<RaceFull>>, viewModel: RacesViewModel) {
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series") }
                RgTh { Text("Name") }
                RgTh { Text("Race start") }
                RgTh { Text("Race finish") }
                RgTh { Text("Correction factor") }
                RgTh { Text("Race committee") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            races.value.forEach { rf ->
                RgTr {
                    RgTd { Text(rf.series?.name ?: "-") }
                    RgTd { Text(rf.name) }
//                    RgTd { Text(rf.startDate?.display() ?: "-") }
//                    RgTd { Text(rf.endDate?.display() ?: "-") }
//                    RgTd { Text(rf.correctionFactor?.let { "$it" } ?: "-") }
//                    RgTd { Text(rf.rc?.let { "${it.first} ${it.last}" } ?: "-") }
                    RgTd {
                        RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-start")) {
                            viewModel.editRace(rf)
                        }
                    }
                }
            }
            RgTr {
                RgTd(6) { }
                RgTd {
                    RgButton("Create Race", RgButtonStyle.SuccessOutline, customClasses = listOf("float-start")) {
                        viewModel.createRace()
                    }
                }
            }
        }
    }
}

@Composable
fun RaceEdit(
    raceId: Long? = null,
    viewModel: RacesViewModel = remember {
        RacesViewModel(
            fetchRaces = false,
            fetchPeople = true,
            fetchSeries = true,
            fetchCategories = true,
            editRaceId = raceId,
        )
    }
) {
    val state by viewModel.flow.collectAsState()
    when (val race = state.editRace.race) {
        is Complete -> {
            when (state.editRace.operation) {
                Operation.None -> Unit
                Operation.Fetched -> RaceForm(race.value, viewModel = viewModel)
                Operation.Updated -> RgOk("Race", "${race.value.name} updated!") {
                    viewModel.cancelCreate()
                }

                Operation.Deleted -> RgOk("Race", "${race.value.name} deleted!") {
                    viewModel.cancelCreate()
                }
            }

        }

        is Error -> ErrorDisplay(race) {
            viewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}

@Composable
fun RaceForm(
    editRace: Race? = null,
    viewModel: RacesViewModel
) {
    var race by remember { mutableStateOf(editRace?.toPost() ?: RacePost()) }

    var raceTimes by remember { mutableStateOf(emptyList<RaceTime>()) }
    var person by remember {
        mutableStateOf(
            if (editRace is RaceFull) {
                editRace.rc
            } else {
                null
            }
        )
    }
    var series by remember {
        mutableStateOf(
            if (editRace is RaceFull) {
                editRace.series
            } else {
                null
            }
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val state by viewModel.flow.collectAsState()
    if (confirmDelete) {
        RgConfirm("Delete '${race.name}'?") { delete ->
            if (delete) {
                viewModel.deleteRace(race)
            } else {
                confirmDelete = false
            }
        }
    } else {
        race.id?.let {
            H1 { Text("Edit Race") }
        } ?: H1 { Text("Create Race") }

        RgForm {
            Fieldset {
                P {
                    RgInput("Race name", race.name) {
                        race = race.copy(name = it)
                    }
                }
                when (val people = state.people) {
                    is Complete -> {
                        B { Text("Race Committee") }
                        RgSkipperDropdown(people.value, person) {
                            person = it
                            race = race.copy(rcId = it?.id)
                        }
                    }

                    is Error -> ErrorDisplay(people) {
                        viewModel.reloadPeople()
                    }

                    is Loading -> RgSpinner(25f)
                    Uninitialized -> Unit
                }
                Br()
                when (val allSeries = state.series) {
                    is Complete -> {
                        B { Text("Series") }
                        RgSeriesSelect(allSeries.value, series) {
                            series = it
                            race = race.copy(seriesId = it?.id)
                        }
                    }

                    is Error -> ErrorDisplay(allSeries) {
                        viewModel.reloadSeries()
                    }

                    is Loading -> RgSpinner(25f)
                    Uninitialized -> Unit
                }
                Br()
                B { Text("Race Times") }
                raceTimes.forEachIndexed { index, raceTime ->
                    RgDate(label = "Race start", date = raceTime.startDate, time = true) { st ->
                        raceTimes = raceTimes.toMutableList().apply {
                            set(index, raceTime.copy(startDate = st))
                        }
//                    val endDate = if (!endSet) it.plus(2.5.hours) else race.endDate
//                    race = race.copy(
//                        startDate = it,
//                        endDate = endDate
//                    )
                    }

                    RgDate(label = "Race end", date = raceTime.endDate, time = true) { et ->
                        raceTimes = raceTimes.toMutableList().apply {
                            set(index, raceTime.copy(endDate = et))
                        }
                    }

                }
                Br()
//                RgDate(label = "Race end", date = race.endDate, time = true) {
//                    endSet = true
//                    race = race.copy(endDate = it)
//                }
            }
            state.categories.complete(viewModel) {
                RgAddRaceTime(it) {
                    raceTimes = raceTimes.toMutableList().apply { add(it) }
                }

            }
            Br()
            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelCreate()
            }
            RgButton("Save", RgButtonStyle.Primary) {
                viewModel.saveRace(race)
            }
            race.id?.let {
                RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                    confirmDelete = true
                }
            }
        }
    }
}

@Composable
fun RgAddRaceTime(
    categories: List<RaceClassCategory>,
    added: (RaceTime) -> Unit
) {
    var raceClass by remember { mutableStateOf<RaceClass?>(null) }
    var startDate by remember { mutableStateOf<Instant?>(null) }
    var endDate by remember { mutableStateOf<Instant?>(null) }
    var cf by remember { mutableStateOf<Int?>(null) }

    println("RgAddRaceTime $raceClass $startDate $endDate $cf")
    RgClassDropdown(categories, raceClass) {
        raceClass = it
    }
    RgDate(label = "Race start", date = startDate, time = true) { st ->
        if (endDate == null) {
            endDate = st.plus(2.5.hours)
            println("enddate set by start = ${endDate?.display()}")
        }
        startDate = st
        println("startDate= ${startDate?.display()}")
    }
    RgDate(label = "Race end", date = endDate, time = true) { et ->
        endDate = et
        println("enddate = ${endDate?.display()}")
    }
    RgInput(label = "Correction factor", cf?.let { "$it" } ?: "") {
        cf = it.toIntOrNull()
    }
    RgButton(
        "Add",
        disabled = raceClass == null || startDate == null || endDate == null,
        customClasses = listOf(AppStyle.marginAll)
    ) {
        added(RaceTime(raceClass!!, startDate!!, endDate!!, cf))
        raceClass = null
        startDate = null
        endDate = null
        cf = null
    }
}

@Composable
fun RgSeriesSelect(
    allSeries: List<Series>,
    series: Series?,
    handler: (Series?) -> Unit
) {
    Select(attrs = {
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                handler(allSeries.firstOrNull {
                    it.id == id
                })
            }
        }
    }) {
        Option("-1", attrs = {
            if (series == null) {
                selected()
            }
        }) {
            Text("None")
        }
        allSeries.forEach {
            Option(it.id.toString(), attrs = {
                if (it.id == series?.id) {
                    selected()
                }
            }) {
                Text(it.name)
            }
        }
    }
}

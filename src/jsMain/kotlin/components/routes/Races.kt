package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.correctionFactorDefault
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
import kotlin.time.Duration.Companion.minutes

@Composable
fun Races(
    viewModel: RacesViewModel = remember { RacesViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H1 { Text("Races") }
    B { Text("Year") }
    RgRaceYearSelector { viewModel.selectYear(it) }
    Br()
    state.value.races.complete(viewModel) { RaceList(it, viewModel) }
}

@Composable
fun RaceList(races: List<RaceFull>, viewModel: RacesViewModel) {
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series") }
                RgTh { Text("Name") }
                RgTh { Text("Race start") }
                RgTh { Text("Race finish") }
                RgTh { Text("Race committee") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            races.forEach { rf ->
                RgTr {
                    RgTd { Text(rf.series?.name ?: "-") }
                    RgTd { Text(rf.name) }
                    RgTd { Text(rf.startTime?.display() ?: "-") }
                    RgTd { Text(rf.endTime?.display() ?: "-") }
                    RgTd { Text(rf.rc?.let { "${it.first} ${it.last}" } ?: "-") }
                    RgTd {
                        RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-start")) {
                            viewModel.editRace(rf)
                        }
                    }
                }
            }
            RgTr {
                RgTd(5) { }
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
    state.editRace.race.complete(viewModel) {race ->
        when (state.editRace.operation) {
            Operation.None -> Unit
            Operation.Fetched -> RaceForm(race, viewModel = viewModel)
            Operation.Updated -> RgOk("Race", "${race.name} updated!") {
                viewModel.cancelCreate()
            }

            Operation.Deleted -> RgOk("Race", "${race.name} deleted!") {
                viewModel.cancelCreate()
            }
        }
    }
}

@Composable
fun RaceForm(
    editRace: Race? = null,
    viewModel: RacesViewModel
) {
    var race by remember { mutableStateOf(editRace?.toPost() ?: RacePost()) }
    var raceTimes by remember { mutableStateOf(editRace?.raceTimes ?: emptyList()) }
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
                B { Text("Race Start Time(s)") }
                Br()
                RgGrid(RgContainerType.container_fluid) {
                    raceTimes.forEachIndexed { index, raceTime ->
                        RgRow(customizer = { set(space = RgSpace.m, side = RgSide.y, size = RgSz.s4) }) {
                            RgCol {
                                B { Text("Class Category") }
                                state.categories.complete(viewModel) { categories ->
                                    RgClassCatDropDown(categories, raceTime.raceClassCategory) {
                                        raceTimes = raceTimes.toMutableList().apply {
                                            set(index, raceTime.copy(raceClassCategory = it.toRaceClass()))
                                        }
                                    }
                                }
                            }
                            RgCol {
                                RgDate(label = "Race start", date = raceTime.startDate, time = true) { st ->
                                    raceTimes = raceTimes.toMutableList().apply {
                                        set(index, raceTime.copy(startDate = st))
                                    }
                                }
                            }
                            RgCol {
                                RgDate(label = "Race end", date = raceTime.endDate, time = true) { et ->
                                    raceTimes = raceTimes.toMutableList().apply {
                                        set(index, raceTime.copy(endDate = et))
                                    }
                                }
                            }
                            RgCol {
                                RgInput(
                                    label = "Correction",
                                    "${raceTime.correctionFactor}") { cf ->
                                    raceTimes = raceTimes.toMutableList().apply {
                                        set(index, raceTime.copy(correctionFactor = cf.toIntOrNull() ?: correctionFactorDefault))
                                    }
                                }
                            }
                            RgCol {
                                RgRow(customizer = { set(space = RgSpace.m, side = RgSide.x, size = RgSz.s3) }) {
                                    B { Text("Action") }
                                    RgButton("Remove", RgButtonStyle.Danger) {
                                        raceTimes = raceTimes.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    state.categories.complete(viewModel) { cats ->
                        val ids = raceTimes.map { it.raceClassCategory.id }
                        cats.filter { !ids.contains(it.id) }.takeIf { it.isNotEmpty() }?.let {
                            val selected = it.firstOrNull()?.toRaceClass()
                            println("remain $it, selected $selected")
                            RgAddRaceTime(it, selected) {
                                raceTimes = raceTimes.toMutableList().apply { add(it) }
                            }
                        }
                    }
                }
                Br()
            }
            Br()
            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelCreate()
            }
            val saveDisabled = raceTimes.isEmpty() || race.name.isBlank()
            RgButton("Save", RgButtonStyle.Primary, disabled = saveDisabled) {
                viewModel.saveRace(race.copy(raceTimes = raceTimes))
            }
            race.id?.let {
                val disabled = editRace?.resultCount?.let { it > 0 } ?: false
                RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart), disabled = disabled) {
                    confirmDelete = true
                }
            }
        }
    }
}

@Composable
fun RgAddRaceTime(
    categories: List<RaceClassFull>,
    selected: RaceClass?,
    added: (RaceTime) -> Unit
) {
    var raceClassCat by mutableStateOf(selected)
    var startDate by remember { mutableStateOf<Instant?>(null) }
    var endDate by remember { mutableStateOf<Instant?>(null) }
    var cf by remember { mutableStateOf(correctionFactorDefault) }

    RgRow {
        RgCol {
            B { Text("Class Category") }
            RgClassCatDropDown(categories, raceClassCat) {
                raceClassCat = it.toRaceClass()
                println("selected $raceClassCat")
            }
        }
        RgCol {
            RgDate(label = "Race start", date = startDate, time = true) { st ->
                if (endDate == null) {
                    endDate = st.plus(2.5.hours)
                }
                startDate = st
            }
        }
        RgCol {
            RgDate(label = "Race end", date = endDate, time = true) { et ->
                endDate = et
            }

        }
        RgCol {
            RgInput(label = "Correction", "$cf") {
                cf = it.toIntOrNull() ?: correctionFactorDefault
            }
        }
        RgCol {
            RgRow(customizer = { set(space = RgSpace.m, side = RgSide.x, size = RgSz.s3) }) {
                B { Text("Action") }
                RgButton(
                    "Add",
                    disabled = raceClassCat == null || startDate == null || endDate == null,
                ) {
                    added(RaceTime(raceClassCat!!, startDate!!, endDate!!, cf, TODO()))
                    raceClassCat = null
                    startDate = startDate?.plus(5.minutes)
                }
            }
        }
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

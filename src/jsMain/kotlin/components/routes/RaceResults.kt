package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import components.*
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import utils.*
import viewmodel.RaceResultEditViewModel
import viewmodel.ResultsViewModel
import viewmodel.complete

@Composable
fun RaceResultsEdit(
    raceId: Long?,
    viewModel: RaceResultEditViewModel = remember { RaceResultEditViewModel(raceId ?: 0) }
) {
    val state = viewModel.flow.collectAsState()
    var addBoat by remember { mutableStateOf<Boat?>(null) }
    state.value.race.complete(viewModel) { race ->
        var finish by remember { mutableStateOf(race.endTime) }
        H1 {
            Text("${race.name} - ${race.startTime?.year() ?: ""} Results")
        }
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Skipper") }
                    RgTh { Text("Sail Number") }
                    RgTh { Text("Boat Type") }
                    RgTh { Text("PHRF Rating") }
                    RgTh { Text("Start Time") }
                    RgTh { Text("Finish Time") }
                    RgTh { Text("Elapsed Time") }
                    RgTh { Text("Elapsed Seconds") }
                    RgTh { Text("Correction Factor") }
                    RgTh { Text("Corrected Time") }
                    RgTh { Text("Place In Class") }
                    RgTh { Text("Place Overall") }
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
                RgTr {
                    RgTd {
                        state.value.boats.complete(viewModel) {
                            RgBoatDropdown(it, addBoat) { boat ->
                                addBoat = boat
                            }
                        }
                    }
                    RgTd {
                        Text(addBoat?.skipper?.fullName() ?: "")
                    }
                    RgTd {
                        Text(addBoat?.sailNumber ?: "")
                    }
                    RgTd {
                        Text(addBoat?.boatType?: "")
                    }
                    RgTd {
                        Text(addBoat?.boatType?: "")
                    }
                    RgTd {
                        Text(race.startTime?.display() ?: "")
                    }
                    RgTd {
                        RgDate("Finish", finish, placeHolder = true, time = true) {
                            finish = it
                        }
                    }
                    RgTd(6) {
                    }
                    RgTd {
                        RgButton("Save") {

                        }
                    }
                }
                state.value.result.complete(viewModel) { results ->
                    RgTr {
                        RgTd(2) {
                            Text("${results.size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RgBoatDropdown(
    boats: List<Boat>,
    selectedBoat: Boat?,
    handler: (Boat?) -> Unit
) {
    Select(attrs = {
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                handler(boats.firstOrNull { it.id == id })
            }
        }
    }) {
        Option("-1", attrs = {
            if (selectedBoat == null) {
                selected()
            }
        }) {
            Text("None")
        }
        boats.forEach { boat ->
            Option(boat.id.toString(), attrs = {
                if (selectedBoat?.id == boat.id) {
                    selected()
                }
            }) {
                Text(boat.name)
            }
        }
    }
}

@Composable
fun RaceResults(
    viewModel: ResultsViewModel = remember { ResultsViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H1 {
        Text("Race Results")
    }
    B { Text("Year") }
    RgYearSelect(state.value.year, state.value.years()) {
        viewModel.selectYear(it)
    }
    Br()
    state.value.raceBySeries().let {
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Name") }
                    RgTh { Text("Date") }
                    RgTh { Text("Results") }
                    if (state.value.loggedIn) {
                        RgTh { Text("Action") }
                    }
                }
            }
            it.keys.forEach { series ->
                Tr {
                    RgTd(3) {
                        H4 { Text(series.name) }
                    }
                }
                val races = it[series]
                RgTbody {
                    races?.forEach { rf ->
                        RgTr {
                            RgTd {
                                Text(rf.name)
                            }
                            RgTd {
                                Text(rf.startTime?.display() ?: "")
                            }
                            RgTd {
                                RgButton("View Results") {
                                    viewModel.viewResult(rf)
                                }
                            }
                            if (state.value.loggedIn) {
                                Td {
                                    RgButton("Add Results") {
                                        viewModel.addResult(rf)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RgYearSelect(
    year: String?,
    years: List<String>,
    handler: (String?) -> Unit,
) {
    Select(attrs = {
        classes("form-select")
        onChange { change ->
            handler(change.value)
        }
    }) {
        Option("-1", attrs = {
            if (year == null) {
                selected()
            }
        }) {
            Text("None")
        }
        years.forEach {
            Option(it, attrs = {
                if (it === year) {
                    selected()
                }
            }) {
                Text(it)
            }
        }
    }
}

package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.display
import components.*
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.*

@Composable
fun RaceResultsEdit(
    raceId: Long?,
    viewModel: RaceResultEditViewModel = remember { RaceResultEditViewModel(raceId ?: 0) }
) {
    val state = viewModel.flow.collectAsState()
    val addState = viewModel.addViewModel.flow.collectAsState()
    state.value.report.complete(viewModel) { report ->
        H1 {
            Text("${report.race.name} - ${report.race.startTime?.year() ?: ""} Results")
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
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
                if (addState.value.id == null) {
                    EditResultRow(viewModel, state.value, addState.value)
                }
                report.categories.forEach { category ->
                    RgTr {
                        RgTd(12) { H4 { Text(category.category.name) } }
                    }
                    category.classes.forEach { raceClass ->
                        RgTr {
                            RgTd(12) { H6 { Text(raceClass.raceClass.name) } }
                        }
                        raceClass.cards.forEach { card ->
                            if (card.resultRecord.id == addState.value.id) {
                                EditResultRow(viewModel, state.value, addState.value)
                            } else {
                                RgTr {
                                    RgTd { Text(card.boatName) }
                                    RgTd { Text(card.skipper) }
                                    RgTd { Text(card.sail) }
                                    RgTd { Text(card.boatType) }
                                    RgTd { Text(card.phrfRating?.toString() ?: "") }
                                    RgTd { Text(card.startTime?.display() ?: "") }
                                    RgTd { Text(card.finishTime?.display() ?: "") }
                                    RgTd { Text(card.elapsedTime?.toString() ?: "") }
                                    RgTd { Text(card.elapsedTime?.inWholeSeconds?.toString() ?: "") }
                                    RgTd { Text("${card.correctionFactor.asDynamic().toFixed(3)}") }
                                    RgTd { Text(card.correctedTime?.display() ?: "n/a") }
                                    RgTd {
                                        RgButton(label = "Edit") {
                                            viewModel.addViewModel.setCard(card)
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
}

@Composable
fun EditResultRow(
    viewModel: RaceResultEditViewModel,
    state: RaceResultEditState,
    addState: RaceResultAddState,
) {
    RgTr {
        RgTd {
            if (addState.id == null) {
                state.boats.complete(viewModel) {
                    RgBoatDropdown(it, addState.boat) { boat ->
                        viewModel.addViewModel.addBoat(boat)
                    }
                }
            } else {
                Text(addState.boat?.name ?: "")
            }
        }
        RgTd { Text(addState.boat?.skipper?.fullName() ?: "") }
        RgTd { Text(addState.boat?.sailNumber ?: "") }
        RgTd { Text(addState.boat?.boatType ?: "") }
        RgTd { Text(addState.boat?.phrfRating?.toString() ?: "") }
        RgTd {
            addState.start?.let { start ->
                RgDate("Start", start, placeHolder = true, time = true, seconds = true) {
                    viewModel.addViewModel.setStart(it)
                }
                RgButton(label = "DNS") {
                    viewModel.addViewModel.setStart(null)
                    viewModel.addViewModel.setFinish(null)
                }
            } ?: run {
                P { Text("DNS") }
                RgButton(label = "Reset") {
                    viewModel.addViewModel.setStart(addState.race?.startTime)
                }
            }
        }
        RgTd {
            addState.finish?.let { finish ->
                RgDate("Finish", finish, placeHolder = true, time = true, seconds = true) {
                    viewModel.addViewModel.setFinish(it)
                }
                RgButton(label = "DNF") {
                    viewModel.addViewModel.setFinish(null)
                }
            } ?: run {
                P { Text("DNF") }
                RgButton(label = "Reset") {
                    viewModel.addViewModel.setFinish(addState.race?.endTime)
                }
            }
        }
        RgTd(colSpan = 4) { }
        RgTd {
            if (addState.id != null) {
                RgButton(
                    label = "Cancel",
                    customClasses = listOf(AppStyle.marginAll),
                ) {
                    viewModel.addViewModel.setCard()
                }
            }
            RgButton(
                label = "Save",
                style = RgButtonStyle.Success,
                customClasses = listOf(AppStyle.marginAll, AppStyle.marginAll),
            ) {
                viewModel.addResult(addState)
            }
            if (addState.id != null) {
                RgButton(
                    label = "Delete",
                    style = RgButtonStyle.Danger,
                    customClasses = listOf(AppStyle.marginAll),
                ) {
                    viewModel.delete(addState.id)
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
                val races = it[series]
                RgTbody {
                    Tr {
                        RgTd(if (state.value.loggedIn) 4 else 3) {
                            H4 { Text(series.name) }
                        }
                    }
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

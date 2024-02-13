package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.ratingLabel
import components.*
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.CSSUnit
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
            Text("${report.raceSchedule.startTime?.year() ?: ""} - ${report.raceSchedule.race.name} - Results Editor")
        }
        RgButton("Viewer", customClasses = listOf(AppStyle.marginVert)) {
            routeViewModel.pushRoute("/races/results/view/${raceId}")
        }
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Skipper") }
                    RgTh { Text("Sail Number") }
                    RgTh { Text("Boat Type") }
                    RgTh { Text("Rating") }
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
                if (addState.value.id <= 0) {
                    EditResultRow(viewModel, state.value, addState.value)
                }
                report.classReports.forEach { category ->
                    RgTr {
                        RgTd(12) { H4 { Text(category.raceClass.name) } }
                    }
                    category.bracketReport.forEach { raceClass ->
                        RgTr {
                            RgTd(12) { H6 { Text(raceClass.bracket.name) } }
                        }
                        raceClass.cards.forEach { card ->
                            if (card.resultRecord.result.id == addState.value.id) {
                                EditResultRow(viewModel, state.value, addState.value)
                            } else {
                                RgTr {
                                    RgTd { Text(card.boatName) }
                                    RgTd { Text(card.skipper) }
                                    RgTd { Text(card.sail) }
                                    RgTd { Text(card.boatType) }
                                    RgTd { Text(ratingLabel(card.phrfRating, card.windseeker, true)) }
                                    RgTd { Text(card.startText()) }
                                    RgTd { Text(card.finishText()) }
                                    RgTd { Text(card.elapsedText()) }
                                    RgTd { Text(card.elapsedSecText()) }
                                    RgTd { Text(card.cfText()) }
                                    RgTd { Text(card.corTimeText()) }
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
            if (addState.id == 0L) {
                state.boats.complete(viewModel) {
                    RgBoatDropdown(it, addState.boatSkipper) { boat ->
                        viewModel.addViewModel.addBoat(boat)
                    }
                }
            } else {
                Text(addState.boatSkipper?.boat?.name ?: "")
            }
        }
        RgTd { Text(addState.boatSkipper?.skipper?.fullName() ?: "") }
        RgTd { Text(addState.boatSkipper?.boat?.sailNumber ?: "") }
        RgTd { Text(addState.boatSkipper?.boat?.boatType ?: "") }
        RgTd {
            RatingSelections(
                addState.boatType, addState.phrfRating, addState.wsRating, addState.wsFlying,
                { viewModel.addViewModel.setType(it) },
                { viewModel.addViewModel.setPhrfRating(it) },
                { viewModel.addViewModel.setWsRating(it) },
                { viewModel.addViewModel.setWsFlying(it) },
            )
        }
        RgTd {
            addState.start?.let { start ->
                P {
                    Text(start.display())
                }
                RgButton(label = "DNS", customClasses = listOf(AppStyle.marginTop)) {
                    viewModel.addViewModel.setStart(null)
                    viewModel.addViewModel.setFinish(null)
                }
            } ?: run {
                P { Text("DNS") }
                RgButton(label = "Reset") {
                    viewModel.addViewModel.setStart(addState.raceSchedule?.startTime)
                }
            }
        }
        RgTd {
            addState.hocPosition?.let {
                P { Text("HOC $it") }
                RgButton(label = "Reset") {
                    viewModel.addViewModel.hoc(null)
                    viewModel.addViewModel.setFinish(addState.raceSchedule?.endTime)
                }
                RgButton(label = "+", customClasses = listOf(AppStyle.marginStart)) {
                    viewModel.addViewModel.hoc(it + 1)
                }
                RgButton(label = "-", customClasses = listOf(AppStyle.marginStart)) {
                    viewModel.addViewModel.hoc(it - 1)
                }
            } ?: addState.finish?.let { finish ->
                RgDate("Finish", finish, placeHolder = true, time = true, seconds = true) {
                    viewModel.addViewModel.setFinish(it)
                }
                RgButton(label = "DNF", customClasses = listOf(AppStyle.marginTop, AppStyle.marginEnd)) {
                    viewModel.addViewModel.setFinish(null)
                }
                RgButton(label = "HOC", customClasses = listOf(AppStyle.marginTop)) {
                    viewModel.addViewModel.hoc(state.maxHoc)
                }
            } ?: run {
                P { Text("DNF") }
                RgButton(label = "Reset") {
                    viewModel.addViewModel.setFinish(addState.raceSchedule?.endTime)
                }
            }
        }
        RgTd(colSpan = 4) { }
        RgTd {
            if (addState.id != 0L) {
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
            if (addState.id != 0L) {
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
    boats: List<BoatSkipper>,
    selectedBoat: BoatSkipper?,
    handler: (BoatSkipper?) -> Unit
) {
    Select(attrs = {
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                handler(boats.firstOrNull { it.boat?.id == id })
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
            Option(boat.boat?.id.toString(), attrs = {
                if (selectedBoat?.boat?.id == boat.boat?.id) {
                    selected()
                }
            }) {
                Text(boat.boat?.name ?: "")
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
    RgRaceYearSelector { viewModel.selectYear(it) }
    Br()
    state.value.races.complete(viewModel) {
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
                val raceSchedules = it[series]
                RgTbody {
                    Tr {
                        RgTd(if (state.value.loggedIn) 4 else 3) {
                            H4 { Text(series.name) }
                        }
                    }
                    raceSchedules?.forEach { schedule ->
                        RgTr {
                            RgTd {
                                Text(schedule.race.name)
                            }
                            RgTd {
                                Text(schedule.startTime?.display() ?: "")
                            }
                            RgTd {
                                if (schedule.resultCount > 0) {
                                    RgButton("View Results") {
                                        viewModel.viewResult(schedule.race)
                                    }
                                } else {
                                    Text("Results not posted")
                                }
                            }
                            if (state.value.loggedIn) {
                                Td {
                                    RgButton("Edit Results") {
                                        viewModel.addResult(schedule.race)
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
    customClasses: List<String>? = null,
    handler: (String?) -> Unit,
) {
    Select(attrs = {
        classes(listOf("form-select") + (customClasses ?: emptyList()))
        onChange { change ->
            handler(change.value)
        }
    }) {
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

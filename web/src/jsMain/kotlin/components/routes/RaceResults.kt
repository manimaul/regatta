package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.RaceSchedule
import com.mxmariner.regatta.ratingLabel
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
            Text("${report.raceSchedule.startTime.year()} - ${report.raceSchedule.race.name} - Results Editor")
        }
        RgButton("Viewer", customClasses = listOf(AppStyle.marginVert, AppStyle.marginEnd)) {
            routeViewModel.pushRoute("/races/results/view/${raceId}")
        }
        if (addState.value.id <= 0) {
            AddResult(viewModel)
            Br { }
        }
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Rating") }
                    RgTh { Text("Finish Time") }
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
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
                                    RgTd { Text(ratingLabel(card.phrfRating, card.windseeker, true)) }
                                    RgTd { Text(card.finishText()) }
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
fun AddResult(viewModel: RaceResultEditViewModel) {
    val state = viewModel.flow.collectAsState()
    val addState = viewModel.addViewModel.flow.collectAsState()
    RgModal(
        buttonLabel = "Add Result",
        modalTitle = "Add Result",
        openAction = null,
        content = {
            Span { B { Text("Boat") } }
            state.value.boats.complete(viewModel) {
                RgBoatDropdown(it, addState.value.boatSkipper) { boat ->
                    viewModel.addViewModel.addBoat(boat)
                }
            }
            Hr { }
            Span { B { Text("Rating") } }
            RatingSelections(
                addState.value.ratingType, addState.value.phrfRating, addState.value.wsRating, addState.value.wsFlying,
                { viewModel.addViewModel.setType(it) },
                { viewModel.addViewModel.setPhrfRating(it) },
                { viewModel.addViewModel.setWsRating(it) },
                { viewModel.addViewModel.setWsFlying(it) },
            )
            Hr { }
            Span { B { Text("Finish Time") } }
            TimeRow(viewModel, state.value, addState.value, false)
        },
        footer = {
            Button(attrs = {
                classes(*RgButtonStyle.PrimaryOutline.classes)
                attr("data-bs-dismiss", "modal")
            }) {
                Text("Cancel")
            }
            Button(attrs = {
                classes(*RgButtonStyle.Success.classes)
                attr("data-bs-dismiss", "modal")
                onClick {
                    viewModel.addResult(addState.value)
                }
            }) {
                Text("Save")
            }

        }
    )
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
                Text(addState.boatSkipper?.label() ?: "")
            }
        }
        RgTd {
            RatingSelections(
                addState.ratingType, addState.phrfRating, addState.wsRating, addState.wsFlying,
                { viewModel.addViewModel.setType(it) },
                { viewModel.addViewModel.setPhrfRating(it) },
                { viewModel.addViewModel.setWsRating(it) },
                { viewModel.addViewModel.setWsFlying(it) },
            )
        }
        RgTd {
            TimeRow(viewModel, state, addState, true)
        }
        RgTd {
            if (addState.id != 0L) {
                RgButton(
                    label = "Cancel",
                    style = RgButtonStyle.PrimaryOutline,
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
fun TimeRow(
    viewModel: RaceResultEditViewModel,
    state: RaceResultEditState,
    addState: RaceResultAddState,
    useModalTime: Boolean,
) {
    addState.penalty?.let {
        P { Text("Penalty +$it") }
        RgButton(label = "+", customClasses = listOf(AppStyle.marginBot)) {
            viewModel.addViewModel.penalty(it + 1)
        }
        RgButton(label = "-", customClasses = listOf(AppStyle.marginStart, AppStyle.marginBot)) {
            viewModel.addViewModel.penalty(it - 1)
        }
    }
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
        if (useModalTime) {
            Div {
                RgTimeButton("Finish Time", finish) {
                    viewModel.addViewModel.setFinish(it)
                }
            }
        } else {
            RgTime(date = finish, showSeconds = true) {
                viewModel.addViewModel.setFinish(it)
            }
        }
        RgButton(label = "RET", customClasses = listOf(AppStyle.marginTop, AppStyle.marginEnd)) {
            viewModel.addViewModel.setFinish(null)
        }
        RgButton(label = "HOC", customClasses = listOf(AppStyle.marginTop, AppStyle.marginEnd)) {
            viewModel.addViewModel.hoc(state.maxHoc)
        }
        RgButton(label = "Penalty", customClasses = listOf(AppStyle.marginTop)) {
            viewModel.addViewModel.penalty(state.maxHoc)
        }
    } ?: run {
        P { Text("RET") }
        RgButton(label = "Reset") {
            viewModel.addViewModel.setFinish(addState.raceSchedule?.endTime)
        }
    }
}

@Composable
fun RgBoatDropdown(
    boats: List<BoatSkipper>,
    selectedBoat: BoatSkipper?,
    handler: (BoatSkipper?) -> Unit
) {
    RgDropdownNone(boats, selectedBoat, { it.dropLabel() }, { it.shortLabel() }, handler = handler)
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
                                Text(schedule.startTime.display())
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
                    if (series.id != 0L) {
                        RgTr {
                            RgTd(colSpan = 2) {
                                Text("${series.name} Standings")
                            }
                            RgTd {
                                if (raceSchedules.resultCount() > 0) {
                                    RgButton("View Standings") {
                                        viewModel.viewStandings(series)
                                    }
                                } else {
                                    Text("Results not posted")
                                }
                            }

                            if (state.value.loggedIn) {
                                Td { }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun List<RaceSchedule>?.resultCount(): Long {
    return this?.fold(0L) { c, r ->
        c + r.resultCount
    } ?: 0L
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

package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.ClassSchedule
import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.RaceSchedule
import com.mxmariner.regatta.ratingLabel
import components.*
import org.jetbrains.compose.web.attributes.disabled
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
                                EditResultRow(viewModel)
                            } else {
                                RgTr {
                                    RgTd { Text(card.boatName) }
                                    RgTd { Text(ratingLabel(card.phrfRating, card.windseeker, true)) }
                                    RgTd { Text(card.finishText()) }
                                    RgTd {
                                        RgButton(label = "Edit") {
                                            viewModel.addViewModel.setCard(
                                                card = card,
                                                autoRaceClassId = category.raceClass.id,
                                                autoBracketId = raceClass.bracket.id,
                                            )
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
    val state by viewModel.flow.collectAsState()
    val addState by viewModel.addViewModel.flow.collectAsState()
    RgModal(
        buttonLabel = "Add Result",
        modalTitle = "Add Result",
        openAction = null,
        content = {
            Span { B { Text("Boat") } }
            state.boats.complete(viewModel) {
                RgBoatDropdown(it, addState.boatSkipper) { boat ->
                    viewModel.addViewModel.addBoat(boat)
                }
            }
            Hr { }
            Span { B { Text("Rating") } }
            RatingSelections(
                addState.ratingType, addState.phrfRating, addState.wsRating, addState.wsFlying,
                { viewModel.addViewModel.setType(it) },
                { viewModel.addViewModel.setPhrfRating(it) },
                { viewModel.addViewModel.setWsRating(it) },
                { viewModel.addViewModel.setWsFlying(it) },
            )
            Hr { }
            Span { B { Text("Finish Time") } }
            TimeRow(viewModel, false)
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
                if (!addState.isValid) {
                    disabled()
                }
                attr("data-bs-dismiss", "modal")
                onClick {
                    viewModel.addResult(addState)
                }
            }) {
                Text("Save")
            }

        }
    )
}

@Composable
fun EditResultRow(viewModel: RaceResultEditViewModel) {
    val addState by viewModel.addViewModel.flow.collectAsState()
    val state by viewModel.flow.collectAsState()
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

            state.report.complete(viewModel) { report ->
                val selectedRaceClass =
                    report.raceSchedule.schedule.firstOrNull { it.raceClass.id == addState.raceClassId }
                val selectedBracket = selectedRaceClass?.brackets?.firstOrNull { it.id == addState.bracketId }

                viewModel.addViewModel.availableClasses(report.raceSchedule.schedule)
                    .takeIf { it.size > 1 }
                    ?.let { availableClasses ->
                        Br()
                        Text("Class:")
                        RgDropdown(
                            items = listOf<ClassSchedule?>(null) + availableClasses,
                            selectedItem = selectedRaceClass,
                            name = { it?.raceClass?.name ?: "Auto" },
                        ) {
                            viewModel.addViewModel.addResultSelectedClass(it)
                        }
                    }

                viewModel.addViewModel.availableBrackets(selectedRaceClass)?.takeIf { it.size > 1 }?.let { brackets ->
                    if (selectedBracket != null) {
                        Br()
                        Text("Bracket:")
                        RgDropdown(
                            items = brackets,
                            selectedItem = selectedBracket,
                            name = { it.name },
                        ) {
                            viewModel.addViewModel.addResultSelectedBracket(it)
                        }
                    }
                }
            }
        }
        RgTd {
            TimeRow(viewModel, true)
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
                disabled = !addState.isValid
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
    useModalTime: Boolean,
) {
    val state by viewModel.flow.collectAsState()
    val addState by viewModel.addViewModel.flow.collectAsState()
    addState.finish?.let { finish ->
        if (useModalTime) {
            Div {
                RgTimeButton("Finish Time", finish) {
                    viewModel.addViewModel.setFinish(FinishCode.TIME, it)
                }
            }
        } else {
            RgTime(date = finish, showSeconds = true) {
                viewModel.addViewModel.setFinish(FinishCode.TIME, it)
            }
        }

        RgButton(label = "Penalty${addState.penalty?.let { " $it" } ?: " 0"}", customClasses = listOf(
            AppStyle.marginTop,
            AppStyle.marginBot
        )) {
            viewModel.addViewModel.penalty(addState.penalty?.let { it + 1 } ?: 1)
        }
        addState.penalty?.let {
            RgButton(
                style = RgButtonStyle.Danger,
                label = "-",
                customClasses = listOf(AppStyle.marginStart, AppStyle.marginBot, AppStyle.marginTop)
            ) {
                viewModel.addViewModel.penalty(it - 1)
            }
        }
    }
    FinishCodeDrop(
        selected = addState.finishCode,
        hocPosition = addState.hocPosition,
        customClasses = listOf(AppStyle.marginTop)
    ) {
        when (it) {
            FinishCode.TIME -> viewModel.addViewModel.setFinish(FinishCode.TIME, addState.finish ?: now())
            FinishCode.RET,
            FinishCode.DNF,
            FinishCode.NSC -> viewModel.addViewModel.setFinish(it, null, true)

            FinishCode.HOC -> viewModel.addViewModel.hoc(state.maxHoc)
        }
    }
    addState.hocPosition?.let {
        RgButton(
            style = RgButtonStyle.Danger,
            label = "+",
            customClasses = listOf(AppStyle.marginTop)
        ) {
            viewModel.addViewModel.hoc(it + 1)
        }
        if (it > 1) RgButton(
            style = RgButtonStyle.Danger,
            label = "-",
            customClasses = listOf(AppStyle.marginTop, AppStyle.marginStart)
        ) {
            viewModel.addViewModel.hoc(it - 1)
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

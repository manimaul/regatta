package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceSchedule
import com.mxmariner.regatta.data.RatingType
import components.*
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.display
import utils.year
import viewmodel.RaceResultAddState
import viewmodel.RaceResultAddViewModel
import viewmodel.ResultsViewModel
import viewmodel.complete
import viewmodel.routeViewModel
import kotlinx.datetime.Instant
import utils.finishText
import viewmodel.alertsViewModel

@Composable
fun RaceResultsEdit(
    raceId: Long?,
    viewModel: RaceResultAddViewModel = remember { RaceResultAddViewModel(raceId ?: 0) }
) {
    val state by viewModel.flow.collectAsState()
    state.results.complete(viewModel) { results ->
        H1 {
            Text("${results.raceSchedule.startTime.year()} - ${results.raceSchedule.race.name} - Results Editor")
        }

        if (state.results.value?.results?.isNotEmpty() == true) {
            RgButton("Viewer", customClasses = listOf(AppStyle.marginVert, AppStyle.marginEnd)) {
                routeViewModel.pushRoute("/races/results/view/${raceId}")
            }
        }

        RgModalBody(
            id = "race-result-add-edit",
            modalTitle = { "Race Result" },
            content = {
                AddResult(
                    state,
                    viewModel::selectBoat,
                    viewModel::selectClass,
                    viewModel::selectBracket,
                    viewModel::setFinish,
                    viewModel::penalty,
                    viewModel::selectOrc,
                    viewModel::selectRating,
                )
            },
            footer = {
                Div(attrs = { classes("flex-fill", "d-flex", "justify-content-between") }) {
                    Button(attrs = {
                        classes(*RgButtonStyle.PrimaryOutline.classes)
                        attr("data-bs-dismiss", "modal")
                    }) {
                        Text("Cancel")
                    }
                    Button(attrs = {
                        classes(*RgButtonStyle.Success.classes)
                        if (!state.isValid) {
                            disabled()
                        }
                        attr("data-bs-dismiss", "modal")
                        onClick {
                            viewModel.postResult()
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        )
        RgModalButton(
            id = "race-result-add-edit",
            buttonLabel = { "Add Result" },
            customClasses = listOf(AppStyle.marginBot)
        ) {
            viewModel.clearResultForBoatSelection()
        }

        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Class") }
                    RgTh { Text("Rating") }
                    RgTh { Text("Bracket") }
                    RgTh { Text("Finish Time") }
                    RgTh { Text("Penalty") }
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
                results.results.forEach { result ->
                    RgTr {
                        RgTd { Text(result.boat.boat?.name ?: "") }
                        val cls = results.raceSchedule.schedule.firstOrNull { it.raceClass.id == result.raceClassId }
                        val bracket = cls?.brackets?.firstOrNull { it.id == result.bracketId }
                        RgTd {
                            Text(cls?.raceClass?.name ?: "")
                        }
                        RgTd {
                            Text(result.ratingType.ratedLabel(result.phrfRating, result.orcCertificate?.refNo))
                        }
                        RgTd {
                            Text(bracket?.label() ?: "")
                        }
                        RgTd {
                            Text(result.finish?.display() ?: result.finishCode.finishText(result.hocPosition))
                        }
                        RgTd {
                            Text(result.penalty?.toString() ?: "-")
                        }

                        RgTd {
                            RgModalButton(
                                id = "race-result-add-edit",
                                style = RgButtonStyle.PrimaryOutline,
                                buttonLabel = { "Edit" }
                            ) {
                                viewModel.focusResultForEdit(result)
                            }
                            RgButton(
                                label = "Delete",
                                style = RgButtonStyle.Danger,
                                customClasses = listOf(AppStyle.marginStart)
                            ) {
                                alertsViewModel.confirm("Are you sure?", "Delete ${result.boat.boat?.name}'s result?") {
                                    if (it) {
                                        viewModel.deleteResult(result.id)
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
fun AddResult(
    state: RaceResultAddState,
    onBoatSkipper: (BoatSkipper?) -> Unit,
    onClass: (RaceClass) -> Unit,
    onBracket: (Bracket) -> Unit,
    onFinish: (FinishCode, Instant?, Int?) -> Unit,
    onPenalty: (Int?) -> Unit,
    onOrc: (Action, OrcCertificate) -> Unit,
    ratingChange: (RatingType, Int) -> Unit,
) {

    Div {
        Span { B { Text("Boat") } }
        if (state.allowBoatChange) {
            RgBoatDropdown(state.boats, state.raceResult.boat) { boat ->
                onBoatSkipper(boat)
            }
        } else {
            RgBoatDropdown(
                boats = listOf(state.raceResult.boat),
                selectedBoat = state.raceResult.boat,
                allowNone = false
            ) { /* selection ignored */ }
        }

        Br { }
        Span { B { Text("Finish Time") } }
        TimeRow(
            maxHoc = state.maxHoc,
            finish= state.raceResult.finish,
            finishCode = state.raceResult.finishCode,
            showHocOption = state.raceResult.ratingType.isCruising,
            hocPosition = state.raceResult.hocPosition,
            penalty = state.raceResult.penalty,
            start = state.selectedClassStart,
            onFinish = onFinish,
            onPenalty = onPenalty,
        )

        Br { }
        RatingSelections(
            selectMode = true,
            boatType = state.raceResult.ratingType,
            phrfRating = state.raceResult.phrfRating,
            orcCertificate = state.raceResult.orcCertificate,
            hideRatingSelector = state.raceResult.ratingType.isCruising,
            hideCruising = true,
            certs = state.orcCerts,
            onOrc = onOrc,
            typeChange = ratingChange,
        )

        state.selectedClass?.let { selected ->
            Span { B { Text("Class") } }
            RgDropdown(
                state.classes,
                selected,
                { it.name },
            ) {
                onClass(it)
            }
        }

        state.selectedBracket?.let { selected ->
            Br { }
            Span { B { Text("Bracket") } }
            RgDropdown(
                state.brackets,
                selected,
                { it.label() },
            ) {
                onBracket(it)
            }
        }
    }
}

@Composable
fun RgBoatDropdown(
    boats: List<BoatSkipper>,
    selectedBoat: BoatSkipper?,
    allowNone: Boolean = true,
    handler: (BoatSkipper?) -> Unit
) {
    if (allowNone) {
        RgDropdownNone(boats, selectedBoat, { it.dropLabel() }, true, { it.shortLabel() }, handler = handler)
    } else {
        RgDropdown(boats, selectedBoat, { it?.shortLabel() ?: "" }, handler = handler)
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

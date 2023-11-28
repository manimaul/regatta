package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import components.*
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import utils.*
import viewmodel.RaceResultAddViewModel
import viewmodel.ResultsViewModel

@Composable
fun RaceResultsEdit(id: Long?) {

}

@Composable
fun RaceResultsCreate(
    raceId: Long?,
    viewModel: RaceResultAddViewModel = remember { RaceResultAddViewModel(raceId) }
) {
    val state = viewModel.flow.collectAsState()
    H4 {
        Text("Add Race Results")
    }
    when (val race = state.value.race) {
        is Complete -> {

            B { Text(race.value.startDate?.year() ?: "") }

        }

        is Error -> ErrorDisplay(race) {
            viewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}

@Composable
fun RaceResults(
    viewModel: ResultsViewModel = remember { ResultsViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H4 {
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
                            RgTd { Text(rf.name) }
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

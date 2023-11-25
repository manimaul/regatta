package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import components.RgButton
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import viewmodel.ResultsViewModel

@Composable
fun RaceResultsEdit(id: Long?) {

}

@Composable
fun RaceResultsCreate(
    viewModel: ResultsViewModel = remember { ResultsViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H4 {
        Text("Add Race Results")
    }
    B { Text("Year") }
    RgYearSelect(state.value.year, state.value.years()) {
        viewModel.selectYear(it)
    }
    Br()
    state.value.racesByYear().forEach {
        P {
            Text(it.name)
        }
    }
    if (state.value.loggedIn) {
    } else {

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

@Composable
fun RaceResults(
    viewModel: ResultsViewModel = remember { ResultsViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H4 {
        Text("Race Results")
    }
    if (state.value.loggedIn) {
        P {
            RgButton("Add result") {
                viewModel.addResult()
            }
        }
    }
    B { Text("Year") }
    RgYearSelect(state.value.year, state.value.years()) {
        viewModel.selectYear(it)
    }
    Br()
    state.value.racesByYear().forEach {
        P {
            Text(it.name)
        }
    }
}
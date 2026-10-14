package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Series
import components.*
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.SeriesViewModel
import viewmodel.alertsViewModel

@Composable
fun Series(
    viewModel: SeriesViewModel = remember { SeriesViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    when (val series = flowState.series) {
        is Complete -> {
            if (flowState.sortMode) {
                SortSeries(series.value, viewModel)
            } else {
                AllSeries(series.value, viewModel)
            }
        }

        is Error -> ErrorDisplay(series) {
            viewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}

@Composable
fun SortSeries(
    allSeries: List<Series>,
    viewModel: SeriesViewModel,
) {
    H1 { Text("Series Sort Order") }
    var order by remember { mutableStateOf(allSeries) }
    RgSortable(allSeries, { it.name }) {
        order = it.mapIndexed { i, s -> s.copy(sort = i) }
    }
    RgButton("Cancel", customClasses = listOf(AppStyle.marginStart, AppStyle.marginTop)) {
        viewModel.sortMode(false)
    }
    RgButton("Save", style = RgButtonStyle.Success, customClasses = listOf(AppStyle.marginStart, AppStyle.marginTop)) {
        viewModel.saveOrder(order)
    }
}

@Composable
fun AllSeries(
    allSeries: List<Series>,
    viewModel: SeriesViewModel,
) {
    H1 { Text("Series") }
    Div(attrs = {
        classes(AppStyle.marginBot)
    }) {
        RgModalButton(
            id = "add-edit-series",
            style = RgButtonStyle.SuccessOutline,
            buttonLabel = { "Add Series" },
            openAction = { viewModel.editSeries(Series()) }
        )
        AddEditSeries(viewModel)
    }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series Name") }
                RgTh { Text("Total Races In Series") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            allSeries.forEach { series ->
                RgTr {
                    RgTd { Text(series.name) }
                    RgTd { Text("${series.raceCount}") }
                    RgTd {
                        RgModalButton(
                            id = "add-edit-series",
                            style = RgButtonStyle.PrimaryOutline,
                            buttonLabel = { "Edit Series" },
                            openAction = {
                                viewModel.editSeries(series)
                            }
                        )
                    }
                }
            }
        }
    }
    Div(attrs = { classes("flex-wrap") }) {
        RgButton("Change Sort Order") {
            viewModel.sortMode(true)
        }
    }
}

@Composable
fun AddEditSeries(
    viewModel: SeriesViewModel
) {
    val state by viewModel.flow.collectAsState()
    RgModalBody(
        id = "add-edit-series",
        modalTitle = {
            if (state.addEditSeries.id == 0L) {
                "Add Series "
            } else {
                "Edit Series"
            }
        },
        content = {
            RgForm {
                Fieldset {
                    P {
                        RgInput("Series Name", state.addEditSeries.name, true) {
                            viewModel.editSeriesName(it)
                        }
                    }

                }
            }

        },
        footer = {
            RgModalFooterRow {
                if (state.addEditSeries.id != 0L) {
                    RgButton(
                        label = "Delete",
                        style = RgButtonStyle.Danger,
                        disabled = state.addEditSeries.raceCount > 0L,
                        closesModal = true,
                    ) {
                        viewModel.delete(state.addEditSeries) {
                            alertsViewModel.showAlert("${it.name} deleted!")
                        }
                    }
                }
                RgButton(
                    label = "Save",
                    style = RgButtonStyle.Success,
                    disabled = state.addEditSeries.name.isBlank(),
                    closesModal = true,
                ) {
                    viewModel.upsert(state.addEditSeries) {
                        alertsViewModel.showAlert("${it.name} saved!")
                    }
                }
            }
        }
    )
}

package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Series
import components.*
import org.jetbrains.compose.web.dom.*
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.SeriesState
import viewmodel.SeriesViewModel

@Composable
fun Series(
    viewModel: SeriesViewModel = remember { SeriesViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        flowState.deleteSeries?.let { series ->
            RgConfirm("Delete '${series.name}'?") { delete ->
                if (delete) {
                    viewModel.deleteSeries(series)
                }
                viewModel.confirmDeleteSeries(null)
            }
        } ?: when (val series = flowState.series) {
            is Complete -> AllSeries(series.value, flowState.newSeries, viewModel)
            is Error -> ErrorDisplay(series) {
                viewModel.reload()
            }
            is Loading -> RgSpinner()
            Uninitialized -> Unit
        }
    }
}

@Composable
fun AllSeries(
    allSeries: List<Series>,
    newSeries: Series,
    viewModel: SeriesViewModel,
) {
    H1 { Text("Series") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series Name") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            allSeries.forEach { series ->
                RgTr {
                    RgTd { Text(series.name) }
                    RgTd {
                        RgButton("Delete", RgButtonStyle.Danger) {
                            viewModel.confirmDeleteSeries(series)
                        }
                    }
                }
            }
            RgTr {
                RgTd {
                    RgInput("New series name", newSeries.name, true) {
                        viewModel.setNewSeriesName(it)
                    }
                }
                RgTd {
                    RgButton("Add", RgButtonStyle.Primary, disabled = newSeries.name.isBlank()) {
                        viewModel.addSeries()
                    }
                }
            }
        }
    }
}

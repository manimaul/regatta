package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import components.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
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
        } ?: flowState.series.value?.let { allSeries ->
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
                            RgInput("New series name", flowState.newSeries.name, true) {
                                viewModel.setNewSeriesName(it)
                            }
                        }
                        RgTd {
                            RgButton("Add", RgButtonStyle.Primary, disabled = flowState.newSeries.name.isBlank()) {
                                viewModel.addSeries()
                            }
                        }
                    }
                }
            }
        } ?: run {
            RgSpinner()
        }
    }
}

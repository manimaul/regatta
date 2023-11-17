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
            Column {
                Confirm("Delete '${series.name}'?") { delete ->
                    if (delete) {
                        viewModel.deleteSeries(series)
                    }
                    viewModel.confirmDeleteSeries(null)
                }
            }
        } ?: flowState.series.value?.let { allSeries ->
            H1 { Text("Series") }
            Table(attrs = { classes("table", "table-striped-columns") }) {

                Caption {
                    Text("${Clock.System.now().toJSDate().getFullYear()}")
                }
                Tr {
                    Th { Text("Series Name") }
                    Th { Text("Action") }
                }
                allSeries.forEach { series ->
                    Tr {
                        Td { Text(series.name) }
                        Td {
                            RgButton("Delete", RgButtonStyle.Danger) {
                                viewModel.confirmDeleteSeries(series)
                            }
                        }
                    }
                }

                Tr {
                    Td { }
                    Td { }
                }

                Tr {
                    Td {
                        Input(type = InputType.Text) {
                            placeholder("Add series")
                            onInput {
                                viewModel.setNewSeriesName(it.value)
                            }
                            value(flowState.newSeries.name)
                        }
                    }
                    Td {
                        RgButton("Add", RgButtonStyle.Primary) {
                            viewModel.addSeries()
                        }
                    }

                }
            }
        } ?: run {
            Spinner()
        }
    }
}

package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Series
import components.Column
import components.Confirm
import components.RgButton
import components.RgButtonStyle
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.SeriesViewModel
import viewmodel.provideSeriesViewModel

@Composable
fun Series(
    viewModel: SeriesViewModel = provideSeriesViewModel()
) {
    var deleteSeries: Series? by remember { mutableStateOf(null) }
    var addName by remember { mutableStateOf("") }
    Div {
        deleteSeries?.let { series ->
            Column {
                Confirm("Delete '${series.name}'?") { delete ->
                    if (delete) {
                        viewModel.deleteSeries(series)
                    }
                    deleteSeries = null
                }
            }
        } ?: run {
            Article {
                H1 { Text("Series") }
                Table(attrs = { classes("striped") }) {

                    Caption {
                        Text("${Clock.System.now().toJSDate().getFullYear()}")
                    }
                    Tr {
                        Th { Text("Series Name") }
                        Th { Text("Action") }
                    }
                    viewModel.series.forEach { series ->
                        Tr {
                            Td { Text(series.name) }
                            Td {
                                RgButton("Delete", RgButtonStyle.Error) {
                                    deleteSeries = series
                                }
                            }
                        }
                    }

                    Tr {
                        Td {  }
                        Td {  }
                    }

                    Tr {
                        Td {
                            Input(type = InputType.Text) {
                                placeholder("Add series")
                                onInput {
                                    addName = it.value
                                }
                                value(addName)
                            }
                        }
                        Td {
                            RgButton("Add", RgButtonStyle.Primary) {
                                viewModel.addSeries(Series(name = addName))
                                addName = ""
                            }
                        }

                    }
                }
            }
        }
    }
}

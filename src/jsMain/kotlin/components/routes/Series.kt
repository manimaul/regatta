package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Series
import components.*
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.SeriesViewModel

@Composable
fun Series(
    viewModel: SeriesViewModel = remember { SeriesViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    when (val series = flowState.series) {
        is Complete -> AllSeries(series.value, viewModel)
        is Error -> ErrorDisplay(series) {
            viewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}

@Composable
fun AllSeries(
    allSeries: List<Series>,
    viewModel: SeriesViewModel,
) {
    val state by viewModel.flow.collectAsState()
    H1 { Text("Series") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series Name") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            allSeries.forEachIndexed { i, series ->
                if (state.editId == series.id) {
                    EditSeries(series, viewModel, i == 0, i == allSeries.size - 1)
                } else {
                    RgTr {
                        RgTd { Text(series.name) }
                        RgTd {
                            RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-end")) {
                                viewModel.editSeries(series.id)
                            }
                        }
                    }
                }
            }
            EditSeries(Series(), viewModel)
        }
    }
}

@Composable
fun EditSeries(
    edit: Series,
    viewModel: SeriesViewModel,
    first: Boolean = true,
    last: Boolean = true,
) {

    var series by remember { mutableStateOf(edit) }
    if (edit.id != series.id) {
        series = edit
    }
    RgTr {
        RgTd {
            RgInput("Name", series.name, true) {
                series = series.copy(name = it)
            }
        }
        RgTd {
            if (series.id > 0) {
                RgButton(
                    "Cancel",
                    RgButtonStyle.Primary,
                    customClasses = listOf("float-end", AppStyle.marginStart),
                ) {
                    viewModel.editSeries(null)
                    series = Series()
                }
                RgButton(
                    "Delete",
                    RgButtonStyle.Danger,
                    customClasses = listOf("float-end", AppStyle.marginStart),
                ) {
                    viewModel.delete(series)
                    viewModel.editSeries(null)
                    series = Series()
                }
            }
            RgButton(
                "Save",
                RgButtonStyle.Success,
                customClasses = listOf("float-end", AppStyle.marginStart),
            ) {
                viewModel.upsert(series)
                viewModel.editSeries(null)
                series = Series()
            }
            if (!first || !last) {
                if (!first) RgButton(
                    "Up",
                    RgButtonStyle.Success,
                    customClasses = listOf("float-end", AppStyle.marginStart),
                ) {
                    viewModel.moveUp(series)
                }
                if (!last) RgButton(
                    "Down",
                    RgButtonStyle.Success,
                    customClasses = listOf("float-end", AppStyle.marginStart),
                ) {
                    viewModel.moveDown(series)
                }
            }
        }
    }
}
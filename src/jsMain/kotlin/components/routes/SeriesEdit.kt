package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Series
import components.*
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.Operation
import viewmodel.SeriesEditViewModel

@Composable
fun SeriesEdit(
    id: Long? = null,
    viewModel: SeriesEditViewModel = remember { SeriesEditViewModel(id) }
) {
    val state by viewModel.flow.collectAsState()
    when (val series = state.series) {
        is Complete -> {
            when (state.operation) {
                Operation.None -> {}
                Operation.Fetched -> SeriesEditor(series.value, viewModel)
                Operation.Updated -> RgOk("'${series.value.name}' updated!") {
                    viewModel.cancelEdit()
                }

                Operation.Deleted -> RgOk("'${series.value.name}' deleted!") {
                    viewModel.cancelEdit()
                }
            }
        }

        is Error -> Text(series.message)
        is Loading -> RgSpinner()
        Uninitialized -> {}
    }
}

@Composable
fun SeriesEditor(
    series: Series,
    viewModel: SeriesEditViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var newSeries by remember { mutableStateOf(series) }

    if (confirmDelete) {
        RgConfirm("Delete '${newSeries.name}'?") { delete ->
            if (delete) {
                viewModel.delete(series)
            } else {
                confirmDelete = false
            }
        }
    } else {

        H1 { Text("Edit") }
        RgForm {
            Fieldset {
                P {
                    RgInput("Name", newSeries.name) {
                        newSeries = newSeries.copy(name = it)
                    }
                }
            }

            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelEdit()
            }
            RgButton("Save", RgButtonStyle.Primary) {
                viewModel.upsert(newSeries)
            }
            RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                confirmDelete = true
            }
        }
    }
}

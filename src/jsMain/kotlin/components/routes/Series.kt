package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Series
import components.Confirm
import components.TextInputAdd
import org.jetbrains.compose.web.dom.*
import viewmodel.SeriesViewModel
import viewmodel.provideSeriesViewModel
import kotlin.text.Typography.nbsp

@Composable
fun Series(
    viewModel: SeriesViewModel = provideSeriesViewModel()
) {
    var deleteSeries: Series? by remember { mutableStateOf(null) }
    Div {
        H4 {
            Text("Series")
        }
        deleteSeries?.let { series ->
            Confirm("Delete '${series.name}'?") { delete ->
                if (delete) {
                    viewModel.deleteSeries(series)
                }
                deleteSeries = null
            }
        } ?: run {
            viewModel.series.forEach { series ->
                Text(series.name)
                Text("$nbsp")
                Button(attrs = {
                    onClick {
                        deleteSeries = series
                    }
                }) {
                    Text("X")
                }
                Br()
            }
            Hr()
            TextInputAdd {
                viewModel.addSeries(Series(name = it))
            }
        }
    }
}

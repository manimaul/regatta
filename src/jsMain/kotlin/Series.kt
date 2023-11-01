import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Series
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import viewmodel.SeriesViewModel
import viewmodel.provideSeriesViewModel
import kotlin.text.Typography.nbsp

@Composable
fun Series(
    viewModel: SeriesViewModel = provideSeriesViewModel()
) {
    Div {
        H4 {
            Text("Series")
        }
        viewModel.series.forEach { series ->
            Text(series.name)
            Text("$nbsp")
            Button(attrs = {
                onClick {
                    viewModel.deleteSeries(series)
                }
            }) {
                Text("X")
            }
            Br()
        }
        Hr()
        TextInput {
            viewModel.addSeries(Series(name =it))
        }
    }
}

@Composable
fun TextInput(
  onSubmit: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Div {
        Input(type = InputType.Text) {
            onInput {
                name = it.value
            }
            value(name)
        }
        Text("$nbsp")
        Button(attrs = {
            onClick {
                onSubmit(name)
                name = ""
            }
        }) {
            Text("Submit")
        }
    }
}
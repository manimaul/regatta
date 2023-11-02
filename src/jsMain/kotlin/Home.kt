import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import viewmodel.ClockViewModel

@Composable
fun Home() {
    Text("Home")
    Clock()
}

@Composable
fun Clock(
    viewModel: ClockViewModel = remember { ClockViewModel() }
) {
    Div {
        H4 { Text(viewModel.readOut) }
    }
}
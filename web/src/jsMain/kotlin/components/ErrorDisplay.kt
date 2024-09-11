package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import utils.Error


@Composable
fun ErrorDisplay(
    error: Error<*>,
    handler: () -> Unit
) {
    H1 {
        Text("Something went wrong")
    }
    P {
        B { Text(error.message) }
    }
    RgButton("OK", RgButtonStyle.Danger) {
       handler()
    }
}

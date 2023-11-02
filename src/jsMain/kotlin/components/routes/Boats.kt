package components.routes

import androidx.compose.runtime.Composable
import components.Spinner
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.*
import kotlin.math.PI

@Composable
fun Boats() {
    H4 {
        Text("Boats")
    }
    Spinner(50f)
    Spinner(100f)
    Spinner(50f)
}

package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text

@Composable
fun Confirm(
    msg: String,
    handler: (Boolean) -> Unit
) {
    Div {
        H3 {
            Text(msg)
        }
        RgButton("No", RgButtonStyle.Error) {
            handler(false)
        }
        RgButton("Yes", RgButtonStyle.Primary) {
            handler(true)
        }
    }
}

package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
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
        Text(" ")
        Button(attrs = {
            onClick {
                handler(true)
            }
        }) {
            Text("Yes")
        }
        Text(" ")
        Button(attrs = {
            onClick {
                handler(false)
            }
        }) {
            Text("No")
        }
    }
}

package components

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text

@Composable
fun TextInputAdd(
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
        Text("${Typography.nbsp}")
        Button(attrs = {
            onClick {
                onSubmit(name)
                name = ""
            }
        }) {
            Text("Add")
        }
    }
}

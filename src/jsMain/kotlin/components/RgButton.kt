package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

enum class RgButtonStyle(val classes:  Array<String>) {
    Default(arrayOf("button")),
    Primary(arrayOf("button", "primary")),
    Secondary(arrayOf("button", "secondary")),
    Dark(arrayOf("button", "dark")),
    Error(arrayOf("button", "error")),
    Success(arrayOf("button", "success")),
    Clear(arrayOf("button", "clear")),
    SecondaryOutline(arrayOf("button", "secondary", "outline")),
    PrimaryOutline(arrayOf("button", "primary", "outline")),
}
@Composable
fun RgButton(
    label: String,
    style: RgButtonStyle = RgButtonStyle.Default,
    disabled: Boolean = false,
    click: () -> Unit,
) {
    Button(attrs = {
        classes(*style.classes)
        onClick { click() }
        if (disabled) { disabled() }
    }) {
        Text(label)
    }
}

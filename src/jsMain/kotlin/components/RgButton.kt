package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

enum class RgButtonStyle(val classes:  Array<String>) {
    Default(arrayOf("btn")),
    Primary(arrayOf("btn", "btn-primary")), //btn btn-primary
    Secondary(arrayOf("btn", "btn-secondary")),
    Danger(arrayOf("btn", "btn-danger")),
    Success(arrayOf("btn", "btn-success")),
    Light(arrayOf("btn", "btn-light")),
    Dark(arrayOf("btn", "btn-dark")),
    Link(arrayOf("btn", "btn-link")),
    SuccessOutline(arrayOf("btn", "btn-outline-success")),
    SecondaryOutline(arrayOf("btn", "btn-outline-secondary")),
    PrimaryOutline(arrayOf("btn", "btn-outline-primary")),
}
@Composable
fun RgButton(
    label: String,
    style: RgButtonStyle = RgButtonStyle.Primary,
    disabled: Boolean = false,
    customClasses: List<String>? = null,
    click: () -> Unit,
) {
    Button(attrs = {
        customClasses?.let {
               it + style.classes
            }?.toTypedArray()?.let {
                classes(*it)
        } ?: classes(*style.classes)
        onClick { click() }
        if (disabled) { disabled() }
    }) {
        Text(label)
    }
}


@Composable
fun RgButtonGroup(
    content: ContentBuilder<HTMLDivElement>? = null
) {
    Div(attrs = {
        classes("btn-group")
        attr("role", "group")
    }, content)
}

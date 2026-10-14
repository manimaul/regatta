package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

enum class RgButtonStyle(val classes:  Array<String>) {
    Default(arrayOf("rg-btn")),
    Primary(arrayOf("rg-btn", "rg-btn-primary")),
    Secondary(arrayOf("rg-btn", "rg-btn-secondary")),
    Danger(arrayOf("rg-btn", "rg-btn-danger")),
    Success(arrayOf("rg-btn", "rg-btn-success")),
    Light(arrayOf("rg-btn", "rg-btn-light")),
    Dark(arrayOf("rg-btn", "rg-btn-dark")),
    Link(arrayOf("rg-btn", "rg-btn-link")),
    SuccessOutline(arrayOf("rg-btn", "rg-btn-outline-success")),
    SecondaryOutline(arrayOf("rg-btn", "rg-btn-outline-secondary")),
    PrimaryOutline(arrayOf("rg-btn", "rg-btn-outline-primary")),
}

/**
 * @param closesModal when true and this button is inside an [RgModalBody], the modal is closed after [click].
 */
@Composable
fun RgButton(
    label: String,
    style: RgButtonStyle = RgButtonStyle.Primary,
    disabled: Boolean = false,
    customClasses: List<String>? = null,
    closesModal: Boolean = false,
    click: () -> Unit,
) {
    val closeModal = LocalModalClose.current
    Button(attrs = {
        customClasses?.let {
               it + style.classes
            }?.toTypedArray()?.let {
                classes(*it)
        } ?: classes(*style.classes)
        onClick {
            click()
            if (closesModal) {
                closeModal?.invoke()
            }
        }
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

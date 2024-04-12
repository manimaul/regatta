package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLDivElement

private var num = 0

@Composable
fun RgModalButton(
    id: String,
    style: RgButtonStyle = RgButtonStyle.SuccessOutline,
    buttonLabel: () -> String,
    openAction: (() -> Unit)? = null,
) {
    Button(attrs = {
        classes(*style.classes)
        attr("data-bs-toggle", "modal")
        attr("data-bs-target", "#$id")
        onClick { openAction?.invoke() }
    }) {
        Text(buttonLabel())
    }
}

@Composable
fun RgModalBody(
    id: String,
    modalTitle: () -> String,
    content: ContentBuilder<HTMLDivElement>,
    footer: ContentBuilder<HTMLDivElement>? = null,
) {
    Div(attrs = {
        classes("modal", "fade")
        id(id)
        attr("tabindex", "-1")
        attr("aria-hidden", "true")
        attr("aria-labelledby", "$id-title")
    }) {
        Div(attrs = {
            classes("modal-dialog")
        }) {
            Div(attrs = {
                classes("modal-content")
            }) {
                Div(attrs = {
                    classes("modal-header")
                }) {
                    H5(attrs = {
                        classes("modal-title")
                        id("$id-title")
                    }) { Text(modalTitle()) }
                    Button(attrs = {
                        type(ButtonType.Button)
                        classes("btn-close")
                        attr("data-bs-dismiss", "modal")
                        attr("aria-label", "Close")
                    })
                }
                Div(attrs = {
                    classes("modal-body")
                }, content)
                footer?.let {
                    Div(attrs = {
                        classes("modal-footer")
                    }, footer)
                }
            }
        }
    }

}

@Composable
fun RgModal(
    buttonLabel: String,
    modalTitle: String,
    openAction: (() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>,
    footer: ContentBuilder<HTMLDivElement>? = null,
) {
    val modalId = remember { "modal-${++num}" }
    RgModalButton(id = modalId, buttonLabel = { buttonLabel }, openAction = openAction)
    RgModalBody(id = modalId, modalTitle = { modalTitle }, content = content, footer = footer)
}

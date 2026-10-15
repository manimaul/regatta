package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLDialogElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.EventListener
import viewmodel.alertsViewModel
import viewmodel.confirmationModalId

private var num = 0

/**
 * Closes the enclosing [RgModalBody], or null outside a modal. Used by `RgButton(closesModal = true)`.
 */
val LocalModalClose = compositionLocalOf<(() -> Unit)?> { null }

private fun dialog(id: String): HTMLDialogElement? = document.getElementById(id) as? HTMLDialogElement

@Composable
fun RgModalButton(
    id: String,
    style: RgButtonStyle = RgButtonStyle.SuccessOutline,
    customClasses: List<String> = emptyList(),
    buttonLabel: () -> String,
    openAction: (() -> Unit)? = null,
) {
    Button(attrs = {
        classes(*(style.classes + customClasses))
        onClick {
            openAction?.invoke()
            dialog(id)?.takeIf { !it.open }?.showModal()
        }
    }) {
        Text(buttonLabel())
    }
}

/**
 * A native `<dialog>` that fills the viewport and scrolls, wrapping a centered panel that doesn't clip its content
 * (so dropdown menus inside it aren't cut off). Mirrors Bootstrap's `.modal` > `.modal-dialog` structure.
 *
 * @param large widen the panel on large screens (Bootstrap's `.modal-lg`) for content with tables.
 */
@Composable
fun RgModalBody(
    id: String,
    modalTitle: () -> String,
    large: Boolean = false,
    content: ContentBuilder<HTMLDivElement>,
    footer: ContentBuilder<HTMLDivElement>? = null,
) {
    val close = remember(id) { { dialog(id)?.close(); Unit } }
    TagElement<HTMLDialogElement>("dialog", applyAttrs = {
        classes("rg-modal")
        id(id)
        attr("aria-labelledby", "$id-title")
        ref { element ->
            // Backdrop click closes, like Bootstrap: only when the press both started and ended outside the panel,
            // so dragging a text selection out of the panel doesn't dismiss it.
            var pressedOnBackdrop = false
            val down = EventListener { pressedOnBackdrop = it.target == element }
            val click = EventListener { event ->
                if (pressedOnBackdrop && event.target == element) {
                    element.close()
                }
            }
            element.addEventListener("pointerdown", down)
            element.addEventListener("click", click)
            if (id == confirmationModalId) {
                alertsViewModel.confirmationModalCreated(element)
            }
            onDispose {
                element.removeEventListener("pointerdown", down)
                element.removeEventListener("click", click)
                if (id == confirmationModalId) {
                    alertsViewModel.confirmationModalDestroyed()
                }
            }
        }
    }) {
        CompositionLocalProvider(LocalModalClose provides close) {
            Div(attrs = { classes(if (large) listOf("rg-modal-panel", "rg-modal-panel-lg") else listOf("rg-modal-panel")) }) {
                Div(attrs = { classes("rg-modal-header") }) {
                    H5(attrs = {
                        classes("rg-modal-title")
                        id("$id-title")
                    }) { Text(modalTitle()) }
                    Button(attrs = {
                        type(ButtonType.Button)
                        classes("rg-modal-close")
                        attr("aria-label", "Close")
                        onClick { close() }
                    }) {
                        Text("×")
                    }
                }
                Div(attrs = { classes("rg-modal-body") }, content)
                footer?.let {
                    Div(attrs = { classes("rg-modal-footer") }, footer)
                }
            }
        }
    }
}

/**
 * A footer row that spreads its buttons to both ends and wraps them on narrow screens.
 */
@Composable
fun RgModalFooterRow(content: ContentBuilder<HTMLDivElement>) {
    Div(attrs = { classes("rg-modal-footer-row") }, content)
}

@Composable
fun RgModal(
    buttonLabel: String,
    modalTitle: String,
    openAction: (() -> Unit)? = null,
    buttonClasses: List<String> = emptyList(),
    content: ContentBuilder<HTMLDivElement>,
    footer: ContentBuilder<HTMLDivElement>? = null,
) {
    val modalId = remember { "modal-${++num}" }
    RgModalButton(id = modalId, buttonLabel = { buttonLabel }, customClasses = buttonClasses, openAction = openAction)
    RgModalBody(id = modalId, modalTitle = { modalTitle }, content = content, footer = footer)
}

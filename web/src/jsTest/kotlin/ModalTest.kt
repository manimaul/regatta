import components.*
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLDialogElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit
import viewmodel.alertsViewModel
import viewmodel.confirmationModalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 3 (docs/explore_implemented/explore_tailwind.md): modals are native <dialog>s opened by id, closed by RgButton(closesModal),
 * the × button, or a backdrop click; no Bootstrap JS or data-bs-* attributes.
 */
@OptIn(ComposeWebExperimentalTestsApi::class)
class ModalTest {

    private fun HTMLElement.button(label: String): HTMLElement {
        val list = querySelectorAll("button")
        return (0 until list.length).map { list.item(it) as HTMLElement }.first { it.textContent == label }
    }

    private fun dialog(id: String) = document.getElementById(id) as HTMLDialogElement

    private fun pressAndClick(down: EventTarget, up: EventTarget) {
        down.dispatchEvent(Event("pointerdown", EventInit(bubbles = true)))
        up.dispatchEvent(MouseEvent("click", MouseEventInit(bubbles = true)))
    }

    /** State changes go through coroutines; poll instead of guessing how long they take. */
    private suspend fun eventually(what: String, condition: () -> Boolean) {
        repeat(100) {
            if (condition()) return
            delay(20)
        }
        throw AssertionError("timed out waiting for: $what")
    }

    @Test
    fun modalButtonOpensByIdAndClosesModalButtonsClose() = runTest {
        var opened = 0
        var saved = 0
        composition {
            RgModalButton(id = "m1", buttonLabel = { "Open" }, openAction = { opened++ })
            RgModalBody(id = "m1", modalTitle = { "Edit boat" }, content = { Text("body") }, footer = {
                RgModalFooterRow {
                    RgButton("Clear", RgButtonStyle.PrimaryOutline) { }
                    RgButton("Save", RgButtonStyle.Success, closesModal = true) { saved++ }
                }
            })
        }
        val dlg = dialog("m1")
        assertTrue(dlg.classList.contains("rg-modal"))
        assertEquals("Edit boat", document.getElementById("m1-title")?.textContent)
        assertNull(root.querySelector("[data-bs-toggle], [data-bs-target], [data-bs-dismiss]"))
        assertFalse(dlg.open)

        root.button("Open").click()
        assertTrue(dlg.open)
        assertEquals(1, opened)

        dlg.button("Clear").click()
        assertTrue(dlg.open, "buttons without closesModal leave the modal open")

        dlg.button("Save").click()
        assertEquals(1, saved, "the click handler runs before the modal closes")
        assertFalse(dlg.open)
    }

    @Test
    fun closeButtonAndBackdropClose() = runTest {
        composition {
            RgModal(buttonLabel = "Open", modalTitle = "Time", content = { Text("body") })
        }
        val dlg = root.querySelector("dialog") as HTMLDialogElement
        val panel = dlg.querySelector(".rg-modal-panel")!!

        root.button("Open").click()
        dlg.button("×").click()
        assertFalse(dlg.open, "× closes")

        root.button("Open").click()
        pressAndClick(down = panel, up = dlg)
        assertTrue(dlg.open, "a press that starts inside the panel and ends on the backdrop keeps it open")

        pressAndClick(down = dlg, up = dlg)
        assertFalse(dlg.open, "a backdrop click closes")
    }

    @Test
    fun confirmationAnswersAndDismissal() = runTest {
        composition { Alerts() }
        // other tests' Alerts() compositions may still be in the document with the same id: look inside this root
        val dlg = root.querySelector("#$confirmationModalId") as HTMLDialogElement
        // alertsViewModel is a global: wait for *this* composition's dialog, not one left by another test's Alerts()
        eventually("confirmation dialog registered") { alertsViewModel.flow.value.confirmationModal == dlg }

        var answer: Boolean? = null
        alertsViewModel.confirm("Are you sure?", "Delete Kahuna?") { answer = it }
        eventually("confirmation shown") { dlg.open && alertsViewModel.flow.value.confirmation != null }
        eventually("title shows the question") {
            root.querySelector("#$confirmationModalId-title")?.textContent == "Are you sure?"
        }

        dlg.button("Yes").click()
        assertEquals(true, answer)
        assertFalse(dlg.open)
        eventually("answered confirmation cleared") { alertsViewModel.flow.value.confirmation == null }

        // Esc / backdrop dismiss without answering (as with Bootstrap), and the stale question is dropped
        answer = null
        alertsViewModel.confirm("Delete again?") { answer = it }
        eventually("second confirmation shown") { dlg.open && alertsViewModel.flow.value.confirmation != null }
        dlg.close()
        eventually("dismissed confirmation cleared") { alertsViewModel.flow.value.confirmation == null }
        assertNull(answer)
    }
}

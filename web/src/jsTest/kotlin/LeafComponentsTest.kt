import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import components.*
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.EventInit
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavior of the Phase 2 Tailwind leaf components (doc/explore_tailwind.md): Bootstrap classes replaced by rg-*,
 * public APIs unchanged, dropdowns dismiss on outside press / Escape.
 */
@OptIn(ComposeWebExperimentalTestsApi::class)
class LeafComponentsTest {

    private fun HTMLElement.q(selector: String): HTMLElement? = querySelector(selector) as? HTMLElement
    private fun HTMLElement.qa(selector: String): List<HTMLElement> {
        val list = querySelectorAll(selector)
        return (0 until list.length).map { list.item(it) as HTMLElement }
    }

    private fun pressOn(target: EventTarget) =
        target.dispatchEvent(Event("pointerdown", EventInit(bubbles = true, cancelable = true)))

    private fun escape(): KeyboardEvent {
        val event = KeyboardEvent("keydown", KeyboardEventInit(key = "Escape", bubbles = true, cancelable = true))
        document.body!!.dispatchEvent(event)
        return event
    }

    private fun HTMLInputElement.type(text: String) {
        value = text
        dispatchEvent(Event("input", EventInit(bubbles = true)))
    }

    @Test
    fun dropdownOpensSelectsAndCloses() = runTest {
        var picked: String? = null
        composition {
            RgDropdown(listOf("Alpha", "Bravo", "Charlie"), "Alpha", { it }) { picked = it }
        }
        val toggle = root.q(".rg-dropdown > button")!!
        assertTrue(toggle.classList.contains("rg-btn"))
        assertTrue(toggle.classList.contains("rg-btn-primary"))
        assertTrue(toggle.classList.contains("rg-dropdown-toggle"))
        assertNull(root.q(".rg-dropdown-menu"))

        toggle.click()
        waitForRecompositionComplete()
        val items = root.qa(".rg-dropdown-menu .rg-dropdown-item")
        assertEquals(listOf("Alpha", "Bravo", "Charlie"), items.map { it.textContent })
        assertEquals("true", toggle.getAttribute("aria-expanded"))

        items[1].click()
        waitForRecompositionComplete()
        assertEquals("Bravo", picked)
        assertNull(root.q(".rg-dropdown-menu"))
    }

    @Test
    fun dropdownClosesOnOutsidePressButNotInsidePress() = runTest {
        composition {
            RgDropdown(listOf("Alpha", "Bravo"), "Alpha", { it }) { }
        }
        root.q(".rg-dropdown > button")!!.click()
        waitForRecompositionComplete()

        pressOn(root.q(".rg-dropdown-menu")!!)
        // nothing should recompose, so waitForRecompositionComplete() would never return: give a few frames instead
        delay(100)
        assertNotNull(root.q(".rg-dropdown-menu"), "a press inside the menu keeps it open")

        pressOn(document.body!!)
        waitForRecompositionComplete()
        assertNull(root.q(".rg-dropdown-menu"), "a press outside closes it")
    }

    @Test
    fun dropdownClosesOnEscapeAndConsumesIt() = runTest {
        composition {
            RgDropdown(listOf("Alpha", "Bravo"), "Alpha", { it }) { }
        }
        root.q(".rg-dropdown > button")!!.click()
        waitForRecompositionComplete()

        val event = escape()
        waitForRecompositionComplete()
        assertNull(root.q(".rg-dropdown-menu"))
        assertTrue(event.defaultPrevented, "Escape is consumed so an enclosing <dialog> stays open")

        val later = escape()
        assertFalse(later.defaultPrevented, "a closed dropdown no longer listens")
    }

    @Test
    fun dropdownWithOneItemIsDisabled() = runTest {
        composition {
            RgIdDropdown(id = "one", items = listOf("Only"), selectedItem = "Only", name = { it }) { }
        }
        assertTrue((root.q("#one > button") as HTMLButtonElement).disabled)
    }

    @Test
    fun dropdownNoneFiltersAndSelectsNone() = runTest {
        var picked: String? = "unset"
        composition {
            RgDropdownNone(listOf("Zulu Tango", "Kahuna", "Grace E"), "Kahuna", { it }) { picked = it }
        }
        root.q(".rg-dropdown > button")!!.click()
        waitForRecompositionComplete()
        assertNotNull(root.q(".rg-dropdown-menu hr.rg-dropdown-divider"))

        val filter = root.q(".rg-dropdown-menu input.rg-input") as HTMLInputElement
        assertTrue(filter.classList.contains("mx-4"), "Bootstrap mx-3 (1rem) maps to Tailwind mx-4")
        filter.type("zulu")
        waitForRecompositionComplete()
        assertEquals(listOf("None", "Zulu Tango"), root.qa(".rg-dropdown-item").map { it.textContent })

        root.qa(".rg-dropdown-item").first().click()
        waitForRecompositionComplete()
        assertNull(picked)
        assertNull(root.q(".rg-dropdown-menu"))

        // the filter is cleared when the menu closes
        root.q(".rg-dropdown > button")!!.click()
        waitForRecompositionComplete()
        assertEquals(4, root.qa(".rg-dropdown-item").size)
    }

    @Test
    fun buttonClosesEnclosingModalOnlyWhenAsked() = runTest {
        var closed = 0
        var clicked = 0
        composition {
            CompositionLocalProvider(LocalModalClose provides { closed++ }) {
                RgButton("Keep", RgButtonStyle.PrimaryOutline) { clicked++ }
                RgButton("Save", RgButtonStyle.Success, closesModal = true) { clicked++ }
            }
        }
        val (keep, save) = root.qa("button")
        assertEquals("rg-btn rg-btn-outline-primary", keep.className)
        keep.click()
        assertEquals(1, clicked)
        assertEquals(0, closed)
        save.click()
        assertEquals(2, clicked)
        assertEquals(1, closed)
    }

    @Test
    fun numberInputReportsOnlyValidNumbers() = runTest {
        val seen = mutableListOf<Number>()
        composition {
            RgNumberInput(label = "PHRF Rating", value = 72) { seen.add(it) }
        }
        val input = root.q("input") as HTMLInputElement
        assertTrue(input.classList.contains("rg-input"))
        assertEquals("72", input.value)
        input.type("-")
        input.type("-6")
        input.type("abc")
        assertEquals(listOf<Number>(-6), seen)
    }

    @Test
    fun inputWithButtonUsesInputGroupAndOutlineButton() = runTest {
        val events = mutableListOf<Pair<String, Boolean>>()
        var value by mutableStateOf("")
        composition {
            RgInputWithButton(label = "ORC Reference Number", value = value, btnLabel = "Add Cert") { v, clicked ->
                events.add(v to clicked)
                if (!clicked) value = v
            }
        }
        val group = root.q(".rg-input-group")!!
        assertTrue(group.classList.contains("mb-4"), "Bootstrap mb-3 (1rem) maps to Tailwind mb-4")
        val input = group.q("input.rg-input") as HTMLInputElement
        input.type(" 04560004KJP ")
        waitForRecompositionComplete()
        val button = group.q("button")!!
        assertEquals("rg-btn rg-btn-outline-primary", button.className)
        button.click()
        assertEquals(listOf("04560004KJP" to false, "04560004KJP" to true), events)
    }

    @Test
    fun checkAndSwitchRenderAndReport() = runTest {
        var checked = false
        var switched: Boolean? = null
        composition {
            RgCheck(label = "Club member", checked = false) { checked = it }
            RgSwitch(id = "sw", num = 1, label = "PHRF", check = { true }) { switched = it }
        }
        val checks = root.qa(".rg-check")
        assertEquals(2, checks.size)

        val box = checks[0].q("input.rg-checkbox") as HTMLInputElement
        box.checked = true
        box.dispatchEvent(Event("change", EventInit(bubbles = true)))
        assertTrue(checked)

        val switch = checks[1].q("input.rg-switch") as HTMLInputElement
        assertTrue(switch.checked)
        assertEquals("sw1", checks[1].q("label")!!.getAttribute("for"))
        switch.checked = false
        switch.dispatchEvent(Event("change", EventInit(bubbles = true)))
        assertEquals(false, switched)
    }

    @Test
    fun timeUsesCompactInputGroup() = runTest {
        composition {
            RgTime(label = "Start", date = kotlin.time.Clock.System.now(), showSeconds = true) { }
        }
        val group = root.q(".rg-input-group.rg-input-group-sm")!!
        assertEquals(3, group.qa("input.rg-input").size)
        assertEquals(listOf("Start", ":", ":"), group.qa(".rg-input-addon").map { it.textContent })
        assertNull(root.q(".form-control, .input-group, .input-group-text"), "no Bootstrap classes left")
    }
}

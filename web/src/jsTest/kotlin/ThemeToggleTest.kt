import components.ThemeToggle
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLElement
import viewmodel.Theme
import viewmodel.ThemeViewModel
import viewmodel.themeStorageKey
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ComposeWebExperimentalTestsApi::class)
class ThemeToggleTest {

    @Test
    fun defaultsToLightWithNoSavedChoice() {
        window.localStorage.removeItem(themeStorageKey)
        val vm = ThemeViewModel()
        assertEquals(Theme.Light, vm.flow.value.theme)
        assertEquals("light", document.documentElement?.getAttribute("data-theme"))
    }

    @Test
    fun toggleSwitchesThemeAndRemembersIt() = runTest {
        window.localStorage.removeItem(themeStorageKey)
        val vm = ThemeViewModel()
        composition { ThemeToggle(viewModel = vm) }

        val toggle = root.querySelector(".rg-theme-toggle") as HTMLElement
        assertEquals("false", toggle.getAttribute("aria-checked"))

        toggle.click()
        var tries = 0
        while (toggle.getAttribute("aria-checked") != "true" && tries++ < 50) delay(20)
        assertEquals("true", toggle.getAttribute("aria-checked"))
        assertEquals("dark", document.documentElement?.getAttribute("data-theme"))
        assertEquals("dark", window.localStorage.getItem(themeStorageKey))
        assertEquals(Theme.Dark, ThemeViewModel().flow.value.theme, "a fresh load picks up the saved choice")

        toggle.click()
        tries = 0
        while (toggle.getAttribute("aria-checked") != "false" && tries++ < 50) delay(20)
        assertEquals("false", toggle.getAttribute("aria-checked"))
        assertEquals("light", document.documentElement?.getAttribute("data-theme"))

        window.localStorage.removeItem(themeStorageKey)
    }
}

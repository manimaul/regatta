import components.NavBar
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLElement
import viewmodel.Route
import viewmodel.RouteViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ComposeWebExperimentalTestsApi::class)
class NavBarTest {

    @Test
    fun hamburgerTogglesAndLinkClickClosesMenu() = runTest {
        val startUrl = window.location.href
        val routes = RouteViewModel()
        composition { NavBar(viewModel = routes) }

        val toggler = root.querySelector(".rg-navbar-toggler") as HTMLElement
        val collapse = root.querySelector(".rg-navbar-collapse") as HTMLElement
        assertEquals("false", collapse.getAttribute("data-open"))
        assertEquals("false", toggler.getAttribute("aria-expanded"))

        toggler.click()
        waitForRecompositionComplete()
        assertEquals("true", collapse.getAttribute("data-open"))
        assertEquals("true", toggler.getAttribute("aria-expanded"))

        toggler.click()
        waitForRecompositionComplete()
        assertEquals("false", collapse.getAttribute("data-open"), "the hamburger closes it again")

        toggler.click()
        waitForRecompositionComplete()
        val links = root.querySelectorAll(".rg-nav-link")
        val course = (0 until links.length).map { links.item(it) as HTMLElement }.first { it.textContent == "Course" }
        course.click()
        waitForRecompositionComplete()
        assertEquals("false", collapse.getAttribute("data-open"), "choosing a link closes the menu")
        assertEquals("false", toggler.getAttribute("aria-expanded"))

        // the view model updates on a coroutine; then put the Karma page's URL back
        var tries = 0
        while (routes.flow.value.current.route != Route.Course && tries++ < 50) delay(20)
        assertEquals(Route.Course, routes.flow.value.current.route)
        window.history.replaceState(null, "", startUrl)
    }
}

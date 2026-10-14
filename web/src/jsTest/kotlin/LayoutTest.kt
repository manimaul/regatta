import components.*
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLElement
import viewmodel.alertsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 4 (docs/explore_implemented/explore_tailwind.md): layout helpers emit Tailwind / rg-* classes with Bootstrap's sizes.
 */
@OptIn(ComposeWebExperimentalTestsApi::class)
class LayoutTest {

    /**
     * Mirrors `@source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}")` in app.css. Customizer builds these class
     * names at runtime, so Tailwind only generates them because of that safelist: keep the two in sync.
     */
    private val safelisted = Regex("^(m|p)(t|b|s|e|x|y)?-(0|1|2|4|6|12|auto)$")

    private fun classesOf(id: String) = (document.getElementById(id) as HTMLElement).className.split(" ").toSet()

    @Test
    fun everySpacingCombinationIsSafelisted() {
        val sides = listOf<RgSide?>(null) + RgSide.entries
        val sizes = listOf<RgSz?>(null) + RgSz.entries
        for (space in RgSpace.entries) for (side in sides) for (size in sizes) {
            val customizer = Customizer().apply { set(space = space, side = side, size = size) }
            customizer.getClasses().forEach { cls ->
                if (size != null) {
                    assertTrue(safelisted.matches(cls), "$space/$side/$size -> '$cls' is not in the app.css safelist")
                }
            }
        }
    }

    @Test
    fun spacingKeepsBootstrapSizes() = runTest {
        composition {
            RgDiv(id = "a", customizer = { set(space = RgSpace.m, side = RgSide.b, size = RgSz.s3) })
            RgDiv(id = "b", customizer = { set(space = RgSpace.m, side = RgSide.y, size = RgSz.s4) })
            RgDiv(id = "c", customizer = { set(space = RgSpace.p, size = RgSz.s5) })
            RgDiv(id = "d", customizer = {
                set(space = RgSpace.m, size = RgSz.s2)
                addCustom("grow")
                addCustom("flex")
                addCustom("flex-col")
            })
        }
        assertEquals(setOf("mb-4"), classesOf("a"), "Bootstrap mb-3 (1rem) is Tailwind mb-4")
        assertEquals(setOf("my-6"), classesOf("b"), "Bootstrap my-4 (1.5rem) is Tailwind my-6")
        assertEquals(setOf("p-12"), classesOf("c"), "Bootstrap p-5 (3rem) is Tailwind p-12")
        assertEquals(setOf("m-2", "grow", "flex", "flex-col"), classesOf("d"))
    }

    @Test
    fun gridContainerAndPositionClasses() = runTest {
        composition {
            RgGrid(RgContainerType.container_fluid) { }
            RgGrid(RgContainerType.container_md, RgPosition.fixed_top) { }
        }
        val grids = root.children
        assertEquals(
            setOf("rg-container-fluid", "min-h-screen", "flex", "flex-col"),
            (grids.item(0) as HTMLElement).className.split(" ").toSet(),
        )
        assertEquals(
            setOf("rg-container-md", "rg-fixed-top", "min-h-screen", "flex", "flex-col"),
            (grids.item(1) as HTMLElement).className.split(" ").toSet(),
        )
        // classList.add() rejects names containing spaces, so each entry must be exactly one class
        (RgContainerType.entries.map { it.cls } + RgPosition.entries.map { it.cls }).forEach {
            assertTrue(Regex("^rg-[a-z-]+$").matches(it), "'$it' must be a single rg-* class")
        }
    }

    @Test
    fun sortableUsesListGroupClasses() = runTest {
        composition {
            RgSortable(listOf("Harbor Series", "Point Series"), { it }) { }
        }
        val ul = root.querySelector("ul.rg-list-group")!!
        assertEquals(2, ul.querySelectorAll("li.rg-list-group-item").length)
        assertNull(root.querySelector(".list-group, .list-group-item"), "no Bootstrap classes left")
    }

    @Test
    fun successBannerUsesRgAlert() = runTest {
        composition { Alerts() }
        alertsViewModel.showAlert("Tailwind Test added!")
        var banner: HTMLElement? = null
        var tries = 0
        while (banner == null && tries++ < 75) {   // showAlert updates state on a coroutine
            delay(20)
            banner = root.querySelector(".rg-alert-success") as? HTMLElement
        }
        val b = banner!!
        assertEquals("alert", b.getAttribute("role"))
        assertEquals("Tailwind Test added!", b.textContent)
        assertEquals("", b.style.marginTop, "position comes from .rg-alert-success, not an inline style")
        assertNull(root.querySelector(".alert, .alert-success, .fixed-top, .bi"), "no Bootstrap classes left")
    }
}

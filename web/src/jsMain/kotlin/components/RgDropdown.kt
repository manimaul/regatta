package components

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.Series
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.KeyboardEvent
import styles.AppStyle
import kotlin.math.max
import kotlin.math.min


fun itemNameContainsFilter(itemName: String, filter: String) :Boolean{
   return filter.trim().takeIf { it.isNotBlank() }?.split(' ')?.let {list ->
       list.any { itemName.contains(it, true) }
   } ?: true
}

/**
 * Holds a dropdown's root element so outside clicks can be told apart from clicks inside it.
 */
private class DropdownRoot {
    var element: Element? = null
}

/**
 * While [open], closes the dropdown on a pointer press outside [root] or on Escape. Escape is consumed so that a
 * dropdown inside an [RgModalBody] closes only itself, not the dialog.
 */
@Composable
private fun DismissOnOutsideOrEscape(open: Boolean, root: DropdownRoot, dismiss: () -> Unit) {
    if (!open) return
    val currentDismiss by rememberUpdatedState(dismiss)
    DisposableEffect(root) {
        val pointer = EventListener { event ->
            val target = event.target as? Node
            if (target != null && root.element?.contains(target) == false) {
                currentDismiss()
            }
        }
        val key = EventListener { event ->
            if ((event as? KeyboardEvent)?.key == "Escape") {
                event.preventDefault()
                event.stopPropagation()
                currentDismiss()
            }
        }
        // capture phase: runs before anything inside the page can stop the event
        document.addEventListener("pointerdown", pointer, true)
        document.addEventListener("keydown", key, true)
        onDispose {
            document.removeEventListener("pointerdown", pointer, true)
            document.removeEventListener("keydown", key, true)
        }
    }
}

/** Below this much room (px) the menu opens upward when there is more space above the toggle. */
private const val MIN_MENU_HEIGHT = 160.0

/** Gap (px) kept between the menu and the viewport edge. */
private const val VIEWPORT_GAP = 16.0

/** Gap (px) between the toggle and the menu. */
private const val TOGGLE_GAP = 2.0

/**
 * Places the fixed-position [menu] against its toggle button (the previous sibling) in viewport coordinates and caps
 * it to the room left on screen, so a long list scrolls inside the menu rather than the page and nothing between the
 * menu and the viewport (an overflow-x: auto table wrapper, say) can clip it. Opens upward when the toggle sits near the
 * bottom of the screen with more room above.
 */
private fun placeMenu(menu: HTMLElement) {
    val toggle = menu.previousElementSibling?.getBoundingClientRect() ?: return
    val viewportHeight = window.innerHeight.toDouble()
    val viewportWidth = window.innerWidth.toDouble()
    val below = viewportHeight - toggle.bottom - VIEWPORT_GAP
    val above = toggle.top - VIEWPORT_GAP
    val openAbove = below < MIN_MENU_HEIGHT && above > below
    menu.style.maxHeight = "${max(if (openAbove) above else below, MIN_MENU_HEIGHT)}px"
    if (openAbove) {
        menu.style.top = "auto"
        menu.style.bottom = "${viewportHeight - toggle.top + TOGGLE_GAP}px"
    } else {
        menu.style.bottom = "auto"
        menu.style.top = "${toggle.bottom + TOGGLE_GAP}px"
    }
    // keep the menu's right edge on screen; it is wider than the toggle more often than not
    val width = menu.getBoundingClientRect().width
    menu.style.left = "${max(VIEWPORT_GAP, min(toggle.left, viewportWidth - width - VIEWPORT_GAP))}px"
}

/**
 * The open menu: an optional non-scrolling [filter] header above the scrolling [items] pane.
 */
@Composable
private fun DropdownMenu(filter: ContentBuilder<HTMLDivElement>? = null, items: ContentBuilder<HTMLDivElement>) {
    Div(attrs = {
        classes("rg-dropdown-menu")
        ref { menu ->
            placeMenu(menu)
            // a fixed menu doesn't follow its toggle when the page (or the modal's dialog) scrolls or the window resizes
            val reposition = EventListener { event ->
                if ((event.target as? Node)?.let { menu.contains(it) } != true) placeMenu(menu)
            }
            window.addEventListener("scroll", reposition, true)
            window.addEventListener("resize", reposition)
            onDispose {
                window.removeEventListener("scroll", reposition, true)
                window.removeEventListener("resize", reposition)
            }
        }
    }) {
        filter?.let { Div(attrs = { classes("rg-dropdown-filter") }, content = it) }
        Div(attrs = { classes("rg-dropdown-items") }, content = items)
    }
}

@Composable
fun <T> RgDropdownNone(
    items: List<T>,
    selectedItem: T?,
    name: (T) -> String,
    showFilter: Boolean = true,
    shorName: ((T) -> String)? = null,
    handler: (T?) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var toggle by remember { mutableStateOf(false) }
    val root = remember { DropdownRoot() }
    val selected = selectedItem?.let(name) ?: "None"
    DismissOnOutsideOrEscape(toggle, root) {
        toggle = false
        filter = ""
    }
    Div(attrs = {
        classes("rg-dropdown")
        ref {
            root.element = it
            onDispose { root.element = null }
        }
    }) {
        Button(attrs = {
            classes(*RgButtonStyle.Primary.classes, "rg-dropdown-toggle")
            attr("aria-expanded", toggle.toString())
            onClick { toggle = !toggle }
        }) { Text(selectedItem?.let { shorName?.invoke(it) ?: name(it)} ?: "None") }

        if (toggle) DropdownMenu(
            filter = if (showFilter) {
                {
                    // Bootstrap's my-2 (.5rem) in Tailwind's scale; the wrapper supplies the horizontal spacing
                    RgInput("Type to filter", filter, true, customClasses = listOf("w-auto", "my-2")) {
                        filter = it
                    }
                    Hr(attrs = { classes("rg-dropdown-divider") })
                }
            } else null,
        ) {
            Button(attrs = {
                classes("rg-dropdown-item")
                onClick {
                    toggle = false
                    filter = ""
                    handler(null)
                }
            }) {
                Text("None")
            }
            items.forEach { item ->
                val itemName = name(item)
                if (itemNameContainsFilter(itemName, filter)) {
                    Button(attrs = {
                        classes("rg-dropdown-item")
                        onClick {
                            toggle = false
                            filter = ""
                            handler(item)
                        }
                    }) {
                        if (selected == itemName) {
                            B { Text(itemName) }
                        } else {
                            Text(itemName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> RgDropdown(
    items: List<T>,
    selectedItem: T,
    name: (T) -> String,
    handler: (T) -> Unit
) {
    RgIdDropdown(
        items = items,
        selectedItem = selectedItem,
        name = name,
        handler = handler
    )
}

@Composable
fun <T> RgIdDropdown(
    id: String? = null,
    items: List<T>,
    selectedItem: T,
    name: (T) -> String,
    handler: (T) -> Unit
) {

    var toggle by remember { mutableStateOf(false) }
    val root = remember { DropdownRoot() }
    val selected = selectedItem?.let(name) ?: "None"
    DismissOnOutsideOrEscape(toggle, root) { toggle = false }
    Div(attrs = {
        classes("rg-dropdown")
        id?.let { id(it) }
        ref {
            root.element = it
            onDispose { root.element = null }
        }
    }) {
        Button(attrs = {
            classes(*RgButtonStyle.Primary.classes, "rg-dropdown-toggle")
            attr("aria-expanded", toggle.toString())
            if (items.size <= 1) {
               disabled()
            }
            onClick { toggle = !toggle }
        }) { Text(selected) }

        if (toggle) DropdownMenu {
            items.forEach { item ->
                val itemName = name(item)
                Button(attrs = {
                    classes("rg-dropdown-item")
                    onClick {
                        toggle = false
                        handler(item)
                    }
                }) {
                    if (selected == itemName) {
                        B { Text(itemName) }
                    } else {
                        Text(itemName)
                    }
                }
            }
        }
    }
}

@Composable
fun RgSeriesDropdown(
    seriesList: List<Series>,
    series: Series?,
    handler: (Series) -> Unit,
) {
    RgDropdownNone(seriesList, series, { it.name }, true,{ it.name }) { it?.let(handler) }
}

@Composable
fun RgClassDropdown(
    items: List<RaceClassBrackets>,
    current: RaceClass?,
    handler: (RaceClassBrackets?) -> Unit,
) {
    val cb = items.firstOrNull { it.raceClass.id == current?.id }
    RgDropdownNone(items, cb, { it.raceClass.name }) { it?.let(handler) }
}

@Composable
fun RgSkipperDropdown(
    people: List<Person>,
    person: Person?,
    handler: (Person?) -> Unit
) {
    Label(attrs = {
        classes(AppStyle.marginEnd)
    }) { B { Text("Skipper") } }
    RgDropdownNone(people.sortedBy { it.fullName() }, person, { it.fullName() }) {
        handler(it)
    }
}

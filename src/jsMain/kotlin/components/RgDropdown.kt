package components

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.Series
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.dom.*


fun itemNameContainsFilter(itemName: String, filter: String) :Boolean{
   return filter.trim().takeIf { it.isNotBlank() }?.split(' ')?.let {list ->
       list.any { itemName.contains(it, true) }
   } ?: true
}

@Composable
fun <T> RgDropdownNone(
    items: List<T>,
    selectedItem: T?,
    name: (T) -> String,
    handler: (T?) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var toggle by remember { mutableStateOf(false) }
    val selected = selectedItem?.let(name) ?: "None"
    Div {
        Button(attrs = {
            classes("btn", "btn-primary", "dropdown-toggle")
            onClick { toggle = !toggle }
        }) { Text(selectedItem?.let(name) ?: "None") }

        if (toggle) Div(attrs = {
            classes("dropdown-menu", "show")
            style {
                position(Position.Absolute)
                property("z-index", "1000")
            }
        }) {
            RgInput("Type to filter", filter, true, customClasses = listOf("w-auto", "mx-3", "my-2")) {
                filter = it
            }
            Hr { }
            Button(attrs = {
                classes("dropdown-item")
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
                        classes("dropdown-item")
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

    var toggle by remember { mutableStateOf(false) }
    val selected = selectedItem?.let(name) ?: "None"
    Div {
        Button(attrs = {
            classes("btn", "btn-primary", "dropdown-toggle")
            onClick { toggle = !toggle }
        }) { Text(selected) }

        if (toggle) Div(attrs = {
            classes("dropdown-menu", "show")
            style {
                position(Position.Absolute)
                property("z-index", "1000")
            }
        }) {
            items.forEach { item ->
                val itemName = name(item)
                Button(attrs = {
                    classes("dropdown-item")
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
    RgDropdownNone(seriesList, series, { it.name }) { it?.let(handler) }
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
    RgDropdownNone(people.sortedBy { it.fullName() }, person, { it.fullName() }) {
        handler(it)
    }
}

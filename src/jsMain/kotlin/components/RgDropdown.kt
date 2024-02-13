package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.selects.select
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*

private var num = 0

@Composable
fun <T> RgDropdownNone(
    items: List<T>,
    selectedItem: T?,
    name: (T) -> String,
    handler: (T?) -> Unit
) {
    val id = remember { "${++num}_drop" }
    Select(attrs = {
        id(id)
        classes("form-select")
        onChange { change ->
            change.value?.toIntOrNull()?.let { i ->
                if (i >= 0) {
                    handler(items[i])
                } else {
                    handler(null)
                }
            }
        }
    }) {
        Option("-1", attrs = {
            if (selectedItem == null) {
                selected()
            }
        }) {
            Text("None")
        }

        val si = items.indexOf(selectedItem)
        items.forEachIndexed { i, each ->
            Option(i.toString(), attrs = {
                if (i == si) {
                    selected()
                }
            }) {
                Text(name(each))
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
    Div(attrs = { classes("dropdown") }) {
        Button(attrs = {
            classes("btn", "btn-primary", "dropdown-toggle")
            attr("data-bs-toggle", "dropdown")
        }) { Text(name(selectedItem)) }
        Ul(attrs = { classes("dropdown-menu") }) {
            items.forEach { item ->
                Li {
                    Button(attrs = {
                        classes("dropdown-item")
                        onClick {
                            handler(item)
                        }
                    }) {
                        Text(name(item))
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
    val id = remember { "${++num}_drop" }
    Select(attrs = {
        id(id)
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                seriesList.firstOrNull {
                    it.id == id
                }?.let { handler(it) }
            }
        }
    }) {
        Option("-1", attrs = {
            if (series == null) {
                selected()
            }
        }) {
            Text("None")
        }
        seriesList.forEach { s ->
            Option(s.id.toString(), attrs = {
                if (s.id == series?.id) {
                    selected()
                }
            }) {
                Text(s.name)
            }
        }
    }
}

@Composable
fun RgClassDropdown(
    items: List<RaceClassBrackets>,
    current: RaceClass?,
    handler: (RaceClassBrackets?) -> Unit,
) {
    val id = remember { "${++num}_drop" }
    Select(attrs = {
        id(id)
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                handler(
                    items.firstOrNull {
                        it.raceClass.id == id
                    }
                )
            }
        }
    }) {
        Option("-1", attrs = {
            if (current == null) {
                selected()
            }
        }) {
            Text("None")
        }
        items.forEach { cat ->
            val rc = cat.raceClass
            Option(rc.id.toString(), attrs = {
                if (rc.id == current?.id) {
                    selected()
                }
            }) {
                Text(rc.name)
            }
        }
    }
}

@Composable
fun RgSkipperDropdown(
    people: List<Person>,
    person: Person?,
    handler: (Person?) -> Unit
) {
    val id = remember { "${++num}_drop" }
    Select(attrs = {
        id(id)
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                handler(people.firstOrNull {
                    it.id == id
                })
            }
        }
    }) {
        OptGroup("Club Members")
        Option("-1", attrs = {
            if (person == null) {
                selected()
            }
        }) {
            Text("None")
        }
        people.filter { it.clubMember }.forEach {
            Option(it.id.toString(), attrs = {
                if (it.id == person?.id) {
                    selected()
                }
            }) {
                Text("${it.first} ${it.last}")
            }
        }
        OptGroup("Non Members")
        people.filter { !it.clubMember }.forEach {
            Option(it.id.toString(), attrs = {
                if (it.id == person?.id) {
                    selected()
                }
            }) {
                Text("${it.first} ${it.last}")
            }
        }
    }
}

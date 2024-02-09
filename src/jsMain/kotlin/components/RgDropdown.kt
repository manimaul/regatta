package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.Series
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.OptGroup
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text


@Composable
fun RgClassDropDown(
    categories: List<RaceClass>,
    current: RaceClass?,
    handler: (RaceClass) -> Unit,
) {
    Select(attrs = {
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                categories.firstOrNull {
                    it.id == id
                }?.let { handler(it) }
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

        categories.forEach { cat ->
            Option(cat.id.toString(), attrs = {
                if (cat.id == current?.id) {
                    selected()
                }
            }) {
                Text(cat.name)
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
    Select(attrs = {
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
    current: RaceClassBrackets?,
    handler: (RaceClassBrackets?) -> Unit,
) {
    Select(attrs = {
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
                if (rc.id == current?.raceClass?.id) {
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
    Select(attrs = {
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

package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCat
import com.mxmariner.regatta.data.RaceClassCategory
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.OptGroup
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text


@Composable
fun RgClassCatDropDown(
    categories: List<RaceClassCat>,
    current: RaceClassCat?,
    handler: (RaceClassCat) -> Unit,
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
fun RgClassDropdown(
    categories: List<RaceClassCategory>,
    currentClass: RaceClass?,
    handler: (RaceClass) -> Unit,
) {
    val classList = remember { categories.mapNotNull { it.children } }.flatten()
    Select(attrs = {
        classes("form-select")
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                classList.firstOrNull {
                    it.id == id
                }?.let { handler(it) }
            }
        }
    }) {
        Option("-1", attrs = {
            if (currentClass == null) {
                selected()
            }
        }) {
            Text("None")
        }
        categories.forEach { cat ->
            OptGroup(cat.name)
            cat.children?.forEach { rc ->
                Option(rc.id.toString(), attrs = {
                    if (rc.id == currentClass?.id) {
                        selected()
                    }
                }) {
                    Text("${rc.name} ${rc.description}")
                }
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

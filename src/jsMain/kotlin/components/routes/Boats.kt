package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.dom.Table
import utils.*
import viewmodel.BoatViewModel

@Composable
fun Boats(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    flowState.editBoat?.let {
        EditBoat(
            boat = it,
            people = flowState.response.value?.people ?: emptyList(),
            categories = flowState.response.value?.raceClass ?: emptyList(),
            viewModel = viewModel
        )
    } ?: when (val state = flowState.response) {
        is Complete -> BoatList(state.value.boats, state.value.people, state.value.raceClass, viewModel)
        is Error -> {
            Text("error")
            Spinner()
        }

        is Loading -> Spinner()
        Uninitialized -> Unit
    }
}

@Composable
fun EditBoat(
    boat: Boat,
    people: List<Person>,
    categories: List<RaceClassCategory>,
    viewModel: BoatViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var newBoat by remember { mutableStateOf(boat) }

    if (confirmDelete) {
        val msg = boat.skipper?.let {
            "Delete ${boat.name} owned by ${it.first} ${it.last}?"
        } ?: "Delete ${boat.name}?"
        Confirm(msg) { delete ->
            if (delete) {
                viewModel.deleteBoat(boat)
            } else {
                confirmDelete = false
            }
        }
    } else {
        Form {
            Fieldset {
                Legend { Text("Edit id:${boat.id} ${boat.name}") }
                P {
                    Input(InputType.Text) {
                        id("name")
                        value(newBoat.name)
                        onInput { newBoat = newBoat.copy(name = it.value) }
                    }
                    Label("name") { Text("Name") }
                }
                P {
                    Input(InputType.Text) {
                        id("sail")
                        value(newBoat.sailNumber)
                        onInput { newBoat = newBoat.copy(sailNumber = it.value) }
                    }
                    Label("sail") { Text("Sail Number") }
                }
                P {
                    Input(InputType.Text) {
                        id("type")
                        value(newBoat.boatType)
                        onInput { newBoat = newBoat.copy(boatType = it.value) }
                    }
                    Label("type") { Text("Boat Type") }
                }
                P {
                    Input(type = InputType.Text) {
                        id("rating")
                        onInput {
                            newBoat = newBoat.copy(phrfRating = it.value.digits(3).toIntOrNull())
                        }
                        value(newBoat.phrfRating?.toString() ?: "")
                    }
                    Label("rating") { Text("PHRF Rating") }
                }
                P {
                    ClassDropdown(categories, newBoat.raceClass) {
                        newBoat = newBoat.copy(raceClass = it)
                    }
                    Text("Class")
                }
                P {
                    SkipperDropdown(people, newBoat.skipper) {
                        newBoat = newBoat.copy(skipper = it)
                    }
                    Text("Skipper")
                }
            }
        }
        Br()
        RgButton("Cancel", RgButtonStyle.PrimaryOutline) {
            viewModel.setEditBoat(null)
        }
        RgButton("Save", RgButtonStyle.Primary) {
            viewModel.upsertBoat(newBoat)
        }
        RgButton("Delete", RgButtonStyle.Danger) {
            confirmDelete = true
        }
    }
}

@Composable
fun BoatList(
    boats: List<Boat>,
    people: List<Person>,
    categories: List<RaceClassCategory>,
    boatViewModel: BoatViewModel,
) {
    Div {
        H1 { Text("Boats") }
        RgTable {
            RgThead {
                RgTr {
                    Th { Text("Boat Name") }
                    Th { Text("Class") }
                    Th { Text("Skipper") }
                    Th { Text("Sail Number") }
                    Th { Text("Type") }
                    Th { Text("PHRF Rating") }
                    Th { Text("Action") }
                }
            }
            RgTbody {
                AddBoat(categories, people, boatViewModel)
                boats.forEach { boat ->
                    RgTr {
                        RgTd { Text(boat.name) }
                        RgTd { Text(boat.raceClass?.name ?: "None") }
                        RgTd {
                            boat.skipper?.let {
                                Text(
                                    "${boat.skipper.first} ${boat.skipper.last}"
                                )
                            }
                        }
                        RgTd { Text(boat.sailNumber) }
                        RgTd { Text(boat.boatType) }
                        RgTd { Text(boat.phrfRating?.let { "$it" } ?: "None") }
                        RgTd {
                            RgButton("Edit", RgButtonStyle.PrimaryOutline) {
                                boatViewModel.setEditBoat(boat)
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun AddBoat(
    categories: List<RaceClassCategory>,
    people: List<Person>,
    boatViewModel: BoatViewModel,
) {
    var addBoat by remember { mutableStateOf(Boat()) }
    var phrfRating by remember { mutableStateOf("") }
    RgTr {
        RgTd {
            Input(type = InputType.Text) {
                placeholder("Name")
                classes("form-control")
                onInput { addBoat = addBoat.copy(name = it.value) }
                value(addBoat.name)
            }
        }
        RgTd {
            ClassDropdown(categories, addBoat.raceClass) {
                addBoat = addBoat.copy(raceClass = it)
            }
        }
        RgTd {
            SkipperDropdown(people, addBoat.skipper) {
                addBoat = addBoat.copy(skipper = it)
            }
        }
        RgTd {
            Input(type = InputType.Text) {
                placeholder("Sail number")
                classes("form-control")
                onInput { addBoat = addBoat.copy(sailNumber = it.value) }
                value(addBoat.sailNumber)
            }
        }
        RgTd {
            Input(type = InputType.Text) {
                placeholder("Type")
                classes("form-control")
                onInput { addBoat = addBoat.copy(boatType = it.value) }
                value(addBoat.boatType)
            }
        }
        RgTd {
            Input(type = InputType.Text) {
                placeholder("Rating")
                classes("form-control")
                onInput {
                    phrfRating = it.value.digits(3)
                }
                value(phrfRating)
            }
        }
        RgTd {
            RgButton("Add", RgButtonStyle.Primary) {
                boatViewModel.addBoat(addBoat.copy(phrfRating = phrfRating.toIntOrNull()))
                addBoat = Boat()
                phrfRating = ""
            }
        }
    }
}


@Composable
fun ClassDropdown(
    categories: List<RaceClassCategory>,
    currentClass: RaceClass?,
    handler: (RaceClass) -> Unit,
) {
    val classList = remember { categories.mapNotNull { it.children } }.flatten()
    Select(attrs = {
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
fun SkipperDropdown(
    people: List<Person>,
    person: Person?,
    handler: (Person) -> Unit
) {
    Select(attrs = {
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                people.firstOrNull {
                    it.id == id
                }?.let { handler(it) }
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

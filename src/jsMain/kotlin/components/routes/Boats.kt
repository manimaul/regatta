package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import components.Confirm
import components.RgButton
import components.RgButtonStyle
import components.Spinner
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.*
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.BoatViewModel

@Composable
fun Boats(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    flowState.editBoat?.let {
        EditBoat(it, flowState.response.value?.people ?: emptyList(),  viewModel)
    } ?: when (val state = flowState.response) {
        is Complete -> BoatList(state.value.boats, state.value.people, viewModel)
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
        Form(attrs = {
            //todo: disable submit
        }) {
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
                    Input(type = InputType.Number) {
                        id("rating")
                        onInput { newBoat = newBoat.copy(phrfRating = it.value?.toInt()) }
                        value(newBoat.phrfRating?.toString() ?: "")
                    }
                    Label("rating") { Text("PHRF Rating") }
                }
                P {
                    Dropdown(people, newBoat.skipper) {
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
        RgButton("Delete", RgButtonStyle.Error) {
            confirmDelete = true
        }
    }
}

@Composable
fun BoatList(
    boats: List<Boat>,
    people: List<Person>,
    boatViewModel: BoatViewModel,
) {
    var addBoat by remember { mutableStateOf(Boat()) }
    Div {
        Article {
            Table(attrs = { classes("striped") }) {
                Caption {
                    Text("${Clock.System.now().toJSDate().getFullYear()}")
                }
                Tr {
                    Th { Text("Name") }
                    Th { Text("Sail Number") }
                    Th { Text("Type") }
                    Th { Text("PHRF Rating") }
                    Th { Text("Skipper") }
                    Th { Text("Action") }
                }
                boats.forEach { boat ->
                    Tr {
                        Td { Text(boat.name) }
                        Td { Text(boat.sailNumber) }
                        Td { Text(boat.boatType) }
                        Td { Text(boat.phrfRating?.let { "$it" } ?: "-") }
                        Td {
                            boat.skipper?.let {
                                Text(
                                    "${boat.skipper.first} ${boat.skipper.last}"
                                )
                            }
                        }
                        Td {
                            RgButton("Edit", RgButtonStyle.PrimaryOutline) {
                                boatViewModel.setEditBoat(boat)
                            }
                        }
                    }
                }
                Tr {
                    Td {
                        Input(type = InputType.Text) {
                            placeholder("Name")
                            onInput { addBoat = addBoat.copy(name = it.value) }
                            value(addBoat.name)
                        }
                    }
                    Td {
                        Input(type = InputType.Text) {
                            placeholder("Sail number")
                            onInput { addBoat = addBoat.copy(sailNumber = it.value) }
                            value(addBoat.sailNumber)
                        }
                    }
                    Td {
                        Input(type = InputType.Text) {
                            placeholder("Type")
                            onInput { addBoat = addBoat.copy(boatType = it.value) }
                            value(addBoat.boatType)
                        }
                    }
                    Td {
                        Input(type = InputType.Number) {
                            placeholder("Rating")
                            onInput { addBoat = addBoat.copy(phrfRating = it.value?.toInt()) }
                            value(addBoat.phrfRating?.toString() ?: "")
                        }
                    }
                    Td {
                        Dropdown(people, addBoat.skipper) {
                            addBoat = addBoat.copy(skipper = it)
                        }
                    }
                    Td {
                        RgButton("Add", RgButtonStyle.Primary) {
                            boatViewModel.addBoat(addBoat)
                            addBoat = Boat()
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Dropdown(
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

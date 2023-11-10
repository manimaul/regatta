package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import components.RgButton
import components.RgButtonStyle
import components.Spinner
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.BoatStateLoaded
import viewmodel.BoatStateLoading
import viewmodel.BoatViewModel

@Composable
fun Boats(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    when (val state = flowState) {
        BoatStateLoading -> Spinner(50f)
        is BoatStateLoaded -> BoatList(state.boats, state.people, viewModel)
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
                            RgButton("Delete", RgButtonStyle.Error) {
                                boatViewModel.setDeleteBoat(boat)
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
                        Dropdown(people) {
                            addBoat = addBoat.copy(skipper = it)
                        }
                    }
                    Td {
                        RgButton("Add", RgButtonStyle.Primary) {
                            boatViewModel.addBoat(addBoat)
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
    selected: (Person) -> Unit
) {
    Select(attrs = {
        onChange { change ->
            change.value?.toLongOrNull()?.let { id ->
                people.firstOrNull {
                    it.id == id
                }?.let { selected(it) }
            }
        }
    }) {
        OptGroup("Club Members")
        Option("-1") {
            Text("None")
        }
        people.filter { it.clubMember }.forEach {
            Option(it.id.toString()) {
                Text("${it.first} ${it.last}")
            }
        }
        OptGroup("Non Members")
        people.filter { !it.clubMember }.forEach {
            Option(it.id.toString()) {
                Text("${it.first} ${it.last}")
            }
        }
    }
}

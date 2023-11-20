package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import utils.*
import viewmodel.BoatViewModel

@Composable
fun Boats(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    when (val state = flowState.response) {
        is Complete -> BoatList(state.value.boats, state.value.people, state.value.raceClass, viewModel)
        is Error -> ErrorDisplay(state) {
            viewModel.reload()
        }
        is Loading -> RgSpinner()
        Uninitialized -> Unit
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
            RgClassDropdown(categories, addBoat.raceClass) {
                addBoat = addBoat.copy(raceClass = it)
            }
        }
        RgTd {
            RgSkipperDropdown(people, addBoat.skipper) {
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

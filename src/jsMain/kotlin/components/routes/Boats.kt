package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Person
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
        is Complete -> BoatList(state.value.boats, state.value.people, viewModel)
        is Error -> ErrorDisplay(state) {
            viewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}


@Composable
fun BoatList(
    boats: List<BoatSkipper>,
    people: List<Person>,
    boatViewModel: BoatViewModel,
) {
    Div {
        H1 { Text("Boats") }
        RgTable {
            RgThead {
                RgTr {
                    Th { Text("Boat Name") }
                    Th { Text("Skipper") }
                    Th { Text("Sail Number") }
                    Th { Text("Type") }
                    Th { Text("Rating") }
                    Th { Text("Action") }
                }
            }
            RgTbody {
                AddBoat(people, boatViewModel)
                boats.forEach { boat ->
                    RgTr {
                        RgTd { Text(boat.boat?.name ?: "") }
                        RgTd {
                            boat.skipper?.let {
                                Text(it.fullName())
                            }
                        }
                        RgTd { Text(boat.boat?.sailNumber ?: "") }
                        RgTd { Text(boat.boat?.boatType ?: "") }
                        RgTd { Text(boat.boat?.ratingLabel() ?: "None") }
                        RgTd {
                            RgButton("Edit", RgButtonStyle.PrimaryOutline) {
                                boatViewModel.setEditBoat(boat.boat)
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
    people: List<Person>,
    boatViewModel: BoatViewModel,
) {
    var addBoat by remember { mutableStateOf(Boat()) }
    var addSkipper by remember { mutableStateOf<Person?>(null) }
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
            RgSkipperDropdown(people, addSkipper) {
                addSkipper = it
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
                boatViewModel.addBoat(addBoat.copy(
                    phrfRating = phrfRating.toIntOrNull(),
                    skipperId = addSkipper?.id
                ))
                addBoat = Boat()
                addSkipper = null
                phrfRating = ""
            }
        }
    }
}

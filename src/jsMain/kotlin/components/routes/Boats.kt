package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.ratingDefault
import com.mxmariner.regatta.ratingLabel
import components.*
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
                        RgTd { Text(boat.boat?.boatType?: "") }
                        RgTd { Text(ratingLabel(boat.boat?.phrfRating, boat.boat?.windseeker, true)) }
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
    var boatType: RatingType by remember { mutableStateOf(RatingType.PHRF) }
    var addBoat by remember { mutableStateOf(Boat()) }
    var addSkipper by remember { mutableStateOf<Person?>(null) }
    var phrfRating by remember { mutableStateOf("${ratingDefault.toInt()}") }
    var wsRating by remember { mutableStateOf("${ratingDefault.toInt()}") }
    var wsFlying by remember { mutableStateOf(false) }
    RgTr {
        RgTd {
            RgInput(label = "Name", value = addBoat.name, placeHolder = true) {
                addBoat = addBoat.copy(name = it)
            }
        }
        RgTd {
            RgSkipperDropdown(people, addSkipper) {
                addSkipper = it
            }
        }
        RgTd {
            RgInput(label = "Sail number", value = addBoat.sailNumber, placeHolder = true) {
                addBoat = addBoat.copy(sailNumber = it)
            }
        }
        RgTd {
            RgInput(label = "Type", value = addBoat.boatType, placeHolder = true) {
                addBoat = addBoat.copy(boatType = it)
            }
        }
        RgTd {
            RatingSelections(
                boatType, phrfRating, wsRating, wsFlying,
                { boatType = it },
                { phrfRating = it },
                { wsRating = it },
                { wsFlying = it },
            )
        }
        RgTd {
            RgButton(
                "Add",
                RgButtonStyle.Primary,
                disabled = addBoat.name.isBlank() || (boatType == RatingType.PHRF && phrfRating.isBlank())
            ) {
                var pr: Int? = null
                var windseeker: Windseeker? = null
                when (boatType) {
                    RatingType.PHRF -> pr = phrfRating.toIntOrNull()
                    RatingType.Windseeker -> windseeker = Windseeker(
                        rating = wsRating.toIntOrNull() ?: ratingDefault.toInt(),
                        flyingSails = wsFlying
                    )
                }
                boatViewModel.addBoat(
                    addBoat.copy(
                        phrfRating = pr,
                        windseeker = windseeker,
                        skipperId = addSkipper?.id
                    )
                )
                addBoat = Boat()
                addSkipper = null
                phrfRating = "${ratingDefault.toInt()}"
                wsRating = "${ratingDefault.toInt()}"
                wsFlying = false
            }
        }
    }
}

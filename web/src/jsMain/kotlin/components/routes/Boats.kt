package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.ratingLabel
import components.*
import components.RgButtonStyle
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.AddEditBoatMode
import viewmodel.BoatPeopleComposite
import viewmodel.alertsViewModel
import viewmodel.boatViewModel

@Composable
fun Boats(
) {
    val flowState by boatViewModel.flow.collectAsState()
    when (val state = flowState.response) {
        is Complete -> {
            BoatList(flowState.addEditMode, state.value)
        }

        is Error -> ErrorDisplay(state) {
            boatViewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}


@Composable
fun BoatList(
    addEditMode: AddEditBoatMode,
    boatsAndPeople: BoatPeopleComposite,
) {
    Div {
        H1 { Text("Boats") }
        Div(attrs = {
            classes(AppStyle.marginBot)
        }) {
            AddEditBoatModalButton(
                style = RgButtonStyle.SuccessOutline,
                buttonLabel = AddEditBoatMode.Adding.label(),
                openAction = {
                    boatViewModel.setAddEditMode(AddEditBoatMode.Adding)
                }
            )
            AddEditBoatModal(
                people = boatsAndPeople.people,
                modalTitle = { addEditMode.label() },
            )
        }
        RgTable {
            RgThead {
                RgTr {
                    Th { Text("Boat Name") }
                    Th { Text("Skipper") }
                    Th { Text("Sail Number") }
                    Th { Text("Type") }
                    Th { Text("Rating") }
                    Th { Text("Number of races") }
                    Th { Text("Action") }
                }
            }
            RgTbody {
                boatsAndPeople.boats.forEach { boat ->
                    RgTr {
                        RgTd { Text(boat.boat?.name ?: "") }
                        RgTd {
                            boat.skipper?.let {
                                Text(it.fullName())
                            }
                        }
                        RgTd { Text(boat.boat?.sailNumber ?: "") }
                        RgTd { Text(boat.boat?.boatType ?: "") }
                        RgTd { Text(ratingLabel(boat.boat?.phrfRating, boat.boat?.windseeker, true)) }
                        RgTd { Text(boat.boat?.numberOfRaces?.toString() ?: "") }
                        RgTd {
                            AddEditBoatModalButton(
                                style = RgButtonStyle.PrimaryOutline,
                                buttonLabel = AddEditBoatMode.Editing.label(),
                                customClasses = listOf(AppStyle.marginAll),
                                openAction = {
                                    boatViewModel.setAddEditMode(AddEditBoatMode.Editing, boat)
                                }
                            )
                            if (boat.boat?.numberOfRaces == 0L) {
                                RgButton(
                                    label = "Delete Boat",
                                    style = RgButtonStyle.Danger,
                                    customClasses = listOf(AppStyle.marginAll)
                                ) {
                                    boat.boat?.let { deleteBoat ->
                                        alertsViewModel.confirm("Are you sure?", "Delete ${deleteBoat.name}?") {
                                            if (it) {
                                                boatViewModel.deleteBoat(deleteBoat)
                                                alertsViewModel.showAlert("${deleteBoat.name} deleted!")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

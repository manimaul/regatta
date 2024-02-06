package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.*

@Composable
fun BoatEdit(
    id: Long?,
    viewModel: EditBoatViewModel = remember { EditBoatViewModel(id ?: 0) }
) {
    val state by viewModel.flow.collectAsState()
    when (val data = state.data) {
        is Complete -> {
            when (state.operation) {
                Operation.Fetched -> EditBoat(data.value.boat, data.value.people, data.value.raceClass, viewModel)
                Operation.Updated -> RgOk("Updated!", data.value.boat.name) {
                    viewModel.routeVm.goBackOrHome()
                }

                Operation.Deleted -> RgOk("Deleted!", data.value.boat.name) {
                    viewModel.routeVm.goBackOrHome()
                }

                Operation.None -> Unit
            }
        }

        is Error -> P { Text(data.message) }
        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}

@Composable
fun EditBoat(
    boat: Boat,
    people: List<Person>,
    categories: List<RaceClassCategory>,
    viewModel: EditBoatViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var newBoat by remember { mutableStateOf(boat) }

    if (confirmDelete) {
        val msg = boat.skipper?.let {
            "Delete ${boat.name} owned by ${it.first} ${it.last}?"
        } ?: "Delete ${boat.name}?"
        RgConfirm(msg) { delete ->
            if (delete) {
                viewModel.deleteBoat(boat)
            } else {
                confirmDelete = false
            }
        }
    } else {
        H1 { Text("Edit") }
        RgForm {
            RgDiv(customizer = { set(RgSpace.m, RgSide.b, RgSz.s3) }) {
                Fieldset {
                    P {
                        RgInput("Name", newBoat.name) {
                            newBoat = newBoat.copy(name = it)
                        }
                    }
                    P {
                        RgInput("Sail Number", newBoat.sailNumber) {
                            newBoat = newBoat.copy(sailNumber = it)
                        }
                    }
                    P {
                        RgInput("Boat Type", newBoat.boatType) {
                            newBoat = newBoat.copy(boatType = it)
                        }
                    }
                    P {
                        RgInput("PHRF Rating", newBoat.phrfRating?.toString() ?: "") {
                            newBoat = newBoat.copy(phrfRating = it.digits(3).toIntOrNull())
                        }
                    }
                    P {
//                        B { Text("Class") }
//                        RgClassDropdown(categories, newBoat.raceClass) {
//                            newBoat = newBoat.copy(raceClass = it)
//                        }
                    }
                    P {
                        B { Text("Skipper") }
                        RgSkipperDropdown(people, newBoat.skipper) {
                            newBoat = newBoat.copy(skipper = it)
                            println("skipper selected ${it} ${newBoat.skipper}")
                        }
                    }
                }
                RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                    viewModel.cancelEdit()
                }
                RgButton("Save", RgButtonStyle.Primary) {
                    viewModel.upsertBoat(newBoat)
                }
                RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                    confirmDelete = true
                }
            }
        }
    }
}

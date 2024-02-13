package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.ratingDefault
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
                Operation.Fetched -> EditBoat(
                    data.value.boatSkipper.boat ?: Boat(),
                    data.value.boatSkipper.skipper,
                    data.value.people,
                    viewModel
                )

                Operation.Updated -> RgOk("Updated!", data.value.boatSkipper.boat?.name) {
                    viewModel.routeVm.goBackOrHome()
                }

                Operation.Deleted -> RgOk("Deleted!", data.value.boatSkipper.boat?.name) {
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
    skipper: Person?,
    people: List<Person>,
    viewModel: EditBoatViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var newBoat by remember { mutableStateOf(boat) }
    var newSkipper by remember { mutableStateOf(skipper) }

    var ratingType by remember { mutableStateOf(boat.ratingType()) }
    var phrfRating by remember { mutableStateOf(boat.phrfRating?.toString() ?: "") }
    var wsRating by remember { mutableStateOf(boat.windseeker?.rating?.toString() ?: "") }
    var wsFlying by remember { mutableStateOf(boat.windseeker?.flyingSails == true) }

    if (confirmDelete) {
        val msg = skipper?.let {
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
            RgTable {
                RgThead {
                    RgTr {
                        RgTh { Text("Boat Name") }
                        RgTh { Text("Skipper") }
                        RgTh { Text("Sail Number") }
                        RgTh { Text("Boat Type") }
                    }
                }
                RgTbody {
                    RgTr {
                        RgTd {
                            RgInput("Name", newBoat.name, true) {
                                newBoat = newBoat.copy(name = it)
                            }
                        }
                        RgTd {
                            RgSkipperDropdown(people, newSkipper) {
                                newBoat = newBoat.copy(skipperId = it?.id)
                                println("skipper selected ${it} ${newBoat.skipperId}")
                            }
                        }
                        RgTd {
                            RgInput("Sail Number", newBoat.sailNumber, true) {
                                newBoat = newBoat.copy(sailNumber = it)
                            }
                        }
                        RgTd {
                            RgInput("Boat Type", newBoat.boatType, true) {
                                newBoat = newBoat.copy(boatType = it)
                            }
                        }
                    }

                }
                Div(attrs = { classes(AppStyle.marginTop) }) { }
                RatingSelections(
                    ratingType, phrfRating, wsRating, wsFlying,
                    { ratingType = it },
                    { phrfRating = it },
                    { wsRating = it },
                    { wsFlying = it },
                )
                Div(attrs = { classes(AppStyle.marginVert) }) { }
            }
            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelEdit()
            }
            RgButton("Save", RgButtonStyle.Primary) {

                newBoat = when (ratingType) {
                    RatingType.PHRF -> newBoat.copy(windseeker = null, phrfRating = phrfRating.toIntOrNull())
                    RatingType.Windseeker -> newBoat.copy(
                        windseeker = Windseeker(
                            rating = wsRating.toIntOrNull() ?: ratingDefault.toInt(), flyingSails = wsFlying
                        ), phrfRating = null
                    )
                }
                viewModel.upsertBoat(newBoat)
            }
            RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                confirmDelete = true
            }
        }
    }
}

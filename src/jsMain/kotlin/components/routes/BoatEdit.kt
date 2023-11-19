package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.*

data class EditBoatComposite(
    val boat: Boat,
    val people: List<Person>,
    val raceClass: List<RaceClassCategory>,
)

data class EditBoatState(
    val data: Async<EditBoatComposite> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState


class EditBoatViewModel(
    val id: Long?,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<EditBoatState>(EditBoatState()) {
    fun upsertBoat(newBoat: Boat) {
        TODO("Not yet implemented")
    }

    fun deleteBoat(boat: Boat) {
        TODO("Not yet implemented")
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    init {
        launch {
            id?.let {
                setState {
                    EditBoatState(
                        data = combineAsync(
                            Api.getBoat(id),
                            Api.getAllPeople(),
                            Api.getAllCategories()
                        ) { boat, people, cat ->
                            EditBoatComposite(boat, people, cat)
                        }.mapErrorMessage { "error fetching boat id $id" },
                        operation = Operation.Fetched
                    )
                }
            }
        }
    }
}

@Composable
fun BoatEdit(
    id: Long?,
    viewModel: EditBoatViewModel = remember { EditBoatViewModel(id) }
) {
    val state by viewModel.flow.collectAsState()
    when (val data = state.data) {
        is Complete -> EditBoat(data.value.boat, data.value.people, data.value.raceClass, viewModel)
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
            Div(attrs = { classes("mb-3") }) {
                Fieldset {
                    P {
                        Input(InputType.Text) {
                            id("name")
                            classes("form-control")
                            value(newBoat.name)
                            onInput { newBoat = newBoat.copy(name = it.value) }
                        }
                        Label("name") { B { Text("Name") } }
                    }
                    P {
                        Input(InputType.Text) {
                            id("sail")
                            classes("form-control")
                            value(newBoat.sailNumber)
                            onInput { newBoat = newBoat.copy(sailNumber = it.value) }
                        }
                        Label("sail") { B { Text("Sail Number") } }
                    }
                    P {
                        Input(InputType.Text) {
                            id("type")
                            classes("form-control")
                            value(newBoat.boatType)
                            onInput { newBoat = newBoat.copy(boatType = it.value) }
                        }
                        Label("type") { B { Text("Boat Type") } }
                    }
                    P {
                        Input(type = InputType.Text) {
                            id("rating")
                            classes("form-control")
                            onInput {
                                newBoat = newBoat.copy(phrfRating = it.value.digits(3).toIntOrNull())
                            }
                            value(newBoat.phrfRating?.toString() ?: "")
                        }
                        Label("rating") { B { Text("PHRF Rating") } }
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

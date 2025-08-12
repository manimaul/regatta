package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RatingType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import viewmodel.alertsViewModel
import viewmodel.boatViewModel

@Composable
fun AddEditBoatModalButton(
    style: RgButtonStyle,
    buttonLabel: String,
    customClasses: List<String> = emptyList(),
    openAction: (() -> Unit)? = null,
) {
    RgModalButton(
        id = "add-edit-boat",
        style = style,
        customClasses = customClasses,
        buttonLabel = { buttonLabel },
        openAction = openAction,
    )
}

@Composable
fun AddEditBoatModal(
    people: List<Person>,
    modalTitle: () -> String,
) {
    val state by boatViewModel.flow.collectAsState()
    RgModalBody(
        id = "add-edit-boat",
        modalTitle = modalTitle,
        content = {
            RgForm {
                Fieldset {
                    P {
                        RgInput(label = "Boat Name", value = state.addEditState.addBoat.name, placeHolder = true) {
                            println("setting boat name $it")
                            boatViewModel.setEditBoatName(it)
                        }
                    }
                    P {
                        RgSkipperDropdown(people, state.addEditState.addSkipper) {
                            println("setting skipper $it")
                            boatViewModel.setEditBoatSkipper(it)
                        }
                    }
                    P {
                        RgInput(
                            label = "Sail Number",
                            value = state.addEditState.addBoat.sailNumber,
                            placeHolder = true
                        ) {
                            boatViewModel.setEditBoatSailNumber(it)
                        }
                    }
                    P {
                        RgInput(label = "Boat Type", value = state.addEditState.addBoat.boatType, placeHolder = true) {
                            boatViewModel.setSetEditBoatType(it)
                        }
                    }
                    P {
                        RatingSelections(
                            boatType = state.addEditState.addBoat.ratingType(),
                            phrfRating = state.addEditState.addBoat.phrfRating?.toString() ?: "",
                            wsRating = state.addEditState.addBoat.windseeker?.rating?.toString() ?: "",
                            wsFlying = state.addEditState.addBoat.windseeker?.flyingSails == true,
                            typeChagne = { boatViewModel.setEditBoatRatingType(it) },
                            phrfChange = { boatViewModel.setEditBoatPhrfRating(it) },
                            wsRatingChange = { boatViewModel.setEditBoatWsRating(it) },
                            wsFlyingChange = { boatViewModel.setEditBoatWsFlying(it) },
                        )
                    }
                }
            }
        },
        footer = {
            Div(attrs = { classes("flex-fill", "d-flex", "justify-content-between") }) {
                Button(attrs = {
                    classes(*RgButtonStyle.PrimaryOutline.classes)
                    if (state.addEditState.addBoat.id != 0L) {
                        disabled()
                    }
                    onClick {
                        boatViewModel.clearEditBoat()
                    }
                }) {
                    Text("Clear")
                }

                Button(attrs = {
                    classes(*RgButtonStyle.Success.classes)

                    if (state.addEditState.addBoat.name.isBlank()
                        || (state.addEditState.addBoat.ratingType() == RatingType.PHRF && state.addEditState.addBoat.phrfRating == null)
                    ) {
                        disabled()
                    }
                    attr("data-bs-dismiss", "modal")
                    onClick {
                        boatViewModel.saveEditedBoat()
                        if (state.addEditState.addBoat.id != 0L) {
                            alertsViewModel.showAlert("${state.addEditState.addBoat.name} updated!")
                        } else {
                            alertsViewModel.showAlert("${state.addEditState.addBoat.name} added!")
                        }
                    }
                }) {
                    Text("Save")
                }
            }
        }
    )
}

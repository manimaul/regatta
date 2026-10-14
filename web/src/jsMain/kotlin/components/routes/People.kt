package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RatingType
import components.*
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.AddEditBoatMode
import viewmodel.BoatPeopleComposite
import viewmodel.BoatViewModel
import viewmodel.alertsViewModel
import viewmodel.boatViewModel

@Composable
fun People(
    viewModel: BoatViewModel = boatViewModel,
) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        when (val state = flowState.response) {
            is Complete -> PeopleLoaded(state.value, viewModel)
            is Error -> ErrorDisplay(state) {
                viewModel.reload()
            }

            is Loading -> {
                RgSpinner()
                PeopleLoaded(state.value, viewModel)
            }

            Uninitialized -> RgSpinner()
        }
    }
}

@Composable
fun PeopleLoaded(
    composite: BoatPeopleComposite?,
    viewModel: BoatViewModel,
) {
    Div {
        H1 { Text("Skippers") }
        Div(attrs = {
            classes(AppStyle.marginBot)
        }){
            RgModalButton(
                id = "add-edit-skipper",
                style = RgButtonStyle.SuccessOutline,
                buttonLabel = { "Add Skipper" },
                openAction = {
                   viewModel.setEditPerson(Person())
                }
            )
            AddEditPerson()
        }
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("First") }
                    RgTh { Text("Last") }
                    RgTh { Text("Boat") }
                    RgTh { Text("Member") }
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
                composite?.people?.takeIf { it.isNotEmpty() }?.let { people ->
                    people.forEach { person ->
                        RgTr {
                            RgTd { Text(person.first) }
                            RgTd { Text(person.last) }
                            RgTd { Text(viewModel.findBoatName(person, composite)) }
                            RgTd { Text(if (person.clubMember) "Yes" else "No") }
                            RgTd {
                                RgModalButton(
                                    id = "add-edit-skipper",
                                    style = RgButtonStyle.PrimaryOutline,
                                    buttonLabel = { "Edit Skipper" },
                                    openAction = {
                                        viewModel.setEditPerson(person)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditPerson() {
    val state by boatViewModel.flow.collectAsState()
    RgModalBody(
        id = "add-edit-skipper",
        modalTitle = {
            if (state.showConfirmDeletePerson) {
                "Delete '${state.addEditPerson.first} ${state.addEditPerson.last}'?"
            } else if (state.addEditPerson.id == 0L) {
                "Add Skipper"
            }else {
                "Edit Skipper"
            }
        },
        content = {
            if (!state.showConfirmDeletePerson) {
                RgForm {
                    Fieldset {
                        P {
                            RgInput("First", state.addEditPerson.first, true) {
                                boatViewModel.setEditPersonFirst(it)
                            }
                        }
                        P {
                            RgInput("Last", state.addEditPerson.last, true) {
                                boatViewModel.setEditPersonLast(it)
                            }
                        }
                        P {
                            RgCheck(
                                "Club member",
                                state.addEditPerson.clubMember,
                                false,
                            ) {
                                boatViewModel.setEditPersonMember(it)
                            }
                        }
                    }

                }
            }
        },
        footer = {
            RgModalFooterRow {
                if (state.showConfirmDeletePerson) {
                    RgButton(
                        label = "No",
                        style = RgButtonStyle.PrimaryOutline,
                    ) {
                        boatViewModel.confirmDeletePerson(show = false)
                    }
                    RgButton(
                        label = "Yes",
                        style = RgButtonStyle.Danger,
                        closesModal = true,
                    ) {
                        boatViewModel.deletePerson()
                        alertsViewModel.showAlert("${state.addEditPerson.first} ${state.addEditPerson.last} deleted!")
                    }
                } else {
                    if (state.addEditPerson.id == 0L) {
                        RgButton(
                            label = "Clear",
                            style = RgButtonStyle.PrimaryOutline,
                        ) {
                            boatViewModel.setEditPerson(Person())
                        }
                    } else {
                        RgButton(
                            label = "Delete",
                            style = RgButtonStyle.Danger,
                        ) {
                            boatViewModel.confirmDeletePerson()
                        }

                    }

                    RgButton(
                        label = "Save",
                        style = RgButtonStyle.Success,
                        disabled = state.addEditPerson.first.isBlank() || (state.addEditPerson.last.isBlank()),
                        closesModal = true,
                    ) {
                        boatViewModel.upsertPerson()
                        alertsViewModel.showAlert("${state.addEditPerson.first} ${state.addEditPerson.last} updated!")
                    }
                }
            }
        }
    )
}
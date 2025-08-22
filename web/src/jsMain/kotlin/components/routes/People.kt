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
        Div {
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
        Br { }
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
            Div(attrs = { classes("flex-fill", "d-flex", "justify-content-between") }) {
                if (state.showConfirmDeletePerson) {
                    Button(attrs = {
                        classes(*RgButtonStyle.PrimaryOutline.classes)
                        onClick {
                            boatViewModel.confirmDeletePerson(show = false)
                        }
                    }) {
                        Text("No")
                    }
                    Button(attrs = {
                        classes(*RgButtonStyle.Danger.classes)
                        attr("data-bs-dismiss", "modal")
                        onClick {
                            boatViewModel.deletePerson()
                            alertsViewModel.showAlert("${state.addEditPerson.first} ${state.addEditPerson.last} deleted!")

                        }
                    }) {
                        Text("Yes")
                    }
                } else {
                    if (state.addEditPerson.id == 0L) {
                        Button(attrs = {
                            classes(*RgButtonStyle.PrimaryOutline.classes)
                            onClick {
                                boatViewModel.setEditPerson(Person())
                            }
                        }) {
                            Text("Clear")
                        }
                    } else {
                        Button(attrs = {
                            classes(*RgButtonStyle.Danger.classes)
                            onClick {
                                boatViewModel.confirmDeletePerson()
                            }
                        }) {
                            Text("Delete")
                        }

                    }

                    Button(attrs = {
                        classes(*RgButtonStyle.Success.classes)
                        if (state.addEditPerson.first.isBlank() || (state.addEditPerson.last.isBlank())) {
                            disabled()
                        }
                        attr("data-bs-dismiss", "modal")
                        onClick {
                            boatViewModel.upsertPerson()
                            alertsViewModel.showAlert("${state.addEditPerson.first} ${state.addEditPerson.last} updated!")
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    )
}
package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.BoatPeopleComposite
import viewmodel.BoatViewModel

@Composable
fun People(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        flowState.editPerson?.let { person ->
            EditPerson(person, viewModel)
        } ?: when (val state = flowState.response) {
            is Complete -> PeopleLoaded(state.value, viewModel)
            is Error -> {
                Text("Womp Womp")
                PeopleLoaded(state.value, viewModel)
            }

            is Loading -> {
                Spinner()
                PeopleLoaded(state.value, viewModel)
            }

            Uninitialized -> Spinner()
        }
    }
}

@Composable
fun EditPerson(
    person: Person,
    viewModel: BoatViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var newPerson by remember { mutableStateOf(person) }
    if (confirmDelete) {
        Confirm("Delete '${person.first} ${person.last}'?") { delete ->
            if (delete) {
                viewModel.delete(person)
            } else {
                confirmDelete = false
            }
        }
    } else {
        Form {
            Fieldset {
                Legend { Text("Edit id:${person.id} ${person.first} ${person.last}") }
                P {
                    Input(InputType.Text) {
                        id("first")
                        value(newPerson.first)
                        onInput { newPerson = newPerson.copy(first = it.value) }
                    }
                    Label("first") { Text("First name") }
                }
                P {
                    Input(InputType.Text) {
                        id("last")
                        value(newPerson.last)
                        onInput { newPerson = newPerson.copy(last = it.value) }
                    }
                    Label("last") { Text("Last name") }
                }
                P {
                    CheckboxInput(
                        attrs = {
                            id("member")
                            checked(newPerson.clubMember)
                            onChange {
                                println("checked = ${it.value}")
                                newPerson = newPerson.copy(clubMember = it.value)
                                println("newperson ${Json.encodeToString(newPerson)}")
                                println("checked = ${it.value}")
                            }
                        }
                    )
                    Label("member") { Text("Club member") }
                }
            }
        }
        Br()
        RgButton("Cancel", RgButtonStyle.PrimaryOutline) {
            viewModel.setEditPerson(null)
        }
        RgButton("Save", RgButtonStyle.Primary) {
            viewModel.upsertPerson(newPerson)
        }
        RgButton("Delete", RgButtonStyle.Danger) {
            confirmDelete = true
        }
    }
}

@Composable
fun PeopleLoaded(
    composite: BoatPeopleComposite?,
    viewModel: BoatViewModel,
) {
    Div {
        H1 { Text("People") }
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
                                RgButton("Edit", RgButtonStyle.PrimaryOutline) {
                                    viewModel.setEditPerson(person)
                                }
                            }
                        }
                    }
                }
                AddPerson(viewModel)
            }
        }
    }
}

@Composable
fun AddPerson(viewModel: BoatViewModel) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var member by remember { mutableStateOf(false) }
    RgTr {
        RgTd(4) {
            Br()
            H6 { Text("Add person") }
        }
    }
    RgTr {
        RgTd {
            Input(type = InputType.Text) {
                placeholder("First")
                classes("form-control")
                onInput {
                    first = it.value
                }
                value(first)
            }
        }
        RgTd(2) {
            Input(type = InputType.Text) {
                placeholder("Last")
                classes("form-control")
                onInput {
                    last = it.value
                }
                value(last)
            }
        }
        RgTd(classes = listOf("position-relative")) {
            Div(attrs = { classes("form-check", "position-absolute", "top-50", "translate-middle") }) {
                CheckboxInput {
                    id("member")
                    classes("form-check-input")
                    onChange {
                        member = it.value
                    }
                    checked(member)
                }
                Label("member", attrs = {
                    classes("form-check-label")
                }) {
                    Text("Club member")
                }
            }
        }
        RgTd {
            RgButton("Add", RgButtonStyle.Primary, first.isBlank() || last.isBlank()) {
                viewModel.upsertPerson(Person(first = first, last = last, clubMember = member))
                first = ""
                last = ""
                member = false
            }
        }
    }
}
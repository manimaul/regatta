package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.Confirm
import components.RgButton
import components.RgButtonStyle
import components.Spinner
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.*

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
    viewModel: BoatViewModel ,
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
                    CheckboxInput (
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
        RgButton("Delete", RgButtonStyle.Error) {
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
        Article {
            H1 { Text("People") }
        }
        Table(attrs = { classes("striped") }) {
            Tr {
                Th { Text("First") }
                Th { Text("Last") }
                Th { Text("Boat") }
                Th { Text("Member") }
                Th { Text("Action") }
            }
            composite?.people?.takeIf { it.isNotEmpty() }?.let { people ->
                people.forEach { person ->
                    Tr {
                        Td { Text(person.first) }
                        Td { Text(person.last) }
                        Td { Text(viewModel.findBoatName(person, composite)) }
                        Td { Text(if (person.clubMember) "Yes" else "No") }
                        Td {
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

@Composable
fun AddPerson(viewModel: BoatViewModel) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var member by remember { mutableStateOf(false) }
    Tr {
        Td {
            Input(type = InputType.Text) {
                placeholder("First")
                onInput {
                    first = it.value
                }
                value(first)
            }
        }
        Td {
            Input(type = InputType.Text) {
                placeholder("Last")
                onInput {
                    last = it.value
                }
                value(last)
            }
        }
        Td { }
        Td {
            CheckboxInput {
                onChange {
                    member = it.value
                }
                checked(member)
            }
        }
        Td {
            RgButton("Add", RgButtonStyle.Primary, first.isBlank() || last.isBlank()) {
                viewModel.upsertPerson(Person(first = first, last = last, clubMember = member))
                first = ""
                last = ""
                member = false
            }
        }
    }
}
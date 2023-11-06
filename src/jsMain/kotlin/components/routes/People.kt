package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.Confirm
import components.Spinner
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.DeletePerson
import viewmodel.PeopleStateLoaded
import viewmodel.PeopleStateLoading
import viewmodel.PeopleViewModel

@Composable
fun People(
    viewModel: PeopleViewModel = remember { PeopleViewModel() }
) {
    Div {
        H4 {
            Text("People")
        }
        when (val state = viewModel.state) {
            is PeopleStateLoaded -> {
                state.people.takeIf { it.isNotEmpty() }?.let { people ->
                    Table {
                        Tr {
                            Th { Text("First") }
                            Th { Text("Last") }
                            Th { Text("Boat") }
                            Th { Text("Member") }
                            Th { Text("") }
                        }
                        people.forEach { person ->
                            Tr {
                                Td { Text(person.first) }
                                Td { Text(person.last) }
                                Td { Text("-") }
                                Td { Text(if(person.clubMember) "Yes" else "No") }
                                Td {
                                    Button(attrs = {
                                        onClick { viewModel.setDeletePerson(person) }
                                    }) {
                                        Text("X")
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    P { Text("No one was found") }
                }
                Br()
                Hr()
                H4 { Text("Add person") }
                AddPerson(viewModel)
            }
            PeopleStateLoading -> Spinner(50f)
            is DeletePerson -> {
                Confirm("Delete '${state.person.first} ${state.person.last}'?") { delete ->
                    if (delete) {
                        viewModel.delete(state.person)
                    } else {
                        viewModel.reload()
                    }
                }
            }
        }
    }
}

@Composable
fun AddPerson(viewModel: PeopleViewModel) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var member by remember { mutableStateOf(false) }
    Input(type = InputType.Text) {
        placeholder("First")
        onInput {
            first = it.value
        }
        value(first)
    }
    Br()
    Input(type = InputType.Text) {
        placeholder("Last")
        onInput {
            last = it.value
        }
        value(last)
    }
    CheckboxInput {
        id("Member")
        name("Member")
        onChange {
            member = it.value
        }
        checked(member)
    }
    Label("Member") { Text("Club member") }
    if (first.isNotBlank() && last.isNotBlank()) {
        Br()
        Button(attrs = {
            onClick {
                viewModel.upsertPerson(Person(first = first, last = last, clubMember = member))
            }
        }) {
            Text("Add")
        }
    }
}
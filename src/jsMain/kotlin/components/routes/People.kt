package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.Confirm
import components.RgButton
import components.RgButtonStyle
import components.Spinner
import org.jetbrains.compose.web.attributes.InputType
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
    val flowState by viewModel.flow.collectAsState()
    Div {
        when (val state = flowState) {
            is PeopleStateLoaded -> PeopleLoaded(state, viewModel)
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
fun PeopleLoaded(
    state: PeopleStateLoaded,
    viewModel: PeopleViewModel,
) {
    Div {
        Article {
            H1 { Text("People") }
        }
        Table(attrs = { classes("striped") }) {
            state.people.takeIf { it.isNotEmpty() }?.let { people ->
                Tr {
                    Th { Text("First") }
                    Th { Text("Last") }
                    Th { Text("Boat") }
                    Th { Text("Member") }
                    Th { Text("Action") }
                }
                people.forEach { person ->
                    Tr {
                        Td { Text(person.first) }
                        Td { Text(person.last) }
                        Td { Text("-") }
                        Td { Text(if (person.clubMember) "Yes" else "No") }
                        Td {
                            RgButton("Delete", RgButtonStyle.Error) {
                                viewModel.setDeletePerson(person)
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
fun AddPerson(viewModel: PeopleViewModel) {
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
        Td {  }
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
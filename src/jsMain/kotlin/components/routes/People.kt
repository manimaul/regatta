package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.Spinner
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
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
                        }
                        people.forEach { person ->
                            Tr {
                                Td { Text(person.first) }
                                Td { Text(person.last) }
                                Td { Text("?") }
                                Td { CheckboxInput(person.clubMember) }
                            }
                        }
                    }
                } ?: run {
                    P { Text("No one was found") }
                }
                AddPerson(viewModel)
            }
            PeopleStateLoading -> Spinner(50f)
        }
    }
}

@Composable
fun AddPerson(viewModel: PeopleViewModel) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var admin by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf<String?>(null) }
    Input(type = InputType.Text) {
        placeholder("First")
        onInput {
            first = it.value
        }
    }
    Br()
    Input(type = InputType.Text) {
        placeholder("Last")
        onInput {
            last = it.value
        }
    }
    CheckboxInput {
        checked(admin)
        id("Admin")
        name("Admin")
        onChange {
            admin = it.value
        }
    }
    Label("Admin") { Text("Admin") }
    if (admin) {
        Br()
        Input(type = InputType.Text) {
            placeholder("Username")
            onInput {
                userName = it.value
            }
        }
        Br()
        Input(type = InputType.Password) {
            placeholder("Password")
            onInput {
                password = it.value
            }
        }
        password?.let {
            Br()
            Text(it)
        }
    }
}
package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.*
import utils.*
import viewmodel.*

enum class Operation {
    None,
    Fetched,
    Updated,
    Deleted
}

data class EditPersonState(
    val person: Async<Person> = Uninitialized,
    val operation: Operation = Operation.None
) : VmState

class EditPersonViewModel(
    val personId: Long?,
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<EditPersonState>(EditPersonState()) {

    init {
        launch {
            personId?.let {
                setState {
                    EditPersonState(
                        Api.getPerson(personId).toAsync().mapErrorMessage { "error fetching user id $personId" },
                        Operation.Fetched
                    )
                }
            } ?: setState {
                EditPersonState(
                    Error(),
                    Operation.Fetched
                )
            }
        }
    }

    fun delete(person: Person) {
        person.id?.let {
            setState {
                copy(
                    person = Api.deletePerson(person.id).toAsync().map { person }
                        .mapErrorMessage { "error deleting ${person.first}, ${person.last}" },
                    operation = Operation.Deleted
                )
            }
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    fun upsertPerson(person: Person) {
        setState {
            copy(
                person = Api.postPerson(person).toAsync()
                    .mapErrorMessage { "error updating ${person.first}, ${person.last}" },
                operation = Operation.Updated
            )
        }
    }

}

@Composable
fun PeopleEdit(
    id: Long?,
    viewModel: EditPersonViewModel = remember { EditPersonViewModel(id) },
) {
    val state by viewModel.flow.collectAsState()
    when (val person = state.person) {
        is Complete -> {
            when (state.operation) {
                Operation.None -> Unit
                Operation.Fetched -> EditPerson(person.value, viewModel)
                Operation.Updated -> Ok("${person.value.first} ${person.value.last} has been updated") {
                    viewModel.cancelEdit()
                }
                Operation.Deleted -> Ok("${person.value.first} ${person.value.last} has been deleted!") {
                    viewModel.cancelEdit()
                }
            }
        }

        is Error -> P { Text(person.message) }
        is Loading -> Spinner()
        Uninitialized -> Unit
    }
}

@Composable
fun EditPerson(
    person: Person,
    viewModel: EditPersonViewModel,
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
            viewModel.cancelEdit()
        }
        RgButton("Save", RgButtonStyle.Primary) {
            viewModel.upsertPerson(newPerson)
        }
        RgButton("Delete", RgButtonStyle.Danger) {
            confirmDelete = true
        }
    }
}

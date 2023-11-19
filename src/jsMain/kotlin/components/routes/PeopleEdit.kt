package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.*
import viewmodel.*


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
                Operation.Updated -> RgOk("${person.value.first} ${person.value.last} has been updated") {
                    viewModel.cancelEdit()
                }

                Operation.Deleted -> RgOk("${person.value.first} ${person.value.last} has been deleted!") {
                    viewModel.cancelEdit()
                }
            }
        }

        is Error -> P { Text(person.message) }
        is Loading -> RgSpinner()
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
        RgConfirm("Delete '${person.first} ${person.last}'?") { delete ->
            if (delete) {
                viewModel.delete(person)
            } else {
                confirmDelete = false
            }
        }
    } else {
        H1 { Text("Edit") }
        RgForm {
            Fieldset {
                P {
                    RgInput("First name", newPerson.first) {
                        newPerson = newPerson.copy(first = it)
                    }
                }
                P {
                    RgInput("Last name", newPerson.last) {
                        newPerson = newPerson.copy(last = it)
                    }
                }
                P {
                    RgCheck("Club member", newPerson.clubMember) {
                        newPerson = newPerson.copy(clubMember = it)
                    }
                }
            }
            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelEdit()
            }
            RgButton("Save", RgButtonStyle.Primary) {
                viewModel.upsertPerson(newPerson)
            }
            RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                confirmDelete = true
            }
        }
    }
}

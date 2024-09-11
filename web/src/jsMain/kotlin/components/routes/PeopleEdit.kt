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


@Composable
fun PeopleEdit(
    id: Long?,
    viewModel: PeopleEditViewModel = remember { PeopleEditViewModel(id ?: 0) },
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
    viewModel: PeopleEditViewModel,
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

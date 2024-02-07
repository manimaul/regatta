package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Bracket
import components.*
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.BracketEditViewModel
import viewmodel.Operation

@Composable
fun ClassEdit(
    id: Long? = null,
    viewModel: BracketEditViewModel = remember { BracketEditViewModel(id ?: 0) }
) {
    val state by viewModel.flow.collectAsState()
    when (val rc = state.series) {
        is Complete -> {
            when (state.operation) {
                Operation.None -> {}
                Operation.Fetched -> ClassEditor(rc.value, viewModel)
                Operation.Updated -> RgOk("'${rc.value.name}' updated!") {
                    viewModel.cancelEdit()
                }

                Operation.Deleted -> RgOk("'${rc.value.name}' deleted!") {
                    viewModel.cancelEdit()
                }
            }
        }

        is Error -> Text(rc.message)
        is Loading -> RgSpinner()
        Uninitialized -> {}
    }
}

@Composable
fun ClassEditor(
    bracket: Bracket,
    viewModel: BracketEditViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var newClass by remember { mutableStateOf(bracket) }

    if (confirmDelete) {
        RgConfirm("Delete '${newClass.name}'?") { delete ->
            if (delete) {
                viewModel.delete(bracket)
            } else {
                confirmDelete = false
            }
        }
    } else {

        H1 { Text("Edit") }
        RgForm {
            Fieldset {
                P {
                    RgInput("Name", newClass.name) {
                        newClass = newClass.copy(name = it)
                    }
                }
                P {
                    RgInput("Description", newClass.description ?: "") {
                        newClass = newClass.copy(description = it)
                    }
                }
            }

            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelEdit()
            }
            RgButton("Save", RgButtonStyle.Primary) {
                viewModel.upsert(newClass)
            }
            RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                confirmDelete = true
            }
        }
    }
}

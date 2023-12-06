package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceCategory
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
import viewmodel.CategoryEditViewModel
import viewmodel.Operation

@Composable
fun CategoryEdit(
    id: Long?,
    viewModel: CategoryEditViewModel = remember { CategoryEditViewModel(id ?: 0)}
) {
    val state by viewModel.flow.collectAsState()
    when (val cat = state.category) {
        is Complete -> when (state.operation) {
                Operation.None -> Unit
                Operation.Fetched -> CategoryEditor(cat.value, viewModel)
                Operation.Updated -> RgOk("Updated '${cat.value.name}'!") {
                    viewModel.cancelEdit()
                }
                Operation.Deleted -> RgOk("Deleted '${cat.value.name}'!") {
                    viewModel.cancelEdit()
                }
            }
        is Error -> Text(cat.message)
        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}


@Composable
fun CategoryEditor(
    category: RaceCategory,
    viewModel: CategoryEditViewModel,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var updateCat by remember { mutableStateOf(category) }

    if (confirmDelete) {
        RgConfirm("Delete '${updateCat.name}'?") { delete ->
            if (delete) {
                viewModel.delete(category)
            } else {
                confirmDelete = false
            }
        }
    } else {
        H1 { Text("Edit") }
        RgForm {
            Fieldset {
                P {
                    RgInput("Name", updateCat.name) {
                        updateCat = updateCat.copy(name = it)
                    }
                }
            }
            RgButton("Cancel", RgButtonStyle.PrimaryOutline, customClasses = listOf(AppStyle.marginEnd)) {
                viewModel.cancelEdit()
            }
            RgButton("Save", RgButtonStyle.Primary) {
                viewModel.upsert(updateCat)
            }
            RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                confirmDelete = true
            }
        }
    }
}
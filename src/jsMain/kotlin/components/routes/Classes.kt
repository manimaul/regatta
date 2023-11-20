package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.ClassesViewModel

@Composable
fun Classes(
    viewModel: ClassesViewModel = remember { ClassesViewModel() }
) {
    val flowState by viewModel.flow.collectAsState()
    when (val list = flowState.classList) {
        is Complete -> CategoryList(viewModel, list.value)
        is Error -> CategoryError(viewModel, list)
        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}

@Composable
fun CategoryError(
    viewModel: ClassesViewModel,
    error: Error<*>,
) {
    P {
        Text("Something went wrong")
    }
    P {
        Text(error.message)
    }
    viewModel.reload(3000)
}

@Composable
fun CategoryList(
    viewModel: ClassesViewModel,
    list: List<RaceClassCategory>,
) {
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Name") }
                RgTh { Text("Description") }
                RgTh { Text("Action") }
            }
        }
        list.forEachIndexed { index, cat ->
            RgTbody {
                RgTr {
                    RgTd(2) {
                        H2 { Text(cat.name) }
                    }
                    RgTh {
                        RgButton("Edit Category", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-end")) {
                            viewModel.editCategory(cat)
                        }
                    }
                }
                ClassRow(viewModel, cat.children ?: emptyList())
                AddClass(viewModel, cat)
                if (index == list.size - 1) {
                    AddCategory(viewModel)
                }
            }
        }
    }
}

@Composable
fun ClassRow(
    viewModel: ClassesViewModel,
    list: List<RaceClass>,
) {

    list.forEach { rc ->
        RgTr {
            RgTd { Text(rc.name) }
            RgTd { Text(rc.description ?: "-") }
            RgTd {
                RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-end")) {
                    viewModel.setEditClass(rc)
                }
            }
        }
    }
}

@Composable
fun AddCategory(
    viewModel: ClassesViewModel,
) {
    var name by remember { mutableStateOf("") }
    Br()
    Br()
    RgTr {
        RgTd(2) {
            RgInput("Name", name, true) {
                name = it
            }
        }
        RgTd {
            RgButton("Add Category", RgButtonStyle.Primary, name.isBlank(), listOf("float-end")) {
                viewModel.upsertCategory(RaceCategory(name = name))
                name = ""
            }
        }
    }
}

@Composable
fun AddClass(
    viewModel: ClassesViewModel,
    category: RaceClassCategory,
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    RgTr {
        RgTd {
            RgInput("Name", name, true) {
                name = it
            }
        }
        RgTd {
            RgInput("Description", desc, true) {
                desc = it
            }
        }
        RgTd {
            RgButton("Add", RgButtonStyle.Primary, name.isBlank() || desc.isBlank(), listOf("float-end")) {
                viewModel.upsertClass(RaceClass(name = name, description = desc, category = category.id!!))
                name = ""
                desc = ""
            }
        }
    }
}

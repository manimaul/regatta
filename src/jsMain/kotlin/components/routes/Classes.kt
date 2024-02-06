package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import org.jetbrains.compose.web.dom.*
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
        is Error -> {
            ErrorDisplay(list) {
                viewModel.reload()
            }
        }
        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}


@Composable
fun CategoryList(
    viewModel: ClassesViewModel,
    list: List<RaceClassCategory>,
) {
    H1 { Text("Race Classes") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Name") }
                RgTh { Text("Description") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            AddCategory(viewModel)
            list.forEach { cat ->
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
                ClassRow(viewModel, cat.brackets ?: emptyList())
                AddClass(viewModel, cat)
            }
        }
    }
}

@Composable
fun ClassRow(
    viewModel: ClassesViewModel,
    list: List<Bracket>,
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
                viewModel.upsertClass(Bracket(name = name, description = desc, category = category.id!!))
                name = ""
                desc = ""
            }
        }
    }
}

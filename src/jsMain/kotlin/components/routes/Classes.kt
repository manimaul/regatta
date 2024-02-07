package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClassBrackets
import components.*
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
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
    list: List<RaceClassBrackets>,
) {
    val state by viewModel.flow.collectAsState()
    H1 { Text("Race Classes") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Name") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            EditClass(RaceClass(), viewModel)
            list.forEachIndexed { i, each ->
                if (state.editClassId == each.raceClass.id) {
                    EditClass(each.raceClass, viewModel, first = i == 0, last = i == (list.size - 1))
                } else {
                    RgTr {
                        RgTd(classes = listOf("table-info")) {
                            H2 { Text(each.raceClass.name) }
                        }
                        RgTd(classes = listOf("table-info")) {
                            RgButton("Edit Class", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-end")) {
                                viewModel.editClass(each.raceClass)
                            }
                        }
                    }
                }
                RgTr {
                    RgTd(colSpan = 2) {
                        RgTable(caption = "brackets") {
                            RgThead {
                                RgTr {
                                    RgTh { Text("Name") }
                                    RgTh { Text("Description") }
                                    RgTh { Text("Min Rating") }
                                    RgTh { Text("Max Rating") }
                                    RgTh { Text("Action") }
                                }
                            }
                            RgTbody {
                                EditBracket(Bracket(classId = each.raceClass.id), viewModel)
                                BracketRow(viewModel, each.brackets)
                            }
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun BracketRow(
    viewModel: ClassesViewModel,
    list: List<Bracket>,
) {
    val state by viewModel.flow.collectAsState()
    list.forEach { bracket ->
        if (bracket.id == state.editBracketId) {
            EditBracket(bracket, viewModel)
        } else {
            RgTr {
                RgTd { Text(bracket.name) }
                RgTd { Text(bracket.description ?: "") }
                RgTd { Text(bracket.minRating.toString()) }
                RgTd { Text(bracket.maxRating.toString()) }
                RgTd {
                    RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-end")) {
                        viewModel.editBracket(bracket)
                    }
                }
            }
        }
    }
}

@Composable
fun EditClass(
    editClass: RaceClass,
    viewModel: ClassesViewModel,
    first: Boolean = true,
    last: Boolean = true,
) {
    var raceClass by remember { mutableStateOf(editClass) }
    if (editClass.id != raceClass.id) {
        raceClass = editClass
    }
    RgTr {
        RgTd {
            RgInput("Name", raceClass.name, true) {
                raceClass = raceClass.copy(name = it)
            }
        }
        RgTd {
            val label = if (raceClass.id == 0L) {
                "Add Class"
            } else {
                RgButton(
                    "Cancel",
                    RgButtonStyle.Primary,
                    raceClass.name.isBlank(),
                    listOf("float-end", AppStyle.marginStart)
                ) {
                    viewModel.editClass(null)
                }
                if (!first || !last) {
                    if (!first) RgButton("Up",RgButtonStyle.Success, customClasses = listOf(AppStyle.marginStart)) {
                        viewModel.moveUp(raceClass)
                    }
                    if (!last) RgButton("Down", RgButtonStyle.Success, customClasses = listOf(AppStyle.marginStart)) {
                        viewModel.moveDown(raceClass)
                    }
                }
                RgButton("Delete", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginStart)) {
                    viewModel.delete(raceClass)
                }
                "Save"
            }
            RgButton(label, RgButtonStyle.Success, raceClass.name.isBlank(), listOf(AppStyle.marginStart)) {
                viewModel.upsertClass(raceClass)
                raceClass = editClass
            }
        }
    }
}

@Composable
fun EditBracket(
    editBracket: Bracket,
    viewModel: ClassesViewModel,
) {
    var bracket by remember { mutableStateOf(editBracket) }
    if (editBracket.id != bracket.id) {
        bracket = editBracket
    }
    RgTr {
        RgTd {
            RgInput("Name", bracket.name, true) {
                bracket = bracket.copy(name = it)
            }
        }
        RgTd {
            RgInput("Description", bracket.description ?: "", true) {
                bracket = bracket.copy(description = it)
            }
        }
        RgTd {
            RgInput("Min Rating", bracket.minRating.toString(), true) {
                bracket = bracket.copy(minRating = it.toFloatOrNull() ?: 0f)
            }
        }
        RgTd {
            RgInput("Max Rating", bracket.maxRating.toString(), true) {
                bracket = bracket.copy(maxRating = it.toFloatOrNull() ?: 0f)
            }
        }
        RgTd {
            val label = if (editBracket.id == 0L) {
                "Add Bracket"
            } else {
                RgButton(
                    "Cancel",
                    RgButtonStyle.Primary,
                    bracket.name.isBlank(),
                    listOf("float-end", AppStyle.marginStart)
                ) {
                    viewModel.editBracket(null)
                }
                RgButton("Delete", RgButtonStyle.Danger, bracket.name.isBlank(), listOf(AppStyle.marginStart)) {
                    viewModel.delete(bracket)
                }
                "Save"
            }
            RgButton(label, RgButtonStyle.Success, bracket.name.isBlank(), listOf(AppStyle.marginStart)) {
                viewModel.upsertBracket(bracket)
                bracket = editBracket
            }
        }
    }
}

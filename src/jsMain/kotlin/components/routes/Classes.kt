package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClassBrackets
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
    list: List<RaceClassBrackets>,
) {
    H1 { Text("Race Classes") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Name") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            AddClass(viewModel)
            list.forEach { each ->
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
                                AddBracket(viewModel, each)
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
    list.forEach { rc ->
        RgTr {
            RgTd { Text(rc.name) }
            RgTd { Text(rc.description ?: "") }
            RgTd { Text(rc.minRating.toString()) }
            RgTd { Text(rc.maxRating.toString()) }
            RgTd {
                RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-end")) {
                    viewModel.editBracket(rc)
                }
            }
        }
    }
}

@Composable
fun AddClass(
    viewModel: ClassesViewModel,
) {
    var name by remember { mutableStateOf("") }
    RgTr {
        RgTd {
            RgInput("Name", name, true) {
                name = it
            }
        }
        RgTd {
            RgButton("Add Class", RgButtonStyle.Primary, name.isBlank(), listOf("float-end")) {
                viewModel.upsertCategory(RaceClass(name = name))
                name = ""
            }
        }
    }
}

@Composable
fun AddBracket(
    viewModel: ClassesViewModel,
    classBrackets: RaceClassBrackets,
) {
    var bracket by remember { mutableStateOf(Bracket(classId = classBrackets.raceClass.id)) }
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
            RgButton("Add Bracket", RgButtonStyle.Primary, bracket.name.isBlank(), listOf("float-end")) {
                viewModel.upsertBracket(bracket)
                bracket = Bracket(classId = classBrackets.raceClass.id)
            }
        }
    }
}

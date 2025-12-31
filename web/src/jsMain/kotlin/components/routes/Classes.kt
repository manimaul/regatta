package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.RatingType
import components.*
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Fieldset
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
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
        is Complete -> {
            if (flowState.sortMode) {
                SortClasses(list.value.map { it.raceClass }, viewModel)
            } else {
                ClassList(viewModel, list.value)
            }
        }

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
fun SortClasses(
    list: List<RaceClass>,
    viewModel: ClassesViewModel,
) {
    H1 { Text("Series Sort Order") }
    var order by remember { mutableStateOf(list) }
    RgSortable(list, { it.name }) {
        order = it.mapIndexed { i, s -> s.copy(sort = i) }
    }
    RgButton(
        "Cancel",
        style = RgButtonStyle.PrimaryOutline,
        customClasses = listOf(AppStyle.marginStart, AppStyle.marginTop)
    ) {
        viewModel.sortMode(false)
    }
    RgButton("Save", style = RgButtonStyle.Success, customClasses = listOf(AppStyle.marginStart, AppStyle.marginTop)) {
        viewModel.saveClassOrder(order)
    }
}

@Composable
fun ClassList(
    viewModel: ClassesViewModel,
    list: List<RaceClassBrackets>,
) {
    H1 { Text("Race Classes") }
    Div(attrs = {
        classes(AppStyle.marginBot)
    }) {
        RgModalButton(
            id = "add-edit-class",
            style = RgButtonStyle.SuccessOutline,
            buttonLabel = { "Add Class" },
            openAction = {
                viewModel.editClass(RaceClassBrackets(brackets = listOf(Bracket())))
            }
        )
        AddEditClassModal(viewModel)
    }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Name") }
                RgTh { Text("Rating") }
                RgTh { Text("Brackets") }
                RgTh { Text("Number Of Races") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            list.forEach { each ->
                RgTr {
                    RgTd {
                        Text(each.raceClass.name)
                    }
                    RgTd {
                        Text(each.raceClass.ratingType.label)
                    }
                    RgTd {
                        each.brackets.takeIf { it.isNotEmpty() }?.let {
                            Text("(1) ${it.map { it.name }.reduceIndexed { i, l, r -> "${l}, (${i + 1}) $r" }}")
                        }
                    }
                    RgTd {
                        Text("${each.raceClass.numberOfRaces}")
                    }
                    RgTd {
                        RgModalButton(
                            id = "add-edit-class",
                            style = RgButtonStyle.PrimaryOutline,
                            buttonLabel = { "Edit Class" },
                            openAction = {
                                viewModel.editClass(each)
                            }
                        )
                        if (each.raceClass.numberOfRaces == 0L) {
                            RgButton(
                                label = "Delete Class",
                                style = RgButtonStyle.Danger,
                                customClasses = listOf(AppStyle.marginAll)
                            ) {
                                viewModel.delete(each)
                            }
                        }
                    }
                }
            }
        }
    }
    Div(attrs = { classes("flex-wrap") }) {
        RgButton("Change Sort Order") {
            viewModel.sortMode(true)
        }
    }
}

@Composable
fun AddEditClassModal(
    viewModel: ClassesViewModel,
) {
    val state by viewModel.flow.collectAsState()
    RgModalBody(
        id = "add-edit-class",
        modalTitle = { "Add Race Class" }, //
        content = {
            RgForm {
                Fieldset {
                    P {
                        RgInput("Name", state.editClass.raceClass.name, true) {
                            viewModel.editClass(
                                state.editClass.copy(
                                    raceClass = state.editClass.raceClass.copy(
                                        name = it
                                    ),
                                )
                            )
                        }
                    }
                    P {

                        if (state.editClass.raceClass.numberOfRaces == 0L) {
                            RgDropdown(
                                items = RatingType.entries,
                                selectedItem = state.editClass.raceClass.ratingType,
                                name = { it.label }
                            ) {
                                viewModel.editClass(
                                    state.editClass.copy(
                                        raceClass = state.editClass.raceClass.copy(
                                            ratingType = it
                                        )
                                    )
                                )
                            }
                        } else {
                            Text("(${state.editClass.raceClass.ratingType.label} ${state.editClass.raceClass.numberOfRaces} race(s) / cannot be edited)")
                        }
                    }

                    H6 { Text("Brackets") }
                    Hr { }
                    state.editClass.brackets.forEachIndexed { i, bracket ->
                        if (state.editClass.raceClass.numberOfRaces == 0L) {
                            P { EditBracket(i, viewModel) }
                        } else {
                            P { ViewBracket(bracket) }
                        }
                        Hr { }
                    }

                    if (state.editClass.raceClass.numberOfRaces == 0L) {
                        P {
                            RgButton(
                                label = "+",
                            ) {
                                viewModel.addBracket()
                            }
                        }
                    }
                }
            }
        },
        footer = {
            Div(attrs = { classes("flex-fill", "d-flex", "justify-content-between") }) {
                Button(attrs = {
                    classes(*RgButtonStyle.Success.classes)
                    attr("data-bs-dismiss", "modal")
                    onClick {
                        viewModel.upsertClassBrackets(state.editClass)
                    }
                    if (!viewModel.isUpsertClassBracketsValid(state.editClass)) {
                        disabled()
                    }
                }) {
                    Text("Save")
                }
            }
        }
    )
}

@Composable
fun ViewBracket(
    bracket: Bracket
) {
    P {
        Text("${bracket.name} (${bracket.numberOfRaces} race(s) / cannot be edited)")
    }
}

@Composable
fun EditBracket(
    index: Int,
    viewModel: ClassesViewModel,
) {
    val state by viewModel.flow.collectAsState()
    fun bracket() =
        state.editClass.brackets[index]

    Div {
        RgButton(
            label = "-",
            customClasses = listOf(AppStyle.marginEnd)
        ) {
            viewModel.removeBracket(index)
        }
        RgInput(
            label = "Bracket Name",
            value = bracket().name,
            placeHolder = false,
            customClasses = listOf(AppStyle.marginBot)
        ) {
            viewModel.updateBracket(index, bracket().copy(name = it))
        }
    }
    Div {
        RgInput(
            label = "Bracket Description",
            value = bracket().description ?: "",
            placeHolder = false,
            customClasses = listOf(AppStyle.marginBot)
        ) {
            viewModel.updateBracket(index, bracket().copy(description = it))
        }
    }
    Div(attrs = { classes("flex-fill", "d-flex", "justify-content-between") }) {
        RgNumberInput(
            label = "Min Rating",
            value = bracket().minRating,
            placeHolder = false,
            numberType = NumberType.NumberInt
        ) {
            viewModel.updateBracket(index, bracket().copy(minRating = it.toFloat()))
        }
        RgNumberInput(
            label = "Max Rating",
            value = bracket().maxRating,
            placeHolder = false,
            numberType = NumberType.NumberInt
        ) {
            viewModel.updateBracket(index, bracket().copy(maxRating = it.toFloat()))
        }
    }
}

package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import utils.*
import viewmodel.BaseViewModel
import viewmodel.VmState

data class ClassesState(
    val classList: Async<List<RaceClassCategory>> = Uninitialized,
    val editClass: RaceClass? = null,
) : VmState
class ClassesViewModel : BaseViewModel<ClassesState>(ClassesState()){

    init {
        reload()
    }

    fun reload(pause: Long? = null) {
        launch {
            pause?.let { delay(it) }
            setState {
                copy(classList = Api.getAllCategories().toAsync())
            }
        }
    }

    fun setEditClass(rc: RaceClass?) {
        setState { copy(editClass = rc) }
    }

    fun upsertCategory(category: RaceClassCategory) {
        setState {
            val list = Api.postCategory(category).toAsync().flatMap { Api.getAllCategories().toAsync() }
            copy(
                classList = list
            )
        }
    }
    fun upsertClass(raceClass: RaceClass, category: RaceClassCategory) {
        setState {
            val list = Api.postClass(raceClass).toAsync().flatMap { Api.getAllCategories().toAsync() }
            copy(
                classList = list
            )
        }
    }
}

@Composable
fun Classes(
    viewModel: ClassesViewModel = remember { ClassesViewModel()}
) {
    val flowState by viewModel.flow.collectAsState()

    when (val list = flowState.classList) {
        is Complete -> CategoryList(viewModel, list.value)
        is Error -> {
            P {
                Text("Something went wrong")
            }
            list.message?.let {
                P {
                   Text(it)
                }
            }
            viewModel.reload(3000)
        }
        is Loading -> Spinner()
        Uninitialized -> Unit
    }
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
            H2 { Text(cat.name) }
                RgTbody {
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
            Input(type = InputType.Text) {
                placeholder("Name")
                classes("form-control")
                onInput {
                    name = it.value
                }
                value(name)
            }
        }
        RgTd {
            RgButton("Add Category", RgButtonStyle.Primary, name.isBlank(), listOf("float-end") ) {
                viewModel.upsertCategory(RaceClassCategory(name = name))
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
            Input(type = InputType.Text) {
                classes("form-control")
                placeholder("Name")
                onInput {
                    name = it.value
                }
                value(name)
            }
        }
        RgTd {
            Input(type = InputType.Text) {
                classes("form-control")
                placeholder("Description")
                onInput {
                    desc = it.value
                }
                value(desc)
            }
        }
        RgTd {
            RgButton("Add", RgButtonStyle.Primary, name.isBlank() || desc.isBlank(), listOf("float-end")) {
                viewModel.upsertClass(RaceClass(name = name, description = desc, category = category.id!!), category)
                name = ""
                desc = ""
            }
        }
    }
}

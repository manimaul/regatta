package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJSDate
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import utils.Api
import viewmodel.*

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

    Div {
        Article {
            H1 { Text("Race Classes") }
        }
        list.forEach { cat ->
            Table(attrs = { classes("striped") }) {
                Caption {
                    H2 {
                        Text(cat.name)
                    }
                }
                Tr {
                    Th { Text("Name") }
                    Th { Text("Description") }
                    Th { Text("Action") }
                }
                ClassRow(viewModel, cat.children ?: emptyList())
                AddClass(viewModel, cat)
            }
        }
        AddCategory(viewModel)
    }
}

@Composable
fun ClassRow(
    viewModel: ClassesViewModel,
    list: List<RaceClass>,
) {

    list.forEach { rc ->
        Tr {
            Td { Text(rc.name) }
            Td { Text(rc.description ?: "-") }
            Td {
                RgButton("Edit", RgButtonStyle.PrimaryOutline) {
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
    Row {
        Col4 {
            Input(type = InputType.Text) {
                placeholder("Name")
                onInput {
                    name = it.value
                }
                value(name)
            }
        }
        Col4 {
            RgButton("Add Category", RgButtonStyle.Primary, name.isBlank() ) {
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
    Tr {
        Td {
            Input(type = InputType.Text) {
                placeholder("Name")
                onInput {
                    name = it.value
                }
                value(name)
            }
        }
        Td {
            Input(type = InputType.Text) {
                placeholder("Description")
                onInput {
                    desc = it.value
                }
                value(desc)
            }
        }
        Td {  }
        Td {
            RgButton("Add", RgButtonStyle.Primary, name.isBlank() || desc.isBlank()) {
                viewModel.upsertClass(RaceClass(name = name, description = desc, category = category.id!!), category)
                name = ""
                desc = ""
            }
        }
    }
}

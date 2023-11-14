package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceClass
import components.RgButton
import components.RgButtonStyle
import components.Spinner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import utils.Api
import viewmodel.*

data class ClassesState(
    val classList: Async<List<RaceClass>> = Uninitialized
) : VmState
class ClassesViewModel : BaseViewModel<ClassesState>(ClassesState()){

    init {
        reload()
    }

    fun reload(pause: Long? = null) {
        launch {
            pause?.let { delay(it) }
            setState {
                copy(classList = Api.getAllClasses().toAsync())
            }
        }
    }

    fun setEditClass(rc: RaceClass) {
        TODO("Not yet implemented")
    }

    fun upsertClass(raceClass: RaceClass) {
        setState {
            val list = Api.postClass(raceClass).toAsync().flatMap { Api.getAllClasses().toAsync() }
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
        is Complete -> ClassList(viewModel, list.value)
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
fun ClassList(
    viewModel: ClassesViewModel,
    list: List<RaceClass>,
) {

    Div {
        Article {
            H1 { Text("Race Classes") }
        }
        Table(attrs = { classes("striped") }) {
            Tr {
                Th { Text("Name") }
                Th { Text("Description") }
                Th { Text("Action") }
            }
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
                AddClass(viewModel)
        }
    }
}

@Composable
fun AddClass(viewModel: ClassesViewModel) {
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
        Td { }
        Td {
            RgButton("Add", RgButtonStyle.Primary, name.isBlank() || desc.isBlank()) {
                viewModel.upsertClass(RaceClass(name = name, description = desc ))
//                viewModel.upsertPerson(Person(first = first, last = last, clubMember = member))
                name = ""
                desc = ""
            }
        }
    }
}

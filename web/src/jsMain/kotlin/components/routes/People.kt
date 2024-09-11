package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import utils.Complete
import utils.Error
import utils.Loading
import utils.Uninitialized
import viewmodel.BoatPeopleComposite
import viewmodel.BoatViewModel

@Composable
fun People(
    viewModel: BoatViewModel = remember { BoatViewModel() },
) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        when (val state = flowState.response) {
            is Complete -> PeopleLoaded(state.value, viewModel)
            is Error -> ErrorDisplay(state) {
                viewModel.reload()
            }
            is Loading -> {
                RgSpinner()
                PeopleLoaded(state.value, viewModel)
            }

            Uninitialized -> RgSpinner()
        }
    }
}


@Composable
fun PeopleLoaded(
    composite: BoatPeopleComposite?,
    viewModel: BoatViewModel,
) {
    Div {
        H1 { Text("People") }
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("First") }
                    RgTh { Text("Last") }
                    RgTh { Text("Boat") }
                    RgTh { Text("Member") }
                    RgTh { Text("Action") }
                }
            }
            RgTbody {
                AddPerson(viewModel)
                composite?.people?.takeIf { it.isNotEmpty() }?.let { people ->
                    people.forEach { person ->
                        RgTr {
                            RgTd { Text(person.first) }
                            RgTd { Text(person.last) }
                            RgTd { Text(viewModel.findBoatName(person, composite)) }
                            RgTd { Text(if (person.clubMember) "Yes" else "No") }
                            RgTd {
                                RgButton("Edit", RgButtonStyle.PrimaryOutline) {
                                    viewModel.setEditPerson(person)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddPerson(viewModel: BoatViewModel) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var member by remember { mutableStateOf(false) }
    RgTr {
        RgTd {
            RgInput("First", first, true) {
                first = it
            }
        }
        RgTd(2) {
            RgInput("Last", last, true) {
                last = it
            }
        }
        RgTd(classes = listOf("position-relative")) {
            RgCheck(
                "Club member",
                member,
                false,
                listOf("form-check", "position-absolute", "top-50", "start-5", "translate-middle-y")
            ) {
                member = it
            }
        }
        RgTd {
            RgButton("Add", RgButtonStyle.Primary, first.isBlank() || last.isBlank()) {
                viewModel.upsertPerson(Person(first = first, last = last, clubMember = member))
                first = ""
                last = ""
                member = false
            }
        }
    }
}
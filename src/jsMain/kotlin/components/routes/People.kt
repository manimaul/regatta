package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Person
import components.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
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
            is Error -> {
                Text("Womp Womp")
                PeopleLoaded(state.value, viewModel)
            }

            is Loading -> {
                Spinner()
                PeopleLoaded(state.value, viewModel)
            }

            Uninitialized -> Spinner()
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
                AddPerson(viewModel)
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
        RgTd(4) {
            Br()
            H6 { Text("Add person") }
        }
    }
    RgTr {
        RgTd {
            Input(type = InputType.Text) {
                placeholder("First")
                classes("form-control")
                onInput {
                    first = it.value
                }
                value(first)
            }
        }
        RgTd(2) {
            Input(type = InputType.Text) {
                placeholder("Last")
                classes("form-control")
                onInput {
                    last = it.value
                }
                value(last)
            }
        }
        RgTd(classes = listOf("position-relative")) {
            Div(attrs = { classes("form-check", "position-absolute", "top-50", "start-5", "translate-middle-y") }) {
                CheckboxInput {
                    id("member")
                    classes("form-check-input")
                    onChange {
                        member = it.value
                    }
                    checked(member)
                }
                Label("member", attrs = {
                    classes("form-check-label")
                }) {
                    Text("Club member")
                }
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
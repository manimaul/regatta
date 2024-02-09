package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.ClassSchedule
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import viewmodel.RgAddTimeViewModel
import viewmodel.complete

@Composable
fun RgAddTime(
    viewModel: RgAddTimeViewModel = remember { RgAddTimeViewModel() },
    handler: (ClassSchedule) -> Unit,
) {
    val state = viewModel.flow.collectAsState()
    if (state.value.classes.value?.isEmpty() != true) {
        RgTr {
            RgTd {
                state.value.classes.complete(viewModel) {classes ->
                    RgClassDropdown(classes, state.value.raceClass) {
                        viewModel.selectClass(it?.raceClass)
                    }
                }
            }
            RgTd {
                RgDate(label = "Race start", placeHolder = true, date = state.value.startDate, time = true) { t ->
                    viewModel.startTime(t)
                }
            }
            RgTd {
                RgDate(label = "Race end", placeHolder = true, date = state.value.endDate, time = true) { t ->
                    viewModel.endTime(t)
                }
            }
            RgTd {
                RgButton("Add", RgButtonStyle.Success, disabled = !state.value.isValid()) {
                    state.value.asSchedule()?.let {
                        viewModel.add(it)
                        handler(it)
                    }
                }
            }
        }
        RgTr {
            RgTd(colSpan = 4) {
                state.value.availableBrackets?.let { availableBrackets ->
                    P { Text("Brackets") }
                    RgSwitches(
                        items = availableBrackets,
                        label = { "${it.name} ${it.minRating}-${it.maxRating}" },
                        check = { b ->
                            state.value.brackets.firstOrNull { it.id == b.id } != null
                        }) { b, on ->
                        if (on) {
                            viewModel.addBracket(b)
                        } else {
                            viewModel.removeBracket(b)
                        }
                    }
                }
            }
        }
    }
}

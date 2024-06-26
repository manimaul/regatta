package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.ClassSchedule
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.now
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
                state.value.focus?.let { schedule ->
                    RgTd { H6 { Text(schedule.raceClass.name) } }
                } ?: state.value.classes.complete(viewModel) { classes ->
                    RgClassDropdown(classes, state.value.raceClass) {
                        viewModel.selectClass(it?.raceClass)
                    }
                }
            }
            RgTd {
                RgTime(showDate = true, date = state.value.startDate ?: now()) { t ->
                    viewModel.startTime(t)
                }
            }
            RgTd {
                RgTime(showDate = true, date = state.value.endDate ?: now()) { t ->
                    viewModel.endTime(t)
                }
            }
            RgTd {
                if (state.value.focus != null) {
                    RgButton(
                        label = "Cancel",
                        style = RgButtonStyle.PrimaryOutline,
                        customClasses = listOf(AppStyle.marginEnd),
                    ) {
                        viewModel.removeFocus()
                    }
                    RgButton(
                        label = "Update",
                        style = RgButtonStyle.Success,
                        disabled = !state.value.isValid(),
                    ) {
                        viewModel.removeFocus()
                        state.value.asSchedule()?.let {
                            viewModel.removeOption(it.raceClass.id, true)
                            handler(it)
                        }
                    }
                } else {
                    RgButton(
                        label = "Add",
                        style = RgButtonStyle.Success,
                        disabled = !state.value.isValid(),
                    ) {
                        state.value.asSchedule()?.let {
                            viewModel.removeOption(it.raceClass.id, true)
                            handler(it)
                        }
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
                        label = { it.label() },
                        check = { b ->
                            val c = state.value.brackets.firstOrNull { it.id == b.id } != null
                            println("check evaluated for bracket ${b.name} $c")
                            c
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

package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.FinishCode
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.display
import utils.now
import viewmodel.RcViewModel

@Composable
fun FinishCodeDrop(
    selected: FinishCode, customClasses: List<String>? = null, handler: (FinishCode) -> Unit
) {
    Div(attrs = {
        customClasses?.toTypedArray()?.let {
            classes(*it)
        }
    }) {
        RgDropdown(
            items = FinishCode.entries, selectedItem = selected, name = { it.name }, handler = handler
        )
    }
}

@Composable
fun TimeRow(
    viewModel: RcViewModel,
) {
    val state = viewModel.flow.collectAsState()
    val resetFinish = remember { state.value.focus?.finish }
    state.value.focus?.raceStart?.let {
        P { Text("Class start: ${it.display()}") }
    }
    state.value.focus?.elapsedTime()?.let {
        P { Text("Elapsed time: $it") }
    }
    state.value.focus?.penalty?.let {
        P { Text("Penalty +$it") }
        RgButton(label = "+", customClasses = listOf(AppStyle.marginBot)) {
            viewModel.penalty(it + 1)
        }
        RgButton(label = "-", customClasses = listOf(AppStyle.marginStart, AppStyle.marginBot)) {
            viewModel.penalty(it - 1)
        }
    }
    state.value.focus?.hocPosition?.let {
        P { Text("HOC $it") }
        RgButton(label = "Reset") {
            viewModel.setFinish(FinishCode.TIME, resetFinish ?: now())
        }
        RgButton(label = "+", customClasses = listOf(AppStyle.marginStart)) {
            viewModel.hoc(it + 1)
        }
        RgButton(label = "-", customClasses = listOf(AppStyle.marginStart)) {
            viewModel.hoc(it - 1)
        }
    } ?: state.value.focus?.finish?.let { finish ->
        RgTime(date = finish, showDate = true, showSeconds = true) {
            viewModel.setFinish(FinishCode.TIME, it)
        }
        FinishCodeDrop(selected = state.value.focus?.finishCode ?: FinishCode.TIME, customClasses = listOf(AppStyle.marginTop)) {
            when (it) {
                FinishCode.TIME -> viewModel.setFinish(FinishCode.TIME, state.value.focus?.finish)
                FinishCode.RET, FinishCode.DNF, FinishCode.NSC -> viewModel.setFinish(it, null)
                FinishCode.HOC -> viewModel.hoc(state.value.focus?.maxHoc?.plus(1) ?: 1)
            }
        }
        RgButton(label = "Penalty", customClasses = listOf(AppStyle.marginTop)) {
            viewModel.penalty(state.value.focus?.penalty?.plus(1) ?: 1)
        }
    } ?: run {
        P { Text(state.value.focus?.finishCode?.name ?: "--") }
        RgButton(label = "Reset") {
            viewModel.setFinish(FinishCode.TIME,resetFinish ?: now())
        }
    }
}

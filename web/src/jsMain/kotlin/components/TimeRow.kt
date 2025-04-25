package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.mxmariner.regatta.data.FinishCode
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.display
import utils.now
import viewmodel.RcViewModel

@Composable
fun RcTimeRow(
    viewModel: RcViewModel,
) {
    val state = viewModel.flow.collectAsState()
    state.value.focus?.raceStart?.let {
        P { Text("Class start: ${it.display()}") }
    }
    state.value.focus?.elapsedTime()?.let {
        P { Text("Elapsed time: $it") }
    }
    state.value.focus?.finish?.let { finish ->
        RgTime(date = finish, showDate = true, showSeconds = true) {
            viewModel.setFinish(FinishCode.TIME, it)
        }
        RgButton(label = "Penalty${state.value.focus?.penalty?.let { " $it" } ?: " 0"}", customClasses = listOf(
            AppStyle.marginTop,
            AppStyle.marginBot
        )) {
            viewModel.penalty(state.value.focus?.penalty?.plus(1) ?: 1)
        }
        state.value.focus?.penalty?.let {
            RgButton(
                style = RgButtonStyle.Danger,
                label = "-",
                customClasses = listOf(AppStyle.marginStart, AppStyle.marginBot, AppStyle.marginTop)
            ) {
                viewModel.penalty(it - 1)
            }
        }
    }

    FinishCodeDrop(
        selected = state.value.focus?.finishCode ?: FinishCode.TIME,
        hocPosition = state.value.focus?.hocPosition,
        customClasses = listOf(AppStyle.marginTop)
    ) {
        when (it) {
            FinishCode.TIME -> {
                viewModel.setFinish(FinishCode.TIME, state.value.focus?.restoreFinish ?: now())
            }

            FinishCode.RET, FinishCode.DNF, FinishCode.NSC -> {
                viewModel.setFinish(it, null)
            }

            FinishCode.HOC -> {
                viewModel.hoc(state.value.focus?.maxHoc?.plus(1) ?: 1)
            }
        }
    }
    state.value.focus?.hocPosition?.let {
        RgButton(
            style = RgButtonStyle.Danger,
            label = "+",
            customClasses = listOf(AppStyle.marginTop)
        ) {
            viewModel.hoc(it + 1)
        }
        if (it > 1) RgButton(
            style = RgButtonStyle.Danger,
            label = "-",
            customClasses = listOf(AppStyle.marginTop, AppStyle.marginStart)
        ) {
            viewModel.hoc(it - 1)
        }
    }
}

package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RaceSchedule
import com.mxmariner.regatta.display
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.display
import utils.timeStr
import viewmodel.RcViewModel
import kotlin.coroutines.CoroutineContext

//interface TimeRowHandler {
//    val timeFlow: Flow<TimeRowState>
//    val context: CoroutineContext
//    fun penalty(value: Int?)
//    fun hoc(value: Int?)
//    fun setFinish(value: Instant?)
//}
//
//interface TimeRowState {
//    val penalty: Int?
//    val hocPosition: Int?
//    val finish: Instant?
//    val raceSchedule: RaceSchedule?
//    val maxHoc: Int
//}

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
            viewModel.setFinish(resetFinish)
        }
        RgButton(label = "+", customClasses = listOf(AppStyle.marginStart)) {
            viewModel.hoc(it + 1)
        }
        RgButton(label = "-", customClasses = listOf(AppStyle.marginStart)) {
            viewModel.hoc(it - 1)
        }
    } ?: state.value.focus?.finish?.let { finish ->
        RgTime(date = finish, showDate = true, showSeconds = true) {
            viewModel.setFinish(it)
        }
        RgButton(label = "RET", customClasses = listOf(AppStyle.marginTop, AppStyle.marginEnd)) {
            viewModel.setFinish(null)
        }
        RgButton(label = "HOC", customClasses = listOf(AppStyle.marginTop, AppStyle.marginEnd)) {
            viewModel.hoc(state.value.focus?.maxHoc?.plus(1) ?: 1)
        }
        RgButton(label = "Penalty", customClasses = listOf(AppStyle.marginTop)) {
            viewModel.penalty(state.value.focus?.penalty?.plus(1) ?: 1)
        }
    } ?: run {
        P { Text("RET") }
        RgButton(label = "Reset") {
            viewModel.setFinish(resetFinish)
        }
    }
}


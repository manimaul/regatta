package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.display
import kotlinx.datetime.Instant
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.display
import utils.now
import viewmodel.RcViewModel

//@Composable
//fun RcTimeRow(
//    viewModel: RcViewModel,
//) {
//    val state by viewModel.flow.collectAsState()
//    state.focus?.let { f ->
//        TimeRow(
//            maxHoc = f.maxHoc,
//            finish = f.finish,
//            finishCode = f.finishCode,
//            showHocOption = true,
//            hocPosition = f.hocPosition,
//            penalty = f.penalty,
//            start = f.raceStart,
//            onFinish = { code, time ->
//                viewModel.setFinish(code, time)
//            },
//            onPenalty = {
//                viewModel.penalty(it)
//            },
//            onHoc = {
//                viewModel.hoc(it)
//            }
//        )
//    }
//}

@Composable
fun TimeRow(
    maxHoc: Int,
    finish: Instant?,
    finishCode: FinishCode,
    showHocOption: Boolean,
    hocPosition: Int?,
    penalty: Int?,
    start: Instant?,
    onFinish: (FinishCode, Instant?, Int?) -> Unit,
    onPenalty: (Int?) -> Unit,
) {
    finish?.let { finish ->
        RgTime(date = finish, showDate = true, showSeconds = true) {
            onFinish(FinishCode.TIME, it, null)
        }

        if (start != null) {
            P { Text("Elapsed time: ${(finish - start).display()}") }
        }


        RgButton(label = "Penalty${penalty?.let { " $it" } ?: " 0"}", customClasses = listOf(
            AppStyle.marginTop,
            AppStyle.marginBot
        )) {
            onPenalty(penalty?.let { it + 1 } ?: 1)
        }
        penalty?.let {
            RgButton(
                style = RgButtonStyle.Danger,
                label = "-",
                customClasses = listOf(AppStyle.marginStart, AppStyle.marginBot, AppStyle.marginTop)
            ) {
                onPenalty(it - 1)
            }
        }
    }
    FinishCodeDrop(
        selected = finishCode,
        hocPosition = hocPosition,
        showHoc = showHocOption,
        customClasses = listOf(AppStyle.marginTop)
    ) {
        when (it) {
            FinishCode.TIME -> onFinish(FinishCode.TIME, finish ?: now(), null) //todo: use race end time
            FinishCode.RET, FinishCode.DNF, FinishCode.DNS_RC,
            FinishCode.NSC -> onFinish(it, null, null)
            FinishCode.HOC -> onFinish(it, null, maxHoc)
        }
    }
    hocPosition?.let {
        RgButton(
            style = RgButtonStyle.Danger,
            label = "+",
            customClasses = listOf(AppStyle.marginTop)
        ) {
            onFinish(FinishCode.HOC, null, it + 1)
        }
        if (it > 1) RgButton(
            style = RgButtonStyle.Danger,
            label = "-",
            customClasses = listOf(AppStyle.marginTop, AppStyle.marginStart)
        ) {
            onFinish(FinishCode.HOC, null, it - 1)
        }
    }
}



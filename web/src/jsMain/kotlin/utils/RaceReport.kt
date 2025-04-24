package utils

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.display
import kotlinx.datetime.Instant

fun RaceResult.finishText(startTime: Instant?): String {
    return finish?.timeStr()?.takeIf { startTime != null }?.let { t ->
        penalty?.let { "$t P+$it" } ?: t
    } ?: finishCode.finishText(hocPosition)
}

fun FinishCode.finishText(hocPosition: Int?) : String {
    return when(val code = this) {
        FinishCode.TIME,
        FinishCode.RET,
        FinishCode.DNF,
        FinishCode.NSC -> code.name
        FinishCode.HOC -> "HOC $hocPosition"
    }
}

fun RaceReportCard.finishText(): String {
    return finishTime?.timeStr()?.takeIf { startTime != null }?.let { t ->
        penalty?.let { "$t P+$it" } ?: t
    } ?: resultRecord.result.finishCode.finishText(hocPosition)
}

fun RaceReportCard.elapsedText(): String {
    return elapsedTime?.display() ?: "n/a"
}

fun RaceReportCard.cfText(): String {
    return "${correctionFactor.asDynamic().toFixed(3)}"
}

fun RaceReportCard.corTimeText(): String {
    return correctedTime?.display()?.let { "$it (${cfText()})" } ?: "n/a"
}

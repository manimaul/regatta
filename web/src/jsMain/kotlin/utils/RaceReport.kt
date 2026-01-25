package utils

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.display
import kotlin.time.Instant
import kotlin.time.Duration

fun RaceResultFull.finishText(startTime: Instant?): String {
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
        FinishCode.HOC -> "HOC ${hocPosition ?: ""}"
        FinishCode.DNS_RC -> "DNS RC Volunteer"
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

private fun cfText(tcf: Double): String {
    return "${tcf.asDynamic().toFixed(4)}"
}

fun RaceReportCard.corTimePhrfText() = corTimeText(phrfTcf, correctedPhrfTime, "PHRF")
fun RaceReportCard.corTimeOrcText() = corTimeText(orcTcf,correctedOrcTime, "ORC")
private fun corTimeText(tcf: Double, duration: Duration?, label: String): String {
    return duration?.display()?.let { "$it (${cfText(tcf)} $label)" } ?: "n/a"
}

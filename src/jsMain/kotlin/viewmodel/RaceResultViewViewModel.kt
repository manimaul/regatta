package viewmodel

import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.data.RaceReportCard
import com.mxmariner.regatta.display
import utils.*

data class RaceReportState(
    val report: Async<RaceReport> = Loading()
) : VmState

class RaceResultViewViewModel(
    private val raceId: Long
) : BaseViewModel<RaceReportState>(RaceReportState()) {
    override fun reload() {
        setState { copy(report = Loading()) }
        setState { copy(report = Api.getReport(raceId).toAsync()) }
    }

    init {
        setState { copy(report = Api.getReport(raceId).toAsync()) }
    }
}

fun RaceReportCard.startText(): String {
    return resultRecord.result.startCode?.name ?: startTime?.display() ?: "error"
}

fun RaceReportCard.finishText(): String {
    return resultRecord.result.startCode?.name ?: finishTime?.displayTime()?.takeIf { startTime != null }
        ?: "RET".takeIf { startTime != null && hocPosition == null } ?: hocPosition?.let { "HOC $it" } ?: ""
}

fun RaceReportCard.elapsedText(): String {
    return elapsedTime?.display() ?: "n/a"
}

fun RaceReportCard.elapsedSecText(): String {
    return elapsedTime?.inWholeSeconds?.toString() ?: "n/a"
}

fun RaceReportCard.cfText(): String {
    return "${correctionFactor.asDynamic().toFixed(3)}"
}

fun RaceReportCard.corTimeText(): String {
    return correctedTime?.display()?.let { "$it (${cfText()})" } ?: "n/a"
}

fun RaceReportCard.corTimeSecText(): String {
    return correctedTime?.inWholeSeconds?.toString() ?: "n/a"
}

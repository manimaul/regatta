package utils

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.display
import kotlinx.datetime.Instant

fun RaceResult.finishText(startTime: Instant?): String {
    return finish?.timeStr()?.takeIf { startTime != null }?.let { t ->
        penalty?.let { "$t P+$it" } ?: t
    } ?: "RET".takeIf { startTime != null && hocPosition == null } ?: hocPosition?.let { "HOC $it" } ?: ""
}

//fun BoatSkipper.raceClass(schedule: RaceSchedule): RaceClass? {
//    boat?.let {
//        schedule.schedule.firstOrNull {
//
//        }
//    }
//}

fun RaceReportCard.startText(): String {
    return startTime?.display() ?: "error"
}

fun RaceReportCard.finishText(): String {
    return finishTime?.timeStr()?.takeIf { startTime != null }?.let { t ->
        penalty?.let { "$t P+$it" } ?: t
    } ?: "RET".takeIf { startTime != null && hocPosition == null } ?: hocPosition?.let { "HOC $it" } ?: ""
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

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
        FinishCode.NSC,
        FinishCode.DNS,
        FinishCode.DSQ -> code.name
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

// Place columns of the race results table. Each function returns the lines of one cell; the
// view renders them separated by <br>. Pure, so they are covered by jsTest.

/** "Place In Bracket" cell of a class with more than one bracket. */
fun RaceReportCard.bracketPlaceLines(showOrc: Boolean): List<String> {
    return when {
        !showOrc -> listOf(placeInBracket.toString())
        ratingType() == RatingType.ORC_PHRF && placeInBracketOrc != 0 ->
            listOf("${placeInBracket}(PHRF), ", "${placeInBracketOrc}(ORC)")
        ratingType().isPHRF -> listOf("${placeInBracket}(PHRF)")
        ratingType() == RatingType.ORC -> listOf("${placeInBracketOrc}(ORC)")
        else -> emptyList()
    }
}

/** "Place In Class" cell of a class with more than one bracket. */
fun RaceReportCard.classPlaceLines(showOrc: Boolean): List<String> {
    return when {
        !showOrc -> listOf(placeInClass.toString())
        ratingType() == RatingType.ORC_PHRF && placeInClassOrc != 0 ->
            listOf("${placeInClass}(PHRF), ", "${placeInClassOrc}(ORC)")
        ratingType().isPHRF -> listOf("${placeInClass}(PHRF)")
        ratingType() == RatingType.ORC -> listOf("${placeInClassOrc}(ORC)")
        else -> emptyList()
    }
}

/** The single "Place In Class" cell of a class with one bracket, where bracket place equals class place. */
fun RaceReportCard.singleBracketPlaceLines(): List<String> {
    return when {
        ratingType() == RatingType.ORC_PHRF && placeInBracketOrc != 0 ->
            listOf("${placeInBracket}(PHRF), ", "${placeInBracketOrc}(ORC)")
        ratingType().isPHRF -> listOf("${placeInBracket}(PHRF)")
        ratingType() == RatingType.ORC -> listOf("${placeInBracketOrc}(ORC)")
        else -> listOf(placeInBracket.toString())
    }
}

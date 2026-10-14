package utils

import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.RaceStatus
import com.mxmariner.regatta.data.StandingsBoatSkipper
import com.mxmariner.regatta.data.StandingsRace
import com.mxmariner.regatta.scoreText

/** Text that replaces the whole cell when the race was not sailed for this bracket or class (D2), else null. */
fun StandingsRace.statusText(): String? = when {
    status == RaceStatus.NotYetSailed -> "Not yet sailed"
    status == RaceStatus.Cancelled -> "Cancelled"
    !bracketSailed && !classSailed -> "Not sailed"
    else -> null
}

// Cell text and decoration for the series standings table. Pure, so they are covered by jsTest.
// Class names are literal strings so Tailwind's scanner finds them.

/** Bracket score, shown first in a race cell ("4", "3.5"); a dash when the race was not sailed in the bracket. */
fun StandingsRace.bracketScoreText(): String = if (bracketSailed) placeInBracket.scoreText() else "-"

/** Class score, shown after the bracket score when the class has more than one bracket; a dash when not sailed in the class. */
fun StandingsRace.classScoreText(): String = if (classSailed) ", ${placeInClass.scoreText()}" else ", -"

/** Decoration of the bracket score: red when thrown out of the bracket total. */
fun StandingsRace.bracketScoreClasses(): List<String> = if (throwOut) listOf("text-danger-fg") else emptyList()

/** Decoration of the class score: red when thrown out of the class total. */
fun StandingsRace.classScoreClasses(): List<String> = if (throwOutClass) listOf("text-danger-fg") else emptyList()

/** Finish-code suffix after the scores, e.g. " DNF", " HOC3", " DSQ", " DNS RC Volunteer"; " DNS" for no record; empty for a timed finish. */
fun StandingsRace.codeSuffix(): String {
    return when (val code = finishCode) {
        FinishCode.TIME -> ""
        FinishCode.RET,
        FinishCode.DNF,
        FinishCode.NSC,
        FinishCode.DNS,
        FinishCode.DSQ -> " ${code.name}"
        FinishCode.HOC -> " ${code.name}${hocPosition}"
        FinishCode.DNS_RC -> " DNS RC Volunteer"
        null -> " DNS"
    }
}

/** Bracket total, shown before the class total when the class has more than one bracket. */
fun StandingsBoatSkipper.totalBracketText(): String = "${totalScoreBracket.scoreText()}, "

/** Class total. */
fun StandingsBoatSkipper.totalClassText(): String = totalScoreClass.scoreText()

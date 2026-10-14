package utils

import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.StandingsBoatSkipper
import com.mxmariner.regatta.data.StandingsRace

// Cell text and decoration for the series standings table. Pure, so they are covered by jsTest.
// Class names are literal strings so Tailwind's scanner finds them.

/** Raw bracket score, shown first in a race cell. */
fun StandingsRace.bracketScoreText(): String = "$placeInBracket"

/** Raw class score, shown after the bracket score when the class has more than one bracket. */
fun StandingsRace.classScoreText(): String = ", $placeInClass"

/** The RC volunteer average that replaces the bracket score, or null when there is none. */
fun StandingsRace.bracketCorrectedText(): String? = placeInBracketCorrected?.let { " $it" }

/** The RC volunteer average that replaces the class score, or null when there is none. */
fun StandingsRace.classCorrectedText(): String? = placeInClassCorrected?.let { " $it" }

/** Decoration of the raw bracket score: red when thrown out, struck through when an RC average replaces it. */
fun StandingsRace.bracketScoreClasses(): List<String> = scoreClasses(throwOut, placeInBracketCorrected != null)

/** Decoration of the raw class score: red when thrown out, struck through when an RC average replaces it. */
fun StandingsRace.classScoreClasses(): List<String> = scoreClasses(throwOut, placeInClassCorrected != null)

/** Decoration of an RC volunteer average: red when the race is thrown out. */
fun StandingsRace.correctedScoreClasses(): List<String> = if (throwOut) listOf("text-danger-fg") else emptyList()

internal fun scoreClasses(throwOut: Boolean, corrected: Boolean): List<String> {
    return when {
        corrected && throwOut -> listOf("text-danger-fg", "line-through")
        throwOut -> listOf("text-danger-fg")
        corrected -> listOf("line-through")
        else -> emptyList()
    }
}

/** Finish-code suffix after the scores, e.g. " DNF", " HOC3", " DNS RC Volunteer"; empty for a timed finish. */
fun StandingsRace.codeSuffix(): String {
    return when (val code = finishCode) {
        FinishCode.TIME -> ""
        FinishCode.RET,
        FinishCode.DNF,
        FinishCode.NSC -> " ${code.name}"
        FinishCode.HOC -> " ${code.name}${hocPosition}"
        FinishCode.DNS_RC -> " DNS RC Volunteer"
        null -> " DNS"
    }
}

/** Bracket total, shown before the class total when the class has more than one bracket. */
fun StandingsBoatSkipper.totalBracketText(): String = "$totalScoreBracket, "

/** Class total. */
fun StandingsBoatSkipper.totalClassText(): String = "$totalScoreClass"

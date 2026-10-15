package utils

import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.RaceStatus
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.data.StandingsBoatSkipper
import com.mxmariner.regatta.data.StandingsRace
import com.mxmariner.regatta.scoreText

/** One decorated piece of a standings cell line. */
data class ScoreSegment(val text: String, val classes: List<String> = emptyList(), val bold: Boolean = false)

/** One line of a standings cell: its segments, then a plain suffix (a rating label and/or a finish code). */
data class ScoreLine(val segments: List<ScoreSegment>, val suffix: String = "")

/**
 * D9: which sides of a row the table shows, as (PHRF, ORC). The ORC side appears only when the
 * class is ORC-rated ([showOrc], the race view's gate) and the row is; the PHRF side appears
 * unless the class is ORC-rated and the row is ORC-only.
 */
private fun sides(rating: RatingType, showOrc: Boolean): Pair<Boolean, Boolean> =
    (!showOrc || rating.isPHRF) to (showOrc && rating.isORC)

/**
 * Text that replaces the whole cell when the race was not sailed for any group the row is scored
 * in (D2 / D9), else null. The default arguments are the PHRF side alone.
 */
fun StandingsRace.statusText(rating: RatingType = RatingType.PHRF, showOrc: Boolean = false): String? {
    val (phrf, orc) = sides(rating, showOrc)
    val sailed = (phrf && (bracketSailed || classSailed)) || (orc && (bracketSailedOrc || classSailedOrc))
    return when {
        status == RaceStatus.NotYetSailed -> "Not yet sailed"
        status == RaceStatus.Cancelled -> "Cancelled"
        !sailed -> "Not sailed"
        else -> null
    }
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

// D9: the same four for the ORC groups

fun StandingsRace.bracketScoreTextOrc(): String = if (bracketSailedOrc) placeInBracketOrc.scoreText() else "-"
fun StandingsRace.classScoreTextOrc(): String = if (classSailedOrc) ", ${placeInClassOrc.scoreText()}" else ", -"
fun StandingsRace.bracketScoreClassesOrc(): List<String> = if (throwOutOrc) listOf("text-danger-fg") else emptyList()
fun StandingsRace.classScoreClassesOrc(): List<String> = if (throwOutClassOrc) listOf("text-danger-fg") else emptyList()

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

/**
 * The race cell: one line per side the row is scored on (D9). Each line is the bold bracket score
 * and, in a class with more than one bracket, the bold class score, each red when thrown out of
 * its total, then the rating label when the class is ORC-rated. The finish code follows the last
 * line, except that a boat with no ORC entry that day is `DNS` on the ORC line, and the PHRF line
 * then carries the boat's own code (a finish shows nothing, no record shows `DNS` too).
 * Without [showOrc] this is the single unlabeled line the table has always shown.
 */
fun StandingsRace.scoreLines(showOrc: Boolean, multiBracket: Boolean, rating: RatingType): List<ScoreLine> {
    val (phrf, orc) = sides(rating, showOrc)
    val code = codeSuffix()
    // the ORC line says DNS itself when the boat had no ORC entry; the PHRF line then keeps its own code
    val orcDns = orc && nonStarterOrc
    fun line(bracket: String, bracketClasses: List<String>, klass: String, classClasses: List<String>, suffix: String) = ScoreLine(
        listOfNotNull(
            ScoreSegment(bracket, bracketClasses, bold = true),
            if (multiBracket) ScoreSegment(klass, classClasses, bold = true) else null,
        ),
        suffix,
    )
    return listOfNotNull(
        if (phrf) line(
            bracketScoreText(), bracketScoreClasses(), classScoreText(), classScoreClasses(),
            (if (showOrc) " (PHRF)" else "") + (if (!orc || orcDns) code else ""),
        ) else null,
        if (orc) line(
            bracketScoreTextOrc(), bracketScoreClassesOrc(), classScoreTextOrc(), classScoreClassesOrc(),
            " (ORC)" + (if (orcDns) " DNS" else code),
        ) else null,
    )
}

/**
 * The "Total Score" cell: one line per side (D9), each the bold bracket total (in a class with more
 * than one bracket) and the class total, labeled with the rating when the class is ORC-rated.
 */
fun StandingsBoatSkipper.totalLines(showOrc: Boolean, multiBracket: Boolean): List<ScoreLine> {
    val (phrf, orc) = sides(ratingType, showOrc)
    fun line(bracket: Double, klass: Double, label: String) = ScoreLine(
        listOfNotNull(
            if (multiBracket) ScoreSegment("${bracket.scoreText()}, ", bold = true) else null,
            ScoreSegment(klass.scoreText()),
        ),
        label,
    )
    return listOfNotNull(
        if (phrf) line(totalScoreBracket, totalScoreClass, if (showOrc) " (PHRF)" else "") else null,
        if (orc) line(totalScoreBracketOrc, totalScoreClassOrc, " (ORC)") else null,
    )
}

/** A place cell in the race view's notation: `2(PHRF), ` ⏎ `1(ORC)`, `2(PHRF)`, `1(ORC)`, or the bare place outside an ORC class. */
private fun placeLines(phrfPlace: Int, orcPlace: Int, rating: RatingType, showOrc: Boolean): List<String> = when {
    !showOrc -> listOf("$phrfPlace")
    rating == RatingType.ORC_PHRF && orcPlace != 0 -> listOf("$phrfPlace(PHRF), ", "$orcPlace(ORC)")
    rating.isPHRF -> listOf("$phrfPlace(PHRF)")
    rating == RatingType.ORC -> listOf("$orcPlace(ORC)")
    else -> listOf("$phrfPlace")
}

/** The "Place In Bracket" cell. */
fun StandingsBoatSkipper.bracketPlaceLines(showOrc: Boolean): List<String> = placeLines(placeInBracket, placeInBracketOrc, ratingType, showOrc)

/** The "Place In Class" cell. */
fun StandingsBoatSkipper.classPlaceLines(showOrc: Boolean): List<String> = placeLines(placeInClass, placeInClassOrc, ratingType, showOrc)

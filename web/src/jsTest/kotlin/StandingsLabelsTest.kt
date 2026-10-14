import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.StandingsBoatSkipper
import com.mxmariner.regatta.data.StandingsRace
import utils.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Series standings cell text and decoration (utils/StandingsReport.kt). */
class StandingsLabelsTest {

    private fun race(
        code: FinishCode? = FinishCode.TIME,
        bracket: Int = 3,
        klass: Int = 5,
        throwOut: Boolean = false,
        bracketCorrected: Int? = null,
        classCorrected: Int? = null,
        hoc: Int? = null,
    ) = StandingsRace(
        finish = code == FinishCode.TIME,
        placeInBracket = bracket,
        placeInClass = klass,
        placeInBracketCorrected = bracketCorrected,
        placeInClassCorrected = classCorrected,
        throwOut = throwOut,
        finishCode = code,
        hocPosition = hoc,
        nonStarter = code == null,
    )

    @Test
    fun scoreText() {
        assertEquals("3", race().bracketScoreText())
        assertEquals(", 5", race().classScoreText())
    }

    @Test
    fun correctedTextOnlyWhenAnRcAverageExists() {
        assertNull(race().bracketCorrectedText())
        assertNull(race().classCorrectedText())
        assertEquals(" 2", race(bracketCorrected = 2).bracketCorrectedText())
        assertEquals(" 4", race(classCorrected = 4).classCorrectedText())
    }

    @Test
    fun plainScoreHasNoDecoration() {
        assertEquals(emptyList(), race().bracketScoreClasses())
        assertEquals(emptyList(), race().classScoreClasses())
        assertEquals(emptyList(), race().correctedScoreClasses())
    }

    @Test
    fun throwOutIsRed() {
        assertEquals(listOf("text-danger-fg"), race(throwOut = true).bracketScoreClasses())
        assertEquals(listOf("text-danger-fg"), race(throwOut = true).classScoreClasses())
        assertEquals(listOf("text-danger-fg"), race(throwOut = true).correctedScoreClasses())
    }

    @Test
    fun rcAverageStrikesThroughTheRawScore() {
        assertEquals(listOf("line-through"), race(code = FinishCode.DNS_RC, bracketCorrected = 2).bracketScoreClasses())
        assertEquals(listOf("line-through"), race(code = FinishCode.DNS_RC, classCorrected = 4).classScoreClasses())
        // the bracket and class decorations are independent
        assertEquals(emptyList(), race(code = FinishCode.DNS_RC, bracketCorrected = 2).classScoreClasses())
    }

    @Test
    fun thrownOutRcAverageIsRedAndStruckThrough() {
        val r = race(code = FinishCode.DNS_RC, throwOut = true, bracketCorrected = 2, classCorrected = 4)
        assertEquals(listOf("text-danger-fg", "line-through"), r.bracketScoreClasses())
        assertEquals(listOf("text-danger-fg", "line-through"), r.classScoreClasses())
        assertEquals(listOf("text-danger-fg"), r.correctedScoreClasses())
    }

    @Test
    fun codeSuffixPerFinishCode() {
        assertEquals("", race(FinishCode.TIME).codeSuffix())
        assertEquals(" RET", race(FinishCode.RET).codeSuffix())
        assertEquals(" DNF", race(FinishCode.DNF).codeSuffix())
        assertEquals(" NSC", race(FinishCode.NSC).codeSuffix())
        assertEquals(" HOC3", race(FinishCode.HOC, hoc = 3).codeSuffix())
        assertEquals(" DNS RC Volunteer", race(FinishCode.DNS_RC).codeSuffix())
        assertEquals(" DNS", race(code = null).codeSuffix())
        FinishCode.entries.forEach { race(it, hoc = 1).codeSuffix() } // every code has a suffix
    }

    @Test
    fun totals() {
        val row = StandingsBoatSkipper(totalScoreBracket = 12, totalScoreClass = 17)
        assertEquals("12, ", row.totalBracketText())
        assertEquals("17", row.totalClassText())
    }
}

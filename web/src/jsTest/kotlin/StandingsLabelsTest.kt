import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.RaceStatus
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
        bracket: Double = 3.0,
        klass: Double = 5.0,
        throwOut: Boolean = false,
        hoc: Int? = null,
        bracketSailed: Boolean = true,
        classSailed: Boolean = true,
        status: RaceStatus = RaceStatus.Sailed,
        throwOutClass: Boolean = throwOut,
    ) = StandingsRace(
        finish = code == FinishCode.TIME,
        placeInBracket = bracket,
        placeInClass = klass,
        throwOut = throwOut,
        throwOutClass = throwOutClass,
        finishCode = code,
        hocPosition = hoc,
        nonStarter = code == null,
        bracketSailed = bracketSailed,
        classSailed = classSailed,
        status = status,
    )

    @Test
    fun scoreText() {
        assertEquals("3", race().bracketScoreText())
        assertEquals(", 5", race().classScoreText())
    }

    /** D1: an RC volunteer average is fractional and shown with one or two decimals. */
    @Test
    fun fractionalScoresAreFormatted() {
        assertEquals("3.5", race(FinishCode.DNS_RC, bracket = 3.5).bracketScoreText())
        assertEquals(", 3.33", race(FinishCode.DNS_RC, klass = 10.0 / 3).classScoreText())
        assertEquals("4", race(bracket = 4.0).bracketScoreText())
        val row = StandingsBoatSkipper(totalScoreBracket = 10.5, totalScoreClass = 32.0 / 3)
        assertEquals("10.5, ", row.totalBracketText())
        assertEquals("10.67", row.totalClassText())
    }

    @Test
    fun plainScoreHasNoDecoration() {
        assertEquals(emptyList(), race().bracketScoreClasses())
        assertEquals(emptyList(), race().classScoreClasses())
    }

    @Test
    fun throwOutIsRed() {
        assertEquals(listOf("text-danger-fg"), race(throwOut = true).bracketScoreClasses())
        assertEquals(listOf("text-danger-fg"), race(throwOut = true).classScoreClasses())
    }

    @Test
    fun codeSuffixPerFinishCode() {
        assertEquals("", race(FinishCode.TIME).codeSuffix())
        assertEquals(" RET", race(FinishCode.RET).codeSuffix())
        assertEquals(" DNF", race(FinishCode.DNF).codeSuffix())
        assertEquals(" NSC", race(FinishCode.NSC).codeSuffix())
        assertEquals(" HOC3", race(FinishCode.HOC, hoc = 3).codeSuffix())
        assertEquals(" DNS RC Volunteer", race(FinishCode.DNS_RC).codeSuffix())
        assertEquals(" DNS", race(FinishCode.DNS).codeSuffix())
        assertEquals(" DSQ", race(FinishCode.DSQ).codeSuffix())
        assertEquals(" DNS", race(code = null).codeSuffix())
        FinishCode.entries.forEach { race(it, hoc = 1).codeSuffix() } // every code has a suffix
    }

    @Test
    fun statusTextReplacesTheCellForUnsailedRaces() {
        assertNull(race().statusText())
        assertEquals("Not yet sailed", race(code = null, status = RaceStatus.NotYetSailed, bracketSailed = false, classSailed = false).statusText())
        assertEquals("Cancelled", race(code = null, status = RaceStatus.Cancelled, bracketSailed = false, classSailed = false).statusText())
        assertEquals("Not sailed", race(code = null, bracketSailed = false, classSailed = false).statusText())
        // sailed in the class but not the bracket: the cell still shows, with a dash for the bracket
        assertNull(race(code = null, bracketSailed = false, classSailed = true).statusText())
    }

    @Test
    fun unsailedGroupShowsADash() {
        assertEquals("-", race(code = null, bracketSailed = false).bracketScoreText())
        assertEquals(", -", race(code = null, classSailed = false).classScoreText())
    }

    @Test
    fun classThrowOutDecoratesOnlyTheClassScore() {
        val r = race(throwOut = false, throwOutClass = true)
        assertEquals(emptyList(), r.bracketScoreClasses())
        assertEquals(listOf("text-danger-fg"), r.classScoreClasses())
    }

    @Test
    fun totals() {
        val row = StandingsBoatSkipper(totalScoreBracket = 12.0, totalScoreClass = 17.0)
        assertEquals("12, ", row.totalBracketText())
        assertEquals("17", row.totalClassText())
    }
}

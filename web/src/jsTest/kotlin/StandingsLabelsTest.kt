import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.RaceStatus
import com.mxmariner.regatta.data.RatingType
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
        bracketOrc: Double = 0.0,
        classOrc: Double = 0.0,
        throwOutOrc: Boolean = false,
        throwOutClassOrc: Boolean = throwOutOrc,
        bracketSailedOrc: Boolean = false,
        classSailedOrc: Boolean = bracketSailedOrc,
        nonStarterOrc: Boolean = false,
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
        placeInBracketOrc = bracketOrc,
        placeInClassOrc = classOrc,
        throwOutOrc = throwOutOrc,
        throwOutClassOrc = throwOutClassOrc,
        bracketSailedOrc = bracketSailedOrc,
        classSailedOrc = classSailedOrc,
        nonStarterOrc = nonStarterOrc,
    )

    /** An ORC/PHRF race cell: PHRF 3 / 5, ORC 2 / 2, both groups sailed. */
    private fun orcRace(
        code: FinishCode? = FinishCode.TIME,
        throwOut: Boolean = false,
        throwOutOrc: Boolean = false,
        nonStarterOrc: Boolean = false,
        bracketSailedOrc: Boolean = true,
    ) = race(code = code, throwOut = throwOut, bracketOrc = 2.0, classOrc = 2.0, throwOutOrc = throwOutOrc, bracketSailedOrc = bracketSailedOrc, nonStarterOrc = nonStarterOrc)

    private val red = listOf("text-danger-fg")
    private fun bold(text: String, classes: List<String> = emptyList()) = ScoreSegment(text, classes, bold = true)
    private fun plain(text: String) = ScoreSegment(text)

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

    // ---- D9: the ORC side

    @Test
    fun orcScoreTextAndDecoration() {
        assertEquals("2", orcRace().bracketScoreTextOrc())
        assertEquals(", 2", orcRace().classScoreTextOrc())
        assertEquals("-", race(bracketSailedOrc = false).bracketScoreTextOrc())
        assertEquals(", -", race(classSailedOrc = false).classScoreTextOrc())
        assertEquals(emptyList(), orcRace().bracketScoreClassesOrc())
        assertEquals(red, orcRace(throwOutOrc = true).bracketScoreClassesOrc())
        assertEquals(red, orcRace(throwOutOrc = true).classScoreClassesOrc())
    }

    /** Without the ORC gate the race cell is the single unlabeled line it has always been, whatever the row's rating. */
    @Test
    fun scoreLinesWithoutOrcIsTheSingleUnlabeledLine() {
        assertEquals(listOf(ScoreLine(listOf(bold("3"), bold(", 5")))), orcRace().scoreLines(showOrc = false, multiBracket = true, rating = RatingType.ORC_PHRF))
        assertEquals(listOf(ScoreLine(listOf(bold("3")), " DNF")), orcRace(FinishCode.DNF).scoreLines(showOrc = false, multiBracket = false, rating = RatingType.PHRF))
        assertEquals(listOf(ScoreLine(listOf(bold("3", red)), "")), race(throwOut = true).scoreLines(showOrc = false, multiBracket = false, rating = RatingType.CruisingFlyingSails))
    }

    @Test
    fun scoreLinesPerRating() {
        // both sides, the finish code on the last line
        assertEquals(
            listOf(ScoreLine(listOf(bold("3"), bold(", 5")), " (PHRF)"), ScoreLine(listOf(bold("2"), bold(", 2")), " (ORC) DNF")),
            orcRace(FinishCode.DNF).scoreLines(showOrc = true, multiBracket = true, rating = RatingType.ORC_PHRF),
        )
        // a PHRF-only row in an ORC class
        assertEquals(listOf(ScoreLine(listOf(bold("3")), " (PHRF) DNF")), orcRace(FinishCode.DNF).scoreLines(showOrc = true, multiBracket = false, rating = RatingType.PHRF))
        // an ORC-only row
        assertEquals(listOf(ScoreLine(listOf(bold("2")), " (ORC)")), orcRace().scoreLines(showOrc = true, multiBracket = false, rating = RatingType.ORC))
    }

    /** The PHRF and ORC throw-outs can be different races, so each line carries its own decoration. */
    @Test
    fun scoreLinesDecorateEachSideOnItsOwn() {
        val phrfOut = orcRace(throwOut = true).scoreLines(showOrc = true, multiBracket = false, rating = RatingType.ORC_PHRF)
        assertEquals(listOf(ScoreLine(listOf(bold("3", red)), " (PHRF)"), ScoreLine(listOf(bold("2")), " (ORC)")), phrfOut)
        val orcOut = orcRace(throwOutOrc = true).scoreLines(showOrc = true, multiBracket = false, rating = RatingType.ORC_PHRF)
        assertEquals(listOf(ScoreLine(listOf(bold("3")), " (PHRF)"), ScoreLine(listOf(bold("2", red)), " (ORC)")), orcOut)
    }

    /** A boat with a finish but no ORC entry that day is DNS on the ORC line only. */
    @Test
    fun scoreLinesMarkAnOrcNonStarterOnTheOrcLine() {
        val lines = orcRace(nonStarterOrc = true).scoreLines(showOrc = true, multiBracket = false, rating = RatingType.ORC_PHRF)
        assertEquals(listOf(" (PHRF)", " (ORC) DNS"), lines.map { it.suffix })
        // with no record at all both lines say DNS
        val none = orcRace(code = null, nonStarterOrc = true).scoreLines(showOrc = true, multiBracket = false, rating = RatingType.ORC_PHRF)
        assertEquals(listOf(" (PHRF) DNS", " (ORC) DNS"), none.map { it.suffix })
    }

    @Test
    fun scoreLinesShowADashForAnUnsailedOrcGroup() {
        val lines = orcRace(bracketSailedOrc = false).scoreLines(showOrc = true, multiBracket = true, rating = RatingType.ORC_PHRF)
        assertEquals(listOf(bold("-"), bold(", -")), lines[1].segments)
    }

    @Test
    fun statusTextPerRating() {
        // an ORC-only row in a race its ORC group did not sail
        assertEquals("Not sailed", race(code = null, bracketSailedOrc = false).statusText(RatingType.ORC, showOrc = true))
        assertNull(race(code = null, bracketSailedOrc = true).statusText(RatingType.ORC, showOrc = true))
        // an ORC/PHRF row shows the cell when either side sailed
        assertNull(race(code = null, bracketSailed = false, classSailed = false, bracketSailedOrc = true).statusText(RatingType.ORC_PHRF, showOrc = true))
        assertEquals("Not sailed", race(code = null, bracketSailed = false, classSailed = false).statusText(RatingType.ORC_PHRF, showOrc = true))
        // outside the gate only the PHRF side counts
        assertEquals("Not sailed", race(code = null, bracketSailed = false, classSailed = false, bracketSailedOrc = true).statusText(RatingType.ORC_PHRF, showOrc = false))
    }

    @Test
    fun totalLinesPerRating() {
        val row = StandingsBoatSkipper(totalScoreBracket = 12.0, totalScoreClass = 17.0, totalScoreBracketOrc = 9.0, totalScoreClassOrc = 11.0, ratingType = RatingType.ORC_PHRF)
        assertEquals(listOf(ScoreLine(listOf(plain("17")))), row.totalLines(showOrc = false, multiBracket = false))
        assertEquals(listOf(ScoreLine(listOf(bold("12, "), plain("17")))), row.totalLines(showOrc = false, multiBracket = true))
        assertEquals(
            listOf(ScoreLine(listOf(bold("12, "), plain("17")), " (PHRF)"), ScoreLine(listOf(bold("9, "), plain("11")), " (ORC)")),
            row.totalLines(showOrc = true, multiBracket = true),
        )
        assertEquals(listOf(ScoreLine(listOf(plain("17")), " (PHRF)"), ScoreLine(listOf(plain("11")), " (ORC)")), row.totalLines(showOrc = true, multiBracket = false))
        assertEquals(listOf(ScoreLine(listOf(plain("11")), " (ORC)")), row.copy(ratingType = RatingType.ORC).totalLines(showOrc = true, multiBracket = false))
        assertEquals(listOf(ScoreLine(listOf(plain("17")), " (PHRF)")), row.copy(ratingType = RatingType.PHRF).totalLines(showOrc = true, multiBracket = false))
    }

    @Test
    fun placeLinesPerRating() {
        val row = StandingsBoatSkipper(placeInBracket = 2, placeInClass = 4, placeInBracketOrc = 1, placeInClassOrc = 3, ratingType = RatingType.ORC_PHRF)
        assertEquals(listOf("2"), row.bracketPlaceLines(showOrc = false))
        assertEquals(listOf("4"), row.classPlaceLines(showOrc = false))
        assertEquals(listOf("2(PHRF), ", "1(ORC)"), row.bracketPlaceLines(showOrc = true))
        assertEquals(listOf("4(PHRF), ", "3(ORC)"), row.classPlaceLines(showOrc = true))
        assertEquals(listOf("2(PHRF)"), row.copy(ratingType = RatingType.PHRF).bracketPlaceLines(showOrc = true))
        assertEquals(listOf("1(ORC)"), row.copy(ratingType = RatingType.ORC).bracketPlaceLines(showOrc = true))
        // an ORC/PHRF row without an ORC place yet falls back to the PHRF place
        assertEquals(listOf("2(PHRF)"), row.copy(placeInBracketOrc = 0).bracketPlaceLines(showOrc = true))
    }
}

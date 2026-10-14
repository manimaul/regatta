import com.mxmariner.regatta.data.*
import utils.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Race results labels: finish text, elapsed and corrected time text, and the place columns (utils/RaceReport.kt). */
class RaceReportLabelsTest {

    private val start = Instant.fromEpochSeconds(1_780_000_000)
    private val finish = start + 2.hours + 30.minutes + 15.seconds

    private fun card(
        code: FinishCode = FinishCode.TIME,
        finishTime: Instant? = null,
        startTime: Instant? = null,
        hoc: Int? = null,
        penalty: Int? = null,
    ) = RaceReportCard(
        resultRecord = RaceResultBoatBracket(result = RaceResult(finish = finishTime, finishCode = code, hocPosition = hoc, penalty = penalty)),
        startTime = startTime,
        finishTime = finishTime,
        hocPosition = hoc,
        penalty = penalty,
    )

    @Test
    fun finishCodeTextForEveryCode() {
        assertEquals("TIME", FinishCode.TIME.finishText(null))
        assertEquals("RET", FinishCode.RET.finishText(null))
        assertEquals("DNF", FinishCode.DNF.finishText(null))
        assertEquals("NSC", FinishCode.NSC.finishText(null))
        assertEquals("HOC 3", FinishCode.HOC.finishText(3))
        assertEquals("HOC ", FinishCode.HOC.finishText(null))
        assertEquals("DNS RC Volunteer", FinishCode.DNS_RC.finishText(null))
        FinishCode.entries.forEach { it.finishText(1) } // every code has a label
    }

    @Test
    fun cardFinishTextShowsTheLocalFinishTime() {
        assertEquals(finish.timeStr(), card(finishTime = finish, startTime = start).finishText())
    }

    @Test
    fun cardFinishTextAppendsThePenalty() {
        assertEquals("${finish.timeStr()} P+2", card(finishTime = finish, startTime = start, penalty = 2).finishText())
    }

    @Test
    fun cardFinishTextWithoutAStartFallsBackToTheCode() {
        assertEquals("TIME", card(finishTime = finish, startTime = null).finishText())
        assertEquals("DNF", card(code = FinishCode.DNF, startTime = start).finishText())
        assertEquals("HOC 2", card(code = FinishCode.HOC, startTime = start, hoc = 2).finishText())
    }

    @Test
    fun fullResultFinishText() {
        val full = RaceResultFull(finish = finish, finishCode = FinishCode.TIME, penalty = 1)
        assertEquals("${finish.timeStr()} P+1", full.finishText(start))
        assertEquals("TIME", full.finishText(null))
        assertEquals("DNS RC Volunteer", RaceResultFull(finishCode = FinishCode.DNS_RC).finishText(start))
    }

    @Test
    fun elapsedText() {
        assertEquals("n/a", RaceReportCard().elapsedText())
        assertEquals("2h:30m:15s", RaceReportCard(elapsedTime = 2.hours + 30.minutes + 15.seconds).elapsedText())
    }

    @Test
    fun correctedTimeTextWithTcfToFourDecimals() {
        val card = RaceReportCard(
            phrfTcf = 650.0 / 648,
            orcTcf = 0.98765432,
            correctedPhrfTime = 1.hours,
            correctedOrcTime = 59.minutes + 15.seconds,
        )
        assertEquals("1h:0m:0s (1.0031 PHRF)", card.corTimePhrfText())
        assertEquals("0h:59m:15s (0.9877 ORC)", card.corTimeOrcText())
    }

    @Test
    fun correctedTimeTextWithoutATimeIsNa() {
        assertEquals("n/a", RaceReportCard(phrfTcf = 1.5).corTimePhrfText())
        assertEquals("n/a", RaceReportCard(orcTcf = 1.5).corTimeOrcText())
    }

    private fun placed(ratingType: RatingType, orc: Boolean = true) = RaceReportCard(
        resultRecord = RaceResultBoatBracket(result = RaceResult(ratingType = ratingType)),
        placeInBracket = 3,
        placeInClass = 2,
        placeInBracketOrc = if (orc) 4 else 0,
        placeInClassOrc = if (orc) 5 else 0,
    )

    @Test
    fun multiBracketPlacesWithoutOrcAreTheBareNumbers() {
        RatingType.entries.forEach { rt ->
            assertEquals(listOf("3"), placed(rt).bracketPlaceLines(showOrc = false), rt.name)
            assertEquals(listOf("2"), placed(rt).classPlaceLines(showOrc = false), rt.name)
        }
    }

    @Test
    fun multiBracketPlacesForPhrfInAnOrcClass() {
        assertEquals(listOf("3(PHRF)"), placed(RatingType.PHRF).bracketPlaceLines(showOrc = true))
        assertEquals(listOf("2(PHRF)"), placed(RatingType.PHRF).classPlaceLines(showOrc = true))
    }

    @Test
    fun multiBracketPlacesForOrc() {
        assertEquals(listOf("4(ORC)"), placed(RatingType.ORC).bracketPlaceLines(showOrc = true))
        assertEquals(listOf("5(ORC)"), placed(RatingType.ORC).classPlaceLines(showOrc = true))
    }

    @Test
    fun multiBracketPlacesForOrcPhrfBracketColumn() {
        assertEquals(listOf("3(PHRF), ", "4(ORC)"), placed(RatingType.ORC_PHRF).bracketPlaceLines(showOrc = true))
        // without an ORC place it reads as PHRF
        assertEquals(listOf("3(PHRF)"), placed(RatingType.ORC_PHRF, orc = false).bracketPlaceLines(showOrc = true))
        assertEquals(listOf("2(PHRF)"), placed(RatingType.ORC_PHRF, orc = false).classPlaceLines(showOrc = true))
    }

    /** D7: the class column must show the ORC place in class, not in bracket. Ignored until D7 lands, see docs/explore_unimplemented/test_audit_and_plan.md. */
    @Ignore
    @Test
    fun d7_multiBracketOrcPhrfClassColumnShowsClassOrcPlace() {
        assertEquals(listOf("2(PHRF), ", "5(ORC)"), placed(RatingType.ORC_PHRF).classPlaceLines(showOrc = true))
    }

    @Test
    fun multiBracketPlacesForCruisingInAnOrcClassAreEmpty() {
        assertEquals(emptyList(), placed(RatingType.CruisingFlyingSails).bracketPlaceLines(showOrc = true))
        assertEquals(emptyList(), placed(RatingType.CruisingNonFlyingSails).classPlaceLines(showOrc = true))
    }

    @Test
    fun singleBracketPlaces() {
        assertEquals(listOf("3(PHRF)"), placed(RatingType.PHRF).singleBracketPlaceLines())
        assertEquals(listOf("4(ORC)"), placed(RatingType.ORC).singleBracketPlaceLines())
        assertEquals(listOf("3(PHRF), ", "4(ORC)"), placed(RatingType.ORC_PHRF).singleBracketPlaceLines())
        assertEquals(listOf("3(PHRF)"), placed(RatingType.ORC_PHRF, orc = false).singleBracketPlaceLines())
        assertEquals(listOf("3"), placed(RatingType.CruisingFlyingSails).singleBracketPlaceLines())
        assertEquals(listOf("3"), placed(RatingType.CruisingNonFlyingSails).singleBracketPlaceLines())
    }
}

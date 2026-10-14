package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.display
import com.mxmariner.regatta.db.RaceResultsTable
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Elapsed time, PHRF time-on-time (GSI 14.3), ORC correction and the season switch. */
class CorrectedTimeTest {

    private fun RaceReport.card(boat: String): RaceReportCard =
        classReports.flatMap { it.bracketReport }.flatMap { it.cards }.first { it.boatName == boat }

    @Test
    fun elapsedTimeIsFromTheBoatsOwnClassStart() {
        val report = race(start = "18:00") {
            phrfClass("Fast", start = "18:00") { bracket("I") { boat("A", rating = 50) finishes "20:00:00" } }
            phrfClass("Half Fast", start = "18:05") { bracket("II") { boat("B", rating = 50) finishes "20:00:00" } }
        }.report()
        assertEquals(2.hours, report.card("A").elapsedTime)
        assertEquals(1.hours + 55.minutes, report.card("B").elapsedTime)
        assertEquals(report.classStart(report.card("B").resultRecord.bracket.classId), report.card("B").startTime)
    }

    /** ToT: corrected = elapsed × 650 / (B + rating), for each published B-factor. */
    @Test
    fun timeOnTimeForEachBFactor() {
        PhrfBFactor.entries.forEach { b ->
            val report = race(bFactor = b.cf) {
                phrfClass("Fast") { bracket("I") { boat("A", rating = 48) finishes "19:00:00" } }
            }.report()
            val card = report.card("A")
            val tcf = 650.0 / (b.cf + 48)
            assertEquals(tcf, card.phrfTcf, b.name)
            assertEquals((3_600_000 * tcf).toLong(), card.correctedPhrfTime?.inWholeMilliseconds, b.name)
        }
    }

    @Test
    fun timeOnTimeKnownValues() {
        val report = race(bFactor = 600) {
            phrfClass("Fast") { bracket("I") {
                boat("Even", rating = 50) finishes "19:00:00"   // 650 / 650 = 1.0
                boat("Fast", rating = 48) finishes "19:00:00"   // 650 / 648
            } }
        }.report()
        assertEquals(1.0, report.card("Even").phrfTcf)
        assertEquals(3_600_000, report.card("Even").correctedPhrfTime?.inWholeMilliseconds)
        assertEquals(3_611_111, report.card("Fast").correctedPhrfTime?.inWholeMilliseconds)
    }

    @Test
    fun cruisingTcfIsOne() {
        val report = race(bFactor = 480) {
            cruisingClass("Windseekers", flying = true) { bracket("A") { boat("A", rating = 48) finishes "19:00:00" } }
        }.report()
        assertEquals(1.0, report.card("A").phrfTcf)
        assertEquals(1.hours, report.card("A").correctedPhrfTime)
    }

    /** D8 legacy behavior: a PHRF result saved without a rating scores with TCF 1.0. */
    @Test
    fun nullRatingOnLegacyRowGetsTcfOne() {
        val report = race(bFactor = 600) {
            phrfClass("Fast") { bracket("I") { boat("A") finishes "19:00:00" } }
        }.report()
        assertNull(report.card("A").phrfRating)
        assertEquals(1.0, report.card("A").phrfTcf)
        assertEquals(1.hours, report.card("A").correctedPhrfTime)
    }

    @Test
    fun finishBeforeStartGivesNegativeElapsedTime() {
        val report = race(start = "18:00") {
            phrfClass("Fast") { bracket("I") { boat("A", rating = 50) finishes "17:00:00" } }
        }.report()
        assertEquals((-1).hours, report.card("A").elapsedTime)
        assertEquals((-1).hours, report.card("A").correctedPhrfTime)
    }

    @Test
    fun elapsedTimeOver24Hours() {
        val report = race(date = "2026-06-17", start = "18:00") {
            phrfClass("Fast") { bracket("I") { boat("A", rating = 50) finishes "2026-06-18 19:00:00" } }
        }.report()
        assertEquals(25.hours, report.card("A").elapsedTime)
        assertEquals("25h:0m:0s", report.card("A").elapsedTime?.display())
    }

    @Test
    fun millisecondPrecisionIsKept() {
        val report = race(start = "18:00") {
            phrfClass("Fast") { bracket("I") {
                boat("A", rating = 50) finishes "20:00:00.250"
                boat("B", rating = 50) finishes "20:00:00.251"
            } }
        }.report()
        assertEquals(2.hours + 250.milliseconds, report.card("A").elapsedTime)
        assertEquals(7_200_250, report.card("A").correctedPhrfTime?.inWholeMilliseconds)
        assertEquals(7_200_251, report.card("B").correctedPhrfTime?.inWholeMilliseconds)
        report.assertBracketScores("A" to 1, "B" to 2)
    }

    @Test
    fun orcCorrectedTimeAlongsidePhrfForOrcPhrf() {
        val cert = OrcCertificate(refNo = "C1", fiveBandWlLowMedTot = 0.9, fiveBandWlMedTot = 0.8)
        val report = race(bFactor = 600, orcScoringOption = OrcScoringOption.FiveBandWindwardLeeward) {
            orcPhrfClass("Fast") { bracket("I") {
                boat("A", rating = 50) finishes "19:00:00" orc "C1"
                boat("B", rating = 50, ratingType = RatingType.PHRF) finishes "19:00:00"
            } }
        }.report(mapOf("C1" to cert))
        val a = report.card("A")
        assertEquals(1.0, a.phrfTcf)
        assertEquals(1.hours, a.correctedPhrfTime)
        assertEquals(0.9, a.orcTcf) // B 600 -> Orc5Band.LowMedium
        assertEquals(3_240_000, a.correctedOrcTime?.inWholeMilliseconds)
        val b = report.card("B")
        assertEquals(1.0, b.orcTcf)
        assertEquals(1.hours, b.correctedOrcTime)
    }

    private fun classSchedule(ratingType: RatingType, start: Instant) = ClassSchedule(
        raceClass = RaceClass(id = 1, name = "Fast", ratingType = ratingType),
        startDate = start,
    )

    /** The 2026 season starts in Pacific time: 2025-12-31 23:00 Pacific is 2026-01-01 07:00 UTC but still 2025. */
    @Test
    fun seasonSwitchUsesPacificYear() {
        val lateNewYearsEve = pacific("2025-12-31", "23:00")
        val earlyNewYear = pacific("2026-01-01", "00:30")
        assertEquals(RatingType.PHRF, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.ORC_PHRF, lateNewYearsEve)).ratingType)
        assertEquals(RatingType.ORC_PHRF, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.ORC_PHRF, earlyNewYear)).ratingType)
    }

    @Test
    fun seasonSwitchBefore2026ScoresRatedClassesAsPhrfOnly() {
        val in2025 = pacific("2025-06-18", "18:00")
        assertEquals(RatingType.PHRF, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.ORC, in2025)).ratingType)
        assertEquals(RatingType.PHRF, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.ORC_PHRF, in2025)).ratingType)
        assertEquals(RatingType.PHRF, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.PHRF, in2025)).ratingType)
        assertEquals(RatingType.CruisingFlyingSails, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.CruisingFlyingSails, in2025)).ratingType)
        assertEquals(RatingType.CruisingNonFlyingSails, RaceResultReporter.effectiveRaceClass(classSchedule(RatingType.CruisingNonFlyingSails, in2025)).ratingType)
    }

    @Test
    fun seasonSwitchFrom2026KeepsTheClassAsIs() {
        val in2026 = pacific("2026-06-17", "18:00")
        RatingType.entries.forEach { rt ->
            assertEquals(rt, RaceResultReporter.effectiveRaceClass(classSchedule(rt, in2026)).ratingType, rt.name)
        }
    }

    @Test
    fun seasonSwitchWithNoStartScoresAsPhrfOnly() {
        val noStart = ClassSchedule(raceClass = RaceClass(id = 1, ratingType = RatingType.ORC_PHRF))
        assertEquals(RatingType.PHRF, RaceResultReporter.effectiveRaceClass(noStart).ratingType)
    }

    /**
     * Before 2026 the season switch only relabels the class as PHRF, which is what hides the ORC
     * columns in the web view. ORC places are still computed from each result's own rating type,
     * so hasOrcResults and the ORC places are set in both years.
     */
    @Test
    fun seasonSwitchRelabelsTheClassButStillComputesOrcPlaces() {
        val cert = OrcCertificate(refNo = "C1", fiveBandWlLowMedTot = 0.9)
        val in2025 = race(date = "2025-06-18") {
            orcPhrfClass("Fast") { bracket("I") { boat("A", rating = 50) finishes "19:00:00" orc "C1" } }
        }.report(mapOf("C1" to cert))
        assertEquals(RatingType.PHRF, in2025.classReports.single().raceClass.ratingType)
        assertEquals(true, in2025.classReports.single().hasOrcResults)
        assertEquals(1.0, in2025.classScoresOrc()["A"])
        val in2026 = race(date = "2026-06-17") {
            orcPhrfClass("Fast") { bracket("I") { boat("A", rating = 50) finishes "19:00:00" orc "C1" } }
        }.report(mapOf("C1" to cert))
        assertEquals(RatingType.ORC_PHRF, in2026.classReports.single().raceClass.ratingType)
        assertEquals(true, in2026.classReports.single().hasOrcResults)
        assertEquals(1.0, in2026.classScoresOrc()["A"])
    }

    private fun legacyRow(finish: Instant?, code: String?): ResultRow = ResultRow.createAndFillValues(
        mapOf(
            RaceResultsTable.id to 1L,
            RaceResultsTable.raceId to 2L,
            RaceResultsTable.boatId to 3L,
            RaceResultsTable.finish to finish,
            RaceResultsTable.phrfRating to null,
            RaceResultsTable.orcRefNo to null,
            RaceResultsTable.ratingType to RatingType.PHRF.name,
            RaceResultsTable.hoc to null,
            RaceResultsTable.raceClass to 4L,
            RaceResultsTable.bracket to 5L,
            RaceResultsTable.finishCode to code,
            RaceResultsTable.penalty to null,
        )
    )

    /** F6: 733 rows in the production dump have a NULL finish_code. The row mapper infers TIME from a finish time, else RET. */
    @Test
    fun nullFinishCodeFallsBackToTimeOrRet() {
        val finish = pacific("2026-06-17", "20:00")
        val timed = RaceResultsTable.rowToResult(legacyRow(finish, null))
        assertEquals(FinishCode.TIME, timed.finishCode)
        assertEquals(finish, timed.finish)
        val noTime = RaceResultsTable.rowToResult(legacyRow(null, null))
        assertEquals(FinishCode.RET, noTime.finishCode)
        assertNull(noTime.finish)
    }

    @Test
    fun storedFinishCodeWins() {
        val finish = pacific("2026-06-17", "20:00")
        assertEquals(FinishCode.DNF, RaceResultsTable.rowToResult(legacyRow(finish, "DNF")).finishCode)
        assertEquals(FinishCode.DNS_RC, RaceResultsTable.rowToResult(legacyRow(null, "DNS_RC")).finishCode)
    }
}

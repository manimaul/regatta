import com.mxmariner.regatta.data.ClassSchedule
import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcScoringOption
import com.mxmariner.regatta.data.Race
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceSchedule
import utils.withClassStart
import utils.withOrcOption
import utils.withPhrfBFactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Race config edits shared by the RC page, the races editor and the Config modal (utils/RaceConfig.kt, R4a). */
class RaceConfigTest {

    private val schedule = RaceSchedule(
        race = Race(name = "R", phrfBFactor = 600, orcScoringOption = OrcScoringOption.FiveBandWindwardLeeward),
        schedule = listOf(
            ClassSchedule(RaceClass(id = 1, name = "Fast"), startDate = Instant.parse("2026-06-17T17:00:00Z")),
            ClassSchedule(RaceClass(id = 2, name = "Half Fast"), startDate = Instant.parse("2026-06-17T17:05:00Z")),
        ),
    )

    @Test
    fun bFactorDerivesBothBands() {
        val r = schedule.withPhrfBFactor(550).race
        assertEquals(550, r.phrfBFactor)
        assertEquals(Orc3Band.Medium, r.orc3Band)
        assertEquals(Orc5Band.Medium, r.orc5Band)
        assertEquals(Orc5Band.High, schedule.withPhrfBFactor(480).race.orc5Band)
    }

    @Test
    fun nullBFactorIsTheDefault() {
        val r = schedule.withPhrfBFactor(null).race
        assertEquals(600, r.phrfBFactor)
        assertEquals(Orc5Band.LowMedium, r.orc5Band)
    }

    @Test
    fun aFiveBandChoiceSetsTheBFactorAndDerivesTheThreeBand() {
        val r = schedule.withOrcOption(OrcScoringOption.FiveBandWindwardLeeward, Orc3Band.Low, Orc5Band.High).race
        assertEquals(480, r.phrfBFactor)
        assertEquals(Orc5Band.High, r.orc5Band)
        assertEquals(Orc3Band.High, r.orc3Band)
    }

    @Test
    fun aThreeBandChoiceSetsTheBFactorAndDerivesTheFiveBand() {
        val r = schedule.withOrcOption(OrcScoringOption.TripleNumberAllPurpose, Orc3Band.Medium, Orc5Band.Low).race
        assertEquals(OrcScoringOption.TripleNumberAllPurpose, r.orcScoringOption)
        assertEquals(550, r.phrfBFactor)
        assertEquals(Orc3Band.Medium, r.orc3Band)
        assertEquals(Orc5Band.Medium, r.orc5Band)
    }

    @Test
    fun aSingleNumberChoiceKeepsTheBFactor() {
        val r = schedule.withOrcOption(OrcScoringOption.SingleNumberAllPurpose, Orc3Band.High, Orc5Band.High).race
        assertEquals(600, r.phrfBFactor)
        assertEquals(OrcScoringOption.SingleNumberAllPurpose, r.orcScoringOption)
        assertEquals(Orc3Band.Low, r.orc3Band)
        assertEquals(Orc5Band.LowMedium, r.orc5Band)
    }

    @Test
    fun classStartChangesOnlyThatClass() {
        val start = Instant.parse("2026-06-17T17:30:00Z")
        val s = schedule.withClassStart(schedule.schedule[1], start)
        assertEquals(schedule.schedule[0], s.schedule[0])
        assertEquals(start, s.schedule[1].startDate)
        assertEquals(schedule.schedule[1].endDate, s.schedule[1].endDate)
        assertEquals(schedule.race, s.race)
    }
}

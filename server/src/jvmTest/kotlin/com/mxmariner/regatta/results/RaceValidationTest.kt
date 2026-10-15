package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcScoringOption
import com.mxmariner.regatta.data.Race
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** validateRace guards the POST race and POST raceSchedule routes (R4a, orc_series_standings.md). */
class RaceValidationTest {

    private fun race(
        cf: Int,
        option: OrcScoringOption,
        orc3Band: Orc3Band = Orc3Band.fromPhrfBFactor(cf),
        orc5Band: Orc5Band = Orc5Band.fromPhrfBFactor(cf),
    ) = Race(name = "R", phrfBFactor = cf, orcScoringOption = option, orc3Band = orc3Band, orc5Band = orc5Band)

    @Test
    fun every_band_is_accepted_with_its_own_b_factor() {
        Orc3Band.entries.forEach { band ->
            val r = race(band.phrfBFactor, OrcScoringOption.TripleNumberAllPurpose, orc3Band = band)
            assertNull(validateRace(r), "3-band ${band.name}")
        }
        Orc5Band.entries.forEach { band ->
            val r = race(band.phrfBFactor, OrcScoringOption.FiveBandWindwardLeeward, orc5Band = band)
            assertNull(validateRace(r), "5-band ${band.name}")
        }
    }

    @Test
    fun r4a_rejects_a_5_band_option_whose_band_does_not_follow_the_b_factor() {
        assertEquals(
            "PHRF B factor 550 is Medium, but the ORC wind band is Low/Medium",
            validateRace(race(550, OrcScoringOption.FiveBandWindwardLeeward, orc5Band = Orc5Band.LowMedium))
        )
    }

    @Test
    fun r4a_rejects_a_3_band_option_whose_band_does_not_follow_the_b_factor() {
        assertEquals(
            "PHRF B factor 550 is Medium, but the ORC wind band is Low",
            validateRace(race(550, OrcScoringOption.TripleNumberAllPurpose, orc3Band = Orc3Band.Low))
        )
    }

    /** Only the band in use is checked: the RC page leaves the other band as it was. */
    @Test
    fun the_unused_band_may_disagree() {
        assertNull(validateRace(race(550, OrcScoringOption.FiveBandWindwardLeeward, orc3Band = Orc3Band.Low)))
        assertNull(validateRace(race(550, OrcScoringOption.TripleNumberWindwardLeeward, orc5Band = Orc5Band.LowMedium)))
    }

    @Test
    fun a_single_number_option_is_accepted_with_any_bands() {
        assertNull(validateRace(race(550, OrcScoringOption.SingleNumberAllPurpose, Orc3Band.Low, Orc5Band.High)))
    }
}

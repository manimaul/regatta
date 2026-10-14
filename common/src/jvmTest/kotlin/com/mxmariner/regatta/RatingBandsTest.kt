package com.mxmariner.regatta

import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.OrcScoringOption
import com.mxmariner.regatta.data.PhrfBFactor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Band edges of the PHRF B-factor and the ORC wind bands derived from it. */
class RatingBandsTest {

    @Test
    fun phrfBFactorAboveCalmIsStillCalm() {
        assertEquals(PhrfBFactor.Calm, PhrfBFactor.fromBFactor(651))
        assertEquals(PhrfBFactor.Calm, PhrfBFactor.fromBFactor(1000))
    }

    @Test
    fun phrfBFactorEdges() {
        assertEquals(PhrfBFactor.Hvy, PhrfBFactor.fromBFactor(480))
        assertEquals(PhrfBFactor.Hvy, PhrfBFactor.fromBFactor(515))
        assertEquals(PhrfBFactor.Avg, PhrfBFactor.fromBFactor(516))
        assertEquals(PhrfBFactor.Avg, PhrfBFactor.fromBFactor(550))
        assertEquals(PhrfBFactor.Avg, PhrfBFactor.fromBFactor(575))
        assertEquals(PhrfBFactor.Light, PhrfBFactor.fromBFactor(576))
        assertEquals(PhrfBFactor.Light, PhrfBFactor.fromBFactor(600))
        assertEquals(PhrfBFactor.Light, PhrfBFactor.fromBFactor(625))
        assertEquals(PhrfBFactor.Calm, PhrfBFactor.fromBFactor(626))
        assertEquals(PhrfBFactor.Calm, PhrfBFactor.fromBFactor(650))
    }

    @Test
    fun orc3BandEdges() {
        (0..700).forEach { cf ->
            val expected = when {
                cf >= 600 -> Orc3Band.Low
                cf >= 550 -> Orc3Band.Medium
                else -> Orc3Band.High
            }
            assertEquals(expected, Orc3Band.fromPhrfBFactor(cf), "cf = $cf")
        }
    }

    @Test
    fun orc3BandFromEachPhrfBFactor() {
        assertEquals(Orc3Band.Low, Orc3Band.fromPhrfBFactor(PhrfBFactor.Calm.cf))
        assertEquals(Orc3Band.Low, Orc3Band.fromPhrfBFactor(PhrfBFactor.Light.cf))
        assertEquals(Orc3Band.Medium, Orc3Band.fromPhrfBFactor(PhrfBFactor.Avg.cf))
        assertEquals(Orc3Band.High, Orc3Band.fromPhrfBFactor(PhrfBFactor.Hvy.cf))
    }

    @Test
    fun orc5BandEdges() {
        (0..700).forEach { cf ->
            val expected = when {
                cf >= 650 -> Orc5Band.Low
                cf >= 600 -> Orc5Band.LowMedium
                cf >= 550 -> Orc5Band.Medium
                cf >= 510 -> Orc5Band.MediumHigh
                else -> Orc5Band.High
            }
            assertEquals(expected, Orc5Band.fromPhrfBFactor(cf), "cf = $cf")
        }
    }

    @Test
    fun orc5BandFromEachPhrfBFactor() {
        assertEquals(Orc5Band.Low, Orc5Band.fromPhrfBFactor(PhrfBFactor.Calm.cf))
        assertEquals(Orc5Band.LowMedium, Orc5Band.fromPhrfBFactor(PhrfBFactor.Light.cf))
        assertEquals(Orc5Band.Medium, Orc5Band.fromPhrfBFactor(PhrfBFactor.Avg.cf))
        assertEquals(Orc5Band.High, Orc5Band.fromPhrfBFactor(PhrfBFactor.Hvy.cf))
    }

    /** virtualPhrf = 500 / (5-band W/L low-medium TOT) - 550, rounded. */
    @Test
    fun virtualPhrfFromLowMediumTot() {
        assertEquals(-50, OrcCertificate(fiveBandWlLowMedTot = 1.0).virtualPhrf())
        assertEquals(75, OrcCertificate(fiveBandWlLowMedTot = 0.8).virtualPhrf())
        assertEquals(219, OrcCertificate(fiveBandWlLowMedTot = 0.65).virtualPhrf()) // 769.23 - 550
        assertEquals(-133, OrcCertificate(fiveBandWlLowMedTot = 1.2).virtualPhrf()) // 416.67 - 550
    }

    @Test
    fun virtualPhrfIgnoresOtherTotFields() {
        val cert = OrcCertificate(fiveBandWlLowMedTot = 1.0, allPurposeTot = 0.5, fiveBandWlMedTot = 0.5)
        assertEquals(-50, cert.virtualPhrf())
    }

    @Test
    fun orcBandLabelForEveryOption() {
        val band5 = Orc5Band.MediumHigh
        val band3 = Orc3Band.Medium
        OrcScoringOption.entries.forEach { option ->
            val label = option.orcBandLabel(band5, band3)
            when (option) {
                OrcScoringOption.SingleNumberAllPurpose,
                OrcScoringOption.SingleNumberWindwardLeeward,
                OrcScoringOption.SingleNumberPredominantUpwind,
                OrcScoringOption.SingleNumberPredominantReaching,
                OrcScoringOption.SingleNumberPredominantDownwind -> {
                    assertNull(label, option.name)
                    assertEquals(false, option.uses3Band, option.name)
                    assertEquals(false, option.uses5Band, option.name)
                }

                OrcScoringOption.TripleNumberAllPurpose,
                OrcScoringOption.TripleNumberWindwardLeeward -> {
                    assertEquals("Medium - 9 to 14 kts", label, option.name)
                    assertEquals(true, option.uses3Band, option.name)
                    assertEquals(false, option.uses5Band, option.name)
                }

                OrcScoringOption.PredominantUpwind,
                OrcScoringOption.PredominantDownwind,
                OrcScoringOption.PredominantReaching,
                OrcScoringOption.FiveBandWindwardLeeward,
                OrcScoringOption.WindwardLeeward60_40,
                OrcScoringOption.FiveBandAllPurpose -> {
                    assertEquals("Medium/High - 13 to 17 kts", label, option.name)
                    assertEquals(false, option.uses3Band, option.name)
                    assertEquals(true, option.uses5Band, option.name)
                }
            }
        }
        assertNotNull(OrcScoringOption.entries.size)
    }
}

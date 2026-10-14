package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.OrcScoringOption
import com.mxmariner.regatta.data.OrcScoringOption.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** orcTcf picks one certificate field per scoring option and wind band. Every field has a distinct value here. */
class OrcTcfTest {

    private val cert = OrcCertificate(
        allPurposeTot = 1.001,
        wlSingleNumberTot = 1.002,
        tripleNumberAllPurposeLowTot = 1.011,
        tripleNumberAllPurposeMedTot = 1.012,
        tripleNumberAllPurposeHiTot = 1.013,
        tripleNumberWlLowTot = 1.021,
        tripleNumberWlMedTot = 1.022,
        tripleNumberWlHiTot = 1.023,
        singleNumberPredominantUpwindTot = 1.031,
        singleNumberPredominantReachingTot = 1.032,
        singleNumberPredominantDownwindTot = 1.033,
        predominantUpwindLowTot = 1.041,
        predominantUpwindLowMedTot = 1.042,
        predominantUpwindMedTot = 1.043,
        predominantUpwindMedHiTot = 1.044,
        predominantUpwindHiTot = 1.045,
        predominantDownwindLowTot = 1.051,
        predominantDownwindLowMedTot = 1.052,
        predominantDownwindMedTot = 1.053,
        predominantDownwindMedHiTot = 1.054,
        predominantDownwindHiTot = 1.055,
        predominantReachingLowTot = 1.061,
        predominantReachingLowMedTot = 1.062,
        predominantReachingMedTot = 1.063,
        predominantReachingMedHiTot = 1.064,
        predominantReachingHiTot = 1.065,
        fiveBandWlLowTot = 1.071,
        fiveBandWlLowMedTot = 1.072,
        fiveBandWlMedTot = 1.073,
        fiveBandWlMedHiTot = 1.074,
        fiveBandWlHiTot = 1.075,
        usWl6040LTot = 1.081,
        usWl6040LmTot = 1.082,
        usWl6040MTot = 1.083,
        usWl6040MhTot = 1.084,
        usWl6040HTot = 1.085,
        fiveBandAllPurposeLowTot = 1.091,
        fiveBandAllPurposeLowMedTot = 1.092,
        fiveBandAllPurposeMedTot = 1.093,
        fiveBandAllPurposeMedHiTot = 1.094,
        fiveBandAllPurposeHiTot = 1.095,
    )

    private val singleNumber = mapOf(
        SingleNumberAllPurpose to 1.001,
        SingleNumberWindwardLeeward to 1.002,
        SingleNumberPredominantUpwind to 1.031,
        SingleNumberPredominantReaching to 1.032,
        SingleNumberPredominantDownwind to 1.033,
    )

    private val threeBand = mapOf(
        TripleNumberAllPurpose to listOf(1.011, 1.012, 1.013),
        TripleNumberWindwardLeeward to listOf(1.021, 1.022, 1.023),
    )

    private val fiveBand = mapOf(
        PredominantUpwind to listOf(1.041, 1.042, 1.043, 1.044, 1.045),
        PredominantDownwind to listOf(1.051, 1.052, 1.053, 1.054, 1.055),
        PredominantReaching to listOf(1.061, 1.062, 1.063, 1.064, 1.065),
        FiveBandWindwardLeeward to listOf(1.071, 1.072, 1.073, 1.074, 1.075),
        WindwardLeeward60_40 to listOf(1.081, 1.082, 1.083, 1.084, 1.085),
        FiveBandAllPurpose to listOf(1.091, 1.092, 1.093, 1.094, 1.095),
    )

    @Test
    fun everyOptionIsCovered() {
        assertEquals(OrcScoringOption.entries.toSet(), singleNumber.keys + threeBand.keys + fiveBand.keys)
    }

    @Test
    fun singleNumberOptionsIgnoreTheBands() {
        singleNumber.forEach { (option, expected) ->
            assertEquals(expected, RaceResultReporter.orcTcf(null, null, option, cert), option.name)
            Orc3Band.entries.forEach { b3 ->
                Orc5Band.entries.forEach { b5 ->
                    assertEquals(expected, RaceResultReporter.orcTcf(b3, b5, option, cert), "${option.name} $b3 $b5")
                }
            }
        }
    }

    @Test
    fun threeBandOptionsFollowOrc3Band() {
        threeBand.forEach { (option, expected) ->
            Orc3Band.entries.forEachIndexed { i, b3 ->
                // the 5-band value must not matter
                Orc5Band.entries.forEach { b5 ->
                    assertEquals(expected[i], RaceResultReporter.orcTcf(b3, b5, option, cert), "${option.name} $b3 $b5")
                }
                assertEquals(expected[i], RaceResultReporter.orcTcf(b3, null, option, cert), "${option.name} $b3")
            }
        }
    }

    @Test
    fun fiveBandOptionsFollowOrc5Band() {
        fiveBand.forEach { (option, expected) ->
            Orc5Band.entries.forEachIndexed { i, b5 ->
                // the 3-band value must not matter
                Orc3Band.entries.forEach { b3 ->
                    assertEquals(expected[i], RaceResultReporter.orcTcf(b3, b5, option, cert), "${option.name} $b3 $b5")
                }
                assertEquals(expected[i], RaceResultReporter.orcTcf(null, b5, option, cert), "${option.name} $b5")
            }
        }
    }

    @Test
    fun bandedOptionsRequireTheirBand() {
        threeBand.keys.forEach { option ->
            assertFailsWith<IllegalArgumentException>(option.name) { RaceResultReporter.orcTcf(null, Orc5Band.Low, option, cert) }
        }
        fiveBand.keys.forEach { option ->
            assertFailsWith<IllegalArgumentException>(option.name) { RaceResultReporter.orcTcf(Orc3Band.Low, null, option, cert) }
        }
    }
}

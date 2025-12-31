package com.mxmariner.regatta

import com.mxmariner.regatta.data.PhrfBFactor
import kotlin.test.Test
import kotlin.test.assertEquals

class PhrfTest {

    @Test
    fun testPhrfBFactor() {
        (0..515).forEach { cf ->
            assertEquals(PhrfBFactor.Hvy, PhrfBFactor.fromBFactor(cf))
        }
        (516..575).forEach { cf ->
            assertEquals(PhrfBFactor.Avg, PhrfBFactor.fromBFactor(cf))
        }
        (576..625).forEach { cf ->
            assertEquals(PhrfBFactor.Light, PhrfBFactor.fromBFactor(cf))
        }
        (626..650).forEach { cf ->
            assertEquals(PhrfBFactor.Calm, PhrfBFactor.fromBFactor(cf), "cf = $cf")
        }
    }
}
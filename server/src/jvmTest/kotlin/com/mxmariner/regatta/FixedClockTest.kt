package com.mxmariner.regatta

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class FixedClockTest {

    @Test
    fun parsesAnIsoInstant() {
        assertEquals(Instant.parse("2026-10-14T02:10:11Z"), FixedClock.parse("2026-10-14T02:10:11Z"))
        assertEquals(Instant.parse("2026-10-14T02:10:11Z"), FixedClock.parse(" 2026-10-14T02:10:11Z\n"))
    }

    @Test
    fun absentOrBlankIsNull() {
        assertNull(FixedClock.parse(null))
        assertNull(FixedClock.parse(""))
        assertNull(FixedClock.parse("   "))
    }

    @Test
    fun nowHonoursThePropertyWhenSetAndTheWallClockOtherwise() {
        val previous = System.getProperty(FixedClock.PROPERTY)
        try {
            System.setProperty(FixedClock.PROPERTY, "2026-10-14T02:10:11Z")
            assertEquals(Instant.parse("2026-10-14T02:10:11Z"), FixedClock.now())
            System.clearProperty(FixedClock.PROPERTY)
            val drift = Clock.System.now() - FixedClock.now()
            assertTrue(drift.absoluteValue < 1.minutes, "FixedClock.now() should track the wall clock, drift $drift")
        } finally {
            if (previous == null) System.clearProperty(FixedClock.PROPERTY) else System.setProperty(FixedClock.PROPERTY, previous)
        }
    }
}

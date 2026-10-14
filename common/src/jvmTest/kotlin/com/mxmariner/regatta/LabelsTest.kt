package com.mxmariner.regatta

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RatingType
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Shared label helpers: Bracket.label, RatingType.ratedLabel and Duration.display. */
class LabelsTest {

    @Test
    fun bracketLabelWithBothEndsOpenIsJustTheName() {
        assertEquals("I", Bracket(name = "I").label())
        assertEquals("I", Bracket(name = "I", minRating = -1000f, maxRating = 1000f).label())
        assertEquals("I", Bracket(name = "I", minRating = -2000f, maxRating = 2000f).label())
    }

    @Test
    fun bracketLabelWithOpenMinimum() {
        assertEquals("I 100.0 -", Bracket(name = "I", maxRating = 100f).label())
    }

    @Test
    fun bracketLabelWithOpenMaximum() {
        assertEquals("II 101.0 +", Bracket(name = "II", minRating = 101f).label())
    }

    @Test
    fun bracketLabelWithBothEnds() {
        assertEquals("II 101.0 to 160.5", Bracket(name = "II", minRating = 101f, maxRating = 160.5f).label())
    }

    @Test
    fun ratedLabelPerRatingType() {
        assertEquals("PHRF (48)", RatingType.PHRF.ratedLabel(48))
        assertEquals("PHRF (null)", RatingType.PHRF.ratedLabel(null))
        assertEquals("ORC", RatingType.ORC.ratedLabel())
        assertEquals("ORC(~75)", RatingType.ORC.ratedLabel(orcInfo = "~75"))
        assertEquals("PHRF (48), ORC", RatingType.ORC_PHRF.ratedLabel(48))
        assertEquals("PHRF (48), ORC (~75)", RatingType.ORC_PHRF.ratedLabel(48, "~75"))
        assertEquals("Cruising Flying Sails", RatingType.CruisingFlyingSails.ratedLabel(48, "~75"))
        assertEquals("Cruising Non-Flying Sails", RatingType.CruisingNonFlyingSails.ratedLabel())
    }

    /** D1: whole scores have no decimal, fractions one or two. */
    @Test
    fun scoreTextFormatsFractions() {
        assertEquals("4", 4.0.scoreText())
        assertEquals("0", 0.0.scoreText())
        assertEquals("3.5", 3.5.scoreText())
        assertEquals("3.33", (10.0 / 3).scoreText())
        assertEquals("10.67", (32.0 / 3).scoreText())
        assertEquals("2.1", 2.1.scoreText())
        assertEquals("3", 2.999.scoreText())
    }

    @Test
    fun displayZero() {
        assertEquals("0h:0m:0s", Duration.ZERO.display())
    }

    @Test
    fun displayHoursMinutesSeconds() {
        assertEquals("1h:2m:3s", (1.hours + 2.minutes + 3.seconds).display())
        assertEquals("0h:54m:12s", (54.minutes + 12.seconds).display())
    }

    @Test
    fun displayOver24Hours() {
        assertEquals("25h:0m:0s", 25.hours.display())
        assertEquals("49h:59m:59s", (49.hours + 59.minutes + 59.seconds).display())
    }

    @Test
    fun displayWholeSecondsAreUnchangedBySubSecondZero() {
        assertEquals("0h:0m:12s", (12.seconds + 0.milliseconds).display())
    }

    /** D7: labels round to the nearest second instead of truncating. */
    @Test
    fun d7_display_rounds_to_nearest_second() {
        assertEquals("0h:0m:13s", (12.seconds + 900.milliseconds).display())
        assertEquals("0h:0m:12s", (12.seconds + 400.milliseconds).display())
        assertEquals("0h:1m:0s", (59.seconds + 600.milliseconds).display())
        assertEquals("1h:0m:0s", (59.minutes + 59.seconds + 500.milliseconds).display())
    }
}

package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.RaceResult
import com.mxmariner.regatta.data.RatingType
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** validateResult guards the POST results route (D8). */
class ResultValidationTest {

    @Test
    fun accepts_a_phrf_result_with_a_rating() {
        assertNull(validateResult(RaceResult(ratingType = RatingType.PHRF, phrfRating = 48)))
        assertNull(validateResult(RaceResult(ratingType = RatingType.ORC_PHRF, phrfRating = 48)))
    }

    @Test
    fun accepts_cruising_and_orc_results_without_a_phrf_rating() {
        assertNull(validateResult(RaceResult(ratingType = RatingType.CruisingFlyingSails)))
        assertNull(validateResult(RaceResult(ratingType = RatingType.CruisingNonFlyingSails)))
        assertNull(validateResult(RaceResult(ratingType = RatingType.ORC, orcRef = "C1")))
    }

    /** D8: a PHRF or ORC_PHRF result needs a PHRF rating; the message is what the UI shows. */
    @Test
    fun d8_rejects_a_phrf_result_without_a_rating() {
        assertNotNull(validateResult(RaceResult(ratingType = RatingType.PHRF)))
        assertNotNull(validateResult(RaceResult(ratingType = RatingType.ORC_PHRF, orcRef = "C1")))
        assertEquals("A PHRF result needs a PHRF rating", validateResult(RaceResult(ratingType = RatingType.PHRF)))
    }
}

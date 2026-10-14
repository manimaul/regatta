package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.RaceResult

/**
 * Checks a result before it is saved. Returns an error message for the client, or null when the
 * result is acceptable. D8: a PHRF or ORC/PHRF result needs a PHRF rating, or it would score
 * with a time correction factor of 1.
 */
fun validateResult(result: RaceResult): String? {
    if (result.ratingType.isPHRF && result.phrfRating == null) {
        return "A ${result.ratingType.label} result needs a PHRF rating"
    }
    return null
}

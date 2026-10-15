package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.RaceResult

/**
 * Checks a result before it is saved. Returns an error message for the client, or null when the
 * result is acceptable. D8: a PHRF or ORC/PHRF result needs a PHRF rating, and R2
 * (orc_series_standings.md): an ORC or ORC/PHRF result needs an ORC certificate, or it would
 * score with a time correction factor of 1.
 */
fun validateResult(result: RaceResult): String? {
    if (result.ratingType.isPHRF && result.phrfRating == null) {
        return "A ${result.ratingType.label} result needs a PHRF rating"
    }
    if (result.ratingType.isORC && result.orcRef == null) {
        return "An ${result.ratingType.label} result needs an ORC certificate"
    }
    return null
}

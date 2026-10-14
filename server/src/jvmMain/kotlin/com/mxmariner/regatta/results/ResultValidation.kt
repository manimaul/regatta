package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.RaceResult

/**
 * Checks a result before it is saved. Returns an error message for the client, or null when the
 * result is acceptable. Accepts everything until D8 (a PHRF or ORC_PHRF result needs a PHRF
 * rating); see docs/explore_unimplemented/test_audit_and_plan.md.
 */
@Suppress("UNUSED_PARAMETER")
fun validateResult(result: RaceResult): String? = null

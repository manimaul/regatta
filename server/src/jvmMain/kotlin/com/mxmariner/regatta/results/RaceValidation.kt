package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.Race

/**
 * Checks a race before it is saved. Returns an error message for the client, or null when the
 * race is acceptable. R4a (orc_series_standings.md): the ORC wind band the scoring option uses
 * must be the one the PHRF B factor maps to, so the ORC time correction and the PHRF one agree
 * on the wind. Only the band in use is checked; a single-number option has none. Nothing is
 * corrected here: the client is told what to change.
 */
fun validateRace(race: Race): String? {
    val cf = race.phrfBFactor
    if (race.orcScoringOption.uses3Band) {
        val expected = Orc3Band.fromPhrfBFactor(cf)
        if (race.orc3Band != expected) {
            return "PHRF B factor $cf is ${expected.label}, but the ORC wind band is ${race.orc3Band.label}"
        }
    }
    if (race.orcScoringOption.uses5Band) {
        val expected = Orc5Band.fromPhrfBFactor(cf)
        if (race.orc5Band != expected) {
            return "PHRF B factor $cf is ${expected.label}, but the ORC wind band is ${race.orc5Band.label}"
        }
    }
    return null
}

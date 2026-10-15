package utils

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.ClassSchedule
import com.mxmariner.regatta.data.Orc3Band
import com.mxmariner.regatta.data.Orc5Band
import com.mxmariner.regatta.data.OrcScoringOption
import com.mxmariner.regatta.data.RaceSchedule
import kotlin.time.Instant

// Race config edits shared by the RC page, the races editor and the results editor's Config modal
// (R4a, orc_series_standings.md). Pure, so they are covered by jsTest. The PHRF B factor is the
// authority on the wind: the ORC bands always follow it, and choosing a band sets the B factor.

/** Sets the PHRF B factor and derives both ORC wind bands from it. A null factor is the default. */
fun RaceSchedule.withPhrfBFactor(cf: Int?): RaceSchedule {
    val factor = cf ?: correctionFactorDefault
    return copy(
        race = race.copy(
            phrfBFactor = factor,
            orc3Band = Orc3Band.fromPhrfBFactor(factor),
            orc5Band = Orc5Band.fromPhrfBFactor(factor),
        )
    )
}

/**
 * Sets the ORC scoring option and bands. The band the option uses sets the PHRF B factor, and the
 * other band is derived from that factor; a single-number option keeps the B factor and derives
 * both bands from it.
 */
fun RaceSchedule.withOrcOption(option: OrcScoringOption, orc3Band: Orc3Band, orc5Band: Orc5Band): RaceSchedule {
    val factor = when {
        option.uses3Band -> orc3Band.phrfBFactor
        option.uses5Band -> orc5Band.phrfBFactor
        else -> race.phrfBFactor
    }
    return copy(
        race = race.copy(
            phrfBFactor = factor,
            orcScoringOption = option,
            orc3Band = if (option.uses3Band) orc3Band else Orc3Band.fromPhrfBFactor(factor),
            orc5Band = if (option.uses5Band) orc5Band else Orc5Band.fromPhrfBFactor(factor),
        )
    )
}

/** Moves one class's start; the other classes are untouched. */
fun RaceSchedule.withClassStart(classSchedule: ClassSchedule, start: Instant): RaceSchedule =
    copy(
        schedule = schedule.map { cs ->
            if (cs.raceClass.id == classSchedule.raceClass.id) cs.copy(startDate = start) else cs
        }
    )

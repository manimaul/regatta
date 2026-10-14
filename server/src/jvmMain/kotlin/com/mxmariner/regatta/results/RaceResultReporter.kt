package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object RaceResultReporter {

    /**
     * DB wrapper around [standings]: fetches the series' race reports and the series itself.
     * [now] defaults to the wall clock; the golden test pins it (see docs/explore_implemented/test_audit_and_plan.md §5).
     */
    suspend fun getStandingsReport(seriesId: Long, year: Int, now: Instant = Clock.System.now()): StandingsSeries? {
        val reports = RegattaDatabase.seriesRaces(seriesId, year).mapNotNull { raceId ->
            getReport(raceId)
        }
        return RegattaDatabase.findSeries(seriesId)?.let { series ->
            standings(series, year, reports, now)
        }
    }

    /**
     * Pure series standings from the race reports. [now] tells a canceled race from one not yet
     * sailed (D2) when a race has no results at all.
     */
    fun standings(series: Series, year: Int, reports: List<RaceReport>, now: Instant): StandingsSeries {
        val sorted = reports.sortedBy { it.raceSchedule.startTime }
        val races = sorted.map { it.raceSchedule.race }
        val statuses = sorted.associate { report ->
            val end = report.raceSchedule.schedule.maxOfOrNull { it.endDate }
            report.raceSchedule.race.id to when {
                report.classReports.isNotEmpty() -> RaceStatus.Sailed
                end != null && end > now -> RaceStatus.NotYetSailed
                else -> RaceStatus.Cancelled
            }
        }

        val standings = StandingsSeries(
            year = year,
            series = series,
            standings = getStandingsClass(races, sorted.asSequence(), statuses),
            races = races
        )

        //calculate place in class
        standings.standings.forEach { standingsClass: StandingsClass ->
            val rows = standingsClass.standings.flatMap { it.standings }
            assignPlaces(rows, { it.totalScoreClass }, { it.placeInClass }, { it.classSailed }) { row, place ->
                row.placeInClass = place
            }
        }
        return standings
    }

    /**
     * Sorts the rows by total, then by the tiebreak, and writes 1-based places. Boats the
     * tiebreak cannot separate share the place, and the next place skips (D5). The sort is
     * stable and the comparator only looks at scores, so the input order never shows.
     */
    private fun assignPlaces(
        rows: List<StandingsBoatSkipper>,
        total: (StandingsBoatSkipper) -> Double,
        score: (StandingsRace) -> Double,
        sailed: (StandingsRace) -> Boolean,
        set: (StandingsBoatSkipper, Int) -> Unit,
    ) {
        val comparator = Comparator<StandingsBoatSkipper> { lhs, rhs ->
            total(lhs).compareTo(total(rhs)).takeIf { it != 0 } ?: tieBreaker(lhs, rhs, score, sailed)
        }
        val sorted = rows.sortedWith(comparator)
        var place = 0
        sorted.forEachIndexed { i, row ->
            if (i == 0 || comparator.compare(sorted[i - 1], row) != 0) {
                place = i + 1
            }
            set(row, place)
        }
    }

    /**
     * GSI 14.5: "If two or more boats are tied in a series, the boat with the highest finishing
     * place in the last race of the series will be scored the series winner." D5 continues to
     * the next-to-last race and so on when a race is equal. Races not sailed for this group
     * (D2) and races where either boat was the RC volunteer (GSI 1.4.1.2) are skipped. Raw
     * scores are compared, thrown-out races included. 0 means the boats cannot be separated.
     */
    internal fun tieBreaker(
        lhs: StandingsBoatSkipper,
        rhs: StandingsBoatSkipper,
        score: (StandingsRace) -> Double,
        sailed: (StandingsRace) -> Boolean,
    ): Int {
        for (i in lhs.raceStandings.indices.reversed()) {
            val left = lhs.raceStandings[i]
            val right = rhs.raceStandings.getOrNull(i) ?: continue
            if (!sailed(left) || !sailed(right)) continue
            if (left.finishCode == FinishCode.DNS_RC || right.finishCode == FinishCode.DNS_RC) continue
            val c = score(left).compareTo(score(right))
            if (c != 0) return c
        }
        return 0
    }

    private fun getStandingsClass(
        races: List<Race>,
        reports: Sequence<RaceReport>,
        statuses: Map<Long, RaceStatus>,
    ): List<StandingsClass> {
        val classes = reports.flatMap { it.classReports }.map {
            it.raceClass
        }.distinctBy { it.id }.sortedBy { it.sort }.toList()

        return classes.map { raceClass ->
            StandingsClass(
                raceClass = raceClass,
                standings = getStandingsBracket(raceClass, races, reports, statuses)
            )
        }
    }

    private fun getStandingsBracket(
        raceClass: RaceClass,
        races: List<Race>,
        raceReports: Sequence<RaceReport>,
        statuses: Map<Long, RaceStatus>,
    ): List<StandingsBracket> {
        val brackets =
            raceReports.flatMap { it.classReports }.filter { it.raceClass.id == raceClass.id }
                .flatMap { it.bracketReport }.map { it.bracket }.distinctBy { it.id }
        return brackets.map {
            StandingsBracket(
                bracket = it,
                standings = getStandingsBoatBracket(raceClass, it, races, raceReports, statuses)
            )
        }.toList()
    }

    private fun getStandingsBoatBracket(
        raceClass: RaceClass,
        bracket: Bracket,
        races: List<Race>,
        raceReports: Sequence<RaceReport>,
        statuses: Map<Long, RaceStatus>,
    ): List<StandingsBoatSkipper> {

        val classRecords = raceReports.flatMap { it.classReports.filter { it.raceClass.id == raceClass.id } }
            .flatMap { it.bracketReport }.flatMap { it.cards }.toList()

        val bracketRecords = classRecords.filter { it.resultRecord.bracket.id == bracket.id }

        val raceOrder = races.withIndex().associate { (i, race) -> race.id to i }
        val result = bracketRecords.groupBy { it.resultRecord.boatSkipper.boat?.id ?: 0L }.map { (_, raceReportCards) ->
            // D8: one row per boat, shown with the skipper of its most recent race in the series
            val boatSkipper = raceReportCards.maxBy { raceOrder[it.resultRecord.result.raceId] ?: -1 }.resultRecord.boatSkipper
            val bracketStarters = races.map { race -> bracketRecords.count { it.resultRecord.result.raceId == race.id && it.isStarter() } }
            val classStarters = races.map { race -> classRecords.count { it.resultRecord.result.raceId == race.id && it.isStarter() } }
            val raw = races.mapIndexed { i, race ->
                val status = statuses[race.id] ?: RaceStatus.Sailed
                // D2: a race is sailed for a group when the group had a starter
                val bracketSailed = bracketStarters[i] > 0
                val classSailed = classStarters[i] > 0
                val card = raceReportCards.find { it.resultRecord.result.raceId == race.id }
                if (card != null) {
                    StandingsRace(
                        nonStarter = false,
                        placeInBracket = if (bracketSailed) card.placeInBracket.toDouble() else 0.0,
                        placeInClass = if (classSailed) card.placeInClass.toDouble() else 0.0,
                        finish = card.finishTime != null,
                        hocPosition = card.hocPosition,
                        finishCode = card.resultRecord.result.finishCode,
                        bracketSailed = bracketSailed,
                        classSailed = classSailed,
                        status = status,
                    )
                } else {
                    // GSI 14.1.2: DNS = starters + 1, per group
                    StandingsRace(
                        nonStarter = true,
                        finish = false,
                        placeInBracket = if (bracketSailed) bracketStarters[i] + 1.0 else 0.0,
                        placeInClass = if (classSailed) classStarters[i] + 1.0 else 0.0,
                        hocPosition = null,
                        finishCode = null,
                        bracketSailed = bracketSailed,
                        classSailed = classSailed,
                        status = status,
                    )
                }
            }

            val bracket = groupScores(raw, { it.bracketSailed }, { it.placeInBracket }, { it.placeInClass }) { i -> bracketStarters[i] + 1.0 }
            val klass = groupScores(raw, { it.classSailed }, { it.placeInClass }, { it.placeInBracket }) { i -> classStarters[i] + 1.0 }
            val standings = raw.mapIndexed { i, cell ->
                cell.copy(
                    placeInBracket = bracket.scores[i] ?: 0.0,
                    placeInClass = klass.scores[i] ?: 0.0,
                    throwOut = i == bracket.throwOut,
                    throwOutClass = i == klass.throwOut,
                )
            }

            StandingsBoatSkipper(
                boatSkipper = boatSkipper,
                raceStandings = standings,
                totalScoreBracket = bracket.total,
                totalScoreClass = klass.total,
                placeInBracket = 0,
                placeInClass = 0,
            )
        }

        assignPlaces(result, { it.totalScoreBracket }, { it.placeInBracket }, { it.bracketSailed }) { row, place ->
            row.placeInBracket = place
        }
        return result.sortedBy { it.placeInBracket }
    }

    internal class GroupScores(val scores: List<Double?>, val throwOut: Int?, val total: Double)

    /** RRS A10(a): to the nearest tenth of a point, 0.05 rounded upward. The epsilon absorbs binary representation error. */
    internal fun roundToTenth(score: Double): Double = floor(score * 10 + 0.5 + 1e-9) / 10

    /**
     * Scores one group (bracket or class) of a boat's series row, by race index.
     *
     * - D2: a race not sailed for the group scores null and is left out of everything.
     * - GSI 14.4 / D2: with 5 or more sailed races, the worst scored race is thrown out. Among
     *   equal worst scores the one with the worse [tieScore] goes, the first on a full tie.
     * - GSI 1.4.1.2 / D1: the boat's first RC volunteer race scores the average of its other
     *   sailed races, the throw-out left out, rounded to the nearest tenth of a point with 0.05
     *   rounded up (RRS A10(a), [roundToTenth]); later RC races score DNS ([dns]) and count in
     *   that average; with no other race to average, the RC race scores DNS too. The RC race is
     *   never the throw-out, since the average cannot be worse than the worst of the races it is
     *   taken from.
     * - The total is the sum of the sailed races that were not thrown out.
     */
    internal fun groupScores(
        cells: List<StandingsRace>,
        sailed: (StandingsRace) -> Boolean,
        score: (StandingsRace) -> Double,
        tieScore: (StandingsRace) -> Double,
        dns: (Int) -> Double,
    ): GroupScores {
        val sailedIdx = cells.indices.filter { sailed(cells[it]) }
        val rcIdx = sailedIdx.filter { cells[it].finishCode == FinishCode.DNS_RC }
        val scores = MutableList<Double?>(cells.size) { null }
        sailedIdx.forEach { scores[it] = score(cells[it]) }
        rcIdx.drop(1).forEach { scores[it] = dns(it) }
        val scoredIdx = sailedIdx - rcIdx.take(1).toSet()

        val throwOut = if (sailedIdx.size >= 5 && scoredIdx.isNotEmpty()) {
            val worst = scoredIdx.maxOf { scores[it]!! }
            scoredIdx.filter { scores[it] == worst }.maxByOrNull { tieScore(cells[it]) }
        } else {
            null
        }

        rcIdx.firstOrNull()?.let { rc ->
            val others = scoredIdx.filter { it != throwOut }
            scores[rc] = if (others.isEmpty()) dns(rc) else roundToTenth(others.sumOf { scores[it]!! } / others.size)
        }

        val total = sailedIdx.filter { it != throwOut }.sumOf { scores[it]!! }
        return GroupScores(scores, throwOut, total)
    }

    fun classCard(
        raceSchedule: RaceSchedule,
        classSchedule: ClassSchedule,
        boatCards: List<RaceReportCard>,
        raceClass: RaceClass,
    ): ClassReportCards? {
        //Class Places
        var hasOrcResults = false
        val classCards = boatCards.filter {
            it.resultRecord.bracket.classId == raceClass.id
        }.place { p: Int, card: RaceReportCard ->
            card.placeInClass = p
        }

        classCards.place(orc = true) { p, card ->
            hasOrcResults = true
            card.placeInClassOrc = p
        }

        //Bracket Places
        val bracketCards = classSchedule.brackets.mapNotNull { bracket ->
            val bracketCards = classCards.filter {
                it.resultRecord.bracket.id == bracket.id
            }.place { p: Int, card: RaceReportCard ->
                card.placeInBracket = p
            }

            bracketCards.place(orc = true) { p, card ->
                card.placeInBracketOrc = p
            }

            if (bracketCards.isNotEmpty()) {
                BracketReportCards(
                    bracket = bracket,
                    cards = bracketCards,
                )
            } else {
                null
            }
        }

        return ClassReportCards(
            raceClass = raceClass,
            bracketReport = bracketCards,
            phrfBFactor = raceSchedule.race.phrfBFactor,
            hasOrcResults = hasOrcResults
        ).takeIf { it.bracketReport.isNotEmpty() }
    }

    /**
     * The 2025→2026 switch: ORC scoring only applies from the 2026 season on. Before that a rated
     * class scores as PHRF only, whatever its rating type says. The season is the class start's
     * year in America/Los_Angeles. Cruising classes are returned unchanged.
     */
    fun effectiveRaceClass(classSchedule: ClassSchedule): RaceClass {
        val raceClass = classSchedule.raceClass
        if (!raceClass.ratingType.isORCorPHRF) {
            return raceClass
        }
        val year = classSchedule.raceStart()?.toLocalDateTime(TimeZone.of("America/Los_Angeles"))?.year ?: 0
        return if (year >= 2026) {
            raceClass
        } else {
            raceClass.copy(ratingType = RatingType.PHRF)
        }
    }

    /** DB wrapper around [report]: fetches the schedule and the results, and builds the cards. */
    suspend fun getReport(raceId: Long): RaceReport? {
        return RegattaDatabase.findRaceSchedule(raceId)?.let { raceSchedule: RaceSchedule ->
            val schedules: Map<Long, ClassSchedule> = raceSchedule.schedule.associateBy { it.raceClass.id }
            val cards = RegattaDatabase.resultsBoatBracketByRaceId(raceId).map { reduceToCard(it, raceSchedule, schedules) }
            report(raceSchedule, cards)
        }
    }

    /**
     * Pure race report: places every card in its class and bracket. Each card belongs to at most
     * one class (by its bracket's class id), so the cards can be shared across the class loop.
     */
    fun report(raceSchedule: RaceSchedule, cards: List<RaceReportCard>): RaceReport {
        val classReportList = mutableListOf<ClassReportCards>()

        //Places
        raceSchedule.schedule.forEach { classSchedule: ClassSchedule ->
            val rt = classSchedule.raceClass.ratingType
            val classCards = if (rt.isORCorPHRF) {
                cards.filter { it.ratingType().isORCorPHRF }
            } else {
                cards.filter { it.ratingType() == rt }
            }
            classCard(
                raceSchedule,
                classSchedule,
                classCards,
                effectiveRaceClass(classSchedule)
            )?.let { classReportList.add(it) }
        }

        //Orphan results
        val orphans = cards.filter { it.resultRecord.bracket.id == 0L }
        orphans.place { i, it ->
            it.placeInBracket = i
            it.placeInClass = i
        }
        if (orphans.isNotEmpty()) {
            val orphanCards = BracketReportCards(
                bracket = Bracket(name = "Invalid result records"),
                cards = orphans
            )
            classReportList.add(
                ClassReportCards(
                    raceClass = RaceClass(name = "Incorrectly classed"),
                    bracketReport = listOf(orphanCards),
                    phrfBFactor = raceSchedule.race.phrfBFactor,
                    orcScoringOption = raceSchedule.race.orcScoringOption,
                    orc3Band = raceSchedule.race.orc3Band,
                    orc5Band = raceSchedule.race.orc5Band,
                )
            )
        }

        return RaceReport(
            raceSchedule = raceSchedule,
            classReports = classReportList,
        )
    }

    /** DB wrapper around [card]: looks up the ORC certificate and the class start. */
    private suspend fun reduceToCard(
        record: RaceResultBoatBracket,
        raceSchedule: RaceSchedule,
        classSchedules: Map<Long, ClassSchedule>
    ): RaceReportCard {
        val orcCert = record.result.orcRef?.let { ref ->
            requireNotNull(RegattaDatabase.findCertificate(ref)) {
                "could not find certificate for result id ${record.result.id} ref = $ref"
            }
        }
        return card(record, classSchedules[record.bracket.classId]?.startDate, raceSchedule.race, orcCert)
    }

    /**
     * Pure card: elapsed time from the class start, PHRF and ORC time correction factors, and
     * corrected times. Places are left at 0 for [classCard] to fill in.
     */
    fun card(
        record: RaceResultBoatBracket,
        classStart: Instant?,
        race: Race,
        orcCert: OrcCertificate?,
    ): RaceReportCard {
        val result = record.result
        val boat = record.boatSkipper.boat
        val skipper = record.boatSkipper.skipper
        val time = result.finish?.let { finish ->
            classStart?.let { start ->
                finish - start
            }
        }

        val phrfTcf = if (result.ratingType.isCruising) 1.0 else phrfTcf(race.phrfBFactor, result.phrfRating)

        val orcTcf = orcCert?.let {
            orcTcf(race.orc3Band, race.orc5Band, race.orcScoringOption, it)
        } ?: 1.0

        return RaceReportCard(
            resultRecord = record,
            boatName = boat?.name ?: "",
            sail = boat?.sailNumber ?: "",
            skipper = skipper?.fullName() ?: "",
            boatType = boat?.boatType ?: "",
            phrfRating = result.phrfRating,
            startTime = classStart,
            finishTime = result.finish,
            elapsedTime = time,
            phrfTcf = phrfTcf,
            orcTcf = orcTcf,
            correctedPhrfTime = boatCorrectedTime(
                tcf = phrfTcf,
                start = classStart,
                finish = result.finish
            ).takeIf { result.finish != null },
            correctedOrcTime = boatCorrectedTime(
                tcf = orcTcf,
                start = classStart,
                finish = result.finish
            ).takeIf { result.finish != null },
            placeInBracket = 0,
            placeInClassOrc = 0,
            placeInClass = 0,
            placeInBracketOrc = 0,
            hocPosition = result.hocPosition,
            penalty = result.penalty,
        )
    }

    internal fun phrfTcf(factor: Int?, phrfRating: Int?): Double {
        return factor?.let { cf ->
            phrfRating?.let { rating ->
                650.0 / (cf.toDouble() + rating.toDouble())
            }
        } ?: 1.0
    }

    internal fun orcTcf(
        orc3Band: Orc3Band? = null,
        orc5Band: Orc5Band? = null,
        orcScoringOption: OrcScoringOption,
        orcCertificate: OrcCertificate
    ): Double {
        return when (orcScoringOption) {
            OrcScoringOption.SingleNumberAllPurpose -> orcCertificate.allPurposeTot
            OrcScoringOption.SingleNumberWindwardLeeward -> orcCertificate.wlSingleNumberTot
            OrcScoringOption.TripleNumberAllPurpose -> when (requireNotNull(orc3Band)) {
                Orc3Band.Low -> orcCertificate.tripleNumberAllPurposeLowTot
                Orc3Band.Medium -> orcCertificate.tripleNumberAllPurposeMedTot
                Orc3Band.High -> orcCertificate.tripleNumberAllPurposeHiTot
            }

            OrcScoringOption.TripleNumberWindwardLeeward -> when (requireNotNull(orc3Band)) {
                Orc3Band.Low -> orcCertificate.tripleNumberWlLowTot
                Orc3Band.Medium -> orcCertificate.tripleNumberWlMedTot
                Orc3Band.High -> orcCertificate.tripleNumberWlHiTot
            }

            OrcScoringOption.SingleNumberPredominantUpwind -> orcCertificate.singleNumberPredominantUpwindTot
            OrcScoringOption.SingleNumberPredominantReaching -> orcCertificate.singleNumberPredominantReachingTot
            OrcScoringOption.SingleNumberPredominantDownwind -> orcCertificate.singleNumberPredominantDownwindTot
            OrcScoringOption.PredominantUpwind -> when (requireNotNull(orc5Band)) {
                Orc5Band.Low -> orcCertificate.predominantUpwindLowTot
                Orc5Band.LowMedium -> orcCertificate.predominantUpwindLowMedTot
                Orc5Band.Medium -> orcCertificate.predominantUpwindMedTot
                Orc5Band.MediumHigh -> orcCertificate.predominantUpwindMedHiTot
                Orc5Band.High -> orcCertificate.predominantUpwindHiTot
            }

            OrcScoringOption.PredominantDownwind -> when (requireNotNull(orc5Band)) {
                Orc5Band.Low -> orcCertificate.predominantDownwindLowTot
                Orc5Band.LowMedium -> orcCertificate.predominantDownwindLowMedTot
                Orc5Band.Medium -> orcCertificate.predominantDownwindMedTot
                Orc5Band.MediumHigh -> orcCertificate.predominantDownwindMedHiTot
                Orc5Band.High -> orcCertificate.predominantDownwindHiTot
            }

            OrcScoringOption.PredominantReaching -> when (requireNotNull(orc5Band)) {
                Orc5Band.Low -> orcCertificate.predominantReachingLowTot
                Orc5Band.LowMedium -> orcCertificate.predominantReachingLowMedTot
                Orc5Band.Medium -> orcCertificate.predominantReachingMedTot
                Orc5Band.MediumHigh -> orcCertificate.predominantReachingMedHiTot
                Orc5Band.High -> orcCertificate.predominantReachingHiTot
            }

            OrcScoringOption.FiveBandWindwardLeeward -> when (requireNotNull(orc5Band)) {
                Orc5Band.Low -> orcCertificate.fiveBandWlLowTot
                Orc5Band.LowMedium -> orcCertificate.fiveBandWlLowMedTot
                Orc5Band.Medium -> orcCertificate.fiveBandWlMedTot
                Orc5Band.MediumHigh -> orcCertificate.fiveBandWlMedHiTot
                Orc5Band.High -> orcCertificate.fiveBandWlHiTot
            }

            OrcScoringOption.WindwardLeeward60_40 -> when (requireNotNull(orc5Band)) {
                Orc5Band.Low -> orcCertificate.usWl6040LTot
                Orc5Band.LowMedium -> orcCertificate.usWl6040LmTot
                Orc5Band.Medium -> orcCertificate.usWl6040MTot
                Orc5Band.MediumHigh -> orcCertificate.usWl6040MhTot
                Orc5Band.High -> orcCertificate.usWl6040HTot
            }

            OrcScoringOption.FiveBandAllPurpose -> when (requireNotNull(orc5Band)) {
                Orc5Band.Low -> orcCertificate.fiveBandAllPurposeLowTot
                Orc5Band.LowMedium -> orcCertificate.fiveBandAllPurposeLowMedTot
                Orc5Band.Medium -> orcCertificate.fiveBandAllPurposeMedTot
                Orc5Band.MediumHigh -> orcCertificate.fiveBandAllPurposeMedHiTot
                Orc5Band.High -> orcCertificate.fiveBandAllPurposeHiTot
            }
        }

    }

    internal fun boatCorrectedTime(tcf: Double, start: Instant?, finish: Instant?): Duration? {
        if (start != null && finish != null) {
            val ms = ((finish - start).inWholeMilliseconds) * tcf
            return ms.toDuration(DurationUnit.MILLISECONDS)
        }
        return null
    }
}

val cardComparePhrf: Comparator<RaceReportCard> = Comparator { lhs, rhs ->
    compare(lhs, rhs) { card ->
        card.correctedPhrfTime
    }
}

val cardCompareOrc: Comparator<RaceReportCard> = Comparator { lhs, rhs ->
    compare(lhs, rhs) { card ->
        card.correctedOrcTime
    }
}

fun compare(lhs: RaceReportCard, rhs: RaceReportCard, cTime: (RaceReportCard) -> Duration?): Int {
    // compare corrected time
    val lhsCorrectedTime = cTime(lhs)
    val rhsCorrectedTime = cTime(rhs)
    return if (lhsCorrectedTime != null && rhsCorrectedTime != null) {
        lhsCorrectedTime.inWholeMilliseconds.compareTo(rhsCorrectedTime.inWholeMilliseconds)
    } else if (lhsCorrectedTime != null) {
        -1
    } else if (rhsCorrectedTime != null) {
        1
    } else {
        // compare HOC
        if (lhs.hocPosition != null && rhs.hocPosition != null) {
            lhs.hocPosition!!.compareTo(rhs.hocPosition!!)
        } else if (lhs.hocPosition != null) {
            -1
        } else if (rhs.hocPosition != null) {
            1
        } else {
            lhs.resultRecord.result.finishCode.weight.compareTo(rhs.resultRecord.result.finishCode.weight)
        }
    }
}

private data class TempPlace(
    var place: Int,
    val card: RaceReportCard,
)

fun Iterable<RaceReportCard>.place(
    orc: Boolean = false,
    placeHandler: (Int, RaceReportCard) -> Unit
): List<RaceReportCard> {
    val cardList = if (orc) this.filter { it.ratingType().isORC } else this
    // GSI 14.1 with D3 (test_audit_and_plan.md): starters are every entry except RC volunteers and DNS;
    // finishers are timed finishers and HOC, so DNF = RET = finishers + 1 and NSC = starters + 1.
    // A boat's own score counts the *other* starters, as a DNF boat is not among the finishers it
    // is scored against: an NSC or DSQ boat is a starter for everyone else, not for itself.
    val starters = cardList.count { it.isStarter() }
    val finishers = cardList.count { it.isFinisher() }
    fun RaceReportCard.otherStarters() = if (isStarter()) starters - 1 else starters

    val cardCompare = if (orc) cardCompareOrc else cardComparePhrf
    //sorted by corrected time then HOC
    val list = cardList.sortedWith(cardCompare).let {
        var last: RaceReportCard? = null
        var position = 1
        var finished = 0
        it.map { ea ->
            when (ea.resultRecord.result.finishCode) {
                FinishCode.TIME,
                FinishCode.HOC -> {
                    // D7: equal corrected times share the place, and the next place skips (1, 1, 3)
                    finished++
                    if (last == null || cardCompare.compare(ea, last) != 0) {
                        position = finished
                    }
                    last = ea
                    TempPlace(place = position, card = ea)
                }

                FinishCode.DNS_RC,
                FinishCode.RET,
                FinishCode.DNF -> {
                    TempPlace(place = finishers + 1, card = ea)
                }

                FinishCode.NSC,
                FinishCode.DNS -> {
                    TempPlace(place = ea.otherStarters() + 1, card = ea)
                }

                FinishCode.DSQ -> {
                    TempPlace(place = ea.otherStarters() + 2, card = ea)
                }
            }
        }
    }.toMutableList()

    // D4 / GSI 9.2 / RRS 44.3(c): a penalty adds to the boat's own finishing place, capped at the
    // DNF score, and changes nobody else. Only timed finishers and HOC can carry one; a penalty
    // left on another code is ignored.
    list.forEach { tp ->
        val penalty = tp.card.penalty ?: return@forEach
        if (tp.card.isFinisher()) {
            tp.place = min(tp.place + penalty, finishers + 1)
        }
    }

    return list.sortedBy { it.place }.map {
        placeHandler(it.place, it.card)
        it.card
    }
}

/** GSI 14.1 / D3: every entry except RC volunteers (and DNS) started the race. */
fun RaceReportCard.isStarter(): Boolean = resultRecord.result.finishCode.isStarter

/** GSI 14.1 / D3: timed finishers and HOC boats are finishers. */
fun RaceReportCard.isFinisher(): Boolean = resultRecord.result.finishCode.isFinisher

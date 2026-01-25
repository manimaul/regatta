package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object RaceResultReporter {

    suspend fun getStandingsReport(seriesId: Long, year: Int): StandingsSeries? {
        val reports = RegattaDatabase.seriesRaces(seriesId, year).mapNotNull { raceId ->
            getReport(raceId)
        }.asSequence()

        val races = reports.map { it.raceSchedule }.sortedBy { it.startTime }.map { it.race }.toList()

        val standings = RegattaDatabase.findSeries(seriesId)?.let { series ->
            StandingsSeries(
                year = year,
                series = series,
                standings = getStandingsClass(races, reports),
                races = races
            )
        }

        //calculate place in class
        standings?.standings?.forEach { standingsClass: StandingsClass ->
            val bracketStandings = standingsClass.standings.flatMap { it.standings }

            var place = 0
            var previous: Long? = null
            bracketStandings.sortedWith { lhs, rhs ->
                lhs.totalScoreClass.compareTo(rhs.totalScoreClass).takeIf { it != 0 } ?: run {
                    standingsClass.raceClass
                    tieBreaker(races.size, lhs, rhs) { it.placeInClass }
                }
            }.forEach {
                if (it.tiedWith.contains(previous)) {
                    it.placeInClass = place
                } else {
                    it.placeInClass = ++place
                }
                previous = it.boatSkipper.boat?.id
            }
        }
        return standings
    }

    private fun tieBreaker(
        numberOfRacesInSeries: Int,
        lhs: StandingsBoatSkipper,
        rhs: StandingsBoatSkipper,
        field: (StandingsRace) -> Int
    ): Int {
        //https://cyct.com/wp-content/uploads/2025/01/2025-CYCT-General-Sailing-Instructions-Final.pdf
        //14.5. If two or more boats are tied in a series, the boat with the highest finishing place in
        //the last race of the series will be scored the series winner. This changes RRS Appendix
        // A8.1, A8.2
        if (lhs.raceStandings.size == numberOfRacesInSeries || rhs.raceStandings.size == numberOfRacesInSeries) {
            val left = lhs.raceStandings.getOrNull(numberOfRacesInSeries - 1)?.let { field(it) } ?: Int.MAX_VALUE
            val right = rhs.raceStandings.getOrNull(numberOfRacesInSeries - 1)?.let { field(it) } ?: Int.MAX_VALUE
            return left.compareTo(right)
        }

        //In the event of a tie:
        //a. If two or more boats have the same lowest individual overall score, then the
        //tiebreaker shall be determined by the total number of said lowest individual scores, with
        //the yacht having the highest count of said scores shall be the winner
        //b. If a tie still exists after (a) and (b) above, then the next lowest individual
        //score shall be used to break the tie, and so on until scores no longer match
        val min = (lhs.raceStandings + rhs.raceStandings).minOf { field(it) }
        val max = (lhs.raceStandings + rhs.raceStandings).maxOf { field(it) }
        (min..max).forEach { score ->
            val lCount = lhs.raceStandings.count { field(it) == score }
            val rCount = rhs.raceStandings.count { field(it) == score }
            if (lCount != rCount) {
                return rCount.compareTo(lCount)
            }
        }
        lhs.tiedWith.add(rhs.boatSkipper.boat?.id ?: 0)
        rhs.tiedWith.add(lhs.boatSkipper.boat?.id ?: 0)
        return 0
    }

    private fun getStandingsClass(
        races: List<Race>,
        reports: Sequence<RaceReport>,
    ): List<StandingsClass> {
        val classes = reports.flatMap { it.classReports }.map {
            it.raceClass
        }.distinctBy { it.id }.sortedBy { it.sort }.toList()

        return classes.map { raceClass ->
            StandingsClass(
                raceClass = raceClass,
                standings = getStandingsBracket(raceClass, races, reports)
            )
        }
    }

    private fun getStandingsBracket(
        raceClass: RaceClass,
        races: List<Race>,
        raceReports: Sequence<RaceReport>,
    ): List<StandingsBracket> {
        val brackets =
            raceReports.flatMap { it.classReports }.filter { it.raceClass.id == raceClass.id }
                .flatMap { it.bracketReport }.map { it.bracket }.distinctBy { it.id }
        return brackets.map {
            StandingsBracket(
                bracket = it,
                standings = getStandingsBoatBracket(raceClass, it, races, raceReports)
            )
        }.toList()
    }

    private fun getStandingsBoatBracket(
        raceClass: RaceClass,
        bracket: Bracket,
        races: List<Race>,
        raceReports: Sequence<RaceReport>,
    ): List<StandingsBoatSkipper> {

        val classRecords = raceReports.flatMap { it.classReports.filter { it.raceClass.id == raceClass.id } }
            .flatMap { it.bracketReport }.flatMap { it.cards }

        val bracketRecords = raceReports.flatMap { it.classReports.filter { it.raceClass.id == raceClass.id } }
            .flatMap { it.bracketReport }.filter { it.bracket.id == bracket.id }.flatMap { it.cards }


        val result = bracketRecords.groupBy { it.resultRecord.boatSkipper }.map {
            val boatSkipper = it.key
            val raceReportCards = it.value
            val standings = races.map { race ->
                raceReportCards.find { it.resultRecord.result.raceId == race.id }?.let {
                    StandingsRace(
                        nonStarter = false,
                        placeInBracket = it.placeInBracket,
                        placeInClass = it.placeInClass,
                        throwOut = false,
                        finish = it.finishTime != null,
                        hocPosition = it.hocPosition,
                        finishCode = it.resultRecord.result.finishCode
                    )
                } ?: nonStarterPlace(race, bracketRecords, classRecords)
            }.toList()

            if (standings.size >= 5) {
                throwOutWorst(standings)
            }

            /*
            1.4.1.2 If the registered boat chooses to volunteer individuals for Race Committee, and is unable to
            race, that boat will receive a score that is the average of their series score for that specific race. The
            boat’s overall score for racer of the year calculations will be average of their overall finishes in that
            series. The given score will not affect the other boats scored in that class. This can only be utilized
            once per series and cannot be used as a tie breaker per scoring rules. Additionally, sign-ups for the
            RC volunteers must be completed before the start of the series if you are intending to use this rule
            for your score. This amends Section 14.4, 14.5, and all appendices outlined in these two sections.
            As part of crew education and contributing to club support, Club Crew Members are strongly
            encouraged to volunteer once,
             */
            var avgPlaceBracket: Int? = null
            var avgPlaceClass: Int? = null
            standings.takeIf { it.size > 1 }?.forEach { ea ->
                if (ea.finishCode == FinishCode.DNS_RC) {
                    if (avgPlaceBracket == null || avgPlaceClass == null) {
                        val list = standings.filter { it.finishCode != FinishCode.DNS_RC }
                        avgPlaceBracket = list
                            .fold(0.0f) { l, r -> l + r.placeInBracket.toFloat() }.div(list.size).roundToInt()
                        avgPlaceClass = list
                            .fold(0.0f) { l, r -> l + r.placeInClass.toFloat() }.div(list.size).roundToInt()

                        standings.filter { it.finishCode == FinishCode.DNS_RC }.forEach { ea ->
                            ea.placeInBracketCorrected = avgPlaceBracket
                            ea.placeInClassCorrected = avgPlaceClass
                        }
                    }
                }
            }

            //apply logic here for RC volunteer
            StandingsBoatSkipper(
                boatSkipper = boatSkipper,
                raceStandings = standings,
                totalScoreBracket = standings.fold(0) { a, s -> a + if (s.throwOut) 0 else s.placeInBracket },
                totalScoreClass = standings.fold(0) { a, s -> a + if (s.throwOut) 0 else s.placeInClass },
                placeInBracket = 0,
                placeInClass = 0,
            )
        }

        var place = 0
        var previous: Long? = null
        result.sortedWith { lhs, rhs ->
            lhs.totalScoreBracket.compareTo(rhs.totalScoreBracket).takeIf { it != 0 } ?: run {
                tieBreaker(races.size, lhs, rhs) { it.placeInBracket }
            }
        }.forEach {
            if (it.tiedWith.contains(previous)) {
                it.placeInBracket = place
            } else {
                it.placeInBracket = ++place
            }
            previous = it.boatSkipper.boat?.id
        }
        return result.sortedBy { it.placeInBracket }
    }

    private fun throwOutWorst(standings: List<StandingsRace>) {
        val minBracket = standings.minOf { it.placeInBracket }
        val maxBracket = standings.maxOf { it.placeInBracket }
        if (minBracket != maxBracket) {
            standings.filter { it.placeInBracket == maxBracket }.maxByOrNull { it.placeInClass }?.throwOut = true
            return
        }

        val minClass = standings.minOf { it.placeInClass }
        val maxClass = standings.maxOf { it.placeInClass }
        if (minClass != maxClass) {
            standings.first { it.placeInClass == maxClass }.throwOut = true
            return
        }

        standings.lastOrNull()?.throwOut = true
    }

    private fun nonStarterPlace(
        race: Race,
        bracketRecords: Sequence<RaceReportCard>,
        classRecords: Sequence<RaceReportCard>,
    ): StandingsRace {
        val bracketStarters = bracketRecords.filter { it.resultRecord.result.raceId == race.id }
            .map { it.placeInBracket }
            .count()
        val classStarters = classRecords.filter { it.resultRecord.result.raceId == race.id }
            .map { it.placeInBracket }
            .count()

        return StandingsRace(
            nonStarter = true,
            finish = false,
            placeInBracket = bracketStarters + 1,
            placeInClass = classStarters + 1,
            hocPosition = null,
            finishCode = null,
        )
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

    suspend fun getReport(raceId: Long): RaceReport? {
        val classReportList = mutableListOf<ClassReportCards>()
        RegattaDatabase.findRaceSchedule(raceId)?.let { raceSchedule: RaceSchedule ->
            val schedules: Map<Long, ClassSchedule> = raceSchedule.schedule.associateBy { it.raceClass.id }

            //Calculate corrected time
            val allResults: List<RaceResultBoatBracket> = RegattaDatabase.resultsBoatBracketByRaceId(raceId)

            //Places
            raceSchedule.schedule.forEach { classSchedule: ClassSchedule ->
                when (val rt = classSchedule.raceClass.ratingType) {
                    RatingType.ORC,
                    RatingType.PHRF,
                    RatingType.ORC_PHRF -> {
                        val year = classSchedule.raceStart()?.toLocalDateTime(TimeZone.of("America/Los_Angeles"))?.year ?: 0
                        val raceClass = if (year >= 2026) {
                            classSchedule.raceClass
                        } else {
                            classSchedule.raceClass.copy(ratingType = RatingType.PHRF)
                        }

                        classCard(
                            raceSchedule,
                            classSchedule,
                            allResults.filter { it.result.ratingType.isORCorPHRF }.map {
                                reduceToCard(it, raceSchedule, schedules)
                            },
                            raceClass
                        )?.let { classReportList.add(it) }
                    }

                    RatingType.CruisingFlyingSails,
                    RatingType.CruisingNonFlyingSails -> classCard(
                        raceSchedule,
                        classSchedule,
                        allResults.filter { it.result.ratingType == rt }.map {
                            reduceToCard(it, raceSchedule, schedules)
                        },
                        classSchedule.raceClass,
                    )?.let { classReportList.add(it) }
                }
            }

            //Orphan results
            val orphans = allResults.filter { it.bracket.id == 0L }.map {
                reduceToCard(it, raceSchedule, schedules)
            }
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
        return null
    }

    private suspend fun reduceToCard(
        record: RaceResultBoatBracket,
        raceSchedule: RaceSchedule,
        classSchedules: Map<Long, ClassSchedule>
    ): RaceReportCard {
        val schedule = classSchedules[record.bracket.classId]
        val result = record.result
        val boat = record.boatSkipper.boat
        val skipper = record.boatSkipper.skipper
        val time = result.finish?.let { finish ->
            schedule?.startDate?.let { start ->
                finish - start
            }
        }
        val phrfBFactor = raceSchedule.race.phrfBFactor

        val phrfTcf = phrfTcf(phrfBFactor, result.phrfRating)

        val orcTcf = record.result.orcRef?.let { ref ->
            orcTcf(
                raceSchedule.race.orc3Band,
                raceSchedule.race.orc5Band,
                raceSchedule.race.orcScoringOption,
                requireNotNull(RegattaDatabase.findCertificate(ref)) {
                    "could not find certificate for result id ${record.result.id} ref = $ref"
                }
            )
        } ?: 1.0

        return RaceReportCard(
            resultRecord = record,
            boatName = boat?.name ?: "",
            sail = boat?.sailNumber ?: "",
            skipper = skipper?.fullName() ?: "",
            boatType = boat?.boatType ?: "",
            phrfRating = result.phrfRating,
            startTime = schedule?.startDate,
            finishTime = result.finish,
            elapsedTime = time,
            phrfTcf = phrfTcf,
            orcTcf = orcTcf,
            correctedPhrfTime = boatCorrectedTime(
                tcf = phrfTcf,
                start = schedule?.startDate,
                finish = result.finish
            ).takeIf { result.finish != null },
            correctedOrcTime = boatCorrectedTime(
                tcf = orcTcf,
                start = schedule?.startDate,
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

    private fun phrfTcf(factor: Int?, phrfRating: Int?): Double {
        return factor?.let { cf ->
            phrfRating?.let { rating ->
                650.0 / (cf.toDouble() + rating.toDouble())
            }
        } ?: 1.0
    }

    private fun orcTcf(
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

    private fun boatCorrectedTime(tcf: Double, start: Instant?, finish: Instant?): Duration? {
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

private data class PenaltyPosition(
    val num: Int,
    val position: Int,
)

private data class TempPlace(
    var place: Int,
    val card: RaceReportCard,
)

fun Iterable<RaceReportCard>.place(
    orc: Boolean = false,
    placeHandler: (Int, RaceReportCard) -> Unit
): List<RaceReportCard> {
    val cardList = if (orc) this.filter { it.ratingType().isORC } else this
    val penalties = mutableListOf<PenaltyPosition>()
    val starters = cardList.count()
    val finishers = cardList.count { it.resultRecord.result.finish != null }
    val hocCount = cardList.count { it.hocPosition != null }

    val cardCompare = if (orc) cardCompareOrc else cardComparePhrf
    //sorted by corrected time then HOC
    val list = cardList.sortedWith(cardCompare).let {
        var last: RaceReportCard? = null
        var position = 1
        it.mapIndexed { i, ea ->
            ea.penalty?.let {
                penalties.add(PenaltyPosition(it, i))
            }
            when (ea.resultRecord.result.finishCode) {
                FinishCode.TIME,
                FinishCode.HOC -> {
                    last?.let { theLast ->
                        if (cardCompare.compare(ea, theLast) == 1) {
                            position++
                        }
                    }
                    last = ea
                    TempPlace(place = position, card = ea)
                }

                FinishCode.DNS_RC,
                FinishCode.RET,
                FinishCode.DNF -> {
                    TempPlace(place = finishers + hocCount + 1, card = ea)
                }

                FinishCode.NSC -> {
                    TempPlace(place = starters, card = ea)
                }
            }
        }
    }.toMutableList()

    penalties.forEach { p ->
        val card = list.removeAt(p.position)
        val i = min(p.position + p.num - 1, list.size - 1)
        val place = card.place
        val penaltyPlace = card.place + p.num
        card.place = penaltyPlace
        list.add(i, card)
        list.forEach {
            if (it != card && it.place <= penaltyPlace && it.place > place) {
                it.place -= 1
            }
        }
    }

    return list.sortedBy { it.place }.map {
        placeHandler(it.place, it.card)
        it.card
    }
}

package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlinx.datetime.Instant
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

        val windseekerRecords = reports.map { it.classReports }.flatten()
            .map { it.bracketReport }.flatten().map { it.cards }.flatten().filter { it.windseeker != null }

        val phrfRecords = reports.map { it.classReports }.flatten()
            .map { it.bracketReport }.flatten().map { it.cards }.flatten().filter { it.windseeker == null }

        val standings = RegattaDatabase.findSeries(seriesId)?.let { series ->
            StandingsSeries(
                year = year,
                series = series,
                standings = getStandingsClass(races, reports, windseekerRecords, phrfRecords),
                races = races
            )
        }

        //calculate place in class
        standings?.standings?.forEach { standingsClass: StandingsClass ->
            val bracketStandings = standingsClass.standings.map { it.standings }.flatten()

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

        //calculate place overall
        standings?.standings?.map { it.standings }?.flatten()?.map { it.standings }?.flatten()
            ?.groupBy { it.boatSkipper.boat?.ratingType() }?.forEach { (_, standings) ->
                var place = 0
                var previous: Long? = null
                standings.sortedWith { lhs, rhs ->
                    lhs.totalScoreOverall.compareTo(rhs.totalScoreOverall).takeIf { it != 0 } ?: run {
                        tieBreaker(races.size, lhs, rhs) { it.placeOverall }
                    }
                }.forEach {
                    if (it.tiedWith.contains(previous)) {
                        it.placeOverall = place
                    } else {
                        it.placeOverall = ++place
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
        windseekerRecords: Sequence<RaceReportCard>,
        phrfRecords: Sequence<RaceReportCard>,
    ): List<StandingsClass> {
        val classes = reports.map { it.classReports }.flatten().map {
            it.raceClass
        }.distinctBy { it.id }.sortedBy { it.sort }.toList()

        return classes.map { raceClass ->
            StandingsClass(
                raceClass = raceClass,
                standings = getStandingsBracket(raceClass, races, reports, windseekerRecords, phrfRecords)
            )
        }
    }

    private fun getStandingsBracket(
        raceClass: RaceClass,
        races: List<Race>,
        raceReports: Sequence<RaceReport>,
        windseekerRecords: Sequence<RaceReportCard>,
        phrfRecords: Sequence<RaceReportCard>,
    ): List<StandingsBracket> {
        val brackets =
            raceReports.map { it.classReports }.flatten().filter { it.raceClass.id == raceClass.id }
                .map { it.bracketReport }.flatten().map { it.bracket }.distinctBy { it.id }
        return brackets.map {
            StandingsBracket(
                bracket = it,
                standings = getStandingsBoatBracket(raceClass, it, races, raceReports, windseekerRecords, phrfRecords)
            )
        }.toList()
    }

    private fun getStandingsBoatBracket(
        raceClass: RaceClass,
        bracket: Bracket,
        races: List<Race>,
        raceReports: Sequence<RaceReport>,
        windseekerRecords: Sequence<RaceReportCard>,
        phrfRecords: Sequence<RaceReportCard>,
    ): List<StandingsBoatSkipper> {

        val classRecords = raceReports.map { it.classReports.filter { it.raceClass.id == raceClass.id } }.flatten()
            .map { it.bracketReport }.flatten().map { it.cards }.flatten()

        val bracketRecords = raceReports.map { it.classReports.filter { it.raceClass.id == raceClass.id } }.flatten()
            .map { it.bracketReport }.flatten().filter { it.bracket.id == bracket.id }
            .map { it.cards }.flatten()


        val result = bracketRecords.groupBy { it.resultRecord.boatSkipper }.map {
            val boatSkipper = it.key
            val raceReportCards = it.value
            val standings = races.map { race ->
                val overallRecords = if (boatSkipper.boat?.windseeker != null) {
                    windseekerRecords.filter { it.resultRecord.raceSchedule.race.id == race.id }
                } else {
                    phrfRecords.filter { it.resultRecord.raceSchedule.race.id == race.id }
                }
                raceReportCards.find { it.resultRecord.raceSchedule.race.id == race.id }?.let {
                    StandingsRace(
                        nonStarter = false,
                        placeInBracket = it.placeInBracket,
                        placeInClass = it.placeInClass,
                        placeOverall = it.placeOverall,
                        throwOut = false,
                        finish = it.finishTime != null,
                        hocPosition = it.hocPosition,
                        finishCode = it.resultRecord.result.finishCode
                    )
                } ?: nonStarterPlace(race, bracketRecords, classRecords, overallRecords)
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
                println("each ${ea.finishCode}")
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
                totalScoreOverall = standings.fold(0) { a, s -> a + if (s.throwOut) 0 else s.placeOverall },
                placeInBracket = 0,
                placeInClass = 0,
                placeOverall = 0,
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
        overAllRecords: Sequence<RaceReportCard>,
    ): StandingsRace {
        val bracketStarters = bracketRecords.filter { it.resultRecord.raceSchedule.race.id == race.id }
            .map { it.placeInBracket }
            .count()
        val classStarters = classRecords.filter { it.resultRecord.raceSchedule.race.id == race.id }
            .map { it.placeInBracket }
            .count()
        val overallStarters = overAllRecords.filter { it.resultRecord.raceSchedule.race.id == race.id }
            .map { it.placeInBracket }
            .count()

        return StandingsRace(
            nonStarter = true,
            finish = false,
            placeInBracket = bracketStarters + 1,
            placeInClass = classStarters + 1,
            placeOverall = overallStarters + 1,
            hocPosition = null,
            finishCode = null,
        )
    }

    suspend fun getReport(raceId: Long): RaceReport? {
        val classReportList = mutableListOf<ClassReportCards>()
        RegattaDatabase.findRaceSchedule(raceId)?.let { raceSchedule ->
            val schedules = raceSchedule.schedule.associateBy { it.raceClass.id }
            val boatCards = RegattaDatabase.resultsBoatBracketByRaceId(raceId).map { reduceToCard(it, schedules) }

            //PHRF Overall Places
            boatCards.filter { it.phrfRating != null }.place { p, card ->
                card.placeOverall = p
            }

            //Cruising Overall Places
            boatCards.filter { it.phrfRating == null }.place { p, card ->
                card.placeOverall = p
            }

            raceSchedule.schedule.forEach { classSchedule ->
                val raceClass = classSchedule.raceClass

                //Class Places
                val classCards = boatCards.filter { it.resultRecord.bracket.classId == raceClass.id }
                    .place { p, card ->
                        card.placeInClass = p
                    }

                val bracketCards = classSchedule.brackets.mapNotNull { bracket ->

                    val bracketCards = classCards.filter { it.resultRecord.bracket.id == bracket.id }
                        .place { p, card ->
                            card.placeInBracket = p
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
                ClassReportCards(
                    raceClass = raceClass,
                    bracketReport = bracketCards,
                    correctionFactor = raceSchedule.race.correctionFactor
                ).takeIf { it.bracketReport.isNotEmpty() }?.also {
                    classReportList.add(it)
                }
            }

            //Orphan results
            val orphans = boatCards.filter { it.resultRecord.bracket.id == 0L }
            orphans.place { i, it ->
                it.placeInBracket = i
                it.placeOverall = i
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
                        correctionFactor = raceSchedule.race.correctionFactor
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

    private fun reduceToCard(record: RaceResultBoatBracket, classSchedules: Map<Long, ClassSchedule>): RaceReportCard {
        val schedule = classSchedules[record.bracket.classId]
        val result = record.result
        val boat = record.boatSkipper.boat
        val skipper = record.boatSkipper.skipper
        val time = result.finish?.let { finish ->
            schedule?.startDate?.let { start ->
                finish - start
            }
        }

        return RaceReportCard(
            resultRecord = record,
            boatName = boat?.name ?: "",
            sail = boat?.sailNumber ?: "",
            skipper = skipper?.fullName() ?: "",
            boatType = boat?.boatType ?: "",
            phrfRating = result.phrfRating,
            windseeker = result.windseeker,
            startTime = schedule?.startDate,
            finishTime = result.finish,
            elapsedTime = time,
            correctionFactor = correctionFactor(record.raceSchedule.race.correctionFactor, result.phrfRating),
            correctedTime = boatCorrectedTime(
                record.raceSchedule.race.correctionFactor,
                schedule?.startDate,
                result.finish,
                result.phrfRating,
            ).takeIf { result.finish != null },
            placeInBracket = 0,
            placeInClass = 0,
            placeOverall = 0,
            hocPosition = result.hocPosition,
            penalty = result.penalty,
        )
    }

    private fun correctionFactor(factor: Int?, phrfRating: Int?): Double {
        return factor?.let { cf ->
            phrfRating?.let { rating ->
                650.0 / (cf.toDouble() + rating.toDouble())
            }
        } ?: 1.0
    }

    private fun boatCorrectedTime(factor: Int?, start: Instant?, finish: Instant?, phrfRating: Int?): Duration? {
        if (start != null && finish != null) {
            val cf = correctionFactor(factor, phrfRating)
            val ms = ((finish - start).inWholeMilliseconds) * cf
            return ms.toDuration(DurationUnit.MILLISECONDS)
        }
        return null
    }
}


val cardCompare: Comparator<RaceReportCard> = Comparator { lhs, rhs ->
    compare(lhs, rhs)
}

fun compare(lhs: RaceReportCard, rhs: RaceReportCard): Int {
    // compare corrected time
    return if (lhs.correctedTime != null && rhs.correctedTime != null) {
        lhs.correctedTime!!.inWholeMilliseconds.compareTo(rhs.correctedTime!!.inWholeMilliseconds)
    } else if (lhs.correctedTime != null) {
        -1
    } else if (rhs.correctedTime != null) {
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

fun Iterable<RaceReportCard>.place(placeHandler: (Int, RaceReportCard) -> Unit): List<RaceReportCard> {
    val penalties = mutableListOf<PenaltyPosition>()
    val starters = this.count()
    val finishers = this.count { it.resultRecord.result.finish != null }
    val hocCount = this.count { it.hocPosition != null }

    //sorted by corrected time then HOC
    val list = this.sortedWith(cardCompare).let {
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
                        if (compare(ea, theLast) == 1) {
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

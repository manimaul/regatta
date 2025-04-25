package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlinx.datetime.Instant
import kotlin.math.min
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
                    tieBreaker(lhs, rhs) { it.placeInClass }
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
                        tieBreaker(lhs, rhs) { it.placeOverall }
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

    private fun tieBreaker(lhs: StandingsBoatSkipper, rhs: StandingsBoatSkipper, field: (StandingsRace) -> Int) : Int {
        return lhs.raceStandings.reversed().zip(
            rhs.raceStandings.reversed()
        ).firstOrNull {
            field(it.first) != field(it.second)
        }?.let {
            field(it.first).compareTo(field(it.second))
        } ?: run {
            lhs.tiedWith.add(rhs.boatSkipper.boat?.id ?: 0)
            rhs.tiedWith.add(lhs.boatSkipper.boat?.id ?: 0)
            0
        }
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
                standings.sortedBy { it.placeInBracket }.last().let { it.throwOut = true }
            }

            StandingsBoatSkipper(
                boatSkipper = boatSkipper,
                raceStandings = standings,
                totalScoreBracket = standings.fold(0) { a, s -> a + if (s.throwOut) 0 else s.placeInBracket },
                totalScoreClass = standings.fold(0) { a, s -> a + if(s.throwOut) 0 else s.placeInClass },
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
                tieBreaker(lhs, rhs) { it.placeInBracket }
            }
        }.forEach {
            if (it.tiedWith.contains(previous)) {
                it.placeInBracket= place
            } else {
                it.placeInBracket= ++place
            }
            previous = it.boatSkipper.boat?.id
        }
        return result.sortedBy { it.placeInBracket }
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
                            cards = bracketCards
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
                classReports = classReportList
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
    val finishers = this.count { it.resultRecord.result.finish != null}

    //sorted by corrected time then HOC
    val list = this.sortedWith(cardCompare).let{
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

                FinishCode.RET,
                FinishCode.DNF -> {
                    TempPlace(place= finishers + 1, card = ea )
                }

                FinishCode.NSC -> {
                    TempPlace(place= starters, card = ea )
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

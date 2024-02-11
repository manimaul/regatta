package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object RaceResultReporter {


    suspend fun getReport(raceId: Long): RaceReport? {
        val classReportList = mutableListOf<ClassReportCards>()
        RegattaDatabase.findRaceSchedule(raceId)?.let { raceSchedule ->
            val boatCards = RegattaDatabase.resultsByRaceId(raceId).map { reduceToCard(it) }

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
                    category = raceClass,
                    bracketReport = bracketCards,
                    correctionFactor = raceSchedule.race.correctionFactor
                ).takeIf { it.bracketReport.isNotEmpty() }?.also {
                    classReportList.add(it)
                }
            }

            return RaceReport(
                raceSchedule = raceSchedule,
                classReports = classReportList
            )
        }
        return null
    }

    private fun reduceToCard(record: RaceResultBoatBracket): RaceReportCard {
        val result = record.result
        val boat = record.boatSkipper.boat
        val skipper = record.boatSkipper.skipper
        val time = result.finish?.let { finish ->
            result.start?.let { start ->
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
            startTime = result.start,
            finishTime = result.finish,
            elapsedTime = time,
            correctionFactor = correctionFactor(record.raceSchedule.race.correctionFactor, result.phrfRating),
            correctedTime = boatCorrectedTime(
                record.raceSchedule.race.correctionFactor,
                result.start,
                result.finish,
                result.phrfRating,
            ).takeIf { result.start != null && result.finish != null },
            placeInBracket = 0,
            placeInClass = 0,
            placeOverall = 0,
            hocPosition = result.hocPosition,
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
        lhs.correctedTime.inWholeMilliseconds.compareTo(rhs.correctedTime.inWholeMilliseconds)
    } else if (lhs.correctedTime != null) {
        -1
    } else if (rhs.correctedTime != null) {
        1
    } else {
        // compare HOC
        if (lhs.hocPosition != null && rhs.hocPosition != null) {
            lhs.hocPosition.compareTo(rhs.hocPosition)
        } else if (lhs.hocPosition != null) {
            -1
        } else if (rhs.hocPosition != null) {
            1
        } else {
            // compare DNS to DNF
            if (lhs.startTime != null && rhs.startTime != null) {
                0
            } else if (lhs.startTime != null) {
                -1
            } else if (rhs.startTime != null) {
                1
            } else {
                0
            }
        }
    }
}

fun Iterable<RaceReportCard>.place(place: (Int, RaceReportCard) -> Unit): List<RaceReportCard> {
    return this.sortedWith(cardCompare).also {
        var last: RaceReportCard? = null
        var position = 1
        it.forEach { ea ->
            last?.let {
                if (compare(ea, it) == 1) {
                    position++
                }
            }
            place(position, ea)
            last = ea
        }
    }
}

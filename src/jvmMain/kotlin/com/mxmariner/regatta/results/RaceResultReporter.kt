package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object RaceResultReporter {


    suspend fun getReport(raceId: Long): RaceReport? {
        val reportCategories = mutableListOf<RaceReportCategory>()
        RegattaDatabase.findRaceSchedule(raceId)?.let { raceSchedule ->
            val cards = RegattaDatabase.resultsByRaceId(raceId).map { reduceToCard(it) }

            //PHRF
            cards.filter { it.phrfRating != null }.place { p, card ->
                card.placeOverall = p
            }

            //Cruising
            cards.filter { it.phrfRating == null }.place { p, card ->
                card.placeOverall = p
            }

            //class places
            raceSchedule.schedule.forEach { each ->
                val raceClass = each.raceClass
                each.brackets.forEach { bt ->
                    val classCards = cards.filter { it.resultRecord.bracket.classId == bt.classId }
                        .place { p, card ->
                            card.placeInClass = p
                        }

                    //bracket places
                    val brackets =
                        classCards.distinctBy { it.resultRecord.bracket.id }.map { it.resultRecord.bracket }
                            .mapNotNull { raceClass ->
                                RaceReportClass(
                                    bracket = raceClass,
                                    cards = cards.filter { it.resultRecord.bracket.id == raceClass.id }
                                        .place { p, card ->
                                            card.placeInBracket = p
                                        }
                                ).takeIf { it.cards.isNotEmpty() }
                            }

                    RaceReportCategory(
                        category = raceClass,
                        brackets = brackets,
                        correctionFactor = raceSchedule.race.correctionFactor
                    ).takeIf { it.brackets.isNotEmpty() }?.also {
                        reportCategories.add(it)
                    }
                }
            }

            return RaceReport(
                raceSchedule = raceSchedule,
                categories = reportCategories
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

fun compare(lhs: RaceReportCard, rhs: RaceReportCard) : Int {
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
        it.forEach {  ea ->
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

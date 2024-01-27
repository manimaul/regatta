package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.RegattaDatabase
import kotlinx.datetime.Instant
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object RaceResultReporter {

    private val cardCompare: Comparator<RaceReportCard> = Comparator { lhs, rhs ->
        if (lhs.correctedTime != null && rhs.correctedTime != null) {
            lhs.correctedTime.inWholeMilliseconds.compareTo(rhs.correctedTime.inWholeMilliseconds)
        } else if (lhs.correctedTime != null) {
            -1
        } else if (rhs.correctedTime != null) {
            1
        } else {
            if (lhs.hocPosition != null && rhs.hocPosition != null) {
                lhs.hocPosition.compareTo(rhs.hocPosition)
            } else if (lhs.hocPosition != null) {
                -1
            } else if (rhs.hocPosition != null) {
                1
            } else {
                0
            }
        }
    }

    suspend fun getReport(raceId: Long): RaceReport? {
        val reportCategories = mutableListOf<RaceReportCategory>()
        RegattaDatabase.findRace(raceId)?.let { raceFull ->
            val cards = RegattaDatabase.resultsByRaceId(raceId).map { reduceToCard(it) }

            //PHRF
            cards.filter { it.phrfRating != null }.sortedWith(cardCompare).forEachIndexed { i, card ->
                card.placeOverall = i + 1
            }

            //Cruising
            cards.filter { it.phrfRating == null }.sortedWith(cardCompare).forEachIndexed { i, card ->
                card.placeOverall = i + 1
            }

            //class category places
            raceFull.raceTimes.forEach { rt ->
                val cat = rt.raceClassCategory
                val catCards = cards.filter { it.resultRecord.raceClass.category == cat.id }
                    .sortedWith(cardCompare)
                catCards.forEachIndexed { i, card ->
                    card.placeInClass = i + 1
                }

                //class places
                val categoryClasses =
                    catCards.distinctBy { it.resultRecord.raceClass.id }.map { it.resultRecord.raceClass }
                        .mapNotNull { raceClass ->
                            RaceReportClass(
                                raceClass = raceClass,
                                cards = cards.filter { it.resultRecord.raceClass.id == raceClass.id }
                                    .sortedWith(cardCompare).also {
                                        it.forEachIndexed { i, card ->
                                            card.placeInBracket = i + 1
                                        }
                                    }
                            ).takeIf { it.cards.isNotEmpty() }
                        }

                RaceReportCategory(
                    category = cat.toCategory(),
                    classes = categoryClasses
                ).takeIf { it.classes.isNotEmpty() }?.also {
                    reportCategories.add(it)
                }
            }

            return RaceReport(
                race = raceFull,
                categories = reportCategories
            )
        }
        return null
    }

    private fun reduceToCard(result: RaceResultFull): RaceReportCard {
        val raceTime = boatRaceTime(result.race, result.boat)
        val time = result.finish?.let { finish ->
            result.start?.let { start ->
                finish - start
            }
        }

        return RaceReportCard(
            resultRecord = result,
            boatName = result.boat.name,
            sail = result.boat.sailNumber,
            skipper = result.boat.skipper?.fullName() ?: "",
            boatType = result.boat.boatType,
            phrfRating = result.phrfRating,
            startTime = result.start,
            finishTime = result.finish,
            elapsedTime = time,
            correctionFactor = correctionFactor(raceTime?.correctionFactor, result.boat),
            correctedTime = boatCorrectedTime(
                raceTime?.correctionFactor,
                result.start,
                result.finish,
                result.boat
            ).takeIf { result.start != null && result.finish != null },
            placeInBracket = 0,
            placeInClass = 0,
            placeOverall = 0,
            hocPosition = result.hocPosition,
        )
    }

    private fun correctionFactor(factor: Int?, boat: Boat?): Double {
        return factor?.let { cf ->
            boat?.phrfRating?.let { rating ->
                650.0 / (cf.toDouble() + rating.toDouble())
            }
        } ?: 1.0
    }

    private fun boatRaceTime(race: RaceFull, boat: Boat?): RaceTime? {
        return boat?.raceClass?.let { brc ->
            race.raceTimes.firstOrNull { raceTime ->
                raceTime.raceClassCategory.id == brc.category
            }
        }
    }

    private fun boatCorrectedTime(factor: Int?, start: Instant?, finish: Instant?, boat: Boat?): Duration? {
        if (start != null && finish != null) {
            val cf = correctionFactor(factor, boat)
            val seconds = ((finish - start).inWholeSeconds.toDouble() * cf).roundToLong()
            return seconds.toDuration(DurationUnit.SECONDS)
        }
        return null
    }
}


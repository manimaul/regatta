package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.BoatTable.resultRowToBoat
import com.mxmariner.regatta.db.RaceTable.findRaceSchedule
import com.mxmariner.regatta.db.RaceTable.rowToRace
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RaceResultsTable : Table() {
    val id = long("id").autoIncrement()
    val raceId = (long("race_id") references RaceTable.id)
    val boatId = (long("boat_id") references BoatTable.id)
    val finish = timestamp("end_date").nullable().index()
    val phrfRating = integer("phrf_rating").nullable()
    val orcTot = double("orc_tot").nullable()
    val wsFlying = bool("ws_flying").nullable()
    val hoc = integer("hoc").nullable()
    val raceClass = (long("class_id") references RaceClassTable.id).nullable()
    val bracket = (long("bracket_id") references BracketTable.id).nullable()
    val finishCode = varchar("finish_code", 128).nullable()
    val penalty = integer("penalty").nullable()

    override val primaryKey = PrimaryKey(id)

    fun count(raceId: Long): Long {
        return RaceResultsTable.select { RaceResultsTable.raceId eq raceId }.count()
    }

    fun upsertResult(result: RaceResult): RaceResult? {
        deleteWhere { raceId.eq(result.raceId).and(boatId.eq(result.boatId)) }
        return insert {
            if (result.id > 0) {
                it[id] = result.id
            }
            it[raceId] = result.raceId
            it[boatId] = result.boatId
            it[finish] = result.finish.takeIf { result.finishCode == FinishCode.TIME }
            when (result.ratingType) {
                RatingType.ORC -> {
                    it[orcTot] = result.orcTot
                }

                RatingType.ORC_PHRF -> {
                    it[phrfRating] = result.phrfRating
                    it[orcTot] = result.orcTot
                }

                RatingType.PHRF -> {
                    it[phrfRating] = result.phrfRating
                }

                RatingType.CruisingFlyingSails -> {
                    it[wsFlying] = true
                }

                RatingType.CruisingNonFlyingSails -> {
                    it[wsFlying] = false
                }
            }
            it[hoc] = result.hocPosition.takeIf { result.finishCode == FinishCode.HOC }
            it[penalty] = result.penalty
            it[finishCode] = result.finishCode.name
            it[raceClass] = result.raceClassId
            it[bracket] = result.bracketId
        }.resultedValues?.map(::rowToResult)?.singleOrNull()
    }

    fun allResults(): List<RaceResult> {
        return RaceResultsTable.selectAll().map(::rowToResult)
    }

    fun raceCount(boatId: Long): Long {
        return RaceResultsTable.select { RaceResultsTable.boatId eq boatId }.count()
    }

    fun resultsBoatBracketByRaceId(rId: Long): List<RaceResultBoatBracket> {
        return innerJoin(RaceTable).innerJoin(BoatTable).select {
            raceId.eq(rId)
        }.map(::rowToRaceResultBoatBracket)
    }

    fun resultsByRaceId(rId: Long): List<RaceResult> {
        return RaceResultsTable.select { raceId.eq(rId) }.map(::rowToResult)
    }

    fun getResults(year: Int): List<RaceResultBoatBracket> {
        val start = Instant.parse("$year-01-01")
        val end = Instant.parse("${year + 1}-01-01")
        return innerJoin(RaceTable).innerJoin(BoatTable).select {
            finish.greaterEq(start) and finish.less(end)
        }.map(::rowToRaceResultBoatBracket)
    }

    fun rowToRaceResultBoatBracket(row: ResultRow): RaceResultBoatBracket {
        val raceSchedule = rowToRace(row).let {
            findRaceSchedule(it.id) ?: RaceSchedule()
        }

        val result = rowToResult(row)
        val bracket = findBoatBracket(raceSchedule, result)
        return RaceResultBoatBracket(
            result = result,
            raceSchedule = raceSchedule,
            boatSkipper = BoatSkipper(
                boat = resultRowToBoat(row)
                    .copy(
                        ratingType = result.ratingType,
                        phrfRating = result.phrfRating,
                    ),
                skipper = row[BoatTable.skipper]?.let { PersonTable.selectPerson(it) }
            ),
            bracket = bracket ?: Bracket(),
        )
    }

    fun rowToResult(row: ResultRow): RaceResult {
        val time = row[finish]
        val code = row[finishCode]?.let { FinishCode.valueOf(it) } ?: run {
            time?.let { FinishCode.TIME } ?: FinishCode.RET
        }
        val phrf= row[phrfRating]
        val tot = row[orcTot]
        val flying = row[wsFlying]
        val rating = when {
            phrf != null && tot != null -> RatingType.ORC_PHRF
            phrf != null -> RatingType.PHRF
            tot != null -> RatingType.ORC
            flying == true -> RatingType.CruisingFlyingSails
            else -> RatingType.CruisingNonFlyingSails
        }
        return RaceResult(
            id = row[id],
            raceId = row[raceId],
            boatId = row[boatId],
            finish = time,
            phrfRating = phrf,
            orcTot = tot,
            ratingType = rating,
            hocPosition = row[hoc],
            penalty = row[penalty],
            raceClassId = row[raceClass],
            bracketId = row[bracket],
            finishCode = code
        )
    }
}

fun findBoatBracket(race: RaceSchedule, result: RaceResult): Bracket? {
    return if (result.bracketId != null) {
        race.schedule.firstNotNullOfOrNull { sch ->
            sch.brackets.firstOrNull {
                it.id == result.bracketId
            }
        }
    } else {
        when (result.ratingType) {
            RatingType.ORC,
            RatingType.ORC_PHRF -> throw IllegalStateException("ORC rating types require bracket registration")

            RatingType.PHRF -> {
                val phrfRating = requireNotNull(result.phrfRating)
                race.schedule.firstNotNullOfOrNull { sch ->
                    sch.brackets.takeIf { sch.raceClass.ratingType == RatingType.PHRF }?.firstOrNull {
                        phrfRating >= it.minRating && phrfRating <= it.maxRating
                    }
                }
            }

            RatingType.CruisingFlyingSails -> {
                race.schedule.firstNotNullOfOrNull { schedule ->
                    schedule.brackets.firstOrNull { schedule.raceClass.ratingType == RatingType.CruisingFlyingSails }
                }
            }

            RatingType.CruisingNonFlyingSails -> {
                race.schedule.firstNotNullOfOrNull { schedule ->
                    schedule.brackets.firstOrNull { schedule.raceClass.ratingType == RatingType.CruisingNonFlyingSails }
                }
            }
        }
    }
}

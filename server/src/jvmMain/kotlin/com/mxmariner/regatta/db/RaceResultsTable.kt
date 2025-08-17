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
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    val wsRating = integer("ws_rating").nullable()
    val wsFlying = bool("ws_flying").nullable()
    val hoc = integer("hoc").nullable()
    val raceClass = (long("class_id") references RaceClassTable.id).nullable()
    val bracket = (long("bracket_id") references BracketTable.id).nullable()

    val finishCode = varchar("finish_code", 128).nullable()
    val penalty= integer("penalty").nullable()

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
            result.windseeker?.let { ws ->
                it[wsRating] = ws.rating
                it[wsFlying] = ws.flyingSails
            } ?: run {
                it[phrfRating] = result.phrfRating
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

    fun raceCount(boatId: Long) : Long {
        return RaceResultsTable.select { RaceResultsTable.boatId eq boatId}.count()
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
                        windseeker = result.windseeker,
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
        return RaceResult(
            id = row[id],
            raceId = row[raceId],
            boatId = row[boatId],
            finish = time,
            phrfRating = row[phrfRating],
            windseeker = row[wsRating]?.let { r ->
                Windseeker(r, row[wsFlying] ?: false)
            },
            hocPosition = row[hoc],
            penalty = row[penalty],
            raceClassId = row[raceClass],
            bracketId = row[bracket],
            finishCode = code
        )
    }
}

fun findBoatBracket(race: RaceSchedule, result: RaceResult): Bracket? {
    val phrfRating = result.phrfRating
    val windseeker = result.windseeker
    return if (result.bracketId != null) {
        race.schedule.firstNotNullOfOrNull { sch ->
            sch.brackets.firstOrNull {
                it.id == result.bracketId
            }
        }
    }
    else if (phrfRating != null) {
        race.schedule.firstNotNullOfOrNull { sch ->
            sch.brackets.takeIf { sch.raceClass.isPHRF }?.firstOrNull {
                phrfRating >= it.minRating && phrfRating <= it.maxRating
            }
        }
    } else if (windseeker?.flyingSails == true) {
        race.schedule.firstNotNullOfOrNull { schedule ->
            schedule.brackets.takeIf { schedule.raceClass.wsFlying }?.firstOrNull {
                windseeker.rating >= it.minRating && windseeker.rating <= it.maxRating
            }
        }
    } else if (windseeker != null) {
        race.schedule.firstNotNullOfOrNull { schedule ->
            schedule.brackets.takeIf { !schedule.raceClass.isPHRF && !schedule.raceClass.wsFlying }?.firstOrNull {
                windseeker.rating >= it.minRating && windseeker.rating <= it.maxRating
            }
        }
    } else {
        null
    }
}

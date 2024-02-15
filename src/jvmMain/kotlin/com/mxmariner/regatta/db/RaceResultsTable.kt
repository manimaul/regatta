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
    //todo(DEPRECATED)
    val start = timestamp("start_date").nullable()
    val startCode = integer("start_code").nullable()
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    val wsRating = integer("ws_rating").nullable()
    val wsFlying = bool("ws_flying").nullable()
    val hoc = integer("hoc").nullable()
    override val primaryKey = PrimaryKey(id)

    fun count(raceId: Long): Long {
        return RaceResultsTable.select { RaceResultsTable.raceId eq raceId }.count()
    }

    fun updateStartCodes() {
        update(where = { start.isNull() }) {
            it[startCode] = StartCode.DNS.code
        }
    }

    fun upsertResult(result: RaceResult): RaceResult? {
        deleteWhere { raceId.eq(result.raceId).and(boatId.eq(result.boatId)) }
        return insert {
            if (result.id > 0) {
                it[id] = result.id
            }
            it[raceId] = result.raceId
            it[boatId] = result.boatId
            it[startCode] = result.startCode?.code
            it[finish] = result.finish
            result.windseeker?.let { ws ->
                it[wsRating] = ws.rating
                it[wsFlying] = ws.flyingSails
            } ?: run {
                it[phrfRating] = result.phrfRating
            }
            it[hoc] = result.hocPosition
        }.resultedValues?.map(::rowToResult)?.singleOrNull()
    }

    fun allResults(): List<RaceResult> {
        return RaceResultsTable.selectAll().map(::rowToResult)
    }

    fun resultsByRaceId(rId: Long): List<RaceResultBoatBracket> {
        return innerJoin(RaceTable).innerJoin(BoatTable).select {
            raceId.eq(rId)
        }.map(::rowToRaceResultBoatBracket)
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
                boat = resultRowToBoat(row),
                skipper = row[BoatTable.skipper]?.let { PersonTable.selectPerson(it) }
            ),
            bracket = bracket ?: Bracket(),
        )
    }

    fun rowToResult(row: ResultRow) = RaceResult(
        id = row[id],
        raceId = row[raceId],
        boatId = row[boatId],
        finish = row[finish],
        startCode = StartCode.from(row[startCode]),
        phrfRating = row[phrfRating],
        windseeker = row[wsRating]?.let { r ->
            Windseeker(r, row[wsFlying] ?: false)
        },
        hocPosition = row[hoc],
    )
}

fun findBoatBracket(race: RaceSchedule, result: RaceResult): Bracket? {
    return if (result.phrfRating != null) {
        race.schedule.firstNotNullOfOrNull { sch ->
            sch.brackets.takeIf { sch.raceClass.isPHRF }?.firstOrNull {
                result.phrfRating >= it.minRating && result.phrfRating <= it.maxRating
            }
        }
    } else if (result.windseeker?.flyingSails == true) {
        race.schedule.firstNotNullOfOrNull { schedule ->
            schedule.brackets.takeIf { schedule.raceClass.wsFlying }?.firstOrNull {
                result.windseeker.rating >= it.minRating && result.windseeker.rating <= it.maxRating
            }
        }
    } else if (result.windseeker != null) {
        race.schedule.firstNotNullOfOrNull { schedule ->
            schedule.brackets.takeIf { !schedule.raceClass.isPHRF && !schedule.raceClass.wsFlying }?.firstOrNull {
                result.windseeker.rating >= it.minRating && result.windseeker.rating <= it.maxRating
            }
        }
    } else {
        null
    }
}

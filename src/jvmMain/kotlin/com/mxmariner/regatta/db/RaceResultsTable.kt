package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.BoatTable.resultRowToBoat
import com.mxmariner.regatta.db.BracketTable.resultRowToBracket
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
    val start = timestamp("start_date").nullable()
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    val hoc = integer("hoc").nullable()
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
            it[start] = result.start
            it[finish] = result.finish
            it[phrfRating] = result.phrfRating
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
        val boat = resultRowToBoat(row)
        val raceSchedule = rowToRace(row).let {
            findRaceSchedule(it.id) ?: RaceSchedule()
        }
        val bracket = findBoatBracket(raceSchedule, boat)
        return RaceResultBoatBracket(
            result = rowToResult(row),
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
        start = row[start],
        finish = row[finish],
        phrfRating = row[phrfRating],
        hocPosition = row[hoc],
    )
}

fun findBoatBracket(race: RaceSchedule?, boat: Boat?): Bracket? {
    return if (boat?.phrfRating != null) {
        race?.schedule?.firstNotNullOfOrNull { sch ->
            sch.brackets.takeIf { sch.raceClass.isPHRF }?.firstOrNull {
                boat.phrfRating >= it.minRating && boat.phrfRating <= it.maxRating
            }
        }
    } else if (boat?.windseeker?.flyingSails == true) {
        race?.schedule?.firstNotNullOfOrNull { schedule ->
            schedule.brackets.takeIf { schedule.raceClass.wsFlying }?.firstOrNull {
                boat.windseeker.rating >= it.minRating && boat.windseeker.rating <= it.maxRating
            }
        }
    } else if (boat?.windseeker != null) {
        race?.schedule?.firstNotNullOfOrNull { schedule ->
            schedule.brackets.takeIf { !schedule.raceClass.isPHRF && !schedule.raceClass.wsFlying }?.firstOrNull {
                boat.windseeker.rating >= it.minRating && boat.windseeker.rating <= it.maxRating
            }
        }
    } else {
        null
    }
}

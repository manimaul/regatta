package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.BoatTable.resultRowToBoat
import com.mxmariner.regatta.db.BracketTable.resultRowToBracket
import com.mxmariner.regatta.db.RaceTable.findRaceSchedule
import com.mxmariner.regatta.db.RaceTable.rowToRace
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RaceResultsTable : Table() {
    val id = long("id").autoIncrement()
    val raceId = (long("race_id") references RaceTable.id)
    val boatId = (long("boat_id") references BoatTable.id)
    val bracketId = (long("bracket_id") references BracketTable.id)
    val start = timestamp("start_date").nullable()
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    val hoc = integer("hoc").nullable()
    override val primaryKey = PrimaryKey(id)

    fun count(raceId: Long): Long {
        return RaceResultsTable.select { RaceResultsTable.raceId eq raceId }.count()
    }

    fun upsertResult(result: RaceResult): RaceResult? {
        return upsert {
            if (result.id > 0) { it[id] = result.id }
            it[raceId] = result.raceId
            it[boatId] = result.boatId
            it[bracketId] = result.bracketId
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
        return innerJoin(RaceTable).innerJoin(BoatTable).innerJoin(BracketTable).select {
            raceId.eq(rId)
        }.map(::rowToRaceResultBoatBracket)
    }


    fun getResults(year: Int): List<RaceResultBoatBracket> {
        val start = Instant.parse("$year-01-01")
        val end = Instant.parse("${year + 1}-01-01")
        return innerJoin(RaceTable).innerJoin(BoatTable).innerJoin(BracketTable).select {
            finish.greaterEq(start) and finish.less(end)
        }.map(::rowToRaceResultBoatBracket)
    }

    fun rowToRaceResultBoatBracket(row: ResultRow) = RaceResultBoatBracket(
        result = rowToResult(row),
        raceSchedule = rowToRace(row).let {
            findRaceSchedule(it.id) ?: RaceSchedule()
        },
        boatSkipper = BoatSkipper(
            boat = resultRowToBoat(row),
            skipper = row[BoatTable.skipper]?.let { PersonTable.selectPerson(it) }
        ),
        bracket = resultRowToBracket(row),
    )

    fun rowToResult(row: ResultRow) = RaceResult(
        id = row[id],
        raceId = row[raceId],
        boatId = row[boatId],
        bracketId = row[bracketId],
        start = row[start],
        finish = row[finish],
        phrfRating = row[phrfRating],
        hocPosition = row[hoc],
    )
}

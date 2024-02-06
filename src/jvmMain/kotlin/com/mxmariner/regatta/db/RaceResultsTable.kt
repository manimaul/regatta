package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.BoatTable.resultRowToBoat
import com.mxmariner.regatta.db.BracketTable.resultRowToBracket
import com.mxmariner.regatta.db.RaceTable.rowToRace
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RaceResultsTable : Table() {
    val id = long("id").autoIncrement()
    val raceId = (long("race_id") references RaceTable.id)
    val boatId = (long("boat_id") references BoatTable.id)
    val raceClass = (long("class_id") references BracketTable.id)
    val start = timestamp("start_date").nullable()
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    val hoc = integer("hoc").nullable()
    override val primaryKey = PrimaryKey(id)

    fun count(raceId: Long): Long {
        return RaceResultsTable.select { RaceResultsTable.raceId eq raceId }.count()
    }

    fun upsertResult(result: RaceResult): RaceResultFull? {
        val resultId = result.id
        if (resultId != null) {
            update(where = { id eq resultId }) {
                it[raceId] = result.raceId
                it[boatId] = result.boatId
                it[raceClass] = result.bracketId
                it[start] = result.start
                it[finish] = result.finish
                it[phrfRating] = result.phrfRating
                it[hoc] = result.hocPosition
            }.takeIf { it > 0 }?.let { resultId }
        } else {
            insert {
                it[raceId] = result.raceId
                it[boatId] = result.boatId
                it[raceClass] = result.bracketId
                it[start] = result.start
                it[finish] = result.finish
                it[phrfRating] = result.phrfRating
                it[hoc] = result.hocPosition
            }.resultedValues?.singleOrNull()?.let {
                it[id]
            }
        }
        return resultId?.let {
            getResult(it)
        }
    }

    suspend fun allResults(): List<RaceResultFull> {
        return RaceResultsTable.selectAll().map {
            val race = RegattaDatabase.findRace(it[raceId])!!
            val boat = RegattaDatabase.findBoat(it[boatId])!!
            val bracket = BracketTable.findBracket(it[raceClass])!!
            rowToResult(it, race, boat, bracket)
        }
    }

    fun resultsByRaceId(raceId: Long): List<RaceResultFull> {
        return RaceResultsTable.select {
            RaceResultsTable.raceId eq raceId
        }.map {
            val race = RaceTable.findRace(it[RaceResultsTable.raceId])!!
            val boat = BoatTable.findBoat(it[boatId])!!
            val bracket = BracketTable.findBracket(it[raceClass])!!
            rowToResult(it, race, boat, bracket)
        }
    }

    fun getResult(resultId: Long): RaceResultFull? {
        return RaceResultsTable.select {
            id eq resultId
        }.map {
            val race = RaceTable.findRace(it[raceId])!!
            val boat = BoatTable.findBoat(it[boatId])!!
            val bracket = BracketTable.findBracket(it[raceClass])!!
            rowToResult(it, race, boat, bracket)
        }.singleOrNull()
    }

    suspend fun getResults(year: Int): List<RaceResultFull> {
        val start = Instant.parse("$year-01-01")
        val end = Instant.parse("${year + 1}-01-01")
        return innerJoin(RaceTable).innerJoin(BoatTable).innerJoin(BracketTable).select {
            finish.greaterEq(start) and finish.less(end)
        }.map {
            val person = it[BoatTable.skipper]?.let { id -> RegattaDatabase.findPerson(id) }
            val series = it[RaceTable.seriesId]?.let { id -> RegattaDatabase.findSeries(id) }
            val raceClass = resultRowToBracket(it)
            val raceId = it[RaceTable.id]
            val count = RaceResultsTable.select { RaceResultsTable.raceId eq raceId }.count()
            val race = rowToRace(it, series, person, RegattaDatabase.findRaceTimes(raceId), count)
            val boat = resultRowToBoat(it, person)
            rowToResult(it, race, boat, raceClass)
        }
    }

    fun rowToResult(row: ResultRow, race: RaceFull, boat: Boat, bracket: Bracket) = RaceResultFull(
        id = row[id],
        race = race,
        boat = boat,
        bracket = bracket,
        start = row[start],
        finish = row[finish],
        phrfRating = row[phrfRating],
        hocPosition = row[hoc],
    )
}

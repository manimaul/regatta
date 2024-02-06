package com.mxmariner.regatta.db

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.PersonTable.resultRowToPerson
import com.mxmariner.regatta.db.SeriesTable.resultRowToSeries
import org.jetbrains.exposed.sql.*

object RaceTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val seriesId = (long("series_id") references SeriesTable.id).nullable()
    val rcId = (long("rc_id") references PersonTable.id).nullable()
    override val primaryKey = PrimaryKey(id)

    fun rowToRace(
        row: ResultRow,
        series: Series?,
        person: Person?,
        raceTimes: List<RaceTime>,
        resultCount: Long,
    ) = RaceFull(
        id = row[id],
        name = row[name],
        series = series,
        raceTimes = raceTimes,
        rc = person,
        resultCount = resultCount,
    )

    fun upsertRace(race: Race): RaceFull? {
        val id = race.id
        val racePost = if (id != null) {
            RaceTable.update(where = { RaceTable.id eq id }) {
                it[name] = race.name.trim()
                it[seriesId] = race.seriesId
                it[rcId] = race.rcId
            }.takeIf { it > 0 }?.let { race }
        } else {
            RaceTable.insert {
                it[name] = race.name.trim()
                it[seriesId] = race.seriesId
                it[rcId] = race.rcId
            }.resultedValues?.map { rowToRacePost(it) }?.singleOrNull()
        }

        return racePost?.let { r ->
            RaceFull(
                id = r.id,
                name = r.name,
                series = r.seriesId?.let { SeriesTable.selectSeries(it) },
                raceTimes = r.raceTimes,
                rc = PersonTable.selectPerson(r.rcId!!),
            )
        }?.also { rf ->
            RaceTimeTable.updateRaceTimes(rf.id!!, race.raceTimes)
        }
    }
    fun findRace(raceId: Long): RaceFull? {
        return select { id eq raceId }.map { raceRow ->
            val series = raceRow[seriesId]?.let { seriesId ->
                SeriesTable.select { SeriesTable.id eq seriesId }.map(::resultRowToSeries).singleOrNull()
            }

            val person = raceRow[rcId]?.let { personId ->
                PersonTable.select { PersonTable.id eq personId }.map(::resultRowToPerson).singleOrNull()
            }
            val times = RaceTimeTable.select { RaceTimeTable.raceId eq raceId }.map { timeRow ->
                val category = RaceClassTable.select {
                    RaceClassTable.id eq timeRow[RaceTimeTable.raceClassCategory]
                }.map { catRow ->
                    RaceClass(
                        id = catRow[RaceClassTable.id],
                        name = catRow[RaceClassTable.name],
                        active = catRow[RaceClassTable.active],
                    )
                }.singleOrNull()
                RaceTime(
                    raceClassCategory = category!!,
                    startDate = timeRow[RaceTimeTable.startDate],
                    endDate = timeRow[RaceTimeTable.endDate],
                    correctionFactor = timeRow[RaceTimeTable.correctionFactor] ?: correctionFactorDefault,
                    brackets = RaceBracketJunction.selectBrackets(raceId)
                )
            }

            val count = RaceResultsTable.select { RaceResultsTable.raceId eq raceId }.count()
            rowToRace(raceRow, series, person, times, count)
        }.singleOrNull()
    }
    private fun rowToRacePost(row: ResultRow) = RacePost(
        id = row[id],
        name = row[name],
        seriesId = row[seriesId],
        rcId = row[rcId],
    )
}

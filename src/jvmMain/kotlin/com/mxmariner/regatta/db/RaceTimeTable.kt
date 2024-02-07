package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RaceTimeTable : Table() {
    val raceId = (long("race_id") references BracketTable.id)
    val bracketId = (long("bracket_id") references BracketTable.id)
    val startDate = timestamp("start_date")
    val endDate = timestamp("end_date")

    fun allYears(): List<String> {
        val years = mutableSetOf<Int>()
        RaceTimeTable.selectAll().map { row ->
            val startDate = row[startDate]
            val endDate = row[endDate]
            startDate.toString().substring(0, 4).toIntOrNull()?.let {
                years.add(it)
            }
            endDate.toString().substring(0, 4).toIntOrNull()?.let {
                years.add(it)
            }
        }
        return years.sortedDescending().map { "$it" }
    }

    fun updateRaceTimes(times: List<RaceTime>): List<RaceTime> {
        return times.mapNotNull { time ->
            upsert(raceId, bracketId, where = { raceId.eq(time.raceId).and(bracketId.eq(time.bracketId)) }) {
                it[raceId] = time.raceId
                it[bracketId] = time.bracketId
                it[startDate] = time.startDate
                it[endDate] = time.endDate
            }.resultedValues?.map(::rowToTime)
        }.flatten()
    }

    fun rowToTime(row: ResultRow): RaceTime {
        return RaceTime(
            startDate = row[startDate],
            endDate = row[endDate],
            bracketId = row[bracketId],
            raceId = row[raceId],
        )
    }

    fun selectByRaceId(raceId: Long): List<RaceTime> {
        return select { RaceTimeTable.raceId eq raceId }.map(::rowToTime)
    }

    fun findByRaceAndBracketId(rId: Long, bId: Long): RaceTime? {
        return select { bracketId.eq(bId).and(raceId.eq(rId)) }.map(::rowToTime).singleOrNull()
    }
}

package com.mxmariner.regatta.db

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.mxmariner.regatta.data.RaceTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

object RaceTimeTable : Table() {
    val raceId = (long("race_id") references RaceTable.id)
    val classId = (long("class_id") references RaceClassTable.id)
    val startDate = timestamp("start_date")
    val endDate = timestamp("end_date")

    /** Every season year with a scheduled race, by the Pacific calendar (D8), newest first. */
    fun allYears(): List<String> {
        val zone = TimeZone.of("America/Los_Angeles")
        val years = mutableSetOf<Int>()
        RaceTimeTable.selectAll().map { row ->
            years.add(row[startDate].toLocalDateTime(zone).year)
            years.add(row[endDate].toLocalDateTime(zone).year)
        }
        return years.sortedDescending().map { "$it" }
    }

    fun deleteRace(raceId: Long) {
        deleteWhere {
            RaceTimeTable.raceId.eq(raceId)
        }
    }
    fun updateRaceTime(time: RaceTime): RaceTime? {
        deleteWhere {
            raceId.eq(time.raceId).and(classId.eq(time.classId))
        }
        //error here
        return insert {
            it[raceId] = time.raceId
            it[classId] = time.classId
            it[startDate] = time.startDate
            it[endDate] = time.endDate
        }.resultedValues?.map(::rowToTime)?.singleOrNull()
    }

    fun rowToTime(row: ResultRow): RaceTime {
        return RaceTime(
            startDate = row[startDate],
            endDate = row[endDate],
            classId = row[classId],
            raceId = row[raceId],
        )
    }

    fun findByRaceAndClassId(rId: Long, cId: Long): RaceTime? {
        return selectAll().where { classId.eq(cId).and(raceId.eq(rId)) }.map(::rowToTime).singleOrNull()
    }
}

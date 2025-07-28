package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RaceTimeTable : Table() {
    val raceId = (long("race_id") references RaceTable.id)
    val classId = (long("class_id") references RaceClassTable.id)
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

    fun selectByRaceId(raceId: Long): List<RaceTime> {
        return select { RaceTimeTable.raceId eq raceId }.map(::rowToTime)
    }

    fun findByRaceAndClassId(rId: Long, cId: Long): RaceTime? {
        return select { classId.eq(cId).and(raceId.eq(rId)) }.map(::rowToTime).singleOrNull()
    }
}

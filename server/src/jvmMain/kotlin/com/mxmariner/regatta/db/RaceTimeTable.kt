package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceTime
import kotlinx.coroutines.selects.select
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

    fun findByRaceAndClassId(rId: Long, cId: Long): RaceTime? {
        return selectAll().where { classId.eq(cId).and(raceId.eq(rId)) }.map(::rowToTime).singleOrNull()
    }
}

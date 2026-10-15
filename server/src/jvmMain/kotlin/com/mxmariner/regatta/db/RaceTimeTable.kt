package com.mxmariner.regatta.db

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.mxmariner.regatta.data.RaceTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

object RaceTimeTable : Table() {
    val raceId = (long("race_id") references RaceTable.id)
    val classId = (long("class_id") references RaceClassTable.id)
    val startDate = timestamp("start_date")
    val endDate = timestamp("end_date")

    init {
        uniqueIndex("racetime_race_class_unique", raceId, classId)
    }

    /** Every season year with a scheduled race, by the Pacific calendar (D8), newest first. */
    fun allYears(): List<String> {
        val zone = TimeZone.of("America/Los_Angeles")
        val years = mutableSetOf<Int>()
        select(startDate, endDate).forEach { row ->
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

    /**
     * The class start and end times of [raceIds], keyed by race id to class id, in one query.
     * A race and class pair with more than one row is left out, as it is not a usable schedule.
     */
    fun findByRaces(raceIds: Collection<Long>): Map<Pair<Long, Long>, RaceTime> {
        if (raceIds.isEmpty()) return emptyMap()
        return selectAll().where { raceId inList raceIds }.map(::rowToTime)
            .groupBy { it.raceId to it.classId }
            .mapNotNull { (key, times) -> times.singleOrNull()?.let { key to it } }
            .toMap()
    }
}

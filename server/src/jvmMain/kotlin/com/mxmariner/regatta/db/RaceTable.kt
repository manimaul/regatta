package com.mxmariner.regatta.db

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.*
import org.jetbrains.exposed.sql.*

object RaceTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val seriesId = (long("series_id") references SeriesTable.id).nullable()
    val rcId = (long("rc_id") references PersonTable.id).nullable()
    val correctionFactor = integer("correction_factor").nullable()
    override val primaryKey = PrimaryKey(id)

    fun rowToRace(row: ResultRow) = Race(
        id = row[id],
        name = row[name],
        seriesId = row[seriesId],
        rcId = row[rcId],
        reportImage = ImageTable.getRaceReportImageName(row[id]),
        correctionFactor = row[correctionFactor] ?: correctionFactorDefault,
    )

    fun raceCountForSeries(seriesId: Long) : Long {
        return RaceTable.select { RaceTable.seriesId eq seriesId}.count()
    }

    fun upsertRace(race: Race): Race? {
        return RaceTable.upsert {
            if (race.id > 0) {
                it[id] = race.id
            }
            it[name] = race.name.trim()
            it[seriesId] = race.seriesId
            it[rcId] = race.rcId
            it[correctionFactor] = race.correctionFactor
        }.resultedValues?.map { row ->
            rowToRace(row)
        }?.singleOrNull()
    }

    private fun insertClassSchedule(raceId: Long, schedule: ClassSchedule): Boolean {
        return RaceTimeTable.updateRaceTime(
            RaceTime(
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                classId = schedule.raceClass.id,
                raceId = raceId
            )
        )?.let {
            println("inserted race time $it")
            RaceBracketJunction.setBrackets(raceId, schedule.raceClass.id, schedule.brackets)
        } == schedule.brackets.size
    }

    fun insertSchedule(schedule: RaceSchedule): RaceSchedule? {
        RaceTimeTable.deleteRace(schedule.race.id)
        RaceBracketJunction.deleteRace(schedule.race.id)
        return upsertRace(schedule.race)?.let { race ->
            schedule.schedule.forEach {
                insertClassSchedule(race.id, it)
            }
            findRaceSchedule(race.id)
        }
    }

    fun findRaceSchedule(raceId: Long): RaceSchedule? {
        val schedule = RaceBracketJunction.selectBrackets(raceId).groupBy { it.classId }.let {
            it.keys.mapNotNull { classId ->
                RaceTimeTable.findByRaceAndClassId(raceId, classId)?.let { rt ->
                    ClassSchedule(
                        raceClass = RaceClassTable.selectById(classId) ?: RaceClass(),
                        brackets = it[classId] ?: emptyList(),
                        startDate = rt.startDate,
                        endDate = rt.endDate
                    )
                }
            }
        }

        return findRace(raceId)?.let { race ->
            RaceSchedule(
                race = race,
                resultCount = RaceResultsTable.count(raceId),
                series = race.seriesId?.let { SeriesTable.selectSeries(it) },
                rc = race.rcId?.let { PersonTable.selectPerson(it) },
                schedule = schedule
            )
        }
    }

    fun findRace(raceId: Long): Race? {
        return select { id eq raceId }.map { row ->
            rowToRace(row)
        }.singleOrNull()
    }
}

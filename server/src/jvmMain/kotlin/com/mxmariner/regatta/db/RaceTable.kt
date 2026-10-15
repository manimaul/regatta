package com.mxmariner.regatta.db

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

object RaceTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val seriesId = (long("series_id") references SeriesTable.id).nullable()
    val rcId = (long("rc_id") references PersonTable.id).nullable()
    val correctionFactor = integer("correction_factor").nullable()
    val orcScoringOption = varchar("orc_option", 128).nullable()
    val orc3Band = varchar("orc_3", 128).nullable()
    val orc5Band = varchar("orc_5", 128).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index("race_series_id", false, seriesId)
    }

    fun rowToRace(row: ResultRow): Race = rowToRace(row, ImageTable.getRaceReportImageName(row[id]))

    fun rowToRace(row: ResultRow, reportImage: String?) = Race(
        id = row[id],
        name = row[name],
        seriesId = row[seriesId],
        rcId = row[rcId],
        reportImage = reportImage,
        phrfBFactor = row[correctionFactor] ?: correctionFactorDefault,
        orcScoringOption = row[orcScoringOption]?.let { OrcScoringOption.valueOf(it) } ?: OrcScoringOption.FiveBandWindwardLeeward,
        orc3Band = row[orc3Band]?.let { Orc3Band.valueOf(it) } ?: Orc3Band.fromPhrfBFactor(correctionFactorDefault),
        orc5Band = row[orc5Band]?.let { Orc5Band.valueOf(it) } ?: Orc5Band.fromPhrfBFactor(correctionFactorDefault)
    )

    fun raceCountForSeries(seriesId: Long): Long = raceCountsBySeries(listOf(seriesId))[seriesId] ?: 0L

    /** Races per series, one grouped query for all of [seriesIds]. Series with no races are absent. */
    fun raceCountsBySeries(seriesIds: Collection<Long>): Map<Long, Long> {
        if (seriesIds.isEmpty()) return emptyMap()
        val count = id.count()
        return select(seriesId, count).where { seriesId inList seriesIds }.groupBy(seriesId)
            .mapNotNull { row -> row[seriesId]?.let { it to row[count] } }
            .toMap()
    }

    fun upsertRace(race: Race): Race? {
        return RaceTable.upsert {
            if (race.id > 0) {
                it[id] = race.id
            }
            it[name] = race.name.trim()
            it[seriesId] = race.seriesId
            it[rcId] = race.rcId
            it[correctionFactor] = race.phrfBFactor
            it[orcScoringOption] = race.orcScoringOption.name
            it[orc3Band] = race.orc3Band.name
            it[orc5Band] = race.orc5Band.name
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

    fun findRaceSchedule(raceId: Long): RaceSchedule? = findRaceSchedules(listOf(raceId)).firstOrNull()

    /**
     * The schedules of [raceIds], in that order, skipping ids with no race row. Fourteen statements
     * however many races are asked for: the races, their report image names, the bracket junctions,
     * the brackets and classes those name (with race counts), the class times, the result counts,
     * the series (with race counts) and the RC volunteers.
     *
     * Classes are ordered by class sort then class id, and brackets within a class by min rating
     * then id, so the order does not depend on how the junction rows happen to be stored.
     */
    fun findRaceSchedules(raceIds: Collection<Long>): List<RaceSchedule> {
        val ids = raceIds.distinct()
        if (ids.isEmpty()) return emptyList()
        val raceRows = selectAll().where { id inList ids }.associateBy { it[id] }
        val imageNames = ImageTable.reportImageNames(ids)
        val junctions = RaceBracketJunction.selectForRaces(ids).groupBy { it.raceId }
        val brackets = BracketTable.findBrackets(junctions.values.flatten().map { it.bracketId }.distinct())
        val classes = RaceClassTable.findClasses(brackets.values.map { it.classId }.distinct())
        val times = RaceTimeTable.findByRaces(ids)
        val resultCounts = RaceResultsTable.countsByRace(ids)
        val series = SeriesTable.findSeries(raceRows.values.mapNotNull { it[seriesId] }.distinct())
        val people = PersonTable.findPeople(raceRows.values.mapNotNull { it[rcId] }.distinct())
        return ids.mapNotNull { raceId ->
            val row = raceRows[raceId] ?: return@mapNotNull null
            val race = rowToRace(row, imageNames[raceId])
            val schedule = (junctions[raceId] ?: emptyList())
                .mapNotNull { brackets[it.bracketId] }
                .sortedWith(compareBy({ classes[it.classId]?.sort }, { it.classId }, { it.minRating }, { it.id }))
                .groupBy { it.classId }
                .mapNotNull { (classId, classBrackets) ->
                    times[raceId to classId]?.let { time ->
                        ClassSchedule(
                            raceClass = classes[classId] ?: RaceClass(),
                            brackets = classBrackets,
                            startDate = time.startDate,
                            endDate = time.endDate
                        )
                    }
                }
            RaceSchedule(
                race = race,
                resultCount = resultCounts[raceId] ?: 0L,
                series = race.seriesId?.let { series[it] },
                rc = race.rcId?.let { people[it] },
                schedule = schedule
            )
        }
    }

    fun findRace(raceId: Long): Race? {
        return selectAll().where { id eq raceId }.map { row ->
            rowToRace(row)
        }.singleOrNull()
    }
}

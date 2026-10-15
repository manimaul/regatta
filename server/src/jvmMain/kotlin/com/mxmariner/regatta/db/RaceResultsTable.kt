package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.BoatTable.resultRowToBoat
import com.mxmariner.regatta.db.RaceTable.findRaceSchedule
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

object RaceResultsTable : Table() {
    val id = long("id").autoIncrement()
    val raceId = (long("race_id") references RaceTable.id)
    val boatId = (long("boat_id") references BoatTable.id)
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    val orcRefNo = (varchar("orc_ref", 128) references OrcTable.refNo).nullable()
    val ratingType = varchar("rating_type", 128)
    val hoc = integer("hoc").nullable()
    val raceClass = (long("class_id") references RaceClassTable.id)
    val bracket = (long("bracket_id") references BracketTable.id)
    val finishCode = varchar("finish_code", 128).nullable()
    val penalty = integer("penalty").nullable()

    init {
        uniqueIndex("idx_boat_race_unique", boatId, raceId)
        index("raceresults_race_id", false, raceId)
    }

    override val primaryKey = PrimaryKey(id)

    fun count(raceId: Long): Long = countsByRace(listOf(raceId))[raceId] ?: 0L

    /** Results per race, one grouped query for all of [raceIds]. Races with no results are absent. */
    fun countsByRace(raceIds: Collection<Long>): Map<Long, Long> {
        if (raceIds.isEmpty()) return emptyMap()
        val count = id.count()
        return select(raceId, count).where { raceId inList raceIds }.groupBy(raceId)
            .associate { it[raceId] to it[count] }
    }

    fun upsertResult(result: RaceResult): RaceResult? {
        return upsert(boatId, raceId) {
            it[raceId] = result.raceId
            it[boatId] = result.boatId
            it[finish] = result.finish.takeIf { result.finishCode == FinishCode.TIME }
            it[phrfRating] = result.phrfRating
            it[orcRefNo] = result.orcRef
            it[ratingType] = result.ratingType.name
            it[hoc] = result.hocPosition.takeIf { result.finishCode == FinishCode.HOC }
            it[raceClass] = result.raceClassId
            it[bracket] = result.bracketId
            it[finishCode] = result.finishCode.name
            it[penalty] = result.penalty
        }.resultedValues?.map(::rowToResult)?.singleOrNull()
    }

//    fun allResults(): List<RaceResult> {
//        return RaceResultsTable.selectAll().map(::rowToResult)
//    }

    fun raceCount(boatId: Long): Long = raceCountsByBoat(listOf(boatId))[boatId] ?: 0L

    /** Results per boat, one grouped query for all of [boatIds]. Boats with no results are absent. */
    fun raceCountsByBoat(boatIds: Collection<Long>): Map<Long, Long> {
        if (boatIds.isEmpty()) return emptyMap()
        val count = id.count()
        return select(boatId, count).where { boatId inList boatIds }.groupBy(boatId)
            .associate { it[boatId] to it[count] }
    }

    /**
     * The results of race [rId] with their boat, skipper and bracket, placed against [raceSchedule]
     * (the schedule of that same race, loaded once by the caller). Four statements for any number
     * of results: the results joined to boat, then the boats' race counts, ORC certificates and skippers.
     */
    fun resultsBoatBracketByRaceId(rId: Long, raceSchedule: RaceSchedule): List<RaceResultBoatBracket> {
        val rows = innerJoin(RaceTable).innerJoin(BoatTable).selectAll().where {
            raceId.eq(rId)
        }.toList()
        val boatIds = rows.map { it[boatId] }.distinct()
        val raceCounts = raceCountsByBoat(boatIds)
        val certs = OrcTable.findCertificatesByBoat(boatIds)
        val skippers = PersonTable.findPeople(rows.mapNotNull { it[BoatTable.skipper] }.distinct())
        return rows.map { row ->
            val result = rowToResult(row)
            val bId = row[boatId]
            RaceResultBoatBracket(
                result = result,
                boatSkipper = BoatSkipper(
                    boat = resultRowToBoat(row, raceCounts[bId] ?: 0L, certs[bId] ?: emptyList())
                        .copy(
                            ratingType = result.ratingType,
                            phrfRating = result.phrfRating,
                        ),
                    skipper = row[BoatTable.skipper]?.let { skippers[it] }
                ),
                bracket = findBoatBracket(raceSchedule, result) ?: Bracket(),
            )
        }
    }

    fun resultsByRaceId(rId: Long): List<RaceResult> {
        return RaceResultsTable.selectAll().where { raceId.eq(rId) }.map(::rowToResult)
    }

    /** The schedule of race [rId] with its full results; the boats and certificates load in bulk. */
    fun scheduleResultsByRaceId(rId: Long): RaceScheduleResults? {
        return findRaceSchedule(rId)?.let { raceSchedule ->
            val rows = RaceResultsTable.selectAll().where { raceId.eq(rId) }.toList()
            val boats = BoatTable.findBoatSkippers(rows.map { it[boatId] }.distinct())
            val certs = OrcTable.findCertificates(rows.mapNotNull { it[orcRefNo] }.distinct())
            RaceScheduleResults(
                raceSchedule = raceSchedule,
                results = rows.map { rowToResultFull(it, boats, certs) }
            )
        }
    }

    private fun rowToResultFull(
        row: ResultRow,
        boats: Map<Long, BoatSkipper>,
        certs: Map<String, OrcCertificate>,
    ): RaceResultFull {
        val time = row[finish]
        val code = row[finishCode]?.let { FinishCode.valueOf(it) } ?: run {
            time?.let { FinishCode.TIME } ?: FinishCode.RET
        }
        return RaceResultFull(
            id = row[id],
            raceId = row[raceId],
            boat = requireNotNull(boats[row[boatId]]),
            finish = time,
            phrfRating = row[phrfRating],
            orcCertificate = row[orcRefNo]?.let { certs[it] },
            ratingType = RatingType.valueOf(row[ratingType]),
            hocPosition = row[hoc],
            raceClassId = row[raceClass],
            bracketId = row[bracket],
            finishCode = code,
            penalty = row[penalty],
        )
    }
    fun rowToResult(row: ResultRow): RaceResult {
        val time = row[finish]
        val code = row[finishCode]?.let { FinishCode.valueOf(it) } ?: run {
            time?.let { FinishCode.TIME } ?: FinishCode.RET
        }
        return RaceResult(
            id = row[id],
            raceId = row[raceId],
            boatId = row[boatId],
            finish = time,
            phrfRating = row[phrfRating],
            orcRef = row[orcRefNo],
            ratingType = RatingType.valueOf(row[ratingType]),
            hocPosition = row[hoc],
            raceClassId = row[raceClass],
            bracketId = row[bracket],
            finishCode = code,
            penalty = row[penalty],
        )
    }
}

fun findBoatBracket(race: RaceSchedule, result: RaceResult): Bracket? {
    return if (result.bracketId != null) {
        race.schedule.firstNotNullOfOrNull { sch ->
            sch.brackets.firstOrNull {
                it.id == result.bracketId
            }
        }
    } else {
        when (result.ratingType) {
            RatingType.ORC -> {
                val phrfRating = requireNotNull(result.phrfRating ?: OrcTable.findCertificate(result.orcRef)?.virtualPhrf())
                race.schedule.firstNotNullOfOrNull { sch ->
                    sch.brackets.takeIf { sch.raceClass.ratingType.isORC }?.firstOrNull {
                        phrfRating >= it.minRating && phrfRating <= it.maxRating
                    }
                }
            }
            RatingType.ORC_PHRF -> {
                val phrfRating = requireNotNull(result.phrfRating ?: OrcTable.findCertificate(result.orcRef)?.virtualPhrf())
                race.schedule.firstNotNullOfOrNull { sch ->
                    sch.brackets.takeIf { sch.raceClass.ratingType.isORCorPHRF }?.firstOrNull {
                        phrfRating >= it.minRating && phrfRating <= it.maxRating
                    }
                }
            }

            RatingType.PHRF -> {
                val phrfRating = requireNotNull(result.phrfRating)
                race.schedule.firstNotNullOfOrNull { sch ->
                    sch.brackets.takeIf { sch.raceClass.ratingType.isPHRF }?.firstOrNull {
                        phrfRating >= it.minRating && phrfRating <= it.maxRating
                    }
                }
            }

            RatingType.CruisingFlyingSails -> {
                race.schedule.firstNotNullOfOrNull { schedule ->
                    schedule.brackets.firstOrNull { schedule.raceClass.ratingType == RatingType.CruisingFlyingSails }
                }
            }

            RatingType.CruisingNonFlyingSails -> {
                race.schedule.firstNotNullOfOrNull { schedule ->
                    schedule.brackets.firstOrNull { schedule.raceClass.ratingType == RatingType.CruisingNonFlyingSails }
                }
            }
        }
    }
}

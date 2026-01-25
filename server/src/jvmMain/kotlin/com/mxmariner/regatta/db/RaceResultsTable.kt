package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.*
import com.mxmariner.regatta.db.BoatTable.resultRowToBoat
import com.mxmariner.regatta.db.RaceTable.findRaceSchedule
import com.mxmariner.regatta.db.RaceTable.rowToRace
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

object RaceResultsTable : Table() {
    val id = long("id").autoIncrement()
    val raceId = (long("race_id") references RaceTable.id)
    val boatId = (long("boat_id") references BoatTable.id)
    val finish = timestamp("end_date").nullable().index()
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
    }

    override val primaryKey = PrimaryKey(id)

    fun count(raceId: Long): Long {
        return RaceResultsTable.selectAll().where { RaceResultsTable.raceId eq raceId }.count()
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

    fun allResults(): List<RaceResult> {
        return RaceResultsTable.selectAll().map(::rowToResult)
    }

    fun raceCount(boatId: Long): Long {
        return RaceResultsTable.selectAll().where { RaceResultsTable.boatId eq boatId }.count()
    }

    fun resultsBoatBracketByRaceId(rId: Long): List<RaceResultBoatBracket> {
        return innerJoin(RaceTable).innerJoin(BoatTable).selectAll().where {
            raceId.eq(rId)
        }.map(::rowToRaceResultBoatBracket)
    }

    fun resultsByRaceId(rId: Long): List<RaceResult> {
        return RaceResultsTable.selectAll().where { raceId.eq(rId) }.map(::rowToResult)
    }

    fun scheduleResultsByRaceId(rId: Long): RaceScheduleResults? {
        return findRaceSchedule(rId)?.let { raceSchedule ->
            RaceScheduleResults(
                raceSchedule = raceSchedule,
                results = RaceResultsTable.selectAll().where { raceId.eq(rId) }.map(::rowToResultFull)
            )
        }
    }

    fun getResults(year: Int): List<RaceResultBoatBracket> {
        val start = Instant.parse("$year-01-01")
        val end = Instant.parse("${year + 1}-01-01")
        return innerJoin(RaceTable).innerJoin(BoatTable).selectAll().where {
            finish.greaterEq(start) and finish.less(end)
        }.map(::rowToRaceResultBoatBracket)
    }

    fun rowToRaceResultBoatBracket(row: ResultRow): RaceResultBoatBracket {
        val raceSchedule = rowToRace(row).let {
            findRaceSchedule(it.id) ?: RaceSchedule()
        }

        val result = rowToResult(row)
        val bracket = findBoatBracket(raceSchedule, result)
        return RaceResultBoatBracket(
            result = result,
            boatSkipper = BoatSkipper(
                boat = resultRowToBoat(row)
                    .copy(
                        ratingType = result.ratingType,
                        phrfRating = result.phrfRating,
                    ),
                skipper = row[BoatTable.skipper]?.let { PersonTable.selectPerson(it) }
            ),
            bracket = bracket ?: Bracket(),
        )
    }

    fun rowToResultFull(row: ResultRow): RaceResultFull {
        val time = row[finish]
        val code = row[finishCode]?.let { FinishCode.valueOf(it) } ?: run {
            time?.let { FinishCode.TIME } ?: FinishCode.RET
        }
        return RaceResultFull(
            id = row[id],
            raceId = row[raceId],
            boat = requireNotNull(BoatTable.findBoatSkipper(row[boatId])),
            finish = time,
            phrfRating = row[phrfRating],
            orcCertificate =OrcTable.findCertificate(row[orcRefNo]),
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

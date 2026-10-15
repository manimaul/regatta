package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select

object RaceBracketJunction : Table() {
    val bracket = (long("bracket_id") references BracketTable.id)
    val race = (long("race_id") references RaceTable.id)
    val raceClass = (long("class_id") references RaceClassTable.id)

    init {
        uniqueIndex("racebracketjunction_unique", race, raceClass, bracket)
        index("racebracketjunction_bracket_id", false, bracket)
        index("racebracketjunction_class_id", false, raceClass)
    }

    /** One junction row: [bracketId] sails in [raceId] under [classId]. */
    data class Row(val raceId: Long, val classId: Long, val bracketId: Long)

    fun raceCountForBracket(bracketId: Long): Long = raceCountsByBracket(listOf(bracketId))[bracketId] ?: 0L

    fun raceCountForClass(raceClassId: Long): Long = raceCountsByClass(listOf(raceClassId))[raceClassId] ?: 0L

    /** Distinct races per bracket, one grouped query for all of [bracketIds]. Brackets with no races are absent. */
    fun raceCountsByBracket(bracketIds: Collection<Long>): Map<Long, Long> {
        if (bracketIds.isEmpty()) return emptyMap()
        val count = race.countDistinct()
        return select(bracket, count).where { bracket inList bracketIds }.groupBy(bracket)
            .associate { it[bracket] to it[count] }
    }

    /** Distinct races per class, one grouped query for all of [classIds]. Classes with no races are absent. */
    fun raceCountsByClass(classIds: Collection<Long>): Map<Long, Long> {
        if (classIds.isEmpty()) return emptyMap()
        val count = race.countDistinct()
        return select(raceClass, count).where { raceClass inList classIds }.groupBy(raceClass)
            .associate { it[raceClass] to it[count] }
    }

    fun setBrackets(raceId: Long, classId: Long, list: List<Bracket>): Int {
        val deleteCount = deleteWhere { race.eq(raceId).and(raceClass.eq(classId)) }
        println("deleted bracket race junctions $deleteCount")
        var count = 0
        list.distinctBy { it.id }.forEach { b ->
            count += insert {
                it[bracket] = b.id
                it[race] = raceId
                it[raceClass] = classId
            }.insertedCount
        }
        return count
    }

    /** The junction rows of [raceIds] in table order, one query. */
    fun selectForRaces(raceIds: Collection<Long>): List<Row> {
        if (raceIds.isEmpty()) return emptyList()
        return select(race, raceClass, bracket).where { race inList raceIds }
            .map { Row(raceId = it[race], classId = it[raceClass], bracketId = it[bracket]) }
    }

    fun deleteRace(raceId: Long) {
        deleteWhere { race.eq(raceId) }
    }
}

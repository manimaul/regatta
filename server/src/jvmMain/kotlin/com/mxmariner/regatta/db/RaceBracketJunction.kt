package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

object RaceBracketJunction : Table() {
    val bracket = (long("bracket_id") references BracketTable.id)
    val race = (long("race_id") references RaceTable.id)
    val raceClass = (long("class_id") references RaceClassTable.id)

    fun raceCountForBracket(bracketId: Long): Long {
        return select(race.countDistinct()).where { bracket eq bracketId }.first()[race.countDistinct()]
    }

    fun raceCountForClass(raceClassId: Long): Long {
        return select(race.countDistinct()).where { raceClass eq raceClassId }.first()[race.countDistinct()]
    }

    fun setBrackets(raceId: Long, classId: Long, list: List<Bracket>): Int {
        val deleteCount = deleteWhere { race.eq(raceId).and(raceClass.eq(classId)) }
        println("deleted bracket race junctions $deleteCount")
        var count = 0
        list.forEach { b ->
            count += insert {
                it[bracket] = b.id
                it[race] = raceId
                it[raceClass] = classId
            }.insertedCount
        }
        return count
    }

    fun selectBrackets(raceId: Long): List<Bracket> {
        return RaceBracketJunction.selectAll().where { race eq raceId }.mapNotNull { row ->
            val bracketId = row[bracket]
            BracketTable.findBracket(bracketId)
        }
    }

    fun deleteRace(raceId: Long) {
        deleteWhere { race.eq(raceId) }
    }
}


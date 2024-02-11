package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RaceBracketJunction : Table() {
    val bracket = (long("bracket_id") references BracketTable.id)
    val race = (long("race_id") references RaceTable.id)
    val raceClass = (long("class_id") references RaceClassTable.id)

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
        return RaceBracketJunction.select { race eq raceId }.mapNotNull { row ->
            val bracketId = row[bracket]
            BracketTable.findBracket(bracketId)
        }
    }

    fun deleteRace(raceId: Long) {
        deleteWhere { race.eq(raceId) }
    }
}


package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceFull
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select


object RaceBracketJunction : Table() {
    val bracket = (long("bracket_id") references BracketTable.id)
    val race = (long("race_id") references RaceTable.id)

    fun selectBrackets(raceId: Long): List<Bracket> {
        return RaceBracketJunction.select { race eq raceId }.mapNotNull { row ->
            val bracketId = row[bracket]
            BracketTable.findBracket(bracketId)
        }
    }

    fun selectRaces(bracketId: Long): List<RaceFull> {
        return RaceBracketJunction.select { bracket eq bracketId }.mapNotNull { row ->
            val raceId = row[race]
            RaceTable.findRace(raceId)
        }
    }
}


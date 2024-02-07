package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus

/**
 * Each [RaceClassFull] / [RaceClassTable] has a list of [Bracket]s / [BracketTable].
 * A [RaceClass] / [RaceClassTable] does not have [BracketTable]s
 *
 * [RaceBracketJunction] is used to associate a [BracketTable] with a [RaceTable]
 *
 * A bracket can only be a child of a single class. So the [RaceClassTable.id] is a reference.
 * There should be a small set of brackets to choose from in the whole system.
 *
 */
object BracketTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val description = varchar("description", 1024).nullable()
    val active = bool("active")
    val minRating = float("min_r")
    val maxRating = float("max_r")
    val raceClass = (long("race_class") references RaceClassTable.id)
    override val primaryKey = PrimaryKey(id)

    fun upsertBracket(item: Bracket): Bracket? {
        return upsert {
            it[name] = item.name.trim()
            it[description] = item.description?.trim()
            it[active] = item.active
            it[minRating] = item.minRating
            it[maxRating] = item.maxRating
            it[raceClass] = item.classId
        }.resultedValues?.singleOrNull()?.let(::resultRowToBracket)
    }

    fun deleteBracket(bracketId: Long): Int {
        return deleteWhere { id eq bracketId }
    }

    fun findClassBrackets(rId: Long): List<Bracket> {
        return select { raceClass.eq(rId) }.map(::resultRowToBracket).sortedBy { it.minRating }
    }

    fun findBracket(bracketId: Long): Bracket? {
        return select { id.eq(bracketId) }.map(::resultRowToBracket).singleOrNull()
    }

    fun resultRowToBracket(row: ResultRow) = Bracket(
        id = row[id],
        name = row[name],
        description = row[description],
        active = row[active],
        minRating = row[minRating],
        maxRating = row[maxRating],
        classId = row[raceClass],
    )
}

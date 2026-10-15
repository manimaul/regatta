package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

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
            if (item.id > 0) { it[id] = item.id }
            it[name] = item.name.trim()
            it[description] = item.description?.trim()
            it[active] = item.active
            it[minRating] = item.minRating
            it[maxRating] = item.maxRating
            it[raceClass] = item.classId
        }.resultedValues?.singleOrNull()?.let { resultRowToBracket(it, RaceBracketJunction.raceCountForBracket(it[id])) }
    }

    fun upsertBrackets(brackets: RaceClassBrackets) : List<Bracket> {
        findClassBrackets(brackets.raceClass.id).map {
            it.id
        }.filter { id ->
            brackets.brackets.firstOrNull { it.id == id } == null
        }.forEach {
            deleteBracket(it)
        }
        return brackets.brackets.mapNotNull { upsertBracket(it) }
    }

    fun deleteBracket(bracketId: Long): Int {
        return deleteWhere { id eq bracketId }
    }

    fun findClassBrackets(rId: Long): List<Bracket> = findClassBrackets(listOf(rId))[rId] ?: emptyList()

    /** The brackets of each class in [classIds], sorted by min rating; two statements for any number of classes. */
    fun findClassBrackets(classIds: Collection<Long>): Map<Long, List<Bracket>> {
        if (classIds.isEmpty()) return emptyMap()
        return toBrackets(selectAll().where { raceClass inList classIds }.toList())
            .groupBy { it.classId }
            .mapValues { (_, brackets) -> brackets.sortedBy { it.minRating } }
    }

    fun findBracket(bracketId: Long): Bracket? = findBrackets(listOf(bracketId))[bracketId]

    /** The brackets with [bracketIds], by id; two statements for any number of brackets. */
    fun findBrackets(bracketIds: Collection<Long>): Map<Long, Bracket> {
        if (bracketIds.isEmpty()) return emptyMap()
        return toBrackets(selectAll().where { id inList bracketIds }.toList()).associateBy { it.id }
    }

    /** Hydrates [rows] with their race counts from one grouped query. */
    private fun toBrackets(rows: List<ResultRow>): List<Bracket> {
        val counts = RaceBracketJunction.raceCountsByBracket(rows.map { it[id] })
        return rows.map { resultRowToBracket(it, counts[it[id]] ?: 0L) }
    }

    fun resultRowToBracket(row: ResultRow, numberOfRaces: Long) = Bracket(
        id = row[id],
        name = row[name],
        description = row[description],
        active = row[active],
        numberOfRaces = numberOfRaces,
        minRating = row[minRating],
        maxRating = row[maxRating],
        classId = row[raceClass],
    )
}

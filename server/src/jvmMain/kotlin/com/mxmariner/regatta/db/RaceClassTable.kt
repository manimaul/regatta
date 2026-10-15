package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.RatingType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val sort = integer("sort")
    val ratingType = varchar("rating_type", 128)
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    /** Every class with its brackets, in sort order; four statements however many classes there are. */
    fun allClasses(): List<RaceClassBrackets> {
        return withBrackets(toClasses(selectAll().orderBy(sort).toList()))
    }

    fun upsertClassBrackets(raceClassBrackets: RaceClassBrackets): RaceClassBrackets? {
        return upsertClass(raceClassBrackets.raceClass)?.let { raceClass ->
            val brackets = BracketTable.upsertBrackets(
                RaceClassBrackets(
                    raceClass,
                    raceClassBrackets.brackets.map { it.copy(classId = raceClass.id) }
                )
            )
            return RaceClassBrackets(raceClass, brackets)
        }
    }

    fun upsertClass(item: RaceClass): RaceClass? {
        return upsert {
            if (item.id > 0) {
                it[id] = item.id
            }
            it[name] = item.name.trim()
            it[active] = item.active
            it[ratingType] = item.ratingType.name
            it[sort] = item.sort
        }.resultedValues?.singleOrNull()?.let { resultRowToClass(it, RaceBracketJunction.raceCountForClass(it[id])) }
    }

    fun upsertClassList(list: List<RaceClass>): List<RaceClassBrackets> {
        return withBrackets(list.mapNotNull { upsertClass(it) }).sortedBy { it.raceClass.sort }
    }

    fun deleteClass(catId: Long): Int {
        return RaceClassTable.deleteWhere { id eq catId }
    }

    private fun withBrackets(classes: List<RaceClass>): List<RaceClassBrackets> {
        val brackets = BracketTable.findClassBrackets(classes.map { it.id })
        return classes.map { RaceClassBrackets(raceClass = it, brackets = brackets[it.id] ?: emptyList()) }
    }

    /** Hydrates [rows] with their race counts from one grouped query. */
    private fun toClasses(rows: List<ResultRow>): List<RaceClass> {
        val counts = RaceBracketJunction.raceCountsByClass(rows.map { it[id] })
        return rows.map { resultRowToClass(it, counts[it[id]] ?: 0L) }
    }

    private fun resultRowToClass(row: ResultRow, numberOfRaces: Long): RaceClass {
        return RaceClass(
            id = row[id],
            name = row[name],
            sort = row[sort],
            ratingType = RatingType.valueOf(row[ratingType]),
            numberOfRaces = numberOfRaces,
            active = row[active],
        )
    }

    fun selectById(id: Long): RaceClass? = findClasses(listOf(id))[id]

    /** The classes with [classIds], by id; two statements for any number of classes. */
    fun findClasses(classIds: Collection<Long>): Map<Long, RaceClass> {
        if (classIds.isEmpty()) return emptyMap()
        return toClasses(selectAll().where { id inList classIds }.toList()).associateBy { it.id }
    }
}

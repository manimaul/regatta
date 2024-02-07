package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBracketTimes
import com.mxmariner.regatta.data.RaceClassBrackets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun allClasses(): List<RaceClassBrackets> {
        return selectAll().map {
            val c = resultRowToClass(it)
            RaceClassBrackets(
                raceClass = c,
                brackets = BracketTable.findClassBrackets(c.id)
            )
        }
    }

    fun upsertClass(item: RaceClass): RaceClass? {
        return upsert {
            it[name] = item.name.trim()
            it[active] = item.active
        }.resultedValues?.singleOrNull()?.let(::resultRowToClass)
    }

    fun deleteClass(catId: Long): Int {
        return RaceClassTable.deleteWhere { id eq catId }
    }

    private fun resultRowToClass(row: ResultRow) = RaceClass(
        id = row[id],
        name = row[name],
        active = row[active],
    )

    fun selectById(id: Long): RaceClass? {
        return RaceClassTable.select {
            RaceClassTable.id eq id
        }.map(::resultRowToClass).singleOrNull()
    }
}
package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val sort = integer("sort")
    val phrf = bool("phrf")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun allClasses(): List<RaceClassBrackets> {
        return selectAll().orderBy(sort).map {
            val c = resultRowToClass(it)
            RaceClassBrackets(
                raceClass = c,
                brackets = BracketTable.findClassBrackets(c.id)
            )
        }
    }

    fun upsertClass(item: RaceClass): RaceClass? {
        return upsert {
            if (item.id > 0) {
                it[id] = item.id
            }
            it[name] = item.name.trim()
            it[active] = item.active
            it[phrf] = item.isPHRF
            it[sort] = item.sort
        }.resultedValues?.singleOrNull()?.let(::resultRowToClass)
    }

    fun deleteClass(catId: Long): Int {
        return RaceClassTable.deleteWhere { id eq catId }
    }

    private fun resultRowToClass(row: ResultRow) = RaceClass(
        id = row[id],
        name = row[name],
        sort = row[sort],
        isPHRF = row[phrf],
        active = row[active],
    )

    fun selectById(id: Long): RaceClass? {
        return RaceClassTable.select {
            RaceClassTable.id eq id
        }.map(::resultRowToClass).singleOrNull()
    }
}
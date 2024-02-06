package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassAble
import com.mxmariner.regatta.data.RaceClassFull
import com.mxmariner.regatta.db.BracketTable.resultRowToBracket
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun allCategories(): List<RaceClassFull> {
        return selectAll().map(::resultRowToClassCategory).map { cat ->
            cat.copy(brackets = BracketTable.select { BracketTable.category eq cat.id!! }.map(::resultRowToBracket))
        }
    }

    fun upsertRaceCategory(item: RaceClassAble): RaceClassAble? {
        val id = item.id
        return if (id != null) {
            RaceClassTable.update(where = { RaceClassTable.id eq id }) {
                it[name] = item.name.trim()
                it[active] = item.active
            }.takeIf { it == 1 }?.let { item }
        } else {
            RaceClassTable.insert {
                it[name] = item.name.trim()
                it[active] = item.active
            }.resultedValues?.singleOrNull()?.let(::resultRowToCategory)
        }
    }

    fun deleteCategory(catId: Long): Int {
        return RaceClassTable.deleteWhere { id eq catId }
    }

    private fun resultRowToClassCategory(row: ResultRow) = RaceClassFull(
        id = row[id],
        name = row[name],
        active = row[active],
        brackets = BracketTable.selectBrackets(row[id])
    )

    private fun resultRowToCategory(row: ResultRow) = RaceClass(
        id = row[id],
        name = row[name],
        active = row[active],
    )

    fun findRaceCategory(id: Long) : RaceClass? {
        return RaceClassTable.select {
            RaceClassTable.id eq id
        }.map {
            RaceClass(
                id = it[RaceClassTable.id],
                name = it[name],
                active = it[active],
            )
        }.singleOrNull()
    }
    fun selectById(id: Long): RaceClass? {
        return select {
            RaceClassTable.id eq id
        }.map {
            RaceClass(
                id = it[RaceClassTable.id],
                name = it[name],
                active = it[active],
            )
        }.singleOrNull()
    }
}
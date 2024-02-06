package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceCategory
import com.mxmariner.regatta.data.RaceClassCat
import com.mxmariner.regatta.data.RaceClassCategory
import com.mxmariner.regatta.db.BracketTable.resultRowToBracket
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RaceClassCategoryTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun allCategories(): List<RaceClassCategory> {
        return selectAll().map(::resultRowToClassCategory).map { cat ->
            cat.copy(brackets = BracketTable.select { BracketTable.category eq cat.id!! }.map(::resultRowToBracket))
        }
    }

    fun upsertRaceCategory(item: RaceClassCat): RaceClassCat? {
        val id = item.id
        return if (id != null) {
            RaceClassCategoryTable.update(where = { RaceClassCategoryTable.id eq id }) {
                it[name] = item.name.trim()
                it[active] = item.active
            }.takeIf { it == 1 }?.let { item }
        } else {
            RaceClassCategoryTable.insert {
                it[name] = item.name.trim()
                it[active] = item.active
            }.resultedValues?.singleOrNull()?.let(::resultRowToCategory)
        }
    }

    fun deleteCategory(catId: Long): Int {
        return RaceClassCategoryTable.deleteWhere { id eq catId }
    }

    private fun resultRowToClassCategory(row: ResultRow) = RaceClassCategory(
        id = row[id],
        name = row[name],
        active = row[active],
        brackets = TODO()
    )

    private fun resultRowToCategory(row: ResultRow) = RaceCategory(
        id = row[id],
        name = row[name],
        active = row[active],
    )

    fun findRaceCategory(id: Long) : RaceCategory? {
        return RaceClassCategoryTable.select {
            RaceClassCategoryTable.id eq id
        }.map {
            RaceCategory(
                id = it[RaceClassCategoryTable.id],
                name = it[name],
                active = it[active],
            )
        }.singleOrNull()
    }
    fun selectById(id: Long): RaceCategory? {
        return select {
            RaceClassCategoryTable.id eq id
        }.map {
            RaceCategory(
                id = it[RaceClassCategoryTable.id],
                name = it[name],
                active = it[active],
            )
        }.singleOrNull()
    }
}
package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Bracket
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object BracketTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val description = varchar("description", 1024).nullable()
    val active = bool("active")
    val category = (long("category") references RaceClassCategoryTable.id)
    override val primaryKey = PrimaryKey(id)

    fun selectAllBrackets() : List<Bracket> {
        return BracketTable.selectAll().map(::resultRowToBracket)
    }

    fun upsertBracket(item: Bracket): Bracket? {
        return if (item.id != null) {
            BracketTable.update(where = { id eq item.id }) {
                it[name] = item.name.trim()
                it[description] = item.description?.trim()
                it[active] = item.active
                it[category] = item.category
            }.takeIf { it == 1 }?.let { item }
        } else {
            BracketTable.insert {
                it[name] = item.name.trim()
                it[description] = item.description?.trim()
                it[active] = item.active
                it[category] = item.category
            }.resultedValues?.singleOrNull()?.let(::resultRowToBracket)
        }
    }

    fun deleteBracket(bracketId: Long) : Int {
        return BracketTable.deleteWhere { id eq bracketId }
    }
    fun findBracket(bracketId: Long): Bracket? {
        return BracketTable.select { id.eq(bracketId) }.map(::resultRowToBracket).singleOrNull()
    }

    fun resultRowToBracket(row: ResultRow) = Bracket(
        id = row[id],
        name = row[name],
        description = row[description],
        active = row[active],
        category = row[category],
    )
}

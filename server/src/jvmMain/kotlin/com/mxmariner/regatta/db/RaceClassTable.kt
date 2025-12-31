package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassBrackets
import com.mxmariner.regatta.data.RatingType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val sort = integer("sort")
    val ratingType = varchar("ratingtype", 128)
//    val phrf = bool("phrf")
//    val wsFlying = bool("wsf")
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
//            it[wsFlying] = item.wsFlying
//            it[phrf] = item.isPHRF
            it[sort] = item.sort
        }.resultedValues?.singleOrNull()?.let(::resultRowToClass)
    }

    fun upsertClassList(list: List<RaceClass>): List<RaceClassBrackets> {
        return list.mapNotNull { item ->
            upsertClass(item)
        }.map { c ->
            RaceClassBrackets(
                raceClass = c,
                brackets = BracketTable.findClassBrackets(c.id)
            )
        }.sortedBy { it.raceClass.sort }
    }

    fun deleteClass(catId: Long): Int {
        return RaceClassTable.deleteWhere { id eq catId }
    }

    private fun resultRowToClass(row: ResultRow): RaceClass {
        val classId = row[id]
        return RaceClass(
            id = classId,
            name = row[name],
            sort = row[sort],
            ratingType = RatingType.valueOf(row[ratingType]),
//            isPHRF = row[phrf],
//            wsFlying = row[wsFlying],
            numberOfRaces = RaceBracketJunction.raceCountForClass(classId),
            active = row[active],
        )
    }

    fun selectById(id: Long): RaceClass? {
        return RaceClassTable.select {
            RaceClassTable.id eq id
        }.map(::resultRowToClass).singleOrNull()
    }
}
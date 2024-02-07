package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Person
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object PersonTable : Table() {
    val id = long("id").autoIncrement()
    val first = varchar("first", 128)
    val last = varchar("last", 128)
    val clubMember = bool("club_member")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)
    val compoundIdx = uniqueIndex(first, last)

    fun selectPerson(personId: Long): Person? {
        return select { id eq personId }.map(::resultRowToPerson).singleOrNull()
    }

    fun resultRowToPerson(row: ResultRow) = Person(
        id = row[id],
        first = row[first],
        last = row[last],
        clubMember = row[clubMember],
        active = row[active]
    )


    fun findPerson(name: String): List<Person> {
        return select {
            (first ilike LikePattern("%$name%")) or (last ilike LikePattern("%$name%"))
        }.map(::resultRowToPerson)
    }

    fun upsertPerson(person: Person): Person? {
        return upsert {
            it[first] = person.first.trim()
            it[last] = person.last.trim()
            it[clubMember] = person.clubMember
            it[active] = person.active
        }.resultedValues?.singleOrNull()?.let(::resultRowToPerson)
    }

    fun selectAllPeople(): List<Person> {
        return PersonTable.selectAll().map(::resultRowToPerson).sortedBy { it.first }
    }

    fun deletePerson(personId: Long): Int {
        return PersonTable.deleteWhere { id eq personId }
    }
}

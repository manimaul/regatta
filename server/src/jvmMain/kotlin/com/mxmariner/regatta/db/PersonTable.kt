package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Person
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

object PersonTable : Table() {
    val id = long("id").autoIncrement()
    val first = varchar("first", 128)
    val last = varchar("last", 128)
    val clubMember = bool("club_member")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)
    val compoundIdx = uniqueIndex(first, last)

    fun selectPerson(personId: Long): Person? {
        return selectAll().where { id eq personId }.map(::resultRowToPerson).singleOrNull()
    }

    /** The people with [personIds], by id, in one query. */
    fun findPeople(personIds: Collection<Long>): Map<Long, Person> {
        if (personIds.isEmpty()) return emptyMap()
        return selectAll().where { id inList personIds }.associate { it[id] to resultRowToPerson(it) }
    }

    fun resultRowToPerson(row: ResultRow) = Person(
        id = row[id],
        first = row[first],
        last = row[last],
        clubMember = row[clubMember],
        active = row[active]
    )


    fun findPerson(name: String): List<Person> {
        return selectAll().where {
            (first ilike LikePattern("%$name%")) or (last ilike LikePattern("%$name%"))
        }.map(::resultRowToPerson)
    }

    fun upsertPerson(person: Person): Person? {
        return upsert {
            if (person.id > 0) { it[id] = person.id }
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

package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


class DbConfig {
    val jdbcURL: String
        get() = System.getProperty("jdbcurl") ?: "jdbc:postgresql://localhost:5432/regatta"
    val user: String
        get() = System.getProperty("user") ?: "admin"
    val password: String
        get() = System.getProperty("password") ?: "mysecretpassword"


}

object RegattaDatabase {
    fun init() {
        val config = DbConfig()
        val driverClassName = "org.postgresql.Driver"
        val database = Database.connect(config.jdbcURL, driverClassName, config.user, config.password)
        transaction(database) {
            SchemaUtils.create(SeriesTable)
            SchemaUtils.create(PersonTable)
            SchemaUtils.create(RaceClassTable)
            SchemaUtils.create(RaceTable)
            SchemaUtils.create(BoatTable)
            SchemaUtils.create(RaceResultsTable)
            SchemaUtils.create(AuthTable)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun resultRowToSeries(row: ResultRow) = Series(
        id = row[SeriesTable.id],
        name = row[SeriesTable.name],
        active = row[SeriesTable.active]
    )

    suspend fun allSeries(): List<Series> = dbQuery {
        SeriesTable.selectAll().map(::resultRowToSeries)
    }

    suspend fun findSeries(id: Long): Series? = dbQuery {
        SeriesTable.select { SeriesTable.id eq id }
            .map(::resultRowToSeries)
            .singleOrNull()
    }

    suspend fun deleteSeries(id: Long) = dbQuery {
        SeriesTable.deleteWhere {
            SeriesTable.id eq id
        }
    }

    suspend fun findSeries(name: String): List<Series> = dbQuery {
        SeriesTable.select { SeriesTable.name ilike LikePattern("%$name%") }
            .map(::resultRowToSeries)
    }

    suspend fun upsertSeries(series: Series): Series? = dbQuery {
        if (series.id != null) {
            SeriesTable.update(where = { SeriesTable.id eq series.id }) {
                it[name] = series.name
                it[active] = series.active
            }.takeIf { it == 1 }?.let { series }
        } else {
            val statement = SeriesTable.insert {
                it[active] = series.active
                it[name] = series.name
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToSeries)
        }
    }

    private fun resultRowToMaybePerson(row: ResultRow): Person? {
        return row[PersonTable.id]?.let {
            Person(
                id = it,
                first = row[PersonTable.first],
                last = row[PersonTable.last],
                clubMember = row[PersonTable.clubMember],
            )
        }
    }

    private fun resultRowToPerson(row: ResultRow) = Person(
        id = row[PersonTable.id],
        first = row[PersonTable.first],
        last = row[PersonTable.last],
        clubMember = row[PersonTable.clubMember],
        active = row[PersonTable.active]
    )

    suspend fun findPerson(id: Long?): Person? = dbQuery {
        id?.let {
            PersonTable.select {
                PersonTable.id eq id
            }.map(::resultRowToPerson).singleOrNull()
        }
    }

    suspend fun findPerson(name: String): List<Person> = dbQuery {
        PersonTable.select {
            (PersonTable.first ilike LikePattern("%$name%")) or (PersonTable.last ilike LikePattern("%$name%"))
        }.map(::resultRowToPerson)
    }

    suspend fun upsertPerson(person: Person): Person? = dbQuery {
        if (person.id != null) {
            PersonTable.update(where = { PersonTable.id eq person.id }) { stmt ->
                stmt[first] = person.first
                stmt[last] = person.last
                stmt[clubMember] = person.clubMember
                stmt[active] = person.active
            }.takeIf { it == 1 }?.let { person }
        } else {
            PersonTable.insert { stmt ->
                stmt[first] = person.first
                stmt[last] = person.last
                stmt[clubMember] = person.clubMember
                stmt[active] = person.active
            }.resultedValues?.singleOrNull()?.let(::resultRowToPerson)
        }
    }

    suspend fun allPeople(): List<Person> = dbQuery {
        PersonTable.selectAll().map(::resultRowToPerson)
    }

    suspend fun deletePerson(id: Long) = dbQuery {
        removePersonFromBoats(id).takeIf { it > 0 }?.let {
            println("removed person id $id from their boat")
        }
        PersonTable.deleteWhere {
            PersonTable.id eq id
        }
    }

    suspend fun removePersonFromBoats(personId: Long) = dbQuery {
        BoatTable.update(where = {
            BoatTable.skipper eq personId
        }) {
            it[skipper] = null
        }
    }

    fun resultRowToAuth(row: ResultRow): AuthRecord {
        return AuthRecord(
            id = row[AuthTable.id],
            hash = row[AuthTable.hash],
            userName = row[AuthTable.userName],
        )
    }

    suspend fun adminExists() = dbQuery {
        AuthTable.selectAll().count() > 0
    }

    suspend fun getAuth(userName: String) = dbQuery {
        AuthTable.select { AuthTable.userName eq userName }.singleOrNull()?.let(::resultRowToAuth)
    }

    suspend fun getAuth(id: Long) = dbQuery {
        AuthTable.select { AuthTable.id eq id }.singleOrNull()?.let {
            AuthRecord(
                id = it[AuthTable.id],
                hash = it[AuthTable.hash],
                userName = it[AuthTable.userName],
            )
        }
    }

    suspend fun saveAuth(record: AuthRecord) = dbQuery {
        record.id?.let { id ->
            val statement = AuthTable.update(where = { AuthTable.id eq id }) {
                it[hash] = record.hash
                it[userName] = record.userName
            }
            if (statement > 0) {
                record
            } else {
                null
            }
        } ?: run {
            val statement = AuthTable.insert {
                it[hash] = record.hash
                it[userName] = record.userName
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToAuth)
        }
    }

    suspend fun upsertBoat(boat: Boat): Boat? = dbQuery {
        if (boat.id != null) {
            BoatTable.update(where = { BoatTable.id eq boat.id }) {
                it[name] = boat.name
                it[sailNumber] = boat.sailNumber
                it[boatType] = boat.boatType
                it[phrfRating] = boat.phrfRating
                it[active] = boat.active
                boat.skipper?.id?.let { skipperId ->
                    it[skipper] = skipperId
                }
            }.takeIf { it == 1 }?.let { boat }
        } else {
            BoatTable.insert {
                it[name] = boat.name
                it[sailNumber] = boat.sailNumber
                it[boatType] = boat.boatType
                it[phrfRating] = boat.phrfRating
                it[active] = boat.active
                boat.skipper?.id?.let { skipperId ->
                    it[skipper] = skipperId
                }
            }.resultedValues?.singleOrNull()?.let {
                resultRowToBoat(it)
            }
        }
    }

    suspend fun allBoats(): List<Boat> = dbQuery {
        val query = (BoatTable innerJoin PersonTable).selectAll()
        query.map {
            val person = resultRowToPerson(it)
            resultRowToBoat(it, person)
        }.plus(
            BoatTable.select { BoatTable.skipper eq null }.map {
                resultRowToBoat(it, null)
            }
        )
    }

    suspend fun findBoatForPerson(personId: Long): Boat? = dbQuery {
        val query = (BoatTable innerJoin PersonTable)
            .select { BoatTable.skipper eq personId }
        query.singleOrNull()?.let {
            val person = resultRowToPerson(it)
            resultRowToBoat(it, person)
        }
    }

    private fun resultRowToBoat(row: ResultRow, person: Person? = null) = Boat(
        id = row[BoatTable.id],
        name = row[BoatTable.name],
        sailNumber = row[BoatTable.sailNumber],
        boatType = row[BoatTable.boatType],
        phrfRating = row[BoatTable.phrfRating],
        skipper = person
    )

    suspend fun deleteBoat(id: Long) = dbQuery {
        BoatTable.deleteWhere {
            BoatTable.id eq id
        }
    }
}

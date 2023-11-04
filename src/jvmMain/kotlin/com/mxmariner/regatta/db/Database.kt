package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object RegattaDatabase {
    fun init() {
        val driverClassName = "org.postgresql.Driver"
        val jdbcURL = "jdbc:postgresql://localhost:5432/regatta"
        val database = Database.connect(jdbcURL, driverClassName, "admin", "mysecretpassword")
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
        SeriesTable.select { SeriesTable.name ilike LikePattern("%$name%")}
            .map(::resultRowToSeries)
    }

    suspend fun upsertSeries(series: Series): Series? = dbQuery {
        series.id?.let {
            val updated = SeriesTable.update({ SeriesTable.id eq it }) {
                it[name] = series.name
            } > 0
            if (updated) {
                series
            } else {
                null
            }
        } ?: run {
            val statement = SeriesTable.insert {
                it[name] = series.name
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToSeries)
        }
    }

    private fun resultRowToPerson(row: ResultRow) = Person(
        id = row[PersonTable.id],
        first = row[PersonTable.first],
        last = row[PersonTable.last],
        clubMember = row[PersonTable.clubMember],
    )

    suspend fun findPerson(id: Long): Person? = dbQuery {
        PersonTable.select {
            PersonTable.id eq id
        }.map(::resultRowToPerson).singleOrNull()
    }
    suspend fun findPerson(name: String): List<Person> = dbQuery {
        PersonTable.select {
            (PersonTable.first ilike LikePattern("%$name%")) or (PersonTable.last ilike LikePattern("%$name%"))
        }.map(::resultRowToPerson)
    }
    suspend fun upsertPerson(person: Person): Person? = dbQuery {
        person.id?.let {
            val updated = PersonTable.update({PersonTable.id eq it}) {
                it[first] = first
                it[last] = last
            } > 0
            if (updated) {
                person
            } else {
                null
            }
        } ?: run {
            val statement = PersonTable.insert {
                it[first] = person.first
                it[last] = person.last
                it[clubMember] = person.clubMember
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToPerson)
        }
        null
    }

    suspend fun allPeople(): List<Person> = dbQuery {
        PersonTable.selectAll().map(::resultRowToPerson)
    }

    suspend fun deletePerson(id: Long) = dbQuery {
        PersonTable.deleteWhere {
            PersonTable.id eq id
        }
    }

    fun resultRowToAuth(row: ResultRow) : AuthRecord {
        return AuthRecord(
            id = row[AuthTable.id],
            admin = row[AuthTable.isAdmin],
            hash = row[AuthTable.hash],
            userName = row[AuthTable.userName],
        )
    }
    suspend fun adminExists() = dbQuery {
        AuthTable.select { AuthTable.isAdmin eq Op.TRUE }.count() > 0
    }

    suspend fun getAuth(userName: String) = dbQuery {
        AuthTable.select { AuthTable.userName eq userName}.singleOrNull()?.let(::resultRowToAuth)
    }
    suspend fun getAuth(id: Long) = dbQuery {
        AuthTable.select { AuthTable.id eq id }.singleOrNull()?.let {
            AuthRecord(
                id = it[AuthTable.id],
                admin = it[AuthTable.isAdmin],
                hash = it[AuthTable.hash],
                userName = it[AuthTable.userName],
            )
        }
    }

    suspend fun saveAuth(record: AuthRecord) = dbQuery {
        record.id?.let {
            val statement = AuthTable.update {
                it[hash] = record.hash
                it[userName] = record.userName
                it[isAdmin] = record.admin
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
                it[isAdmin] = record.admin
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToAuth)
        }
    }
}

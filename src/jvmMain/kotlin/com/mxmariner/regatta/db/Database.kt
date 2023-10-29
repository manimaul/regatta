package com.mxmariner.regatta.db

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
            SchemaUtils.create(Series)
            SchemaUtils.create(Person)
            SchemaUtils.create(RaceClass)
            SchemaUtils.create(Race)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun resultRowToSeries(row: ResultRow) = Series(
        id = row[Series.id],
        name = row[Series.name],
    )

    suspend fun allSeries(): List<Series> = dbQuery {
        Series.selectAll().map(::resultRowToSeries)
    }

    suspend fun findSeries(id: Long): Series? = dbQuery {
        Series.select { Series.id eq id }
            .map(::resultRowToSeries)
            .singleOrNull()
    }

    suspend fun findSeries(name: String): Series? = dbQuery {
        Series.select { Series.name eq name }
            .map(::resultRowToSeries)
            .singleOrNull()
    }

    suspend fun upsertSeries(series: Series): Series? = dbQuery {
        series.id?.let {
            val updated = Series.update({ Series.id eq it }) {
                it[name] = series.name
            } > 0
            if (updated) {
                series
            } else {
                null
            }
        } ?: run {
            val statement = Series.insert {
                it[name] = series.name
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToSeries)
        }
    }
}

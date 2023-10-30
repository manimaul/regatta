package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
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

    suspend fun findSeries(name: String): Series? = dbQuery {
        SeriesTable.select { SeriesTable.name eq name }
            .map(::resultRowToSeries)
            .singleOrNull()
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
}

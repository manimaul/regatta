package com.mxmariner.regatta.db


import com.mxmariner.regatta.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinInstantColumnType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet
import kotlin.reflect.jvm.internal.impl.util.Check


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
        val tables = arrayOf(
            SeriesTable,
            PersonTable,
            RaceClassCategoryTable,
            BracketTable,
            RaceTable,
            BoatTable,
            RaceResultsTable,
            AuthTable,
            RaceTimeTable,
            RaceBracketJunction,
            CheckinTable,
            SkipperBoatJunction,
        )
        transaction(database) {
            exec(
                "alter table boat drop column if exists class_id"
            )
            SchemaUtils.create(*tables)
            execInBatch(
                SchemaUtils.addMissingColumnsStatements(*tables, withLogs = true)
            )
//            exec(
//                "alter table raceresults drop column if exists name"
//            )
//            exec(
//                "alter table raceresults drop column if exists completion"
//            ALTER TABLE table_name
//RENAME COLUMN column_name TO new_column_name;
//            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    private suspend fun <T> dbRawQuery(
        sql: String,
        args: Iterable<Pair<IColumnType, Any?>>? = null,
        queryHandler: suspend (ResultSet) -> T,
    ): T {
        return newSuspendedTransaction(Dispatchers.IO) {

            val statement = connection.prepareStatement(sql, false).apply { args?.let { fillParameters(args) } }
            queryHandler(statement.executeQuery())
        }
    }

    // Series ------------------------
    suspend fun allSeries(): List<Series> = dbQuery { SeriesTable.selectAllSeries() }
    suspend fun findSeries(id: Long): Series? = dbQuery { SeriesTable.selectSeries(id) }
    suspend fun deleteSeries(id: Long): Int = dbQuery { SeriesTable.deleteSeries(id) }
    suspend fun findSeries(name: String): List<Series> = dbQuery { SeriesTable.selectByName(name) }
    suspend fun upsertSeries(series: Series): Series? = dbQuery { SeriesTable.upsertSeries(series) }

    // Person ------------------------
    suspend fun findPerson(id: Long?): Person? = dbQuery { id?.let { PersonTable.selectPerson(it) } }
    suspend fun findPerson(name: String): List<Person> = dbQuery { PersonTable.findPerson(name) }
    suspend fun upsertPerson(person: Person): Person? = dbQuery { PersonTable.upsertPerson(person) }
    suspend fun allPeople(): List<Person> = dbQuery { PersonTable.selectAllPeople() }
    suspend fun deletePerson(id: Long) = dbQuery {
        BoatTable.removePerson(id)
        PersonTable.deletePerson(id)
    }

    // Boat ------------------------
    suspend fun findBoat(id: Long?): Boat? = dbQuery { BoatTable.findBoat(id) }
    suspend fun upsertBoat(boat: Boat): Boat? = dbQuery { BoatTable.upsertBoat(boat) }
    suspend fun allBoats(): List<Boat> = dbQuery { BoatTable.selectAllBoats() }
    suspend fun findBoatForPerson(personId: Long): Boat? = dbQuery { BoatTable.findBoatForPerson(personId) }
    suspend fun deleteBoat(id: Long): Int = dbQuery { BoatTable.deleteBoat(id) }

    // Auth ------------------------

    suspend fun adminExists() = dbQuery { AuthTable.selectAll().count() > 0 }
    suspend fun getAuth(userName: String) = dbQuery { AuthTable.getAuth(userName) }
    suspend fun getAuth(id: Long) = dbQuery { AuthTable.getAuth(id) }
    suspend fun saveAuth(record: AuthRecord) = dbQuery { AuthTable.saveAuth(record) }

    // Race Bracket ------------------------

    suspend fun findBracket(id: Long) = dbQuery { BracketTable.findBracket(id) }
    suspend fun upsertBracket(item: Bracket): Bracket? = dbQuery { BracketTable.upsertBracket(item) }
    suspend fun allBrackets() = dbQuery { BracketTable.selectAllBrackets() }
    suspend fun deleteBracket(id: Long) = dbQuery { BracketTable.deleteBracket(id) }

    // Race Class ------------------------
    suspend fun upsertRaceCategory(item: RaceClassCat) = dbQuery { RaceClassCategoryTable.upsertRaceCategory(item) }
    suspend fun allCategories(): List<RaceClassCategory> = dbQuery { RaceClassCategoryTable.allCategories() }
    suspend fun deleteCategory(id: Long): Int = dbQuery { RaceClassCategoryTable.deleteCategory(id) }
    suspend fun findRaceCategory(id: Long): RaceCategory? = dbQuery { RaceClassCategoryTable.findRaceCategory(id) }

    // Race ------------------------

    suspend fun allRaces(year: Int): List<RaceFull> {
        val start = Instant.parse("$year-01-01T00:00:00Z")
        val end = Instant.parse("${year + 1}-01-01T00:00:00Z")
        return dbRawQuery<List<Long>>(
            sql = "SELECT DISTINCT race_id FROM racetime WHERE start_date > ? AND start_date < ?;",
            args = listOf(KotlinInstantColumnType() to start, KotlinInstantColumnType() to end)
        ) {
            val raceIds = mutableListOf<Long>()
            while (it.next()) {
                val raceId = it.getLong(1)
                raceIds.add(raceId)
            }
            raceIds
        }.mapNotNull {
            RaceTable.findRace(it)
        }
    }

    suspend fun findRace(id: Long): RaceFull? = dbQuery { RaceTable.findRace(id) }

    suspend fun upsertRace(race: Race): RaceFull? = dbQuery { RaceTable.upsertRace(race) }

    suspend fun deleteRace(id: Long) = dbQuery {
        RaceTimeTable.deleteWhere { RaceTimeTable.raceId eq id }
        RaceTable.deleteWhere { RaceTable.id eq id }
    }

    // Race Times ------------------------
    suspend fun allYears(): List<String> = dbQuery { RaceTimeTable.allYears() }
    suspend fun findRaceTimes(raceId: Long) = dbQuery { RaceTimeTable.selectByRaceId(raceId) }

    // Results ------------------------
    suspend fun deleteResult(id: Long) = dbQuery { RaceResultsTable.deleteWhere { RaceResultsTable.id eq id } }
    suspend fun getResults(year: Int) = dbQuery { RaceResultsTable.getResults(year) }
    suspend fun resultsByRaceId(raceId: Long): List<RaceResultFull> =
        dbQuery { RaceResultsTable.resultsByRaceId(raceId) }
    suspend fun allResults() = dbQuery { RaceResultsTable.allResults() }
    suspend fun upsertResult(result: RaceResult): RaceResultFull? = dbQuery {
        RaceResultsTable.upsertResult(result)
    }
    suspend fun resultCount(raceId: Long) = dbQuery {
        RaceResultsTable.count(raceId)
    }
}

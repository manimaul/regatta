package com.mxmariner.regatta.db


import com.mxmariner.regatta.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
        val tables = arrayOf(
            SeriesTable,
            PersonTable,
            RaceClassCategoryTable,
            RaceClassTable,
            RaceTable,
            BoatTable,
            RaceResultsTable,
            AuthTable,
        )
        transaction(database) {
            SchemaUtils.create(*tables)
            execInBatch(
                SchemaUtils.addMissingColumnsStatements(*tables, withLogs = true)
            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun rowToRace(row: ResultRow, series: Series? = null, person: Person? = null) = RaceFull(
        id = row[RaceTable.id],
        name = row[RaceTable.name],
        series = series,
        startDate = row[RaceTable.startDate],
        endDate = row[RaceTable.endDate],
        rc = person,
        correctionFactor = row[RaceTable.correctionFactor]
    )

    private fun rowToRacePost(row: ResultRow) = RacePost(
        id = row[RaceTable.id],
        name = row[RaceTable.name],
        seriesId = row[RaceTable.seriesId],
        startDate = row[RaceTable.startDate],
        endDate = row[RaceTable.endDate],
        rcId = row[RaceTable.rcId],
        correctionFactor = row[RaceTable.correctionFactor]
    )

    private fun resultRowToSeries(row: ResultRow) = Series(
        id = row[SeriesTable.id], name = row[SeriesTable.name], active = row[SeriesTable.active]
    )

    suspend fun allSeries(): List<Series> = dbQuery {
        SeriesTable.selectAll().map(::resultRowToSeries)
    }

    suspend fun findSeries(id: Long): Series? = dbQuery {
        SeriesTable.select { SeriesTable.id eq id }.map(::resultRowToSeries).singleOrNull()
    }

    suspend fun deleteSeries(id: Long) = dbQuery {
        SeriesTable.deleteWhere {
            SeriesTable.id eq id
        }
    }

    suspend fun findSeries(name: String): List<Series> = dbQuery {
        SeriesTable.select { SeriesTable.name ilike LikePattern("%$name%") }.map(::resultRowToSeries)
    }

    suspend fun upsertSeries(series: Series): Series? = dbQuery {
        if (series.id != null) {
            SeriesTable.update(where = { SeriesTable.id eq series.id }) {
                it[name] = series.name.trim()
                it[active] = series.active
            }.takeIf { it == 1 }?.let { series }
        } else {
            val statement = SeriesTable.insert {
                it[name] = series.name.trim()
                it[active] = series.active
            }
            statement.resultedValues?.singleOrNull()?.let(::resultRowToSeries)
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

    suspend fun findBoat(id: Long?): Boat? = dbQuery {
        id?.let {

            BoatTable.innerJoin(PersonTable).innerJoin(RaceClassTable).select {
                BoatTable.id eq id
            }.map {
                val person = resultRowToPerson(it)
                val raceClass = resultRowToClass(it)
                resultRowToBoat(it, person, raceClass)
            }.singleOrNull() ?: BoatTable.innerJoin(PersonTable).select {
                BoatTable.id eq id
            }.map {
                val person = resultRowToPerson(it)
                resultRowToBoat(it, person, null)
            }.singleOrNull() ?: BoatTable.innerJoin(RaceClassTable).select {
                BoatTable.id eq id
            }.map {
                val raceClass = resultRowToClass(it)
                resultRowToBoat(it, null, raceClass)
            }.singleOrNull() ?: BoatTable.select {
                BoatTable.id eq id
            }.map {
                resultRowToBoat(it, null, null)
            }.singleOrNull()
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
                stmt[first] = person.first.trim()
                stmt[last] = person.last.trim()
                stmt[clubMember] = person.clubMember
                stmt[active] = person.active
            }.takeIf { it == 1 }?.let { person }
        } else {
            PersonTable.insert { stmt ->
                stmt[first] = person.first.trim()
                stmt[last] = person.last.trim()
                stmt[clubMember] = person.clubMember
                stmt[active] = person.active
            }.resultedValues?.singleOrNull()?.let(::resultRowToPerson)
        }
    }

    suspend fun allPeople(): List<Person> = dbQuery {
        PersonTable.selectAll().map(::resultRowToPerson).sortedBy { it.first }
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
                it[name] = boat.name.trim()
                it[sailNumber] = boat.sailNumber.trim()
                it[boatType] = boat.boatType.trim()
                it[phrfRating] = boat.phrfRating
                it[active] = boat.active
                it[currentClass] = boat.raceClass?.id
                it[skipper] = boat.skipper?.id
            }.takeIf { it == 1 }?.let { boat }
        } else {
            BoatTable.insert {
                it[name] = boat.name.trim()
                it[sailNumber] = boat.sailNumber.trim()
                it[boatType] = boat.boatType.trim()
                it[phrfRating] = boat.phrfRating
                it[active] = boat.active
                it[currentClass] = boat.raceClass?.id
                it[skipper] = boat.skipper?.id
            }.resultedValues?.singleOrNull()?.let {
                resultRowToBoat(it)
            }
        }
    }

    suspend fun upsertRaceCategory(item: RaceCategory) = dbQuery {
        if (item.id != null) {
            RaceClassCategoryTable.update(where = { RaceClassCategoryTable.id eq item.id }) {
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

    suspend fun upsertRaceClass(item: RaceClass): RaceClass? = dbQuery {
        if (item.id != null) {
            RaceClassTable.update(where = { RaceClassTable.id eq item.id }) {
                it[name] = item.name.trim()
                it[description] = item.description?.trim()
                it[active] = item.active
                it[category] = item.category
            }.takeIf { it == 1 }?.let { item }
        } else {
            RaceClassTable.insert {
                it[name] = item.name.trim()
                it[description] = item.description?.trim()
                it[active] = item.active
                it[category] = item.category
            }.resultedValues?.singleOrNull()?.let(::resultRowToClass)
        }
    }

    suspend fun allBoats(): List<Boat> = dbQuery {
        BoatTable.innerJoin(PersonTable).innerJoin(RaceClassTable).selectAll().asSequence().map { row ->
            val person = resultRowToPerson(row)
            val raceClass = resultRowToClass(row)
            resultRowToBoat(row, person, raceClass)
        }.plus(BoatTable.innerJoin(PersonTable).select {
            BoatTable.currentClass eq (null)
        }.map {
            val person = resultRowToPerson(it)
            resultRowToBoat(it, person, null)
        }).plus(BoatTable.innerJoin(RaceClassTable).select {
            BoatTable.skipper eq (null)
        }.map {
            val raceClass = resultRowToClass(it)
            resultRowToBoat(it, null, raceClass)
        }).plus(BoatTable.select {
            (BoatTable.skipper eq null) and (BoatTable.currentClass eq null)
        }.map {
            resultRowToBoat(it, null, null)
        }).sortedWith { lhs, rhs ->
            if (lhs.phrfRating != null && rhs.phrfRating != null) {
                lhs.phrfRating.compareTo(rhs.phrfRating)
            } else if (lhs.phrfRating != null) {
                -1
            } else if (rhs.phrfRating != null) {
                1
            } else if (lhs.raceClass?.id != null && rhs.raceClass?.id != null) {
                lhs.raceClass.id.compareTo(rhs.raceClass.id)
            } else {
                0
            }
        }.toList()
    }

    suspend fun allCategories(): List<RaceClassCategory> = dbQuery {
        RaceClassCategoryTable.selectAll().map(::resultRowToClassCategory).map { cat ->
            cat.copy(children = RaceClassTable.select { RaceClassTable.category eq cat.id!! }.map(::resultRowToClass))
        }
    }

    suspend fun findBoatForPerson(personId: Long): Boat? = dbQuery {
        val query = (BoatTable innerJoin PersonTable).select { BoatTable.skipper eq personId }
        query.singleOrNull()?.let {
            val person = resultRowToPerson(it)
            resultRowToBoat(it, person)
        }
    }

    private fun resultRowToBoat(
        row: ResultRow,
        person: Person? = null,
        raceClass: RaceClass? = null,
    ) = Boat(
        id = row[BoatTable.id],
        name = row[BoatTable.name],
        sailNumber = row[BoatTable.sailNumber],
        boatType = row[BoatTable.boatType],
        phrfRating = row[BoatTable.phrfRating],
        skipper = person,
        raceClass = raceClass
    )

    suspend fun deleteBoat(id: Long) = dbQuery {
        BoatTable.deleteWhere {
            BoatTable.id eq id
        }
    }

    suspend fun allRaceClasses() = dbQuery {
        RaceClassTable.selectAll().map(::resultRowToClass)
    }

    suspend fun findRaceClass(id: Long) = dbQuery {
        RaceClassTable.select { RaceClassTable.id.eq(id) }.map(::resultRowToClass).singleOrNull()
    }

    private fun resultRowToClass(row: ResultRow) = RaceClass(
        id = row[RaceClassTable.id],
        name = row[RaceClassTable.name],
        description = row[RaceClassTable.description],
        active = row[RaceClassTable.active],
        category = row[RaceClassTable.category],
    )

    private fun resultRowToClassCategory(row: ResultRow) = RaceClassCategory(
        id = row[RaceClassCategoryTable.id],
        name = row[RaceClassCategoryTable.name],
        active = row[RaceClassCategoryTable.active],
    )

    private fun resultRowToCategory(row: ResultRow) = RaceCategory(
        id = row[RaceClassCategoryTable.id],
        name = row[RaceClassCategoryTable.name],
        active = row[RaceClassCategoryTable.active],
    )

    suspend fun deleteRaceClass(id: Long) = dbQuery {
        RaceClassTable.deleteWhere {
            RaceClassTable.id eq id
        }
    }

    suspend fun deleteCategory(id: Long) = dbQuery {
        RaceClassCategoryTable.deleteWhere {
            RaceClassCategoryTable.id eq id
        }
    }

    suspend fun findRaceCategory(id: Long) = dbQuery {
        RaceClassCategoryTable.select {
            RaceClassCategoryTable.id eq id
        }.map {
            RaceCategory(
                id = it[RaceClassCategoryTable.id],
                name = it[RaceClassCategoryTable.name],
                active = it[RaceClassCategoryTable.active],
            )
        }.singleOrNull()
    }

    suspend fun allRaces() = dbQuery {
        RaceTable.innerJoin(SeriesTable).innerJoin(PersonTable).selectAll().map {
            val series = resultRowToSeries(it)
            val person = resultRowToPerson(it)
            rowToRace(it, series, person)
        } + RaceTable.innerJoin(SeriesTable).select {
            RaceTable.rcId eq null
        }.map {
            val series = resultRowToSeries(it)
            rowToRace(it, series)
        } + RaceTable.innerJoin(PersonTable).select {
            RaceTable.seriesId.eq(null)
        }.map {
            val person = resultRowToPerson(it)
            rowToRace(it, null, person)
        } + RaceTable.select {
            RaceTable.seriesId.eq(null).and(RaceTable.rcId.eq(null))
        }.map {
            rowToRace(it)
        }
    }

    suspend fun findRace(id: Long) = dbQuery {
        val races = RaceTable.innerJoin(SeriesTable).innerJoin(PersonTable).select {
            RaceTable.id eq id
        }.map {
            val series = resultRowToSeries(it)
            val person = resultRowToPerson(it)
            rowToRace(it, series, person)
        } + RaceTable.innerJoin(SeriesTable).select {
            RaceTable.id.eq(id).and(RaceTable.rcId.eq(null))
        }.map {
            val series = resultRowToSeries(it)
            rowToRace(it, series, null)
        } + RaceTable.innerJoin(PersonTable).select {
            RaceTable.id.eq(id).and(RaceTable.seriesId.eq(null))
        }.map {
            val person = resultRowToPerson(it)
            rowToRace(it, null, person)
        } + RaceTable.select {
            RaceTable.id.eq(id).and(RaceTable.seriesId.eq(null).and(RaceTable.rcId.eq(null)))
        }.map {
            rowToRace(it, null, null)
        }
        races.singleOrNull()
    }

    suspend fun upsertRace(race: Race): RaceFull? {
        return dbQuery {
            val id = race.id
            if (id != null) {
                RaceTable.update(where = { RaceTable.id eq id }) {
                    it[name] = race.name.trim()
                    it[seriesId] = race.seriesId
                    it[startDate] = race.startDate
                    it[endDate] = race.endDate
                    it[rcId] = race.rcId
                    it[correctionFactor] = race.correctionFactor
                }.takeIf { it > 0 }?.let { race }
            } else {
                RaceTable.insert {
                    it[name] = race.name.trim()
                    it[seriesId] = race.seriesId
                    it[startDate] = race.startDate
                    it[endDate] = race.endDate
                    it[rcId] = race.rcId
                    it[correctionFactor] = race.correctionFactor
                }.resultedValues?.map { rowToRacePost(it) }?.singleOrNull()
            }
        }?.let {
            RaceFull(
                id = it.id,
                name = it.name,
                series = it.seriesId?.let { findSeries(it) },
                startDate = it.startDate,
                endDate = it.endDate,
                rc = findPerson(it.rcId),
                correctionFactor = it.correctionFactor
            )
        }
    }

    suspend fun deleteRace(id: Long) = dbQuery {
        RaceTable.deleteWhere { RaceTable.id eq id }
    }

    suspend fun deleteResult(id: Long) = dbQuery {
        RaceResultsTable.deleteWhere { RaceResultsTable.id eq id }
    }

    fun rowToResult(row: ResultRow, race: RaceFull, boat: Boat, raceClass: RaceClass) = RaceResultFull(
        id = row[RaceResultsTable.id],
        race = race,
        boat = boat,
        raceClass = raceClass,
        finish = row[RaceResultsTable.finish],
        phrfRating = row[RaceResultsTable.phrfRating],
    )

    suspend fun getResults(year: Int) = dbQuery {
        val start = Instant.parse("$year-01-01")
        val end = Instant.parse("${year + 1}-01-01")
        RaceResultsTable.innerJoin(RaceTable).innerJoin(BoatTable).innerJoin(RaceClassTable).select {
            RaceResultsTable.finish.greaterEq(start) and RaceResultsTable.finish.less(end)
        }.map {
            val person = it[BoatTable.skipper]?.let { id -> findPerson(id) }
            val series = it[RaceTable.seriesId]?.let { id -> findSeries(id) }
            val raceClass = resultRowToClass(it)
            val race = rowToRace(it, series, person)
            val boat = resultRowToBoat(it, person, raceClass)
            rowToResult(it, race, boat, raceClass)
        }
    }

    suspend fun getResult(id: Long) = dbQuery {
        RaceResultsTable.select {
            RaceResultsTable.id eq id
        }.map {
            val raceClass = findRaceClass(it[RaceResultsTable.raceClass])!!
            val race = findRace(it[RaceResultsTable.raceId])!!
            val boat = findBoat(it[RaceResultsTable.boatId])!!
            rowToResult(it, race, boat, raceClass)
        }.singleOrNull()
    }

    suspend fun resultsByRaceId(raceId: Long) = dbQuery {
        RaceResultsTable.select {
            RaceResultsTable.raceId eq raceId
        }.map {
            val raceClass = findRaceClass(it[RaceResultsTable.raceClass])!!
            val race = findRace(it[RaceResultsTable.raceId])!!
            val boat = findBoat(it[RaceResultsTable.boatId])!!
            rowToResult(it, race, boat, raceClass)
        }
    }

    suspend fun allResults() = dbQuery {
        RaceResultsTable.selectAll().map {
            val raceClass = findRaceClass(it[RaceResultsTable.raceClass])!!
            val race = findRace(it[RaceResultsTable.raceId])!!
            val boat = findBoat(it[RaceResultsTable.boatId])!!
            rowToResult(it, race, boat, raceClass)
        }
    }

    suspend fun upsertResult(result: RaceResult): RaceResultFull? = dbQuery {
        val id = result.id
        if (id != null) {
            RaceResultsTable.update( where = { RaceResultsTable.id eq id }) {
                it[raceId] = result.raceId
                it[boatId] = result.boatId
                it[raceClass] = result.raceClassId
                it[finish] = result.finish
                it[phrfRating] = result.phrfRating
            }.takeIf { it > 0 }?.let {
                getResult(id)
            }
        } else {
            RaceResultsTable.insert {
                it[raceId] = result.raceId
                it[boatId] = result.boatId
                it[raceClass] = result.raceClassId
                it[finish] = result.finish
                it[phrfRating] = result.phrfRating
            }.resultedValues?.singleOrNull()?.let {
                getResult(it[RaceResultsTable.id])
            }
        }
    }
}

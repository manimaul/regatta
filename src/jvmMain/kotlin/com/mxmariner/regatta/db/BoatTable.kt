package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.Windseeker
import com.mxmariner.regatta.db.PersonTable.resultRowToPerson
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object BoatTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val sailNumber = varchar("sail_number", 128)
    val boatType = varchar("boat_type", 128)
    val phrfRating = integer("phrf_rating").nullable()
    val wsRating = integer("ws_rating").nullable()
    val wsFlying = bool("ws_flying").nullable()
    val skipper = (long("skipper_id") references PersonTable.id).nullable()
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun removePerson(personId: Long): Int {
        return update(where = {
            skipper eq personId
        }) {
            it[skipper] = null
        }
    }

    fun findBoat(boatId: Long?): Boat? {
        return boatId?.let {
            innerJoin(PersonTable).select {
                id eq boatId
            }.map {
                val person = resultRowToPerson(it)
                resultRowToBoat(it, person)
            }.singleOrNull() ?: select {
                (id eq boatId) and (skipper eq null)
            }.map {
                resultRowToBoat(it, null)
            }.singleOrNull()
        }
    }

    fun upsertBoat(boat: Boat) : Boat? {
        return if (boat.id != null) {
            update(where = { id eq boat.id }) {
                it[name] = boat.name.trim()
                it[sailNumber] = boat.sailNumber.trim()
                it[boatType] = boat.boatType.trim()
                it[phrfRating] = boat.phrfRating
                it[active] = boat.active
                it[wsFlying] = boat.windseeker?.flyingSails
                it[wsRating] = boat.windseeker?.rating
                it[skipper] = boat.skipper?.id
            }.takeIf { it == 1 }?.let { boat }
        } else {
            insert {
                it[name] = boat.name.trim()
                it[sailNumber] = boat.sailNumber.trim()
                it[boatType] = boat.boatType.trim()
                it[phrfRating] = boat.phrfRating
                it[active] = boat.active
                it[wsFlying] = boat.windseeker?.flyingSails
                it[wsRating] = boat.windseeker?.rating
                it[skipper] = boat.skipper?.id
            }.resultedValues?.singleOrNull()?.let {
                resultRowToBoat(it)
            }
        }
    }

    fun deleteBoat(boatId: Long): Int {
        return BoatTable.deleteWhere {
            id eq boatId
        }
    }
    fun findBoatForPerson(personId: Long): Boat? {
        return innerJoin(PersonTable).select { skipper eq personId }.singleOrNull()?.let {
            val person = resultRowToPerson(it)
            resultRowToBoat(it, person)
        }
    }
    fun selectAllBoats() : List<Boat> {
        return innerJoin(PersonTable).selectAll().asSequence().map { row ->
            val person = resultRowToPerson(row)
            resultRowToBoat(row, person)
        }.plus(BoatTable.select {
            (BoatTable.skipper eq null)
        }.map {
            resultRowToBoat(it, null)
        }).sortedWith { lhs, rhs ->
            if (lhs.phrfRating != null && rhs.phrfRating != null) {
                lhs.phrfRating.compareTo(rhs.phrfRating)
            } else if (lhs.phrfRating != null) {
                -1
            } else if (rhs.phrfRating != null) {
                1
            } else if (lhs.windseeker?.flyingSails != null && rhs.windseeker?.flyingSails != null) {
                (lhs.windseeker.rating ?: Int.MAX_VALUE).compareTo((rhs.windseeker.rating ?: Int.MAX_VALUE))
            } else if (lhs.windseeker?.flyingSails != null) {
                -1
            } else if (rhs.windseeker?.flyingSails != null) {
                1
            } else {
                (lhs.windseeker?.rating ?: Int.MAX_VALUE).compareTo((rhs.windseeker?.rating ?: Int.MAX_VALUE))
            }
        }.toList()
    }

    fun resultRowToBoat(
        row: ResultRow,
        person: Person? = null,
    ): Boat {
        val phrfRating = row[BoatTable.phrfRating]
        val windseeker: Windseeker? = if (phrfRating == null) {
            Windseeker(
                rating = row[wsRating],
                flyingSails = row[wsFlying] ?: false,
            )
        } else null
        return Boat(
            id = row[id],
            name = row[name],
            sailNumber = row[sailNumber],
            boatType = row[boatType],
            phrfRating = phrfRating,
            windseeker = windseeker,
            skipper = person,

            )
    }
}

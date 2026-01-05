package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Windseeker
import com.mxmariner.regatta.db.PersonTable.resultRowToPerson
import com.mxmariner.regatta.ratingDefault
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

    fun findBoatSkipper(boatId: Long): BoatSkipper? {
        return innerJoin(PersonTable).select { id eq boatId }.singleOrNull()?.let { row ->
            BoatSkipper(
                boat = resultRowToBoat(row),
                skipper = resultRowToPerson(row)
            )
        } ?: select { id.eq(boatId) }.singleOrNull()?.let { row ->
            BoatSkipper(
                boat = resultRowToBoat(row),
                skipper = null
            )
        }
    }

    fun upsertBoatSkipper(boatSkipper: BoatSkipper): BoatSkipper {
        return BoatSkipper(
            boat = boatSkipper.boat?.let { upsertBoat(it) },
            skipper = boatSkipper.skipper?.let { PersonTable.upsertPerson(it) },
        )
    }

    fun upsertBoat(boat: Boat): Boat? {
        return upsert {
            if (boat.id > 0) it[id] = boat.id
            it[name] = boat.name.trim()
            it[sailNumber] = boat.sailNumber.trim()
            it[boatType] = boat.boatType.trim()
            it[phrfRating] = boat.phrfRating
            it[skipper] = boat.skipperId
            it[active] = boat.active
            it[wsFlying] = boat.windseeker?.flyingSails
            it[wsRating] = boat.windseeker?.rating
        }.resultedValues?.singleOrNull()?.let { row ->
            resultRowToBoat(row)
        }
    }

    fun deleteBoat(boatId: Long): Int {
        OrcTable.deleteCerts(boatId)
        return BoatTable.deleteWhere {
            id eq boatId
        }
    }

    fun findBoatForSkipper(personId: Long): BoatSkipper? {
        return innerJoin(PersonTable).select { skipper eq personId }.singleOrNull()?.let { row ->
            BoatSkipper(
                boat = resultRowToBoat(row),
                skipper = resultRowToPerson(row)
            )
        }
    }

    fun selectAllBoats(): List<BoatSkipper> {
        return innerJoin(PersonTable).selectAll().asSequence().map { row ->
            BoatSkipper(
                boat = resultRowToBoat(row),
                skipper = resultRowToPerson(row)
            )
        }.plus(BoatTable.select {
            (skipper eq null)
        }.map {
            BoatSkipper(
                boat = resultRowToBoat(it),
                skipper = null
            )
        }).sortedWith { lhs, rhs ->
            val lhsPhrfRating = lhs.boat?.phrfRating
            val rhsPhrfRating = rhs.boat?.phrfRating
            val lhsWindseeker = lhs.boat?.windseeker
            val rhsWindseeker = rhs.boat?.windseeker
            if (lhsPhrfRating != null && rhsPhrfRating != null) {
                lhsPhrfRating.compareTo(rhsPhrfRating)
            } else if (lhsPhrfRating != null) {
                -1
            } else if (rhsPhrfRating != null) {
                1
            } else if (lhsWindseeker?.flyingSails != null && rhsWindseeker?.flyingSails != null) {
                (lhsWindseeker.rating).compareTo((rhsWindseeker.rating))
            } else if (lhs.boat?.windseeker?.flyingSails != null) {
                -1
            } else if (rhs.boat?.windseeker?.flyingSails != null) {
                1
            } else {
                (lhs.boat?.windseeker?.rating ?: Int.MAX_VALUE).compareTo(
                    (rhs.boat?.windseeker?.rating ?: Int.MAX_VALUE)
                )
            }
        }.toList()
    }

    fun resultRowToBoat(
        row: ResultRow,
    ): Boat {
        val phrfRating = row[phrfRating]
        val windseeker: Windseeker? = if (phrfRating == null) {
            Windseeker(
                rating = row[wsRating] ?: ratingDefault.toInt(),
                flyingSails = row[wsFlying] ?: false,
            )
        } else null
        val boatId = row[id]
        val numberOfRaces = RaceResultsTable.raceCount(boatId)
        return Boat(
            id = boatId,
            name = row[name],
            sailNumber = row[sailNumber],
            boatType = row[boatType],
            phrfRating = phrfRating,
            windseeker = windseeker,
            numberOfRaces = numberOfRaces,
            orcCerts = OrcTable.findCertificates(boatId)
        )
    }
}

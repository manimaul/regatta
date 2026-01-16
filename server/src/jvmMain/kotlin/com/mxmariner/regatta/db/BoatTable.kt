package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.RatingType
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
    val ratingType = varchar("ratingtype", 128)
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

    fun upsertBoat(boat: Boat): Boat? {
        if (boat.id > 0) {
            OrcTable.unlinkCerts(boat.id)
        }
        boat.orcCerts.forEach {
            OrcTable.upsertCert(boat.id, it)
        }
        return upsert {
            if (boat.id > 0) it[id] = boat.id
            it[name] = boat.name.trim()
            it[sailNumber] = boat.sailNumber.trim()
            it[boatType] = boat.boatType.trim()
            it[phrfRating] = boat.phrfRating
            it[skipper] = boat.skipperId
            it[active] = boat.active
            it[ratingType] = boat.ratingType.name
        }.resultedValues?.singleOrNull()?.let { row ->
            resultRowToBoat(row)
        }
    }

    fun deleteBoat(boatId: Long): Int {
        OrcTable.unlinkCerts(boatId)
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
            val left = lhs.boat?.ratingType ?: RatingType.CruisingNonFlyingSails
            val right = rhs.boat?.ratingType ?: RatingType.CruisingNonFlyingSails
            if (left.isORC && right.isORC) {
                (rhs.boat?.orcCerts?.maxOf{ it.allPurposeTot } ?: 0.0).compareTo(
                    lhs.boat?.orcCerts?.maxOf { it.allPurposeTot } ?: 0.0
                )
            }
            if (left == RatingType.PHRF && right == RatingType.PHRF) {
                (lhs.boat?.phrfRating ?: ratingDefault.toInt()).compareTo(
                   rhs.boat?.phrfRating ?: ratingDefault.toInt()
                )
            } else {
                left.compareTo(right)
            }
        }.toList()
    }

    fun resultRowToBoat(
        row: ResultRow,
    ): Boat {
        val phrfRating = row[phrfRating]
        val boatId = row[id]
        val numberOfRaces = RaceResultsTable.raceCount(boatId)
        return Boat(
            id = boatId,
            name = row[name],
            sailNumber = row[sailNumber],
            boatType = row[boatType],
            phrfRating = phrfRating,
            ratingType = RatingType.valueOf(row[ratingType]),
            numberOfRaces = numberOfRaces,
            orcCerts = OrcTable.findCertificates(boatId)
        )
    }
}

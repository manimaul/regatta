package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.db.PersonTable.resultRowToPerson
import com.mxmariner.regatta.ratingDefault
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

object BoatTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val sailNumber = varchar("sail_number", 128)
    val boatType = varchar("boat_type", 128)
    val phrfRating = integer("phrf_rating").nullable()
    val ratingType = varchar("rating_type", 128)
    val skipper = (long("skipper_id") references PersonTable.id).nullable()
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    init {
        index("boat_skipper_id", false, skipper)
    }

    fun removePerson(personId: Long): Int {
        return update(where = {
            skipper eq personId
        }) {
            it[skipper] = null
        }
    }

    fun findBoatSkipper(boatId: Long): BoatSkipper? = findBoatSkippers(listOf(boatId))[boatId]

    /** The boats with [boatIds] and their skippers, by boat id; three statements for any number of boats. */
    fun findBoatSkippers(boatIds: Collection<Long>): Map<Long, BoatSkipper> {
        if (boatIds.isEmpty()) return emptyMap()
        return toBoatSkippers(leftJoin(PersonTable).selectAll().where { id inList boatIds }.toList())
            .associateBy { it.boat?.id ?: 0L }
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
            val boatId = row[id]
            resultRowToBoat(row, RaceResultsTable.raceCount(boatId), OrcTable.findCertificates(boatId))
        }
    }

    fun deleteBoat(boatId: Long): Int {
        OrcTable.unlinkCerts(boatId)
        return BoatTable.deleteWhere {
            id eq boatId
        }
    }

    fun findBoatForSkipper(personId: Long): BoatSkipper? {
        return toBoatSkippers(innerJoin(PersonTable).selectAll().where { skipper eq personId }.toList()).singleOrNull()
    }

    /** Every boat with its skipper, if any, sorted for the boats page; three statements in all. */
    fun selectAllBoats(): List<BoatSkipper> {
        return toBoatSkippers(leftJoin(PersonTable).selectAll().toList())
            .sortedBy { it.skipper == null }
            .sortedWith { lhs, rhs ->
                val left = lhs.boat?.ratingType ?: RatingType.CruisingNonFlyingSails
                val right = rhs.boat?.ratingType ?: RatingType.CruisingNonFlyingSails
                if (left.isPHRF && right.isPHRF) {
                    (lhs.boat?.phrfRating ?: ratingDefault).compareTo(
                        rhs.boat?.phrfRating ?: ratingDefault
                    )
                } else if (left.isORC && right.isORC) {
                    val rmin = rhs.boat?.orcCerts?.maxOf { it.virtualPhrf() } ?: 0
                    val lmin = lhs.boat?.orcCerts?.maxOf { it.virtualPhrf() } ?: 0
                    lmin.compareTo(rmin)
                } else {
                    left.compareTo(right)
                }
            }
    }

    /**
     * Hydrates boat rows joined to [PersonTable] (inner or left) with their race counts and ORC
     * certificates from one grouped query each. A row with no person columns has no skipper.
     */
    private fun toBoatSkippers(rows: List<ResultRow>): List<BoatSkipper> {
        val boatIds = rows.map { it[id] }
        val raceCounts = RaceResultsTable.raceCountsByBoat(boatIds)
        val certs = OrcTable.findCertificatesByBoat(boatIds)
        return rows.map { row ->
            val boatId = row[id]
            BoatSkipper(
                boat = resultRowToBoat(row, raceCounts[boatId] ?: 0L, certs[boatId] ?: emptyList()),
                skipper = row.getOrNull(PersonTable.id)?.let { resultRowToPerson(row) }
            )
        }
    }

    fun resultRowToBoat(
        row: ResultRow,
        numberOfRaces: Long,
        orcCerts: List<OrcCertificate>,
    ): Boat {
        return Boat(
            id = row[id],
            name = row[name],
            sailNumber = row[sailNumber],
            boatType = row[boatType],
            phrfRating = row[phrfRating],
            ratingType = RatingType.valueOf(row[ratingType]),
            numberOfRaces = numberOfRaces,
            orcCerts = orcCerts
        )
    }
}

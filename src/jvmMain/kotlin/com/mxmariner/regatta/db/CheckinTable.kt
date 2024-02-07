package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Checkin
import com.mxmariner.regatta.db.BracketTable.autoIncrement
import org.jetbrains.exposed.sql.*

object CheckinTable : Table() {
    val race = (long("race_id") references RaceTable.id)
    val boat = (long("boat_id") references BoatTable.id)
    val checkedIn = bool("checked_in")

    fun upsertCheckin(checkin: Checkin): Checkin? {
        return upsert(race, boat) {
            it[race] = checkin.raceId
            it[boat] = checkin.boatId
            it[checkedIn] = checkin.checkedIn
        }.resultedValues?.map(::rowToCheckin)?.singleOrNull()
    }

    fun getCheckin(boatId: Long, raceId: Long): Checkin? {
        return select { (boat eq boatId) and (race eq raceId) }
            .map(::rowToCheckin)
            .singleOrNull()
    }

    fun rowToCheckin(row: ResultRow) = Checkin(
        raceId = row[race],
        boatId = row[boat],
        checkedIn = row[checkedIn]
    )
}

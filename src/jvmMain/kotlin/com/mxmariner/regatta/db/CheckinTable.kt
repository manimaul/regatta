package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Checkin
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

object CheckinTable : Table() {
    val bracket = (long("bracket_id") references BracketTable.id)
    val race = (long("race_id") references RaceTable.id)
    val boat = (long("boat_id") references BoatTable.id)
    val checkedIn = bool("checked_in")

    fun getIsCheckIn(boatId: Long, raceId: Long): Boolean? {
        return select { (boat eq boatId) and (race eq raceId) }.map { row ->
            row[checkedIn]
        }.singleOrNull()
    }

    fun getCheckin(boatId: Long, raceId: Long): Checkin? {
        return BoatTable.findBoat(boatId)?.let { bt ->
            RaceTable.findRace(raceId)?.let { re ->
                select { (boat eq boatId) and (race eq raceId) }.map { row ->
                    BracketTable.findBracket(row[bracket])?.let { bk ->
                        Checkin(
                            boat = bt,
                            race = re,
                            bracket = bk,
                            checkedIn = row[checkedIn]
                        )
                    }
                }.singleOrNull()
            }
        }
    }
}

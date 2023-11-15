package com.mxmariner.regatta.db

import com.mxmariner.regatta.db.BoatTable.nullable
import com.mxmariner.regatta.db.BoatTable.references
import com.mxmariner.regatta.db.RaceClassTable.autoIncrement
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object SeriesTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 1024).uniqueIndex("nameIdx")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)
}

object PersonTable : Table() {
    val id = long("id").autoIncrement()
    val first = varchar("first", 128)
    val last = varchar("last", 128)
    val clubMember = bool("club_member")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)
    val compoundIdx = uniqueIndex( first, last)
}

object AuthTable : Table() {
    val id = long("id").autoIncrement()
    //hmac sha512 hash of user's password - hashed client side
    val hash = varchar("hash", 128)
    val userName = varchar("user_name", 128).uniqueIndex("user_name_idx")
    override val primaryKey = PrimaryKey(PersonTable.id)
}

object RaceClassCategoryTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val active = bool("active")
    override val primaryKey = PrimaryKey(RaceClassCategoryTable.id)
}

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val description = varchar("description", 1024).nullable()
    val active = bool("active")
    val category = (long("category") references RaceClassCategoryTable.id)
    override val primaryKey = PrimaryKey(id)
}

object RaceTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val seriesId = (long("series_id") references SeriesTable.id).nullable()
    val startDate = timestamp("start_date").nullable()
    val endDate = timestamp("end_date").nullable()
    val rcId = (long("rc_id") references PersonTable.id).nullable()
    val correctionFactor = integer("correction_factor").nullable()
    override val primaryKey = PrimaryKey(id)
}

object BoatTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val sailNumber = varchar("sail_number", 128)
    val boatType = varchar("boat_type", 128)
    val phrfRating = integer("phrf_rating").nullable()
    val skipper = (long("skipper_id") references PersonTable.id).nullable()
    val currentClass = (long("class_id") references RaceClassTable.id).nullable()
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)
}

object RaceResultsTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val raceId = (long("race_id") references RaceTable.id).nullable()
    val boatId = (long("boat_id") references BoatTable.id).nullable()
    val finish = timestamp("end_date").nullable()
    val phrfRating = integer("phrf_rating").nullable()
    override val primaryKey = PrimaryKey(id)
}

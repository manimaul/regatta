package com.mxmariner.regatta.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object SeriesTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 1024).uniqueIndex("nameIdx")
    override val primaryKey = PrimaryKey(id)
}

object PersonTable : Table() {
    val id = long("id").autoIncrement()
    val first = varchar("first", 128)
    val last = varchar("last", 128)
    val clubMember = bool("club_member")
    override val primaryKey = PrimaryKey(id)
}

object RaceClassTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 128)
    val description = varchar("description", 1024).nullable()
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

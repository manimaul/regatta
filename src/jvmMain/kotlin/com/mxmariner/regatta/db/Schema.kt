package com.mxmariner.regatta.db

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp


@Serializable
data class Series(
    val id: Long? = null,
    val name: String
) {
    companion object : Table() {
        val id = long("id").autoIncrement()
        val name = varchar("name", 1024).uniqueIndex("nameIdx")
        override val primaryKey = PrimaryKey(id)
    }
}

@Serializable
data class Person(
    val id: Long? = null,
    val first: String,
    val last: String,
) {
    companion object : Table() {
        val id = long("id").autoIncrement()
        val first = varchar("first", 128)
        val last = varchar("last", 128)
        val clubMember = bool("club_member")
        override val primaryKey = PrimaryKey(id)
    }
}

@Serializable
data class RaceClass(
    val id: Long? = null,
    val name: String,
    val description: String?,
) {
    companion object : Table() {
        val id = long("id").autoIncrement()
        val name = varchar("name", 128)
        val description = varchar("description", 1024).nullable()
        override val primaryKey = PrimaryKey(id)
    }
}

@Serializable
data class Race(
    val id: Long? = null,
    val name: String,
    val rcId: Long?,
    val seriesId: Long,
    val startDate: Instant?,
    val endDate: Instant?,
    val correctionFactor: Int?,
) {
    companion object : Table() {
        val id = long("id").autoIncrement()
        val name = varchar("name", 128)
        val seriesId = (long("series_id") references Series.id).nullable()
        val startDate = timestamp("start_date").nullable()
        val endDate = timestamp("end_date").nullable()
        val rcId = (long("rc_id") references Person.id).nullable()
        val correctionFactor = integer("correction_factor").nullable()
        override val primaryKey = PrimaryKey(id)
    }
}

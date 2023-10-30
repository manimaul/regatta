package com.mxmariner.regatta.data


import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class Series(
    val id: Long? = null,
    val name: String
)

@Serializable
data class Person(
    val id: Long? = null,
    val first: String,
    val last: String,
)

@Serializable
data class RaceClass(
    val id: Long? = null,
    val name: String,
    val description: String?,
)

@Serializable
data class Race(
    val id: Long? = null,
    val name: String,
    val rcId: Long?,
    val seriesId: Long,
    val startDate: Instant?,
    val endDate: Instant?,
    val correctionFactor: Int?,
)

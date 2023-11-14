package com.mxmariner.regatta.data


import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable


@Serializable
data class Series(
    val id: Long? = null,
    val name: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class Person(
    val id: Long? = null,
    val first: String = "",
    val last: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val clubMember: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class RaceClass(
    val id: Long? = null,
    val name: String,
    val description: String?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class Race(
    val id: Long? = null,
    val name: String,
    val series: Series,
    val startDate: Instant?,
    val endDate: Instant?,
    val rc: Person?,
    val correctionFactor: Int?,
)

@Serializable
data class Boat(
    val id: Long? = null,
    val name: String = "",
    val sailNumber: String = "",
    val boatType: String = "",
    val phrfRating: Int? = null,
    val skipper: Person? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class RaceResult(
    val id: Long? = null,
    val name: String,
    val race: Race,
    val boat: Boat,
    val startTime: Instant?,
    val endTime: Instant?,
    val phrfRating: Int?,
)

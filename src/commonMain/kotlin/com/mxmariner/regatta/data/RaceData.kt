@file:OptIn(ExperimentalSerializationApi::class)

package com.mxmariner.regatta.data


import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable


@Serializable
data class Series(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class Person(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val first: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val last: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val clubMember: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class RaceClassCategory(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val children: List<RaceClass>? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true,
)
@Serializable
data class RaceCategory(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true,
)

@Serializable
data class RaceClass(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val description: String?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val category: Long,
)

@Serializable
data class Race(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val series: Series,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val startDate: Instant?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val endDate: Instant?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val rc: Person?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val correctionFactor: Int?,
)

@Serializable
data class Boat(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val sailNumber: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val boatType: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val phrfRating: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val skipper: Person? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val raceClass: RaceClass? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true
)

@Serializable
data class RaceResult(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val race: Race,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val boat: Boat,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val startTime: Instant?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val endTime: Instant?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val phrfRating: Int?,
)

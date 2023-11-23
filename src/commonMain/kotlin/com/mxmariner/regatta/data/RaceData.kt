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
sealed interface Race {
    val id: Long?
    val name: String
    val startDate: Instant?
    val endDate: Instant?
    val correctionFactor: Int?
    val rcId: Long?
    val seriesId: Long?
}

fun Race.toPost(): RacePost {
    return when (this) {
        is RaceFull -> RacePost(
            id = id,
            name = name,
            startDate = startDate,
            endDate = endDate,
            correctionFactor = correctionFactor,
            rcId = rcId,
            seriesId = seriesId,
        )

        is RacePost -> copy()
    }
}

@Serializable
data class RaceFull(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val name: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val series: Series?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val startDate: Instant?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val endDate: Instant?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val rc: Person?,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val correctionFactor: Int?,
) : Race {
    override val rcId: Long?
        get() = rc?.id
    override val seriesId: Long?
        get() = series?.id
}

@Serializable
data class RacePost(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val name: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val seriesId: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val startDate: Instant? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val endDate: Instant? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val rcId: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val correctionFactor: Int? = null,
) : Race

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
sealed interface RaceResult {
    val id: Long?
    val raceId: Long
    val boatId: Long
    val raceClassId: Long
    val finish: Instant
    val phrfRating: Int?
}

@Serializable
data class RaceResultPost(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val raceId: Long,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val boatId: Long,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val raceClassId: Long,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val finish: Instant,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val phrfRating: Int? = null,
) : RaceResult

@Serializable
data class RaceResultFull(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val race: RaceFull,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val boat: Boat,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val raceClass: RaceClass,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val finish: Instant,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val phrfRating: Int? = null,
) : RaceResult {
    override val raceId: Long
        get() = race.id!!
    override val boatId: Long
        get() = boat.id!!
    override val raceClassId: Long
        get() = raceClass.id!!
}

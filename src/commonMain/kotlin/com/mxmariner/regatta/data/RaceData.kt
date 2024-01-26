@file:OptIn(ExperimentalSerializationApi::class)

package com.mxmariner.regatta.data

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable


@Serializable
data class Series(
    val id: Long? = null,
    val name: String = "",
    @EncodeDefault(ALWAYS) val active: Boolean = true
)

@Serializable
data class Person(
    val id: Long? = null,
    val first: String = "",
    val last: String = "",
    val clubMember: Boolean = false,
    @EncodeDefault(ALWAYS) val active: Boolean = true
) {
    fun fullName(): String {
        return "$first $last"
    }
}

@Serializable
sealed interface RaceClassCat {
    val id: Long?
    val name: String
    val active: Boolean

    fun toCategory() : RaceCategory {
        return when(this) {
            is RaceCategory -> this
            is RaceClassCategory -> RaceCategory(id, name, active)
        }
    }
}
@Serializable
data class RaceClassCategory(
    override val id: Long? = null,
    override val name: String,
    val children: List<RaceClass>? = null,
    @EncodeDefault(ALWAYS) override val active: Boolean = true,
) : RaceClassCat

@Serializable
data class RaceCategory(
    override val id: Long? = null,
    override val name: String,
    @EncodeDefault(ALWAYS) override val active: Boolean = true,
) : RaceClassCat

@Serializable
data class RaceClass(
    val id: Long? = null,
    val name: String,
    val description: String?,
    @EncodeDefault(ALWAYS) val active: Boolean = true,
    val category: Long,
)

@Serializable
sealed interface Race {
    val id: Long?
    val name: String
    val raceTimes: List<RaceTime>
    val rcId: Long?
    val seriesId: Long?

}

fun Race.toPost(): RacePost {
    return when (this) {
        is RaceFull -> RacePost(
            id = id,
            name = name,
            raceTimes = raceTimes,
            rcId = rcId,
            seriesId = seriesId,
        )

        is RacePost -> copy()
    }
}

@Serializable
data class RaceFull(
    override val id: Long? = null,
    override val name: String,
    val series: Series?,
    val rc: Person?,
    override val raceTimes: List<RaceTime> = emptyList(),
) : Race {
    override val rcId: Long?
        get() = rc?.id
    override val seriesId: Long?
        get() = series?.id

    val startTime by lazy {
        raceTimes.minByOrNull { it.startDate }?.startDate
    }

    val endTime by lazy {
        raceTimes.maxByOrNull { it.endDate }?.endDate
    }
}

@Serializable
data class RacePost(
    override val id: Long? = null,
    override val name: String = "",
    override val seriesId: Long? = null,
    override val rcId: Long? = null,
    override val raceTimes: List<RaceTime> = emptyList(),
) : Race

@Serializable
data class RaceTime(
    val raceClassCategory: RaceClassCat,
    val startDate: Instant,
    val endDate: Instant,
    val correctionFactor: Int? = null,
)

@Serializable
data class Boat(
    val id: Long? = null,
    val name: String = "",
    val sailNumber: String = "",
    val boatType: String = "",
    val phrfRating: Int? = null,
    val skipper: Person? = null,
    val raceClass: RaceClass? = null,
    @EncodeDefault(ALWAYS) val active: Boolean = true
)

@Serializable
sealed interface RaceResult {
    val id: Long?
    val raceId: Long
    val boatId: Long
    val raceClassId: Long
    val finish: Instant?
    val phrfRating: Int?
}

@Serializable
data class RaceResultPost(
    override val id: Long? = null,
    override val raceId: Long,
    override val boatId: Long,
    override val raceClassId: Long,
    override val finish: Instant,
    override val phrfRating: Int? = null,
) : RaceResult

@Serializable
data class RaceResultFull(
    override val id: Long? = null,
    val race: RaceFull,
    val boat: Boat,
    val raceClass: RaceClass,
    override val finish: Instant?,
    override val phrfRating: Int? = null,
) : RaceResult {
    override val raceId: Long
        get() = race.id!!
    override val boatId: Long
        get() = boat.id!!
    override val raceClassId: Long
        get() = raceClass.id!!
}

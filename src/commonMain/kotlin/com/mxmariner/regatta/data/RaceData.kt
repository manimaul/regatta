@file:OptIn(ExperimentalSerializationApi::class)

package com.mxmariner.regatta.data

import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.time.Duration


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

    fun toCategory(): RaceCategory {
        return when (this) {
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
    val name: String = "",
    val description: String? = null,
    @EncodeDefault(ALWAYS) val active: Boolean = true,
    val category: Long = -1L,
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
    override val name: String = "",
    val series: Series? = null,
    val rc: Person? = null,
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
    val correctionFactor: Int,
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
    val start: Instant?
    val finish: Instant?
    val phrfRating: Int?
    val hocPosition: Int?
}

@Serializable
data class RaceResultPost(
    override val id: Long? = null,
    override val raceId: Long,
    override val boatId: Long,
    override val raceClassId: Long,
    override val start: Instant?,
    override val finish: Instant?,
    override val phrfRating: Int? = null,
    override val hocPosition: Int? = null,
) : RaceResult

@Serializable
data class RaceResultFull(
    override val id: Long? = null,
    val race: RaceFull = RaceFull(),
    val boat: Boat = Boat(),
    val raceClass: RaceClass = RaceClass(),
    override val start: Instant? = null,
    override val finish: Instant? = null,
    override val phrfRating: Int? = null,
    override val hocPosition: Int? = null,
) : RaceResult {
    override val raceId: Long
        get() = race.id!!
    override val boatId: Long
        get() = boat.id!!
    override val raceClassId: Long
        get() = raceClass.id!!
}

@Serializable
data class RaceReport(
    val race: RaceFull,
    val categories: List<RaceReportCategory>
)

@Serializable
data class RaceReportCategory(
    val category: RaceCategory,
    val correctionFactor: Int,
    val classes: List<RaceReportClass>,
)
@Serializable
data class RaceReportClass(
    val raceClass: RaceClass,
    val cards: List<RaceReportCard>,
)

@Serializable
data class RaceReportCard(
    val resultRecord: RaceResultFull = RaceResultFull(),
    val boatName: String = "",
    val sail: String = "",
    val skipper: String = "",
    val boatType: String = "",
    val phrfRating: Int? = null,
    val startTime: Instant? = null,
    val finishTime: Instant? = null,
    val elapsedTime: Duration? = null,
    val correctionFactor: Double = 1.0,
    val correctedTime: Duration? = null,
    var placeInBracket: Int = 0,
    var placeInClass: Int = 0,
    var placeOverall: Int= 0,
    val hocPosition: Int? = null,
)

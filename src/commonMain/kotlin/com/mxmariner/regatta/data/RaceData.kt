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
sealed interface RaceClassAble {
    val id: Long?
    val name: String
    val active: Boolean

    fun toRaceClass(): RaceClass {
        return when (this) {
            is RaceClass -> this
            is RaceClassFull -> RaceClass(id, name, active)
        }
    }
}

@Serializable
data class RaceClassFull(
    override val id: Long? = null,
    override val name: String,
    val brackets: List<Bracket>? = null,
    @EncodeDefault(ALWAYS) override val active: Boolean = true,
) : RaceClassAble

@Serializable
data class RaceClass(
    override val id: Long? = null,
    override val name: String,
    @EncodeDefault(ALWAYS) override val active: Boolean = true,
) : RaceClassAble

@Serializable
data class Bracket(
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
    val resultCount: Long
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
    override val resultCount: Long = 0,
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
) : Race {
    override val resultCount: Long
        get() = 0
}

@Serializable
data class RaceTime(
    val raceClassCategory: RaceClassAble,
    val startDate: Instant,
    val endDate: Instant,
    val correctionFactor: Int,
    val brackets: List<Bracket>,
)

@Serializable
data class Windseeker(
    val rating: Int? = null,
    val flyingSails: Boolean = false,
)

@Serializable
data class Checkin(
    val boat: Boat,
    val race: RaceFull,
    val bracket: Bracket,
    val checkedIn: Boolean,
)

@Serializable
data class Boat(
    val id: Long? = null,
    val name: String = "",
    val sailNumber: String = "",
    val boatType: String = "",
    val phrfRating: Int? = null,
    val skipper: Person? = null,
    val windseeker: Windseeker? = null,
    @EncodeDefault(ALWAYS) val active: Boolean = true
)

@Serializable
sealed interface RaceResult {
    val id: Long?
    val raceId: Long
    val boatId: Long
    val bracketId: Long
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
    override val bracketId: Long,
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
    val bracket: Bracket = Bracket(),
    override val start: Instant? = null,
    override val finish: Instant? = null,
    override val phrfRating: Int? = null,
    override val hocPosition: Int? = null,
) : RaceResult {
    override val raceId: Long
        get() = race.id!!
    override val boatId: Long
        get() = boat.id!!
    override val bracketId: Long
        get() = bracket.id!!
}

@Serializable
data class RaceReport(
    val race: RaceFull,
    val categories: List<RaceReportCategory>
)

@Serializable
data class RaceReportCategory(
    val category: RaceClass,
    val correctionFactor: Int,
    val classes: List<RaceReportClass>,
)
@Serializable
data class RaceReportClass(
    val bracket: Bracket,
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

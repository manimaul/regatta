@file:OptIn(ExperimentalSerializationApi::class)

package com.mxmariner.regatta.data

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.ratingDefault
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.time.Duration


@Serializable
data class Series(
    val id: Long = 0,
    val name: String = "",
    val sort: Int = 0,
    val active: Boolean = true
)

@Serializable
data class Person(
    val id: Long = 0,
    val first: String = "",
    val last: String = "",
    val clubMember: Boolean = false,
    val active: Boolean = true
) {
    fun fullName(): String {
        return "$first $last"
    }
}

@Serializable
data class RaceClassBracketTimes(
    val raceClass: RaceClass = RaceClass(),
    val bracketTimes: List<BracketTime> = emptyList(),
)

@Serializable
data class RaceClassBrackets(
    val raceClass: RaceClass = RaceClass(),
    val brackets: List<Bracket> = emptyList(),
)

@Serializable
data class RaceClass(
    val id: Long = 0,
    val name: String = "",
    val sort: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class Bracket(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val active: Boolean = true,
    val minRating: Float = ratingDefault,
    val maxRating: Float = ratingDefault,
    val classId: Long = 0,
)

@Serializable
data class BracketTime(
    val bracket: Bracket = Bracket(),
    val time: RaceTime? = null,
)


@Serializable
data class RaceSchedule(
    val race: Race = Race(),
    val resultCount: Long = 0,
    val series: Series? = null,
    val rc: Person? = null,
    val schedule: List<RaceClassBracketTimes> = emptyList(),
) {

    val startTime by lazy {
        schedule.map { it.bracketTimes.mapNotNull { it.time?.startDate } }.flatten().minByOrNull { it }
    }

    val endTime by lazy {
        schedule.map { it.bracketTimes.mapNotNull { it.time?.endDate } }.flatten().maxByOrNull { it }
    }
}

@Serializable
data class Race(
    val id: Long = 0,
    val name: String = "",
    val seriesId: Long? = null,
    val rcId: Long? = null,
    val correctionFactor: Int = correctionFactorDefault,
)

@Serializable
data class RaceTime(
    val startDate: Instant,
    val endDate: Instant,
    val bracketId: Long,
    val raceId: Long,
)

//@Serializable
//data class RaceTimeFull(
//    val startDate: Instant,
//    val endDate: Instant,
//    val bracket: Bracket,
//)

@Serializable
data class Windseeker(
    val rating: Int? = null,
    val flyingSails: Boolean = false,
)

@Serializable
data class Checkin(
    val boatId: Long = 0,
    val raceId: Long = 0,
    @EncodeDefault(ALWAYS)
    val checkedIn: Boolean = false,
)

@Serializable
data class Boat(
    val id: Long = 0,
    val name: String = "",
    val sailNumber: String = "",
    val boatType: String = "",
    val phrfRating: Int? = null,
    val skipperId: Long? = null,
    val windseeker: Windseeker? = null,
    val active: Boolean = true
)

@Serializable
data class BoatSkipper(
    val skipper: Person? = null,
    val boat: Boat? = null,
)

@Serializable
data class RaceResult(
    val id: Long = 0,
    val raceId: Long = 0,
    val boatId: Long = 0,
    val bracketId: Long = 0,
    val start: Instant? = null,
    val finish: Instant? = null,
    val phrfRating: Int? = null,
    val hocPosition: Int? = null,
)

@Serializable
data class RaceResultBoatBracket(
    val result: RaceResult = RaceResult(),
    val raceSchedule: RaceSchedule = RaceSchedule(),
    val boatSkipper: BoatSkipper = BoatSkipper(),
    val bracket: Bracket = Bracket(),
)

@Serializable
data class RaceReport(
    val raceSchedule: RaceSchedule = RaceSchedule(),
    val categories: List<RaceReportCategory> = emptyList()
)

@Serializable
data class RaceReportCategory(
    val category: RaceClass = RaceClass(),
    val correctionFactor: Int = correctionFactorDefault,
    val classes: List<RaceReportClass> = emptyList(),
)

@Serializable
data class RaceReportClass(
    val bracket: Bracket = Bracket(),
    val cards: List<RaceReportCard> = emptyList(),
)

@Serializable
data class RaceReportCard(
    val resultRecord: RaceResultBoatBracket = RaceResultBoatBracket(),
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
    var placeOverall: Int = 0,
    val hocPosition: Int? = null,
)

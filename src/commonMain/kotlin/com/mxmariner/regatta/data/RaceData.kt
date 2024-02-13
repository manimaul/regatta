package com.mxmariner.regatta.data

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.ratingDefault
import kotlinx.datetime.Instant
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
data class RaceClassBrackets(
    val raceClass: RaceClass = RaceClass(),
    val brackets: List<Bracket> = emptyList(),
)

@Serializable
data class RaceClass(
    val id: Long = 0,
    val name: String = "",
    val sort: Int = 0,
    val isPHRF: Boolean = false,
    val wsFlying: Boolean = false,
    val active: Boolean = true,
) {
    fun ratingLabel(): String {
        if (isPHRF) {
            return "PHRF"
        } else if (wsFlying) {
            return "Cruising - Flying Sails"
        } else {
            return "Cruising - Non Flying Sails"
        }
    }
}

@Serializable
data class Bracket(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val active: Boolean = true,
    val minRating: Float = ratingDefault,
    val maxRating: Float = ratingDefault,
    val classId: Long = 0,
) {
    fun label(): String {
        if (minRating <= -1000 && maxRating >= 1000) {
            return name
        } else if (minRating <= -1000) {
            return "$name up to $maxRating"
        } else if (maxRating >= 1000) {
            return "$name ${minRating}+"
        }
        return "$name $minRating to $maxRating"
    }
}

@Serializable
data class ClassSchedule(
    val raceClass: RaceClass = RaceClass(),
    val brackets: List<Bracket> = emptyList(),
    val startDate: Instant = Instant.DISTANT_PAST,
    val endDate: Instant = Instant.DISTANT_FUTURE,
) {
    fun raceStart() =
        startDate.takeIf { it != Instant.DISTANT_PAST }

    fun raceEnd() =
        endDate.takeIf { it != Instant.DISTANT_FUTURE }

}

@Serializable
data class RaceSchedule(
    val race: Race = Race(),
    val resultCount: Long = 0,
    val series: Series? = null,
    val rc: Person? = null,
    val schedule: List<ClassSchedule> = emptyList(),
) {

    val startTime by lazy {
        schedule.map { it.startDate }.minByOrNull { it }
    }

    val endTime by lazy {
        schedule.map { it.endDate }.maxByOrNull { it }
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
    val classId: Long,
    val raceId: Long,
)

@Serializable
data class Windseeker(
    val rating: Int = ratingDefault.toInt(),
    val flyingSails: Boolean = false,
)

@Serializable
data class Checkin(
    val boatId: Long = 0,
    val raceId: Long = 0,
    val checkedIn: Boolean = false,
)

enum class RatingType {
    PHRF,
    Windseeker
}

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
) {
    fun ratingType(): RatingType {
        return windseeker?.let { RatingType.Windseeker } ?: RatingType.PHRF
    }
}

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
    val start: Instant? = null,
    val finish: Instant? = null,
    val phrfRating: Int? = null,
    val hocPosition: Int? = null,
    val windseeker: Windseeker? = null,
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
    val classReports: List<ClassReportCards> = emptyList()
)

@Serializable
data class ClassReportCards(
    val raceClass: RaceClass = RaceClass(),
    val correctionFactor: Int = correctionFactorDefault,
    val bracketReport: List<BracketReportCards> = emptyList(),
)

@Serializable
data class BracketReportCards(
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
    val windseeker: Windseeker? = null,
    val startTime: Instant? = null,
    val finishTime: Instant? = null,
    val elapsedTime: Duration? = null,
    val correctionFactor: Double = 1.0,
    val correctedTime: Duration? = null,
    var placeInBracket: Int = 0,
    var placeInClass: Int = 0,
    var placeOverall: Int = 0,
    val hocPosition: Int? = null,
) {
    fun ratingType(): RatingType {
        return windseeker?.let { RatingType.Windseeker } ?: RatingType.PHRF
    }
}

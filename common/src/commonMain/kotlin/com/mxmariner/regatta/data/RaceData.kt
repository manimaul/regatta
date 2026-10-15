package com.mxmariner.regatta.data

import com.mxmariner.regatta.correctionFactorDefault
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration


@Serializable
data class Series(
    val id: Long = 0,
    val name: String = "",
    val sort: Int = 0,
    val raceCount: Long = 0,
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
    val ratingType: RatingType = RatingType.CruisingNonFlyingSails,
    val numberOfRaces: Long = 0,
    val active: Boolean = true,
) {
    fun label() : String {
        return when (ratingType) {
            RatingType.ORC,
            RatingType.ORC_PHRF,
            RatingType.PHRF -> "${ratingType.label} $name"
            RatingType.CruisingFlyingSails,
            RatingType.CruisingNonFlyingSails -> name
        }
    }
}

@Serializable
data class Bracket(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val active: Boolean = true,
    val numberOfRaces: Long = 0L,
    val minRating: Float = -1000f, //slowest PHRF - fastest ORC
    val maxRating: Float = 1000f,  //fastest PHRF - slowest ORC
    val classId: Long = 0,
) {
    fun label(): String {
        if (minRating <= -1000 && maxRating >= 1000) {
            return name
        } else if (minRating <= -1000) {
            return "$name $maxRating -"
        } else if (maxRating >= 1000) {
            return "$name $minRating +"
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
        schedule.map { it.startDate }.minBy { it }
    }

    val endTime by lazy {
        schedule.map { it.endDate }.maxBy { it }
    }
}

@Serializable
data class Race(
    val id: Long = 0,
    val name: String = "",
    val seriesId: Long? = null,
    val rcId: Long? = null,
    val reportImage: String? = null,
    val phrfBFactor: Int = correctionFactorDefault,
    val orcScoringOption: OrcScoringOption = OrcScoringOption.FiveBandWindwardLeeward,
    val orc3Band: Orc3Band = Orc3Band.fromPhrfBFactor(phrfBFactor),
    val orc5Band: Orc5Band = Orc5Band.fromPhrfBFactor(phrfBFactor),
) {

}

@Serializable
data class RaceTime(
    val startDate: Instant,
    val endDate: Instant,
    val classId: Long,
    val raceId: Long,
)

enum class RatingType(val label: String) {
    ORC("ORC"),
    ORC_PHRF("ORC, PHRF"),
    PHRF("PHRF"),
    CruisingFlyingSails("Cruising Flying Sails"),
    CruisingNonFlyingSails("Cruising Non-Flying Sails");

    val isCruising: Boolean by lazy {
        this == CruisingFlyingSails || this == CruisingNonFlyingSails
    }

    val isPHRF: Boolean by lazy {
        this == PHRF || this == ORC_PHRF
    }

    val isORC: Boolean by lazy {
        this == ORC || this == ORC_PHRF
    }

    val isORCorPHRF: Boolean by lazy {
        this == ORC || this == ORC_PHRF || this == PHRF
    }

    fun isCompatible(other: RatingType) : Boolean {
        return (isCruising && other.isCruising) ||
                (isPHRF && other.isPHRF) ||
                (isORC && other.isORC)
    }

    fun ratedLabel(phrfRating: Int? = null, orcInfo: String? = null) : String {
        return when (this) {
            ORC -> "$label${orcInfo?.let { "($it)" } ?: ""}"
            ORC_PHRF -> "PHRF ($phrfRating), ORC${orcInfo?.let { " ($it)" } ?: ""}"
            PHRF -> "$label ($phrfRating)"
            CruisingFlyingSails,
            CruisingNonFlyingSails -> label
        }
    }
}

@Serializable
data class Boat(
    val id: Long = 0,
    val name: String = "",
    val sailNumber: String = "",
    val boatType: String = "",
    val phrfRating: Int? = null,
    val orcCerts: List<OrcCertificate> = emptyList(),
    val skipperId: Long? = null,
    val ratingType: RatingType = RatingType.CruisingNonFlyingSails,
    val numberOfRaces: Long = 0,
    val active: Boolean = true
) {

    fun orcInfo() : String? {
        return orcCerts.takeIf { it.isNotEmpty() }?.let {
            "~${orcCerts.minOf { it.virtualPhrf() }}"
        }
    }

}

@Serializable
data class BoatSkipper(
    val skipper: Person? = null,
    val boat: Boat? = null,
) {
    fun label(): String {
        if (boat != null && skipper != null) {
            return "${boat.name} - ${skipper.fullName()}"
        } else if (boat != null) {
            return boat.name
        } else if (skipper != null) {
            return skipper.fullName()
        }
        return ""
    }

    fun dropLabel(): String {
        val sail = boat?.sailNumber?.let { " ($it)" } ?: ""
        return "${label()}$sail ${boat?.ratingType?.label ?: ""}"
    }

    fun shortLabel(): String {
        return boat?.name ?: ""
    }

}

@Serializable
data class RaceScheduleResults (
    val raceSchedule: RaceSchedule = RaceSchedule(),
    val results: List<RaceResultFull> = emptyList(),
)

@Serializable
data class RaceResultFull(
    val id: Long = 0,
    val raceId: Long = 0,
    val boat: BoatSkipper = BoatSkipper(),
    val finish: Instant? = null,
    val phrfRating: Int? = null,
    val orcCertificate: OrcCertificate? = null,
    val hocPosition: Int? = null,
    val bracketId: Long = 0,
    val raceClassId: Long = 0,
    val penalty: Int? = null,
    val ratingType: RatingType = RatingType.CruisingNonFlyingSails,
    val finishCode: FinishCode = finish?.let { FinishCode.TIME } ?: FinishCode.RET,
)  {
}

@Serializable
data class RaceResult(
    val id: Long = 0,
    val raceId: Long = 0,
    val boatId: Long = 0,
    val finish: Instant? = null,
    val phrfRating: Int? = null,
    val orcRef: String? = null,
    val hocPosition: Int? = null,
    val bracketId: Long = 0,
    val raceClassId: Long = 0,
    val penalty: Int? = null,
    val ratingType: RatingType = RatingType.CruisingNonFlyingSails,
    val finishCode: FinishCode = finish?.let { FinishCode.TIME } ?: FinishCode.RET,
)

@Serializable
data class RaceResultBoatBracket(
    val result: RaceResult = RaceResult(),
    val boatSkipper: BoatSkipper = BoatSkipper(),
    val bracket: Bracket = Bracket(),
)

@Serializable
data class StandingsSeries(
    val year: Int = 0,
    val series: Series = Series(),
    val standings: List<StandingsClass> = emptyList(),
    val races: List<Race> = emptyList(),
)

@Serializable
data class StandingsClass(
    val raceClass: RaceClass = RaceClass(),
    val standings: List<StandingsBracket> = emptyList(),
)

@Serializable
data class StandingsBracket(
    val bracket: Bracket = Bracket(),
    val standings: List<StandingsBoatSkipper> = emptyList(),
)

@Serializable
data class StandingsBoatSkipper(
    val boatSkipper: BoatSkipper = BoatSkipper(),
    val raceStandings: List<StandingsRace> = emptyList(),
    /** Sum of the counted bracket scores; fractional when an RC volunteer average is in it (D1). */
    val totalScoreBracket: Double = 0.0,
    val totalScoreClass: Double = 0.0,
    /** 1-based series place in the bracket; tied boats share it and the next place skips (D5). */
    var placeInBracket: Int = 0,
    var placeInClass: Int = 0,
    /**
     * D9: which ratings this row is scored under. In an ORC class, `ORC_PHRF` when the boat has
     * both PHRF- and ORC-rated results in the series, else the one it has; elsewhere the rating
     * of its most recent result. The PHRF fields are 0 for an `ORC` row and the ORC fields are 0
     * for a `PHRF` row.
     */
    val ratingType: RatingType = RatingType.CruisingNonFlyingSails,
    /** D9: the ORC series scores, from the ORC race places, by the same rules as the PHRF ones. */
    val totalScoreBracketOrc: Double = 0.0,
    val totalScoreClassOrc: Double = 0.0,
    var placeInBracketOrc: Int = 0,
    var placeInClassOrc: Int = 0,
)

/** D2: whether a race in a series standings column was sailed at all. */
@Serializable
enum class RaceStatus {
    Sailed,
    /** No results yet and the last class end time is still in the future. */
    NotYetSailed,
    /** No results and the race is over. */
    Cancelled,
}

/**
 * One boat's cell in a series standings row. A race is scored per group (D2): [placeInBracket]
 * counts only when [bracketSailed], [placeInClass] only when [classSailed]; the throw-outs are
 * chosen per group too. An RC volunteer's cell holds the average of the boat's other races (D1),
 * which is why the scores are fractional.
 */
@Serializable
data class StandingsRace(
    val nonStarter: Boolean = false,
    val hocPosition: Int? = null,
    val finish: Boolean,
    val placeInBracket: Double = 0.0,
    val placeInClass: Double = 0.0,
    var throwOut: Boolean = false,
    var throwOutClass: Boolean = false,
    val finishCode: FinishCode?,
    val bracketSailed: Boolean = true,
    val classSailed: Boolean = true,
    val status: RaceStatus = RaceStatus.Sailed,
    /** D9: the ORC groups of the race, scored like the PHRF ones among the ORC-rated entries; 0 / false outside an ORC class. */
    val placeInBracketOrc: Double = 0.0,
    val placeInClassOrc: Double = 0.0,
    var throwOutOrc: Boolean = false,
    var throwOutClassOrc: Boolean = false,
    val bracketSailedOrc: Boolean = false,
    val classSailedOrc: Boolean = false,
    /** D9: the boat had no ORC entry in a race sailed for its ORC group (no record, or raced PHRF-only that day), so it scores DNS there. */
    val nonStarterOrc: Boolean = false,
)

@Serializable
data class RaceReport(
    val raceSchedule: RaceSchedule = RaceSchedule(),
    val classReports: List<ClassReportCards> = emptyList(),
) {
    fun classStart(classId: Long): Instant? {
        return raceSchedule.schedule.firstOrNull() { it.raceClass.id == classId }?.startDate
    }
}

@Serializable
data class ClassReportCards(
    val raceClass: RaceClass = RaceClass(),
    val phrfBFactor: Int = correctionFactorDefault,
    val orcScoringOption: OrcScoringOption = OrcScoringOption.FiveBandWindwardLeeward,
    val orc3Band: Orc3Band = Orc3Band.fromPhrfBFactor(phrfBFactor),
    val orc5Band: Orc5Band = Orc5Band.fromPhrfBFactor(phrfBFactor),
    val bracketReport: List<BracketReportCards> = emptyList(),
    val hasOrcResults: Boolean = false,
) {

    fun orcBandLabel() : String? {
        return orcScoringOption.orcBandLabel(orc5Band, orc3Band)
    }

}

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
    val startTime: Instant? = null,
    val finishTime: Instant? = null,
    val elapsedTime: Duration? = null,
    val phrfTcf: Double = 1.0,
    val orcTcf: Double = 1.0,
    val correctedPhrfTime: Duration? = null,
    val correctedOrcTime: Duration? = null,
    var placeInBracket: Int = 0,
    var placeInBracketOrc: Int = 0,
    var placeInClass: Int = 0,
    var placeInClassOrc: Int = 0,
    val hocPosition: Int? = null,
    val penalty: Int? = null,
) {

    fun ratingType(): RatingType {
        return resultRecord.result.ratingType
    }

}

package com.mxmariner.regatta.results

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.toInstant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/*
 * Test fixture DSL for race and series scoring (test_audit_and_plan.md §2). Builds the same
 * RaceSchedule / RaceResultBoatBracket records the database would, then scores them through the
 * pure seams RaceResultReporter.card, report and standings. No database.
 *
 *   val r = race(date = "2026-06-17", start = "18:00") {
 *       phrfClass("Fast") { bracket("I") {
 *           boat("A", rating = 48) finishes "20:51:26"
 *           boat("B", rating = 72) code DNF
 *       } }
 *   }
 *   r.report().assertBracketScores("A" to 1, "B" to 2)
 *
 * Times are Pacific local times. A boat named in a race with no outcome is RET, which is also the
 * RaceResult default. A boat not named in a race has no record, which standings score as DNS.
 */

internal val pacificZone = TimeZone.of("America/Los_Angeles")

/**
 * "2026-06-17" and "18:00[:ss[.SSS]]" in America/Los_Angeles. A time with its own date,
 * "2026-06-18 10:00:00", overrides [date], for finishes on a later day.
 */
internal fun pacific(date: String, time: String): Instant {
    val trimmed = time.trim()
    val (day, clock) = if (" " in trimmed) {
        trimmed.split(" ", limit = 2).let { it[0] to it[1] }
    } else {
        date to trimmed
    }
    val parts = clock.split(":")
    val seconds = parts.getOrElse(2) { "0" }
    val nanos = seconds.substringAfter(".", "").padEnd(9, '0').take(9).toInt()
    val localTime = LocalTime(parts[0].toInt(), parts.getOrElse(1) { "0" }.toInt(), seconds.substringBefore(".").toInt(), nanos)
    return LocalDateTime(LocalDate.parse(day), localTime).toInstant(pacificZone)
}

/** Ids and shared entities, so the same boat, class and bracket keep their ids across the races of a series. */
class FixtureRegistry {
    private var nextId = 1L
    private val boats = mutableMapOf<String, Boat>()
    private val people = mutableMapOf<String, Person>()
    private val classes = mutableMapOf<String, RaceClass>()
    private val brackets = mutableMapOf<Pair<Long, String>, Bracket>()

    fun id(): Long = nextId++

    fun raceClass(name: String, ratingType: RatingType): RaceClass = classes.getOrPut(name) {
        RaceClass(id = id(), name = name, sort = classes.size, ratingType = ratingType)
    }

    fun bracket(raceClass: RaceClass, name: String): Bracket = brackets.getOrPut(raceClass.id to name) {
        Bracket(id = id(), name = name, classId = raceClass.id)
    }

    fun person(name: String): Person = people.getOrPut(name) {
        val parts = name.split(" ", limit = 2)
        Person(id = id(), first = parts[0], last = parts.getOrElse(1) { "" })
    }

    fun boat(name: String, ratingType: RatingType, rating: Int?, skipper: String?): Boat {
        val person = person(skipper ?: "$name Skipper")
        return boats.getOrPut(name) {
            Boat(id = id(), name = name, sailNumber = "", phrfRating = rating, skipperId = person.id, ratingType = ratingType)
        }
    }
}

/** One boat's entry in one race. Set its outcome with the infix helpers below. */
class BoatEntry internal constructor(
    val name: String,
    val raceClass: RaceClass,
    val bracket: Bracket,
    val boat: Boat,
    val skipper: Person,
    var ratingType: RatingType,
    var rating: Int?,
) {
    internal var finish: String? = null
    internal var hocPosition: Int? = null
    internal var finishCode: FinishCode? = null
    internal var penaltyPoints: Int? = null
    internal var orcRef: String? = null
    internal var legacy: Boolean = false
    internal var orphan: Boolean = false

    internal fun result(registry: FixtureRegistry, raceId: Long, date: String): RaceResultBoatBracket {
        val finishInstant = finish?.let { pacific(date, it) }
        val base = RaceResult(
            id = registry.id(),
            raceId = raceId,
            boatId = boat.id,
            finish = finishInstant,
            phrfRating = rating,
            orcRef = orcRef,
            hocPosition = hocPosition,
            bracketId = bracket.id,
            raceClassId = raceClass.id,
            penalty = penaltyPoints,
            ratingType = ratingType,
        )
        val result = if (legacy) {
            base // finishCode left at the data class default, as the row mapper does for a NULL finish_code
        } else {
            base.copy(
                finishCode = when {
                    finishInstant != null -> FinishCode.TIME
                    hocPosition != null -> FinishCode.HOC
                    else -> finishCode ?: FinishCode.RET
                }
            )
        }
        return RaceResultBoatBracket(
            result = if (orphan) result.copy(bracketId = 0, raceClassId = 0) else result,
            boatSkipper = BoatSkipper(skipper = skipper, boat = boat),
            bracket = if (orphan) Bracket() else bracket,
        )
    }
}

/** Timed finish at a Pacific local time on the race date, e.g. "20:51:26", "20:51:26.250", or "2026-06-18 10:00:00" for a later day. */
infix fun BoatEntry.finishes(time: String): BoatEntry = apply { finish = time; hocPosition = null }

/** Horned on course at the given position. */
infix fun BoatEntry.hoc(position: Int): BoatEntry = apply { hocPosition = position; finish = null }

/** A finish code other than TIME or HOC: DNF, RET, NSC, DNS_RC. */
infix fun BoatEntry.code(code: FinishCode): BoatEntry = apply { finishCode = code }

/** Scoring penalty in places (GSI 9.2 / 14.2). */
infix fun BoatEntry.penalty(places: Int): BoatEntry = apply { penaltyPoints = places }

/** ORC certificate reference, looked up in the map passed to [RaceFixture.report]. */
infix fun BoatEntry.orc(ref: String): BoatEntry = apply { orcRef = ref }

/** A legacy row with no finish code stored: the result keeps the RaceResult default (TIME with a finish, else RET). */
fun BoatEntry.legacy(): BoatEntry = apply { legacy = true }

/** A result whose bracket no longer matches the race (bracket id 0), which the report lists under "Incorrectly classed". */
fun BoatEntry.orphan(): BoatEntry = apply { orphan = true }

class BracketBuilder internal constructor(
    private val registry: FixtureRegistry,
    private val raceClass: RaceClass,
    private val bracket: Bracket,
    private val entries: MutableList<BoatEntry>,
) {
    /**
     * Enters a boat. [rating] is the PHRF rating used for this result. [ratingType] defaults to
     * the class's rating type. [skipper] defaults to a person named after the boat.
     */
    fun boat(
        name: String,
        rating: Int? = null,
        skipper: String? = null,
        ratingType: RatingType = raceClass.ratingType,
    ): BoatEntry {
        val boat = registry.boat(name, ratingType, rating, skipper)
        val person = registry.person(skipper ?: "$name Skipper")
        return BoatEntry(name, raceClass, bracket, boat, person, ratingType, rating).also { entries.add(it) }
    }
}

class ClassBuilder internal constructor(
    private val registry: FixtureRegistry,
    private val raceClass: RaceClass,
    private val entries: MutableList<BoatEntry>,
) {
    internal val brackets = mutableListOf<Bracket>()

    fun bracket(name: String, block: BracketBuilder.() -> Unit = {}) {
        val bracket = registry.bracket(raceClass, name)
        brackets.add(bracket)
        BracketBuilder(registry, raceClass, bracket, entries).block()
    }
}

class RaceBuilder internal constructor(
    private val registry: FixtureRegistry,
    private val name: String,
    private val date: String,
    private val start: String,
    private val bFactor: Int,
    private val orcScoringOption: OrcScoringOption,
    private val seriesId: Long?,
) {
    private val raceId = registry.id()
    private val schedules = mutableListOf<ClassSchedule>()
    internal val entries = mutableListOf<BoatEntry>()

    fun phrfClass(name: String, start: String? = null, block: ClassBuilder.() -> Unit) =
        raceClass(name, RatingType.PHRF, start, block)

    fun orcPhrfClass(name: String, start: String? = null, block: ClassBuilder.() -> Unit) =
        raceClass(name, RatingType.ORC_PHRF, start, block)

    fun orcClass(name: String, start: String? = null, block: ClassBuilder.() -> Unit) =
        raceClass(name, RatingType.ORC, start, block)

    fun cruisingClass(name: String, flying: Boolean = false, start: String? = null, block: ClassBuilder.() -> Unit) =
        raceClass(name, if (flying) RatingType.CruisingFlyingSails else RatingType.CruisingNonFlyingSails, start, block)

    /** A class with its own start time ("HH:mm[:ss]", Pacific), defaulting to the race start. */
    fun raceClass(name: String, ratingType: RatingType, start: String? = null, block: ClassBuilder.() -> Unit) {
        val raceClass = registry.raceClass(name, ratingType)
        val classBuilder = ClassBuilder(registry, raceClass, entries).apply(block)
        val startInstant = pacific(date, start ?: this.start)
        schedules.add(
            ClassSchedule(
                raceClass = raceClass,
                brackets = classBuilder.brackets,
                startDate = startInstant,
                endDate = startInstant + 6.hours,
            )
        )
    }

    internal fun build(): RaceFixture {
        require(schedules.isNotEmpty()) { "race '$name' needs at least one class" }
        val race = Race(id = raceId, name = name, seriesId = seriesId, phrfBFactor = bFactor, orcScoringOption = orcScoringOption)
        val schedule = RaceSchedule(race = race, resultCount = entries.size.toLong(), schedule = schedules)
        val results = entries.map { it.result(registry, raceId, date) }
        return RaceFixture(date, schedule, results)
    }
}

/** A built race: its schedule and result records, scored on demand. */
class RaceFixture internal constructor(
    val date: String,
    val schedule: RaceSchedule,
    val results: List<RaceResultBoatBracket>,
) {
    /** Scores the race through the pure seams. [orcCerts] resolves `orc` references. */
    fun report(orcCerts: Map<String, OrcCertificate> = emptyMap()): RaceReport {
        val starts = schedule.schedule.associate { it.raceClass.id to it.startDate }
        val cards = results.map { record ->
            val cert = record.result.orcRef?.let { ref ->
                requireNotNull(orcCerts[ref]) { "no certificate for orc ref $ref" }
            }
            RaceResultReporter.card(record, starts[record.bracket.classId], schedule.race, cert)
        }
        return RaceResultReporter.report(schedule, cards)
    }
}

/** A standalone race. */
fun race(
    name: String = "Race",
    date: String = "2026-06-17",
    start: String = "18:00",
    bFactor: Int = correctionFactorDefault,
    orcScoringOption: OrcScoringOption = OrcScoringOption.FiveBandWindwardLeeward,
    registry: FixtureRegistry = FixtureRegistry(),
    block: RaceBuilder.() -> Unit,
): RaceFixture = RaceBuilder(registry, name, date, start, bFactor, orcScoringOption, seriesId = null).apply(block).build()

class SeriesBuilder internal constructor(
    private val registry: FixtureRegistry,
    private val seriesId: Long,
    private val firstDate: String,
) {
    private val builders = mutableListOf<RaceBuilder>()
    private val volunteers = mutableListOf<Pair<String, Int>>()

    /**
     * Adds a race. [date] defaults to a week after the previous race, starting at the series'
     * first date.
     */
    fun race(
        name: String = "Race ${builders.size + 1}",
        date: String? = null,
        start: String = "18:00",
        bFactor: Int = correctionFactorDefault,
        orcScoringOption: OrcScoringOption = OrcScoringOption.FiveBandWindwardLeeward,
        block: RaceBuilder.() -> Unit,
    ) {
        val raceDate = date ?: LocalDate.parse(firstDate).plus(DatePeriod(days = 7 * builders.size)).toString()
        builders.add(RaceBuilder(registry, name, raceDate, start, bFactor, orcScoringOption, seriesId).apply(block))
    }

    /**
     * Enters [boat] as an RC volunteer (DNS_RC) in race number [race] (1-based), in the class and
     * bracket the boat sailed in its nearest other race of the series. The boat must not already
     * be entered in that race.
     */
    fun rcVolunteer(boat: String, race: Int) {
        volunteers.add(boat to race)
    }

    internal fun build(series: Series, year: Int): SeriesFixture {
        volunteers.forEach { (boatName, raceNumber) ->
            val target = requireNotNull(builders.getOrNull(raceNumber - 1)) { "series has no race $raceNumber" }
            require(target.entries.none { it.name == boatName }) { "$boatName is already entered in race $raceNumber" }
            val example = requireNotNull(
                builders.withIndex()
                    .sortedBy { (i, _) -> kotlin.math.abs(i - (raceNumber - 1)) }
                    .firstNotNullOfOrNull { (_, b) -> b.entries.firstOrNull { it.name == boatName } }
            ) { "$boatName has no other entry in the series to take its class and bracket from" }
            target.entries.add(
                BoatEntry(
                    example.name, example.raceClass, example.bracket, example.boat, example.skipper,
                    example.ratingType, example.rating
                ).also { it.finishCode = FinishCode.DNS_RC }
            )
        }
        return SeriesFixture(series, year, builders.map { it.build() })
    }
}

/** A built series: its races, scored on demand. */
class SeriesFixture internal constructor(
    val series: Series,
    val year: Int,
    val races: List<RaceFixture>,
) {
    fun reports(orcCerts: Map<String, OrcCertificate> = emptyMap()): List<RaceReport> = races.map { it.report(orcCerts) }

    /** Standings through the pure seam. [now] defaults to a day after the last race. */
    fun standings(
        now: Instant = races.maxOf { it.schedule.endTime } + 24.hours,
        orcCerts: Map<String, OrcCertificate> = emptyMap(),
    ): StandingsSeries = RaceResultReporter.standings(series, year, reports(orcCerts), now)
}

/** A series of races that share boats, classes and brackets. */
fun series(
    name: String = "Series",
    year: Int = 2026,
    firstDate: String = "$year-06-17",
    registry: FixtureRegistry = FixtureRegistry(),
    block: SeriesBuilder.() -> Unit,
): SeriesFixture {
    val series = Series(id = registry.id(), name = name)
    return SeriesBuilder(registry, series.id, firstDate).apply(block).build(series, year)
}

// ---- assertions: scores are compared as Double so the same tests compile before and after D1

private fun RaceReport.cards(): List<RaceReportCard> = classReports.flatMap { it.bracketReport }.flatMap { it.cards }

/** Bracket scores of the race by boat name. */
fun RaceReport.bracketScores(): Map<String, Double> = cards().associate { it.boatName to it.placeInBracket.toDouble() }

/** Class scores of the race by boat name. */
fun RaceReport.classScores(): Map<String, Double> = cards().associate { it.boatName to it.placeInClass.toDouble() }

/** ORC bracket scores of the race by boat name (0 when the boat has no ORC place). */
fun RaceReport.bracketScoresOrc(): Map<String, Double> = cards().associate { it.boatName to it.placeInBracketOrc.toDouble() }

/** ORC class scores of the race by boat name (0 when the boat has no ORC place). */
fun RaceReport.classScoresOrc(): Map<String, Double> = cards().associate { it.boatName to it.placeInClassOrc.toDouble() }

fun RaceReport.assertBracketScores(vararg expected: Pair<String, Number>) = assertScores(bracketScores(), *expected)
fun RaceReport.assertClassScores(vararg expected: Pair<String, Number>) = assertScores(classScores(), *expected)

private fun StandingsSeries.rows(): List<StandingsBoatSkipper> =
    standings.flatMap { it.standings }.flatMap { it.standings }

/** The standings row of one boat (its first row, if it appears in more than one bracket). */
fun StandingsSeries.row(boat: String): StandingsBoatSkipper =
    requireNotNull(rows().firstOrNull { it.boatSkipper.boat?.name == boat }) { "no standings row for $boat" }

/** Bracket totals by boat name. A boat that appears in more than one bracket keeps its first row. */
fun StandingsSeries.bracketTotals(): Map<String, Double> =
    rows().groupBy { it.boatSkipper.boat?.name ?: "" }.mapValues { (_, r) -> r.first().totalScoreBracket.toDouble() }

/** Class totals by boat name. */
fun StandingsSeries.classTotals(): Map<String, Double> =
    rows().groupBy { it.boatSkipper.boat?.name ?: "" }.mapValues { (_, r) -> r.first().totalScoreClass.toDouble() }

/** Series places in bracket by boat name. */
fun StandingsSeries.bracketPlaces(): Map<String, Int> =
    rows().groupBy { it.boatSkipper.boat?.name ?: "" }.mapValues { (_, r) -> r.first().placeInBracket }

/** Series places in class by boat name. */
fun StandingsSeries.classPlaces(): Map<String, Int> =
    rows().groupBy { it.boatSkipper.boat?.name ?: "" }.mapValues { (_, r) -> r.first().placeInClass }

/** The per-race bracket scores of one boat, in race order. */
fun StandingsSeries.bracketRaceScores(boat: String): List<Double> = row(boat).raceStandings.map { it.placeInBracket.toDouble() }

/** The per-race class scores of one boat, in race order. */
fun StandingsSeries.classRaceScores(boat: String): List<Double> = row(boat).raceStandings.map { it.placeInClass.toDouble() }

fun StandingsSeries.assertBracketTotals(vararg expected: Pair<String, Number>) = assertScores(bracketTotals(), *expected)
fun StandingsSeries.assertClassTotals(vararg expected: Pair<String, Number>) = assertScores(classTotals(), *expected)

/** Asserts each expected boat's score. Boats not listed are not checked. */
fun assertScores(actual: Map<String, Double>, vararg expected: Pair<String, Number>) {
    expected.forEach { (boat, score) ->
        val value = requireNotNull(actual[boat]) { "no score for $boat in $actual" }
        assertEquals(score.toDouble(), value, "score of $boat")
    }
}

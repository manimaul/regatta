package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.FinishCode.*
import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.data.RaceReportCard
import com.mxmariner.regatta.data.StandingsSeries
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-style checks over seeded random fleets (test_audit_and_plan.md §4). Each test builds
 * many random races or series with the fixture DSL and checks a rule that must hold whatever the
 * fleet looks like. Tests the code fails today are ignored with the decision that fixes them;
 * each was run once without the ignore to confirm it fails.
 */
class InvariantsTest {

    // ---- random fleet specs, buildable in any entry order

    private sealed class Outcome {
        data class Finish(val ms: Long) : Outcome()
        data class Hoc(val position: Int) : Outcome()
        data class Code(val code: FinishCode) : Outcome()
    }

    private data class BoatSpec(val name: String, val outcome: Outcome, val penalty: Int? = null)

    /** bracket name -> entries */
    private class RaceSpec(val brackets: Map<String, List<BoatSpec>>) {
        fun map(f: (String, BoatSpec) -> BoatSpec?) = RaceSpec(brackets.mapValues { (b, boats) -> boats.mapNotNull { f(b, it) } })
    }

    private class SeriesSpec(val races: List<RaceSpec>)

    private fun Long.clock(): String {
        val h = 18 + this / 3_600_000
        val m = this / 60_000 % 60
        val s = this / 1000 % 60
        val ms = this % 1000
        return "%02d:%02d:%02d.%03d".format(h, m, s, ms)
    }

    private fun BracketBuilder.enter(spec: BoatSpec) {
        val entry = boat(spec.name, rating = 50)
        when (val o = spec.outcome) {
            is Outcome.Finish -> entry finishes o.ms.clock()
            is Outcome.Hoc -> entry hoc o.position
            is Outcome.Code -> entry code o.code
        }
        spec.penalty?.let { entry penalty it }
    }

    private fun RaceBuilder.enter(spec: RaceSpec, shuffle: Random?) {
        phrfClass("Fast") {
            spec.brackets.forEach { (name, boats) ->
                bracket(name) { (shuffle?.let { boats.shuffled(it) } ?: boats).forEach { enter(it) } }
            }
        }
    }

    private fun build(spec: RaceSpec, shuffle: Random? = null): RaceReport =
        race(start = "18:00") { enter(spec, shuffle) }.report()

    private fun build(spec: SeriesSpec, shuffle: Random? = null): StandingsSeries =
        series { spec.races.forEach { r -> race { enter(r, shuffle) } } }.standings()

    private val boatNames = ('A'..'L').map { it.toString() }

    /** A random race: 1 or 2 brackets, 2..7 boats each, mixed outcomes, no exact ties, no penalties unless asked. */
    private fun randomRace(
        rnd: Random,
        boats: List<String> = boatNames.shuffled(rnd).take(rnd.nextInt(2, 8)),
        codes: List<FinishCode> = listOf(DNF, RET, NSC),
        withPenalties: Boolean = false,
    ): RaceSpec {
        val bracketCount = if (boats.size >= 4 && rnd.nextBoolean()) 2 else 1
        val usedMs = mutableSetOf<Long>()
        // HOC positions are numbered across the whole race (GSI 14.2), so they are unique per race, not per bracket
        var hocPositions = (1..boats.size).shuffled(rnd)
        val brackets = boats.withIndex().groupBy({ "I".repeat(1 + it.index % bracketCount) }, { it.value })
        return RaceSpec(brackets.mapValues { (_, names) ->
            names.map { name ->
                val outcome = when (rnd.nextInt(10)) {
                    in 0..5 -> Outcome.Finish(generateSequence { rnd.nextLong(0, 3 * 3_600_000) }.first { usedMs.add(it) })
                    in 6..7 -> Outcome.Hoc(hocPositions.first().also { hocPositions = hocPositions.drop(1) })
                    else -> Outcome.Code(codes[rnd.nextInt(codes.size)])
                }
                val penalty = if (withPenalties && outcome !is Outcome.Code && rnd.nextInt(4) == 0) rnd.nextInt(1, 4) else null
                BoatSpec(name, outcome, penalty)
            }
        })
    }

    /** A random series: 1..7 races over a pool of 3..6 boats that keep their bracket; absent boats are DNS. */
    private fun randomSeries(rnd: Random, withRc: Boolean = false): SeriesSpec {
        val pool = boatNames.shuffled(rnd).take(rnd.nextInt(3, 7))
        val bracketOf = pool.associateWith { if (pool.size >= 4 && rnd.nextBoolean()) "II" else "I" }
        val races = (1..rnd.nextInt(1, 8)).map {
            val present = pool.filter { rnd.nextInt(5) != 0 }.ifEmpty { listOf(pool.first()) }
            val codes = if (withRc) listOf(DNF, RET, NSC, DNS_RC) else listOf(DNF, RET, NSC)
            val race = randomRace(rnd, present, codes)
            // regroup into the boats' fixed brackets
            RaceSpec(race.brackets.values.flatten().groupBy { bracketOf.getValue(it.name) })
        }
        return SeriesSpec(races)
    }

    private fun RaceReport.cards(): List<RaceReportCard> = classReports.flatMap { it.bracketReport }.flatMap { it.cards }
    private fun RaceReportCard.code() = resultRecord.result.finishCode
    private fun RaceReportCard.ms() = correctedPhrfTime?.inWholeMilliseconds

    private fun StandingsSeries.snapshot(): Map<String, Any> = standings.flatMap { it.standings }.flatMap { it.standings }
        .associate { row ->
            val boat = row.boatSkipper.boat?.name ?: ""
            boat to listOf(
                row.raceStandings.map { it.placeInBracket },
                row.raceStandings.map { it.placeInClass },
                row.totalScoreBracket, row.totalScoreClass, row.placeInBracket, row.placeInClass,
            )
        }

    // ---- race invariants

    @Test
    fun shuffling_race_entries_never_changes_a_score() {
        repeat(300) { i ->
            val rnd = Random(1000 + i)
            val spec = randomRace(rnd)
            val base = build(spec)
            repeat(3) { j ->
                val shuffled = build(spec, Random(i * 10 + j))
                assertEquals(base.bracketScores(), shuffled.bracketScores(), "seed $i bracket")
                assertEquals(base.classScores(), shuffled.classScores(), "seed $i class")
            }
        }
    }

    /** Timed finishers take places 1..k in corrected-time order, HOC boats k+1..k+h by position, and no score is below 1. */
    @Test
    fun places_are_contiguous_and_follow_corrected_time_then_hoc() {
        repeat(300) { i ->
            val report = build(randomRace(Random(2000 + i)))
            report.classReports.forEach { cr ->
                val groups = cr.bracketReport.map { it.cards } + listOf(cr.bracketReport.flatMap { it.cards })
                groups.forEachIndexed { g, cards ->
                    val score: (RaceReportCard) -> Int = if (g < cr.bracketReport.size) { c -> c.placeInBracket } else { c -> c.placeInClass }
                    val timed = cards.filter { it.code() == TIME }.sortedBy { it.ms() }
                    val hoc = cards.filter { it.code() == HOC }.sortedBy { it.hocPosition }
                    assertEquals((1..timed.size).toList(), timed.map(score), "seed $i timed places")
                    assertEquals((timed.size + 1..timed.size + hoc.size).toList(), hoc.map(score), "seed $i hoc places")
                    assertTrue(cards.all { score(it) >= 1 }, "seed $i: a score below 1")
                }
            }
        }
    }

    /** GSI 14.1.1 / 14.1.3: DNF and RET score finishers + 1, where finishers are TIME and HOC. */
    @Test
    fun dnf_and_ret_score_finishers_plus_one() {
        repeat(300) { i ->
            val report = build(randomRace(Random(3000 + i)))
            report.classReports.forEach { cr ->
                cr.bracketReport.forEach { br ->
                    val finishers = br.cards.count { it.code() == TIME || it.code() == HOC }
                    br.cards.filter { it.code() == DNF || it.code() == RET }.forEach {
                        assertEquals(finishers + 1, it.placeInBracket, "seed $i ${it.boatName}")
                    }
                }
                val classFinishers = cr.bracketReport.flatMap { it.cards }.count { it.code() == TIME || it.code() == HOC }
                cr.bracketReport.flatMap { it.cards }.filter { it.code() == DNF || it.code() == RET }.forEach {
                    assertEquals(classFinishers + 1, it.placeInClass, "seed $i ${it.boatName} class")
                }
            }
        }
    }

    /** NSC scores at least the DNF score. */
    @Test
    fun nsc_is_never_better_than_dnf() {
        repeat(300) { i ->
            val report = build(randomRace(Random(4000 + i)))
            report.classReports.flatMap { it.bracketReport }.forEach { br ->
                val dnf = br.cards.count { it.code() == TIME || it.code() == HOC } + 1
                br.cards.filter { it.code() == NSC }.forEach {
                    assertTrue(it.placeInBracket >= dnf, "seed $i ${it.boatName}: NSC ${it.placeInBracket} < DNF $dnf")
                }
            }
        }
    }

    /** D4: a penalty adds to the penalised boat's own place, capped at DNF, and changes nobody else. */
    @Test
    fun a_penalty_never_changes_another_boats_score() {
        var penalised = 0
        repeat(300) { i ->
            val rnd = Random(5000 + i)
            val spec = randomRace(rnd, withPenalties = true)
            val with = build(spec)
            val without = build(spec.map { _, b -> b.copy(penalty = null) })
            with.classReports.flatMap { it.bracketReport }.forEach { br ->
                val dnf = br.cards.count { it.code() == TIME || it.code() == HOC } + 1
                br.cards.forEach { card ->
                    val clean = without.bracketScores().getValue(card.boatName)
                    val expected = card.penalty?.let { penalised++; minOf(clean + it, dnf.toDouble()) } ?: clean
                    assertEquals(expected, card.placeInBracket.toDouble(), "seed $i ${card.boatName}")
                }
            }
            assertEquals(with.classScores().filterKeys { spec.brackets.values.flatten().first { b -> b.name == it }.penalty == null },
                without.classScores().filterKeys { spec.brackets.values.flatten().first { b -> b.name == it }.penalty == null },
                "seed $i class scores of unpenalised boats")
        }
        assertTrue(penalised > 50, "the generator produced too few penalties: $penalised")
    }

    /** Shuffling with penalties present must not change a score either (D4 reworks the penalty pass). */
    @Test
    fun shuffling_race_entries_with_penalties_never_changes_a_score() {
        repeat(300) { i ->
            val spec = randomRace(Random(6000 + i), withPenalties = true)
            val base = build(spec)
            repeat(3) { j ->
                val shuffled = build(spec, Random(i * 10 + j))
                assertEquals(base.bracketScores(), shuffled.bracketScores(), "seed $i")
            }
        }
    }

    // ---- series invariants

    /** A series total is the sum of the counted race scores, with at most one score excluded, and only when 5 or more races were started. */
    @Test
    fun series_total_is_the_sum_of_counted_scores_with_at_most_one_excluded() {
        repeat(200) { i ->
            val standings = build(randomSeries(Random(7000 + i)))
            standings.standings.flatMap { it.standings }.flatMap { it.standings }.forEach { row ->
                val boat = row.boatSkipper.boat?.name
                // D2: each group has its own throw-out, chosen among the races sailed for that group
                val bracketSailed = row.raceStandings.filter { it.bracketSailed }
                val classSailed = row.raceStandings.filter { it.classSailed }
                val excludedBracket = row.raceStandings.count { it.throwOut }
                val excludedClass = row.raceStandings.count { it.throwOutClass }
                assertTrue(excludedBracket <= 1 && excludedClass <= 1, "seed $i $boat: $excludedBracket / $excludedClass throw-outs")
                if (bracketSailed.size < 5) assertEquals(0, excludedBracket, "seed $i $boat: bracket throw-out with ${bracketSailed.size} sailed races")
                if (classSailed.size < 5) assertEquals(0, excludedClass, "seed $i $boat: class throw-out with ${classSailed.size} sailed races")
                assertTrue(row.raceStandings.none { it.throwOut && !it.bracketSailed }, "seed $i $boat: an unsailed race thrown out")
                assertEquals(bracketSailed.filter { !it.throwOut }.sumOf { it.placeInBracket.toDouble() }, row.totalScoreBracket.toDouble(), "seed $i bracket total")
                assertEquals(classSailed.filter { !it.throwOutClass }.sumOf { it.placeInClass.toDouble() }, row.totalScoreClass.toDouble(), "seed $i class total")
            }
        }
    }

    /** Series places follow totals: a better place never has a worse total, and every boat has one row. */
    @Test
    fun series_places_follow_totals() {
        repeat(200) { i ->
            val standings = build(randomSeries(Random(8000 + i)))
            standings.standings.forEach { sc ->
                sc.standings.forEach { sb ->
                    val rows = sb.standings.sortedBy { it.placeInBracket }
                    rows.zipWithNext().forEach { (a, b) ->
                        assertTrue(a.totalScoreBracket <= b.totalScoreBracket, "seed $i bracket ${sb.bracket.name}: place order disagrees with totals")
                    }
                    assertEquals(rows.size, rows.map { it.boatSkipper.boat?.name }.toSet().size, "seed $i: a boat has two rows")
                }
                val rows = sc.standings.flatMap { it.standings }.sortedBy { it.placeInClass }
                rows.zipWithNext().forEach { (a, b) ->
                    assertTrue(a.totalScoreClass <= b.totalScoreClass, "seed $i class: place order disagrees with totals")
                }
            }
        }
    }

    /** D5: shuffling the entries of every race never changes a standings score or place. */
    @Test
    fun shuffling_series_entries_never_changes_standings() {
        repeat(200) { i ->
            val spec = randomSeries(Random(9000 + i))
            val base = build(spec).snapshot()
            repeat(3) { j ->
                assertEquals(base, build(spec, Random(i * 10 + j)).snapshot(), "seed $i")
            }
        }
    }

    /**
     * GSI 1.4.1.2 / D3: an RC volunteer entry never changes another boat's scores or totals, nor
     * the order of the other boats among themselves. (The volunteer's own total, and so its place
     * between them, is what the rule is about, so it is left out of the comparison.)
     */
    @Test
    fun an_rc_volunteer_never_changes_another_boat() {
        var volunteers = 0
        repeat(200) { i ->
            val spec = randomSeries(Random(10_000 + i), withRc = true)
            val rcBoats = spec.races.flatMap { r -> r.brackets.values.flatten().filter { it.outcome == Outcome.Code(DNS_RC) }.map { it.name } }.toSet()
            if (rcBoats.isEmpty()) return@repeat
            volunteers += rcBoats.size
            val with = build(spec)
            // the same series with the RC entries removed, so those boats are plain DNS in those races
            val without = build(SeriesSpec(spec.races.map { r -> r.map { _, b -> b.takeIf { it.outcome != Outcome.Code(DNS_RC) } } }))
            val others = with.snapshot().keys - rcBoats
            others.forEach { boat ->
                assertEquals(without.bracketRaceScores(boat), with.bracketRaceScores(boat), "seed $i $boat bracket scores")
                assertEquals(without.classRaceScores(boat), with.classRaceScores(boat), "seed $i $boat class scores")
                assertEquals(without.bracketTotals()[boat], with.bracketTotals()[boat], "seed $i $boat bracket total")
                assertEquals(without.classTotals()[boat], with.classTotals()[boat], "seed $i $boat class total")
            }
            // the other boats keep their order within each bracket and within each class
            fun StandingsSeries.bracketOrders() = standings.flatMap { c -> c.standings.map { b -> b.bracket.name to b.standings.filter { it.boatSkipper.boat?.name in others }.sortedBy { it.placeInBracket }.map { it.boatSkipper.boat?.name } } }.toMap()
            fun StandingsSeries.classOrders() = standings.associate { c -> c.raceClass.name to c.standings.flatMap { it.standings }.filter { it.boatSkipper.boat?.name in others }.sortedBy { it.placeInClass }.map { it.boatSkipper.boat?.name } }
            assertEquals(without.bracketOrders(), with.bracketOrders(), "seed $i bracket order of the other boats")
            assertEquals(without.classOrders(), with.classOrders(), "seed $i class order of the other boats")
        }
        assertTrue(volunteers > 30, "the generator produced too few RC volunteers: $volunteers")
    }
}

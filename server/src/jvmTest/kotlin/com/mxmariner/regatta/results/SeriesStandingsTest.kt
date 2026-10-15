package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.FinishCode.*
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.RaceStatus
import com.mxmariner.regatta.data.RatingType
import kotlin.test.Ignore
import kotlin.time.Duration.Companion.days
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Series standings rules: GSI 14.4 / 14.5 / 1.4.1.2 with the D1, D2, D3, D5 and D8 decisions in
 * docs/explore_implemented/test_audit_and_plan.md. Tests the code fails today are ignored with
 * the decision that fixes them. Unless noted, every race is in the PHRF class "Fast", bracket I,
 * with equal ratings, so finishing order is placing order.
 */
class SeriesStandingsTest {

    /** Enters the boats as timed finishers in the given order, one minute apart. */
    private fun BracketBuilder.finishers(vararg boats: String) {
        boats.forEachIndexed { i, name -> boat(name, rating = 50) finishes "19:0$i:00" }
    }

    private fun SeriesBuilder.fastRace(vararg finishers: String) =
        race { phrfClass("Fast") { bracket("I") { finishers(*finishers) } } }

    // ---- DNS (GSI 14.1.2)

    @Test
    fun gsi_14_1_2_dns_scores_starters_plus_one_in_bracket_and_class() {
        val standings = series {
            race {
                phrfClass("Fast") {
                    bracket("I") { boat("A", rating = 50) finishes "19:00:00"; boat("B", rating = 50) finishes "19:01:00"; boat("C", rating = 50) finishes "19:02:00" }
                    bracket("II") { boat("X", rating = 50) finishes "19:03:00"; boat("Y", rating = 50) finishes "19:04:00" }
                }
            }
            race {
                phrfClass("Fast") {
                    bracket("I") { boat("A", rating = 50) finishes "19:00:00"; boat("B", rating = 50) finishes "19:01:00" }
                    bracket("II") { boat("X", rating = 50) finishes "19:03:00"; boat("Y", rating = 50) finishes "19:04:00" }
                }
            }
        }.standings()
        assertEquals(listOf(3.0, 3.0), standings.bracketRaceScores("C"))   // 3rd, then 2 starters + 1
        assertEquals(listOf(3.0, 5.0), standings.classRaceScores("C"))     // 3rd, then 4 starters + 1
    }

    /** D6: a DNS record scores the same as no record, and does not count as a starter for the others. */
    @Test
    fun d6_dns_record_scores_like_no_record() {
        val standings = series {
            fastRace("A", "B", "C")
            race { phrfClass("Fast") { bracket("I") { finishers("A", "B"); boat("C", rating = 50) code DNS } } }
            fastRace("A", "B")
        }.standings()
        assertEquals(listOf(3.0, 3.0, 3.0), standings.bracketRaceScores("C"))
        assertEquals(listOf(1.0, 1.0, 1.0), standings.bracketRaceScores("A"))
    }

    /** D3: an RC volunteer is not a starter, so the DNS score of other boats does not count it. */
    @Test
    fun d3_dns_does_not_count_the_rc_volunteer_as_a_starter() {
        val standings = series {
            fastRace("A", "B", "C", "D")
            fastRace("A", "B")            // C is the RC volunteer, D is absent
            rcVolunteer("C", race = 2)
        }.standings()
        assertEquals(3.0, standings.bracketRaceScores("D")[1])
        assertEquals(3.0, standings.classRaceScores("D")[1])
    }

    /** GSI 1.4.1.2: the RC score does not affect the other boats, in scores or places. */
    @Test
    fun gsi_1_4_1_2_rc_volunteer_does_not_change_other_boats() {
        fun build(withRc: Boolean) = series {
            fastRace("A", "B", "C", "D")
            fastRace("A", "C")            // B absent, or the RC volunteer; D absent
            fastRace("A", "B", "D")       // C absent
            if (withRc) rcVolunteer("B", race = 2)
        }.standings()
        val without = build(false)
        val with = build(true)
        listOf("A", "C", "D").forEach { boat ->
            assertEquals(without.bracketRaceScores(boat), with.bracketRaceScores(boat), boat)
            assertEquals(without.classRaceScores(boat), with.classRaceScores(boat), boat)
            assertEquals(without.bracketPlaces()[boat], with.bracketPlaces()[boat], boat)
        }
    }

    // ---- throw-out (GSI 14.4, D2)

    @Test
    fun gsi_14_4_five_started_races_drop_the_worst_score() {
        val standings = series { repeat(5) { fastRace("A", "B") } }.standings()
        standings.assertBracketTotals("A" to 4, "B" to 8)
        standings.assertClassTotals("A" to 4, "B" to 8)
        assertEquals(1, standings.row("B").raceStandings.count { it.throwOut })
    }

    @Test
    fun gsi_14_4_four_races_have_no_throw_out() {
        val standings = series { repeat(4) { fastRace("A", "B") } }.standings()
        standings.assertBracketTotals("A" to 4, "B" to 8)
        assertEquals(0, standings.row("B").raceStandings.count { it.throwOut })
    }

    @Test
    fun throw_out_drops_the_single_worst_score() {
        val standings = series {
            fastRace("A", "B", "C")
            fastRace("A", "B", "C")
            fastRace("B", "C")          // A DNS = 3
            fastRace("A", "B", "C")
            fastRace("A", "B", "C")
        }.standings()
        standings.assertBracketTotals("A" to 4, "B" to 7, "C" to 11)
        assertEquals(true, standings.row("A").raceStandings[2].throwOut)
    }

    /** D2: a race with no results scores nothing and does not count toward the throw-out threshold. */
    @Test
    fun d2_race_with_no_results_scores_nothing_and_is_not_started() {
        val standings = series {
            repeat(4) { fastRace("A", "B") }
            race { phrfClass("Fast") { bracket("I") { } } }   // cancelled, or not yet sailed
        }.standings()
        assertEquals(5, standings.races.size)
        standings.assertBracketTotals("A" to 4, "B" to 8)   // four started races: no throw-out, nothing for the fifth
    }

    /** D2: a race with no results is "not yet sailed" while its end is in the future, and "cancelled" once it is past. */
    @Test
    fun d2_race_status_depends_on_now() {
        val fixture = series {
            fastRace("A", "B")
            race { phrfClass("Fast") { bracket("I") { } } }
        }
        val before = fixture.standings(now = fixture.races[1].schedule.startTime - 1.days)
        val after = fixture.standings(now = fixture.races[1].schedule.endTime + 1.days)
        assertEquals(listOf(RaceStatus.Sailed, RaceStatus.NotYetSailed), before.row("A").raceStandings.map { it.status })
        assertEquals(listOf(RaceStatus.Sailed, RaceStatus.Cancelled), after.row("A").raceStandings.map { it.status })
        val cell = after.row("A").raceStandings[1]
        assertEquals(false, cell.bracketSailed)
        assertEquals(false, cell.classSailed)
        assertEquals(0.0, cell.placeInBracket.toDouble())
        assertEquals(false, cell.throwOut)
    }

    /** D2: a race sailed by other boats but by nobody in this bracket is "sailed" as a race, but not for the bracket. */
    @Test
    fun d2_race_sailed_elsewhere_is_sailed_but_not_for_the_group() {
        val standings = series {
            race {
                phrfClass("Fast") {
                    bracket("I") { boat("A", rating = 50) finishes "19:00:00" }
                    bracket("II") { boat("X", rating = 50) finishes "19:02:00" }
                }
            }
            race {
                phrfClass("Fast") {
                    bracket("I") { boat("A", rating = 50) finishes "19:00:00" }
                    bracket("II") { }
                }
            }
        }.standings()
        val cell = standings.row("X").raceStandings[1]
        assertEquals(RaceStatus.Sailed, cell.status)
        assertEquals(false, cell.bracketSailed)
        assertEquals(true, cell.classSailed)
        assertEquals(2.0, cell.placeInClass.toDouble())
    }

    /** D2: an RC volunteer alone in a bracket does not make the race sailed for it. */
    @Test
    fun d2_rc_volunteer_alone_is_not_a_sailed_race() {
        val standings = series {
            fastRace("A", "B")
            race { phrfClass("Fast") { bracket("I") { boat("A", rating = 50) code DNS_RC } } }
        }.standings()
        assertEquals(false, standings.row("A").raceStandings[1].bracketSailed)
        standings.assertBracketTotals("A" to 1, "B" to 2)
    }

    /** D2: the last-race tiebreak uses the last race sailed for the group. */
    @Test
    fun d2_tiebreak_uses_the_last_sailed_race() {
        val standings = series {
            fastRace("A", "B")
            fastRace("B", "A")
            race { phrfClass("Fast") { bracket("I") { } } }
        }.standings()
        assertEquals(mapOf("B" to 1, "A" to 2), standings.bracketPlaces())
    }

    /** D2: sailed is decided per bracket, so the same race can count for the class but not for the bracket. */
    @Test
    fun d2_race_not_sailed_in_a_bracket_scores_nothing_there_but_counts_in_the_class() {
        val standings = series {
            race {
                phrfClass("Fast") {
                    bracket("I") { boat("A", rating = 50) finishes "19:00:00"; boat("B", rating = 50) finishes "19:01:00" }
                    bracket("II") { boat("X", rating = 50) finishes "19:02:00" }
                }
            }
            race {
                phrfClass("Fast") {
                    bracket("I") { boat("A", rating = 50) finishes "19:00:00"; boat("B", rating = 50) finishes "19:01:00" }
                    bracket("II") { }
                }
            }
        }.standings()
        standings.assertBracketTotals("X" to 1)        // race 2 was not sailed in bracket II
        standings.assertClassTotals("X" to 3 + 3)      // in the class, race 2 had 2 starters: DNS = 3
    }

    /** D2: the throw-out is chosen per scoring group: worst bracket score for the bracket total, worst class score for the class total. */
    @Test
    fun d2_throw_out_is_chosen_per_scoring_group() {
        fun SeriesBuilder.fastRace(a: String, b: String, z: String, x: String, y: String) = race {
            phrfClass("Fast") {
                bracket("I") { boat("A", rating = 50) finishes a; boat("B", rating = 50) finishes b; boat("Z", rating = 50) finishes z }
                bracket("II") { boat("X", rating = 50) finishes x; boat("Y", rating = 50) finishes y }
            }
        }
        val standings = series {
            fastRace("19:00:00", "19:03:00", "19:04:00", "19:01:00", "19:02:00")   // B: bracket 2, class 4
            fastRace("19:00:00", "19:02:00", "19:01:00", "19:05:00", "19:06:00")   // B: bracket 3, class 3
            repeat(3) { fastRace("19:00:00", "19:01:00", "19:02:00", "19:05:00", "19:06:00") }   // B: 2, 2
        }.standings()
        assertEquals(listOf(2.0, 3.0, 2.0, 2.0, 2.0), standings.bracketRaceScores("B"))
        assertEquals(listOf(4.0, 3.0, 2.0, 2.0, 2.0), standings.classRaceScores("B"))
        standings.assertBracketTotals("B" to 8)   // drops the 3 in race 2
        standings.assertClassTotals("B" to 9)     // drops the 4 in race 1
    }

    // ---- RC volunteer (GSI 1.4.1.2, D1)

    /** D1: the RC race scores the average of the boat's other races, and the total uses it. */
    @Test
    fun d1_rc_volunteer_scores_the_average_of_the_other_races() {
        val standings = series {
            fastRace("A", "B", "C", "D")   // B 2
            fastRace("A", "C", "D")        // B RC
            fastRace("C", "D", "A", "B")   // B 4
            rcVolunteer("B", race = 2)
        }.standings()
        assertEquals(listOf(2.0, 3.0, 4.0), standings.bracketRaceScores("B"))
        standings.assertBracketTotals("B" to 9)
        standings.assertClassTotals("B" to 9)
    }

    /** D1: the average is fractional, to the nearest tenth of a point (RRS A10(a)). */
    @Test
    fun d1_rc_average_is_fractional() {
        val standings = series {
            fastRace("A", "B", "C")   // B 2
            fastRace("A", "C")        // B RC
            fastRace("A", "C", "B")   // B 3
            rcVolunteer("B", race = 2)
        }.standings()
        assertEquals(listOf(2.0, 2.5, 3.0), standings.bracketRaceScores("B"))
        standings.assertBracketTotals("B" to 7.5)
    }

    /** D1: DNS races count in the average. */
    @Test
    fun d1_rc_average_includes_dns_races() {
        val standings = series {
            fastRace("A", "B", "C")   // B 2
            fastRace("A", "C")        // B RC
            fastRace("A", "C")        // B DNS 3
            rcVolunteer("B", race = 2)
        }.standings()
        standings.assertBracketTotals("B" to 2 + 2.5 + 3)
    }

    /** D1 order of operations: the throw-out is chosen from the other races first, and the average leaves it out. */
    @Test
    fun d1_rc_average_leaves_out_the_throw_out() {
        val standings = series {
            fastRace("A", "B", "C", "D", "E")   // B 2
            fastRace("A", "C", "D", "E")        // B RC
            fastRace("A", "C", "D", "E", "B")   // B 5, the throw-out
            fastRace("A", "B", "C", "D", "E")   // B 2
            fastRace("A", "B", "C", "D", "E")   // B 2
            rcVolunteer("B", race = 2)
        }.standings()
        assertEquals(listOf(2.0, 2.0, 5.0, 2.0, 2.0), standings.bracketRaceScores("B"))
        assertEquals(true, standings.row("B").raceStandings[2].throwOut)
        standings.assertBracketTotals("B" to 8)
    }

    /** D1: the average of the remaining races is never worse than the thrown-out score, so the RC race is never the throw-out. */
    @Test
    fun d1_rc_race_is_never_the_throw_out() {
        val standings = series {
            fastRace("A", "B", "C")   // B 2
            fastRace("A", "C")        // B RC
            fastRace("A", "C", "B")   // B 3
            fastRace("A", "C", "B")   // B 3
            fastRace("A", "C", "B")   // B 3
            rcVolunteer("B", race = 2)
        }.standings()
        // one 3 is thrown out; the average of 2, 3, 3 is 2.67, rounded to 2.7
        assertEquals(false, standings.row("B").raceStandings[1].throwOut)
        standings.assertBracketTotals("B" to 2 + 2.7 + 3 + 3)
    }

    /** D1 / RRS A10(a): the average is rounded to the nearest tenth of a point, 0.05 upward. */
    @Test
    fun d1_rc_average_rounds_to_the_nearest_tenth_half_up() {
        val standings = series {
            fastRace("A", "B", "C", "D", "E")   // B 2
            fastRace("A", "C", "D", "E")        // B RC
            fastRace("A", "C", "B", "D", "E")   // B 3
            fastRace("A", "C", "B", "D", "E")   // B 3
            fastRace("A", "C", "B", "D", "E")   // B 3
            fastRace("A", "C", "D", "E", "B")   // B 5, the throw-out
            rcVolunteer("B", race = 2)
        }.standings()
        // the average of 2, 3, 3, 3 is 2.75, which rounds up to 2.8, not to 2.7 or 2.75
        assertEquals(listOf(2.0, 2.8, 3.0, 3.0, 3.0, 5.0), standings.bracketRaceScores("B"))
        standings.assertBracketTotals("B" to 2 + 2.8 + 3 + 3 + 3)
    }

    /** D1: with no other scored race, the RC race scores DNS (starters + 1), not DNF (finishers + 1). */
    @Test
    fun d1_rc_with_no_other_race_scores_dns() {
        val standings = series {
            race { phrfClass("Fast") { bracket("I") { finishers("A"); boat("C", rating = 50) code DNF; boat("B", rating = 50) code DNS_RC } } }
        }.standings()
        standings.assertBracketTotals("A" to 1, "C" to 2, "B" to 3)
    }

    /** D1: once per series; the earliest RC race gets the average and later ones score DNS, which then counts in the average. */
    @Test
    fun d1_rc_is_once_per_series() {
        val standings = series {
            fastRace("A", "B", "C")   // B 2
            fastRace("A", "C")        // B RC: average of 2 and 3
            fastRace("A", "C")        // B RC again: DNS 3
            rcVolunteer("B", race = 2)
            rcVolunteer("B", race = 3)
        }.standings()
        assertEquals(listOf(2.0, 2.5, 3.0), standings.bracketRaceScores("B"))
        standings.assertBracketTotals("B" to 7.5)
    }

    // ---- tiebreak (GSI 14.5, D5)

    @Test
    fun gsi_14_5_tie_goes_to_the_better_last_race() {
        val standings = series {
            fastRace("A", "B")
            fastRace("B", "A")
        }.standings()
        standings.assertBracketTotals("A" to 3, "B" to 3)
        assertEquals(mapOf("B" to 1, "A" to 2), standings.bracketPlaces())
        assertEquals(mapOf("B" to 1, "A" to 2), standings.classPlaces())
    }

    @Test
    fun gsi_14_5_three_way_tie_is_ordered_by_the_last_race() {
        val standings = series {
            fastRace("A", "B", "C")
            fastRace("B", "C", "A")
            fastRace("C", "A", "B")
        }.standings()
        standings.assertBracketTotals("A" to 6, "B" to 6, "C" to 6)
        assertEquals(mapOf("C" to 1, "A" to 2, "B" to 3), standings.bracketPlaces())
    }

    /** D5: an equal last race is broken by the next-to-last race, and so on back. */
    @Test
    fun d5_equal_last_race_goes_back_to_earlier_races() {
        val standings = series {
            fastRace("A", "B", "C")                                                                              // A1 B2 C3
            fastRace("B", "A", "C")                                                                              // A2 B1 C3
            race { phrfClass("Fast") { bracket("I") { finishers("C"); boat("A", rating = 50) code DNF; boat("B", rating = 50) code DNF } } }   // A2 B2 C1
            race { phrfClass("Fast") { bracket("I") { finishers("C"); boat("A", rating = 50) code DNF; boat("B", rating = 50) code DNF } } }   // A2 B2 C1
        }.standings()
        standings.assertBracketTotals("A" to 7, "B" to 7, "C" to 8)
        // last two races equal; race 2 says B
        assertEquals(mapOf("B" to 1, "A" to 2, "C" to 3), standings.bracketPlaces())
        assertEquals(mapOf("B" to 1, "A" to 2, "C" to 3), standings.classPlaces())
    }

    /** D5: when every race is equal the boats share the place and the next place skips. */
    @Test
    fun d5_fully_tied_boats_share_the_place() {
        val standings = series {
            race { phrfClass("Fast") { bracket("I") { finishers("C"); boat("A", rating = 50) code DNF; boat("B", rating = 50) code DNF } } }   // C1 A2 B2, D DNS 4
            race { phrfClass("Fast") { bracket("I") { finishers("C"); boat("A", rating = 50) code DNF; boat("B", rating = 50) code DNF } } }   // C1 A2 B2, D DNS 4
            fastRace("C", "D")                                                                                                                 // C1 D2, A B DNS 3
        }.standings()
        standings.assertBracketTotals("C" to 3, "A" to 7, "B" to 7, "D" to 10)
        assertEquals(mapOf("C" to 1, "A" to 2, "B" to 2, "D" to 4), standings.bracketPlaces())
        assertEquals(mapOf("C" to 1, "A" to 2, "B" to 2, "D" to 4), standings.classPlaces())
    }

    /** D5 / GSI 1.4.1.2: a race where either tied boat has an RC score is skipped in the tiebreak. */
    @Test
    fun d5_rc_race_is_skipped_in_the_tiebreak() {
        val standings = series {
            fastRace("A", "B", "C")   // A1 B2 C3
            fastRace("C", "B", "A")   // A3 B2 C1
            fastRace("C", "A")        // A2, B RC: average 2, C1
            rcVolunteer("B", race = 3)
        }.standings()
        standings.assertBracketTotals("A" to 6, "B" to 6, "C" to 5)
        // the last race is skipped for the A/B tie; race 2 says B
        assertEquals(mapOf("C" to 1, "B" to 2, "A" to 3), standings.bracketPlaces())
    }

    /** D5: the result does not depend on the input order of the boats. */
    @Test
    fun d5_places_do_not_depend_on_input_order() {
        fun build(order: List<String>) = series {
            repeat(2) {
                race { phrfClass("Fast") { bracket("I") { order.forEach { boat(it, rating = 50) code DNF }; finishers("C") } } }
            }
        }.standings().bracketPlaces()
        assertEquals(mapOf("C" to 1, "A" to 2, "B" to 2), build(listOf("A", "B")))
        assertEquals(build(listOf("A", "B")), build(listOf("B", "A")))
    }

    // ---- totals, grouping, single race

    @Test
    fun class_totals_use_class_scores_and_bracket_totals_use_bracket_scores() {
        val standings = series {
            repeat(2) {
                race {
                    phrfClass("Fast") {
                        bracket("I") { boat("A", rating = 50) finishes "19:00:00"; boat("B", rating = 50) finishes "19:03:00" }
                        bracket("II") { boat("X", rating = 50) finishes "19:01:00"; boat("Y", rating = 50) finishes "19:02:00" }
                    }
                }
            }
        }.standings()
        standings.assertBracketTotals("A" to 2, "B" to 4, "X" to 2, "Y" to 4)
        standings.assertClassTotals("A" to 2, "X" to 4, "Y" to 6, "B" to 8)
        assertEquals(mapOf("A" to 1, "B" to 2, "X" to 1, "Y" to 2), standings.bracketPlaces())
        assertEquals(mapOf("A" to 1, "X" to 2, "Y" to 3, "B" to 4), standings.classPlaces())
    }

    /** D8: a boat that changes skipper keeps one standings row, showing the latest skipper. */
    @Test
    fun d8_standings_are_grouped_by_boat_not_by_boat_and_skipper() {
        val standings = series {
            race { phrfClass("Fast") { bracket("I") { boat("A", rating = 50, skipper = "Ann Owner") finishes "19:00:00"; boat("B", rating = 50) finishes "19:01:00" } } }
            race { phrfClass("Fast") { bracket("I") { boat("A", rating = 50, skipper = "Bob Crew") finishes "19:00:00"; boat("B", rating = 50) finishes "19:01:00" } } }
        }.standings()
        val rows = standings.standings.single().standings.single().standings
        assertEquals(2, rows.size)
        assertEquals("Bob Crew", standings.row("A").boatSkipper.skipper?.fullName())
        standings.assertBracketTotals("A" to 2, "B" to 4)
    }

    @Test
    fun single_race_series() {
        val standings = series { fastRace("A", "B", "C") }.standings()
        standings.assertBracketTotals("A" to 1, "B" to 2, "C" to 3)
        assertEquals(mapOf("A" to 1, "B" to 2, "C" to 3), standings.bracketPlaces())
        assertEquals(false, standings.row("A").raceStandings.single().throwOut)
    }

    // ---- ORC series scoring (D9, orc_series_standings.md)

    /** Certificates for the ORC tests: with a B factor of 600 the race scores five-band Low/Medium. */
    private val certs = mapOf(
        "slow" to OrcCertificate(refNo = "slow", fiveBandWlLowMedTot = 1.0),
        "fast" to OrcCertificate(refNo = "fast", fiveBandWlLowMedTot = 0.5),
        "mid" to OrcCertificate(refNo = "mid", fiveBandWlLowMedTot = 0.8),
    )

    /** An ORC/PHRF boat with a PHRF rating of 50 (TCF 1 at B 600, so the PHRF order is the finish order) and a certificate. */
    private fun BracketBuilder.orcBoat(name: String, cert: String, finish: String) =
        boat(name, rating = 50) finishes finish orc cert

    /** A race of the ORC/PHRF class "Fast", bracket I, starting at 18:00. */
    private fun SeriesBuilder.orcRace(block: BracketBuilder.() -> Unit) =
        race(start = "18:00") { orcPhrfClass("Fast") { bracket("I", block) } }

    /**
     * The standard ORC race of these tests. Elapsed times A 60, C 70, B 90 minutes give the PHRF
     * order A, C, B; the ORC corrected times A 60, C 56, B 45 give the ORC order B, C, A.
     */
    private fun SeriesBuilder.standardOrcRace() = orcRace {
        orcBoat("A", "slow", "19:00:00")
        orcBoat("B", "fast", "19:30:00")
        orcBoat("C", "mid", "19:10:00")
    }

    @Test
    fun d9_orc_series_total_is_the_sum_of_orc_race_scores() {
        val standings = series {
            repeat(3) { orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00") } }
        }.standings(orcCerts = certs)
        standings.assertBracketTotals("A" to 3, "B" to 6)
        standings.assertBracketTotalsOrc("A" to 6, "B" to 3)
        standings.assertClassTotalsOrc("A" to 6, "B" to 3)
        assertEquals(mapOf("A" to 1, "B" to 2), standings.bracketPlaces())
        assertEquals(mapOf("A" to 2, "B" to 1), standings.bracketPlacesOrc())
        assertEquals(mapOf("A" to 2, "B" to 1), standings.classPlacesOrc())
        assertEquals(RatingType.ORC_PHRF, standings.row("A").ratingType)
        // the PHRF side is what a PHRF-only fleet of the same races scores
        val phrfOnly = series { repeat(3) { fastRace("A", "B") } }.standings()
        assertEquals(phrfOnly.bracketTotals(), standings.bracketTotals())
        assertEquals(phrfOnly.bracketPlaces(), standings.bracketPlaces())
    }

    /** GSI 14.1.2 / D9: a boat with no ORC entry scores the ORC starters + 1 in the ORC groups, and the PHRF starters + 1 in the PHRF ones. */
    @Test
    fun d9_orc_dns_scores_orc_starters_plus_one() {
        val standings = series {
            orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00"); boat("P", rating = 50, ratingType = RatingType.PHRF) finishes "19:40:00" }
            orcRace { orcBoat("A", "slow", "19:00:00"); boat("P", rating = 50, ratingType = RatingType.PHRF) finishes "19:40:00" }
        }.standings(orcCerts = certs)
        assertEquals(listOf(1.0, 2.0), standings.bracketRaceScoresOrc("B"))   // 1st, then 1 ORC starter + 1
        assertEquals(listOf(2.0, 3.0), standings.bracketRaceScores("B"))      // 2nd, then 2 PHRF starters + 1
        assertEquals(true, standings.row("B").raceStandings[1].nonStarterOrc)
        assertEquals(false, standings.row("B").raceStandings[0].nonStarterOrc)
    }

    /** D2 / D9: a race with no ORC starter is not sailed for the ORC groups, even though it is sailed for the PHRF ones. */
    @Test
    fun d9_orc_race_without_orc_starters_is_not_sailed_for_orc() {
        val standings = series {
            orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00"); boat("P", rating = 50, ratingType = RatingType.PHRF) finishes "19:40:00" }
            orcRace { boat("P", rating = 50, ratingType = RatingType.PHRF) finishes "19:00:00" }
        }.standings(orcCerts = certs)
        val second = standings.row("A").raceStandings[1]
        assertEquals(false, second.bracketSailedOrc)
        assertEquals(false, second.classSailedOrc)
        assertEquals(true, second.bracketSailed)
        assertEquals(false, second.nonStarterOrc)
        assertEquals(listOf(2.0, 0.0), standings.bracketRaceScoresOrc("A"))
        assertEquals(listOf(1.0, 0.0), standings.bracketRaceScoresOrc("B"))
        standings.assertBracketTotalsOrc("A" to 2, "B" to 1)
        standings.assertBracketTotals("A" to 1 + 2, "B" to 2 + 2)   // PHRF DNS in race 2: 1 starter + 1
    }

    /** GSI 14.4 / D9: each rating throws out its own worst race, which can be a different race. */
    @Test
    fun d9_orc_throw_out_is_chosen_separately_from_phrf() {
        val standings = series {
            repeat(3) { standardOrcRace() }
            // A absent: PHRF DNS = 5 starters + 1 = 6, ORC DNS = 2 ORC starters + 1 = 3
            orcRace {
                orcBoat("B", "fast", "19:00:00"); orcBoat("C", "mid", "19:10:00")
                listOf("P1", "P2", "P3").forEachIndexed { i, p -> boat(p, rating = 50, ratingType = RatingType.PHRF) finishes "19:2$i:00" }
            }
            // A first under PHRF, last under ORC: corrected B 45, D 50, C 56, A 60
            orcRace {
                orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00")
                orcBoat("C", "mid", "19:10:00"); orcBoat("D", "fast", "19:40:00")
            }
        }.standings(orcCerts = certs)
        assertEquals(listOf(1.0, 1.0, 1.0, 6.0, 1.0), standings.bracketRaceScores("A"))
        assertEquals(listOf(3.0, 3.0, 3.0, 3.0, 4.0), standings.bracketRaceScoresOrc("A"))
        val cells = standings.row("A").raceStandings
        assertEquals(listOf(false, false, false, true, false), cells.map { it.throwOut })
        assertEquals(listOf(false, false, false, false, true), cells.map { it.throwOutOrc })
        standings.assertBracketTotals("A" to 4)
        standings.assertBracketTotalsOrc("A" to 12)
    }

    /** D1 / D9: the RC volunteer's ORC score is the average of its other ORC race scores. */
    @Test
    fun d9_orc_rc_volunteer_averages_orc_scores() {
        val standings = series {
            standardOrcRace()                                                   // A: PHRF 1, ORC 3
            orcRace { orcBoat("B", "fast", "19:30:00"); orcBoat("C", "mid", "19:10:00") }   // A RC
            orcRace {
                orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00")
                orcBoat("C", "mid", "19:10:00"); orcBoat("D", "fast", "19:40:00")
            }                                                                   // A: PHRF 1, ORC 4
            rcVolunteer("A", race = 2)
        }.standings(orcCerts = certs)
        assertEquals(listOf(1.0, 1.0, 1.0), standings.bracketRaceScores("A"))
        assertEquals(listOf(3.0, 3.5, 4.0), standings.bracketRaceScoresOrc("A"))
        standings.assertBracketTotals("A" to 3)
        standings.assertBracketTotalsOrc("A" to 10.5)
    }

    /** GSI 14.5 / D5 / D9: an ORC tie is broken on the ORC scores of the last race, whatever the PHRF standings say. */
    @Test
    fun d9_orc_tiebreak_uses_orc_race_scores() {
        val standings = series {
            orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "20:10:00") }   // ORC A 60, B 65: A 1, B 2
            orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00") }   // ORC A 60, B 45: B 1, A 2
        }.standings(orcCerts = certs)
        standings.assertBracketTotalsOrc("A" to 3, "B" to 3)
        assertEquals(mapOf("B" to 1, "A" to 2), standings.bracketPlacesOrc())
        assertEquals(mapOf("B" to 1, "A" to 2), standings.classPlacesOrc())
        assertEquals(mapOf("A" to 1, "B" to 2), standings.bracketPlaces())   // PHRF: A wins both races
    }

    /** D9: a PHRF-only boat in an ORC class is not scored under ORC and takes no ORC place. */
    @Test
    fun d9_orc_places_are_among_orc_rows_only() {
        val standings = series {
            repeat(2) { orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00"); boat("P", rating = 50, ratingType = RatingType.PHRF) finishes "19:40:00" } }
        }.standings(orcCerts = certs)
        val p = standings.row("P")
        assertEquals(RatingType.PHRF, p.ratingType)
        assertEquals(0, p.placeInBracketOrc)
        assertEquals(0, p.placeInClassOrc)
        assertEquals(0.0, p.totalScoreBracketOrc)
        assertEquals(listOf(0.0, 0.0), standings.bracketRaceScoresOrc("P"))
        assertEquals(mapOf("B" to 1, "A" to 2, "P" to 0), standings.bracketPlacesOrc())
        assertEquals(mapOf("A" to 1, "B" to 2, "P" to 3), standings.bracketPlaces())
    }

    /** D9: outside an ORC class nothing is scored under ORC. */
    @Test
    fun d9_phrf_only_class_has_no_orc_fields_set() {
        val standings = series { repeat(2) { fastRace("A", "B") } }.standings()
        standings.standings.flatMap { it.standings }.flatMap { it.standings }.forEach { row ->
            assertEquals(RatingType.PHRF, row.ratingType)
            assertEquals(0.0, row.totalScoreBracketOrc)
            assertEquals(0.0, row.totalScoreClassOrc)
            assertEquals(0, row.placeInBracketOrc)
            assertEquals(0, row.placeInClassOrc)
            row.raceStandings.forEach {
                assertEquals(0.0, it.placeInBracketOrc)
                assertEquals(0.0, it.placeInClassOrc)
                assertEquals(false, it.bracketSailedOrc || it.classSailedOrc || it.throwOutOrc || it.throwOutClassOrc || it.nonStarterOrc)
            }
        }
    }

    /** D9 with the 2025→2026 switch: before 2026 a rated class scores as PHRF only, whatever its results say. */
    @Test
    fun d9_orc_fields_are_not_set_before_2026() {
        val standings = series(year = 2025, firstDate = "2025-06-17") {
            repeat(2) { orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00") } }
        }.standings(orcCerts = certs)
        assertEquals(RatingType.PHRF, standings.standings.single().raceClass.ratingType)
        standings.assertBracketTotals("A" to 2, "B" to 4)
        standings.assertBracketTotalsOrc("A" to 0, "B" to 0)
        assertEquals(mapOf("A" to 0, "B" to 0), standings.bracketPlacesOrc())
        assertEquals(listOf(0.0, 0.0), standings.bracketRaceScoresOrc("A"))
    }

    /** D9 / R1: an ORC-only boat is not scored under PHRF, and its row follows the PHRF-placed rows, by ORC place. */
    @Test
    fun d9_orc_only_row_has_no_phrf_fields_set_and_sorts_after_the_phrf_rows() {
        val standings = series {
            repeat(2) { orcRace { orcBoat("A", "slow", "19:00:00"); boat("O", ratingType = RatingType.ORC) finishes "18:50:00" orc "fast" } }
        }.standings(orcCerts = certs)
        val o = standings.row("O")
        assertEquals(RatingType.ORC, o.ratingType)
        assertEquals(0.0, o.totalScoreBracket)
        assertEquals(0.0, o.totalScoreClass)
        assertEquals(0, o.placeInBracket)
        assertEquals(0, o.placeInClass)
        assertEquals(listOf(0.0, 0.0), standings.bracketRaceScores("O"))
        standings.assertBracketTotalsOrc("O" to 2, "A" to 4)   // O corrects to 25 minutes, A to 60
        assertEquals(mapOf("O" to 1, "A" to 2), standings.bracketPlacesOrc())
        assertEquals(mapOf("A" to 1, "O" to 0), standings.bracketPlaces())
        assertEquals(listOf("A", "O"), standings.standings.single().standings.single().standings.map { it.boatSkipper.boat?.name })
    }

    /** D9 / R1: an ORC-only boat is not a PHRF starter, so it does not raise the PHRF DNS score of a boat that stayed home. */
    @Test
    fun d9_orc_only_boat_does_not_count_as_a_phrf_starter() {
        val standings = series {
            orcRace { orcBoat("A", "slow", "19:00:00"); boat("P", rating = 50, ratingType = RatingType.PHRF) finishes "19:10:00"; boat("O", ratingType = RatingType.ORC) finishes "18:50:00" orc "fast" }
            orcRace { orcBoat("A", "slow", "19:00:00"); boat("O", ratingType = RatingType.ORC) finishes "18:50:00" orc "fast" }
        }.standings(orcCerts = certs)
        assertEquals(listOf(2.0, 2.0), standings.bracketRaceScores("P"))   // 2nd, then 1 PHRF starter + 1
    }

    /** D9: a boat that raced PHRF-only before getting a certificate is scored under both, with ORC DNS for the earlier races. */
    @Test
    fun d9_boat_that_switches_to_orc_phrf_mid_series_scores_dns_in_orc_before_the_switch() {
        val standings = series {
            orcRace { orcBoat("A", "slow", "19:00:00"); boat("B", rating = 50, ratingType = RatingType.PHRF) finishes "19:30:00" }
            orcRace { orcBoat("A", "slow", "19:00:00"); orcBoat("B", "fast", "19:30:00") }
        }.standings(orcCerts = certs)
        val b = standings.row("B")
        assertEquals(RatingType.ORC_PHRF, b.ratingType)
        assertEquals(listOf(2.0, 2.0), standings.bracketRaceScores("B"))
        assertEquals(listOf(2.0, 1.0), standings.bracketRaceScoresOrc("B"))   // 1 ORC starter + 1, then 1st
        assertEquals(listOf(true, false), b.raceStandings.map { it.nonStarterOrc })
        assertEquals(listOf(false, false), b.raceStandings.map { it.nonStarter })
    }
}

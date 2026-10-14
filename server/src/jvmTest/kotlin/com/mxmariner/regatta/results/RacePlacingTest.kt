package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.FinishCode.*
import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.data.RatingType
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Race placing rules: GSI 14.1 / 14.2 with the D3, D4 and D7 decisions in
 * docs/explore_unimplemented/test_audit_and_plan.md. Tests the code fails today are ignored with
 * the decision that fixes them.
 */
class RacePlacingTest {

    private fun RaceReport.bracketOrder(): List<String> =
        classReports.single().bracketReport.single().cards.map { it.boatName }

    @Test
    fun ordering_corrected_time_then_hoc_then_code_weight() {
        val report = race(bFactor = 600) {
            phrfClass("Fast") { bracket("I") {
                boat("NSC") code NSC
                boat("HOC2") hoc 2
                boat("DNF") code DNF
                boat("Slow rating fast boat", rating = 150) finishes "19:00:00"  // 650/750 -> 48m
                boat("HOC1") hoc 1
                boat("Fast rating slow boat", rating = 0) finishes "18:50:00"    // 650/600 -> 54m10s
                boat("RET") code RET
                boat("RC") code DNS_RC
            } }
        }.report()
        // timed finishers by corrected time, then HOC by position, then the non-finishers
        assertEquals(listOf("Slow rating fast boat", "Fast rating slow boat", "HOC1", "HOC2"), report.bracketOrder().take(4))
        assertEquals(setOf("DNF", "RET", "NSC", "RC"), report.bracketOrder().drop(4).toSet())
        report.assertBracketScores("Slow rating fast boat" to 1, "Fast rating slow boat" to 2, "HOC1" to 3, "HOC2" to 4)
    }

    /** GSI 14.1.1 / 14.1.3: DNF and RET score finishers + 1, and HOC boats are finishers (D3). */
    @Test
    fun gsi_14_1_1_dnf_and_ret_score_finishers_plus_one() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("B") finishes "19:01:00"
                boat("H") hoc 1
                boat("DNF") code DNF
                boat("RET") code RET
                boat("NSC") code NSC
            } }
        }.report()
        report.assertBracketScores("A" to 1, "B" to 2, "H" to 3, "DNF" to 4, "RET" to 4)
    }

    /** GSI 14.1.4 / D3: NSC scores starters + 1, one worse than the DNF score when every starter but one finished. */
    @Ignore("D3: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun gsi_14_1_4_nsc_scores_starters_plus_one() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("B") finishes "19:01:00"
                boat("DNF") code DNF
                boat("NSC") code NSC
            } }
        }.report()
        report.assertBracketScores("DNF" to 3, "NSC" to 5)
    }

    /** D3: an RC volunteer is not a starter, so it does not raise the NSC score. */
    @Ignore("D3: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun d3_rc_volunteer_is_not_a_starter() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("B") finishes "19:01:00"
                boat("NSC") code NSC
                boat("RC 1") code DNS_RC
                boat("RC 2") code DNS_RC
            } }
        }.report()
        report.assertBracketScores("A" to 1, "B" to 2, "NSC" to 4)
    }

    /** GSI 14.2: HOC boats are numbered by position on the course, after every timed finisher, whatever the input order. */
    @Test
    fun gsi_14_2_hoc_numbering() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("H3") hoc 3
                boat("T2") finishes "19:30:00"
                boat("H1") hoc 1
                boat("T1") finishes "19:00:00"
                boat("H2") hoc 2
            } }
        }.report()
        report.assertBracketScores("T1" to 1, "T2" to 2, "H1" to 3, "H2" to 4, "H3" to 5)
    }

    /** GSI 9.2 / 14.2: the penalised boat scores its finishing place plus the penalty. */
    @Test
    fun gsi_9_2_penalty_adds_to_the_finishing_place() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00" penalty 2
                boat("B") finishes "19:01:00"
                boat("C") finishes "19:02:00"
                boat("D") finishes "19:03:00"
            } }
        }.report()
        report.assertBracketScores("A" to 3)
    }

    /** D4 / RRS 44.3(c): no other boat's score changes, so ties are allowed. */
    @Ignore("D4: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun d4_penalty_leaves_other_boats_unchanged() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00" penalty 2
                boat("B") finishes "19:01:00"
                boat("C") finishes "19:02:00"
                boat("D") finishes "19:03:00"
            } }
        }.report()
        report.assertBracketScores("A" to 3, "B" to 2, "C" to 3, "D" to 4)
    }

    /** D4: a penalty is capped at the DNF score. */
    @Ignore("D4: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun d4_penalty_is_capped_at_dnf() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("B") finishes "19:01:00" penalty 5
                boat("DNF") code DNF
            } }
        }.report()
        report.assertBracketScores("A" to 1, "B" to 3, "DNF" to 3)
    }

    /** D4: several penalties are applied independently against each boat's own unpenalised place. */
    @Ignore("D4: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun d4_several_penalties_apply_independently() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00" penalty 2
                boat("B") finishes "19:01:00" penalty 2
                boat("C") finishes "19:02:00"
                boat("D") finishes "19:03:00"
                boat("E") finishes "19:04:00"
            } }
        }.report()
        report.assertBracketScores("A" to 3, "B" to 4, "C" to 3, "D" to 4, "E" to 5)
    }

    /** D4: a penalty on a non-finisher is ignored; DNF, RET and NSC keep their code score. */
    @Ignore("D4: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun d4_penalty_on_a_non_finisher_is_ignored() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("B") finishes "19:01:00"
                boat("RET") code RET penalty 1
                boat("DNF") code DNF penalty 2
            } }
        }.report()
        report.assertBracketScores("A" to 1, "B" to 2, "RET" to 3, "DNF" to 3)
    }

    /** D4: a penalty on an HOC finisher counts, since HOC is a finishing position (GSI 14.2). */
    @Test
    fun d4_penalty_on_hoc_adds_to_its_position() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("H1") hoc 1 penalty 1
            } }
        }.report()
        report.assertBracketScores("A" to 1, "H1" to 3)
    }

    /** D7: equal corrected times (to the millisecond) share the place, and the next place skips. */
    @Ignore("D7: see docs/explore_unimplemented/test_audit_and_plan.md")
    @Test
    fun d7_equal_corrected_times_share_the_place_and_the_next_skips() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("B") finishes "19:00:00"
                boat("C") finishes "19:00:01"
            } }
        }.report()
        report.assertBracketScores("A" to 1, "B" to 1, "C" to 3)
    }

    @Test
    fun corrected_times_a_millisecond_apart_are_not_tied() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00.001"
                boat("B") finishes "19:00:00.000"
            } }
        }.report()
        report.assertBracketScores("B" to 1, "A" to 2)
    }

    /** D2: starters but no finishers: DNF and RET score 0 + 1 = 1, read literally from 14.1.1. */
    @Test
    fun starters_but_no_finishers_score_one() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") code DNF
                boat("B") code RET
            } }
        }.report()
        report.assertBracketScores("A" to 1, "B" to 1)
    }

    @Test
    fun class_places_span_brackets_and_bracket_places_do_not() {
        val report = race(bFactor = 600) {
            phrfClass("Fast") {
                bracket("I") {
                    boat("I-1", rating = 50) finishes "19:00:00"
                    boat("I-2", rating = 50) finishes "19:02:00"
                    boat("I-DNF", rating = 50) code DNF
                }
                bracket("II") {
                    boat("II-1", rating = 50) finishes "19:01:00"
                    boat("II-2", rating = 50) finishes "19:03:00"
                }
            }
        }.report()
        report.assertBracketScores("I-1" to 1, "I-2" to 2, "I-DNF" to 3, "II-1" to 1, "II-2" to 2)
        report.assertClassScores("I-1" to 1, "II-1" to 2, "I-2" to 3, "II-2" to 4, "I-DNF" to 5)
    }

    @Test
    fun orc_places_are_among_orc_entries_only_and_phrf_places_among_all() {
        val certs = mapOf(
            "slow" to OrcCertificate(refNo = "slow", fiveBandWlLowMedTot = 1.0),
            "fast" to OrcCertificate(refNo = "fast", fiveBandWlLowMedTot = 0.5),
        )
        val report = race(date = "2026-06-17", bFactor = 600) {
            orcPhrfClass("Fast") { bracket("I") {
                boat("Both A", rating = 50) finishes "19:00:00" orc "slow"   // PHRF 1h, ORC 1h
                boat("Both B", rating = 50) finishes "19:30:00" orc "fast"   // PHRF 1h30, ORC 45m
                boat("PHRF only", rating = 50, ratingType = RatingType.PHRF) finishes "19:10:00"
            } }
        }.report(certs)
        report.assertBracketScores("Both A" to 1, "PHRF only" to 2, "Both B" to 3)
        assertEquals(mapOf("Both A" to 2.0, "Both B" to 1.0, "PHRF only" to 0.0), report.bracketScoresOrc())
        assertEquals(mapOf("Both A" to 2.0, "Both B" to 1.0, "PHRF only" to 0.0), report.classScoresOrc())
    }

    @Test
    fun orphan_results_land_in_the_incorrectly_classed_bucket() {
        val report = race {
            cruisingClass("WS") { bracket("All") {
                boat("A") finishes "19:00:00"
                boat("Lost 2").orphan() finishes "19:30:00"
                boat("Lost 1").orphan() finishes "19:20:00"
            } }
        }.report()
        val orphans = report.classReports.single { it.raceClass.name == "Incorrectly classed" }
        assertEquals("Invalid result records", orphans.bracketReport.single().bracket.name)
        // an orphan has no class start, so no elapsed time and no meaningful place
        assertEquals(setOf("Lost 1", "Lost 2"), orphans.bracketReport.single().cards.map { it.boatName }.toSet())
        assertEquals(listOf(null, null), orphans.bracketReport.single().cards.map { it.elapsedTime })
        assertEquals(listOf("A"), report.classReports.single { it.raceClass.name == "WS" }.bracketReport.single().cards.map { it.boatName })
        report.assertBracketScores("A" to 1)
    }
}

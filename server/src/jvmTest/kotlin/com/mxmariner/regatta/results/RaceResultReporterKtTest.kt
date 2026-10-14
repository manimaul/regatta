package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.FinishCode
import kotlin.test.Test
import kotlin.test.assertEquals


class RaceResultReporterKtTest {

    /**
     * Ordering of finish, HOC and non-finishers in one bracket. DNS has no race record and is
     * scored in the series standings, so it is not part of a race report.
     */
    @Test
    fun testHoc() {
        val report = race {
            cruisingClass("Windseekers") {
                bracket("A") {
                    boat("DNF place 4a") code FinishCode.DNF
                    boat("HOC1 place 2") hoc 1
                    boat("RET place 4") code FinishCode.RET
                    boat("DNF place 4b") code FinishCode.DNF
                    boat("HOC2 place 3") hoc 2
                    boat("finish place 1") finishes "20:30:00"
                }
            }
        }.report()

        report.assertClassScores(
            "finish place 1" to 1,
            "HOC1 place 2" to 2,
            "HOC2 place 3" to 3,
            "DNF place 4a" to 4,
            "DNF place 4b" to 4,
            "RET place 4" to 4,
        )
        report.assertBracketScores(
            "finish place 1" to 1,
            "HOC1 place 2" to 2,
            "HOC2 place 3" to 3,
            "DNF place 4a" to 4,
            "DNF place 4b" to 4,
            "RET place 4" to 4,
        )
    }

    /** GSI 14.1.2: a boat with no record for a race scores the number of starters plus one. */
    @Test
    fun gsi_14_1_2_dns_scores_starters_plus_one() {
        val standings = series {
            race {
                phrfClass("Fast") { bracket("I") {
                    boat("A", rating = 48) finishes "20:00:00"
                    boat("B", rating = 72) finishes "20:10:00"
                    boat("C", rating = 90) code FinishCode.DNF
                } }
            }
            race {
                phrfClass("Fast") { bracket("I") {
                    boat("A", rating = 48) finishes "20:00:00"
                    boat("B", rating = 72) finishes "20:10:00"
                } }
            }
        }.standings()

        assertEquals(2, standings.races.size)
        assertEquals(listOf(3.0, 3.0), standings.bracketRaceScores("C"))
        standings.assertBracketTotals("A" to 2, "B" to 4, "C" to 6)
        standings.assertClassTotals("A" to 2, "B" to 4, "C" to 6)
    }

    /** The series builder's rcVolunteer adds a DNS_RC record in the boat's usual class and bracket. */
    @Test
    fun rcVolunteerEntersTheBoatAsDnsRc() {
        val fixture = series {
            race { phrfClass("Fast") { bracket("I") { boat("A", rating = 48) finishes "20:00:00" } } }
            race { phrfClass("Fast") { bracket("I") { boat("B", rating = 72) finishes "20:00:00" } } }
            rcVolunteer("A", race = 2)
        }
        val entry = fixture.races[1].results.first { it.boatSkipper.boat?.name == "A" }
        assertEquals(FinishCode.DNS_RC, entry.result.finishCode)
        assertEquals(fixture.races[0].results.first().bracket.id, entry.bracket.id)
        assertEquals(FinishCode.DNS_RC, fixture.standings().row("A").raceStandings[1].finishCode)
    }
}

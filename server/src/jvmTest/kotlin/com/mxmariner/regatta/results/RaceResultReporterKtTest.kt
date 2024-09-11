package com.mxmariner.regatta.results

import com.mxmariner.regatta.data.RaceReportCard
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals


class RaceResultReporterKtTest {

    @Test
    fun testHoc() {
        val start = Instant.fromEpochMilliseconds(1000)
        val finish = Instant.fromEpochMilliseconds(10000)
        val cards = listOf(
            RaceReportCard(boatName = "DNF place 4a", startTime = start), //DNF
            RaceReportCard(boatName = "DNS place 5a", startTime = null), //DNS
            RaceReportCard(boatName = "DNF place 4b", startTime = start), //DNF
            RaceReportCard(boatName = "HOC1 place 2", startTime = start, hocPosition = 1), //HOC1
            RaceReportCard(boatName = "DNS place 5b", startTime = null), //DNS
            RaceReportCard(boatName = "HOC2 place 3", startTime = start, hocPosition = 2), //HOC2
            RaceReportCard(
                boatName = "finish place 1",
                startTime = start,
                finishTime = finish,
                correctedTime = finish.minus(start)
            ), //finish
        ).sortedWith(cardCompare)

        val results = cards.place { i, raceReportCard ->
            raceReportCard.placeOverall = i
        }.associate { it.boatName to it.placeOverall }

        assertEquals(1, results["finish place 1"])
        assertEquals(2, results["HOC1 place 2"])
        assertEquals(3, results["HOC2 place 3"])
        assertEquals(4, results["DNF place 4a"])
        assertEquals(4, results["DNF place 4b"])
        assertEquals(5, results["DNS place 5a"])
        assertEquals(5, results["DNS place 5b"])
    }


}



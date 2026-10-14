package com.mxmariner.regatta.golden

import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.data.StandingsSeries
import com.mxmariner.regatta.db.RegattaDatabase
import com.mxmariner.regatta.db.currentDbVersion
import com.mxmariner.regatta.results.RaceResultReporter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterisation of the scoring code against the production snapshot: every race report and
 * every series standings are serialised and compared to the approved files. The approved files
 * record current behavior, known deviations included; `database/audit_scoring.py` lists those.
 *
 * Run with `./gradlew :server:jvmGoldenTest` (needs podman or docker), and
 * `-Dgolden.approve=true` to re-approve on purpose. Excluded from `:server:jvmTest`.
 */
class GoldenTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun schemaVersionIsCurrentAfterStartup() {
        GoldenDb.ensureStarted()
        val version = transaction {
            exec("SELECT cast(value as INT) FROM metadata WHERE name = 'version';") { rs ->
                check(rs.next()) { "no metadata version row" }
                rs.getInt(1)
            }
        }
        assertEquals(currentDbVersion, version)
    }

    @Test
    fun raceReportsMatchApproved() = runBlocking {
        GoldenDb.ensureStarted()
        val approvals = Approvals()
        val years = RegattaDatabase.allYears().map { it.toInt() }.sorted()
        assertTrue(years.isNotEmpty(), "the snapshot has no race years")
        var count = 0
        years.forEach { year ->
            RegattaDatabase.allRaces(year).forEach { schedule ->
                val id = schedule.race.id
                val report = requireNotNull(RaceResultReporter.getReport(id)) { "no report for race $id" }
                approvals.check("race-$id", json.encodeToString(RaceReport.serializer(), report))
                count++
            }
        }
        println("checked $count race reports for years $years")
        approvals.assertAllApproved()
    }

    @Test
    fun seriesStandingsMatchApproved() = runBlocking {
        GoldenDb.ensureStarted()
        val approvals = Approvals()
        val years = RegattaDatabase.allYears().map { it.toInt() }.sorted()
        var count = 0
        RegattaDatabase.allSeries().forEach { series ->
            years.forEach { year ->
                val standings = RaceResultReporter.getStandingsReport(series.id, year, now = GoldenDb.dumpInstant)
                // a series with no races in a year has nothing to characterise
                if (standings != null && standings.races.isNotEmpty()) {
                    approvals.check("standings-${series.id}-$year", json.encodeToString(StandingsSeries.serializer(), standings))
                    count++
                }
            }
        }
        println("checked $count series standings for years $years")
        approvals.assertAllApproved()
    }
}

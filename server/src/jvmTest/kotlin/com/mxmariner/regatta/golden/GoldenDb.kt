package com.mxmariner.regatta.golden

import com.mxmariner.regatta.db.RegattaDatabase
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import kotlin.time.Instant

/**
 * A throwaway Postgres loaded with the scrubbed production snapshot, for the golden tests.
 * See docs/explore_implemented/test_audit_and_plan.md §5.
 *
 * Started once per JVM on first use: RegattaDatabase is a process-wide singleton, so all golden
 * tests share this one database. Needs a container runtime; run through `:server:jvmGoldenTest`,
 * which sets DOCKER_HOST for podman.
 */
object GoldenDb {
    /** Classpath resource holding the scrubbed dump. */
    const val DUMP = "golden/regatta-2026-10-13.sql"

    /** The instant the snapshot was taken: 2026-10-13 19:10:11 Pacific. Standings are computed as of this time. */
    val dumpInstant: Instant = Instant.parse("2026-10-14T02:10:11Z")

    private const val USER = "regatta_admin"
    private const val DATABASE = "regatta"

    private val container: PostgreSQLContainer<Nothing> by lazy { start() }

    /** Starts the container, loads the dump and initialises [RegattaDatabase]. Idempotent. */
    fun ensureStarted() {
        container
    }

    private fun start(): PostgreSQLContainer<Nothing> {
        val container = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-bookworm")).apply {
            withUsername(USER)
            withPassword("mysecretpassword")
            withDatabaseName(DATABASE)
        }
        container.start()
        container.copyFileToContainer(MountableFile.forClasspathResource(DUMP), "/dump.sql")
        // JDBC can't run the dump: it uses COPY ... FROM stdin. psql inside the container can, and
        // the official image trusts local connections, so no password is needed.
        val load = container.execInContainer(
            "psql", "-v", "ON_ERROR_STOP=1", "-U", USER, "-d", DATABASE, "-f", "/dump.sql"
        )
        check(load.exitCode == 0) { "loading $DUMP failed (exit ${load.exitCode}):\n${load.stderr}" }

        System.setProperty("jdbcurl", container.jdbcUrl)
        System.setProperty("user", container.username)
        System.setProperty("password", container.password)
        // the production startup path: versioned migrations, then SchemaUtils
        RegattaDatabase.init()
        return container
    }
}

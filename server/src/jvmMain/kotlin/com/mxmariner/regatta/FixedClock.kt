package com.mxmariner.regatta

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Test-only fixed clock. When the JVM system property `regatta.now` holds an ISO-8601 instant
 * (`-Dregatta.now=2026-10-14T02:10:11Z`), [now] returns it instead of the wall clock, so series
 * standings (`NotYetSailed` vs `Cancelled`) and `LoginResponse.expires` are stable across runs.
 * Absent, [now] is `Clock.System.now()` and nothing changes. The only place the property is read.
 */
object FixedClock {
    const val PROPERTY = "regatta.now"

    fun now(): Instant = parse(System.getProperty(PROPERTY)) ?: Clock.System.now()

    /** The fixed instant the property value names, or null when it is absent or blank. */
    fun parse(value: String?): Instant? = value?.trim()?.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }
}

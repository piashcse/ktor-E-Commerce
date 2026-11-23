package com.piashcse.utils.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Single time source for the application. Tests can swap [clock] for a fixed
 * clock; production always uses the system UTC clock.
 */
object AppClock {
    @Volatile
    var clock: Clock = Clock.systemUTC()

    fun now(): Instant = clock.instant()

    fun nowUtc(): LocalDateTime = LocalDateTime.ofInstant(now(), ZoneOffset.UTC)
}

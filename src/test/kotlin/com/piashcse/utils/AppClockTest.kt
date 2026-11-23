package com.piashcse.utils

import com.piashcse.utils.common.AppClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class AppClockTest {
    @Test
    fun `current time and utc timestamp follow the injected clock`() {
        val original = AppClock.clock
        try {
            AppClock.clock = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC)
            assertEquals(Instant.parse("2026-01-02T03:04:05Z"), AppClock.now())
            assertEquals("2026-01-02T03:04:05", AppClock.nowUtc().toString())
        } finally {
            AppClock.clock = original
        }
    }
}

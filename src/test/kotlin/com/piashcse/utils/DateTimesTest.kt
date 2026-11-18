package com.piashcse.utils

import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.DateTimes.toUtcStringOrNull
import com.piashcse.utils.validator.ValidationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DateTimesTest {
    @Test
    fun `utc timestamps use an explicit offset`() {
        val value = LocalDateTime.of(2024, 5, 6, 12, 30, 15)
        assertEquals("2024-05-06T12:30:15Z", value.toUtcString())
        assertEquals(ZoneOffset.UTC, DateTimes.FORMAT.parse("2024-05-06T12:30:15Z").query(java.time.temporal.TemporalQueries.offset()))
    }

    @Test
    fun `nullable timestamps stay null`() {
        assertNull((null as LocalDateTime?).toUtcStringOrNull())
        assertEquals("2024-05-06T12:30:15Z", LocalDateTime.of(2024, 5, 6, 12, 30, 15).toUtcStringOrNull())
    }

    @Test
    fun `parses local date time`() {
        val value = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        assertEquals(value, DateTimes.parseFlexible("2026-01-02T03:04:05", "at"))
    }

    @Test
    fun `parses date at start or end of day`() {
        assertEquals(LocalDate.of(2026, 1, 2).atStartOfDay(), DateTimes.parseFlexible("2026-01-02", "at"))
        assertEquals(
            LocalDate.of(2026, 1, 2).atTime(LocalTime.MAX),
            DateTimes.parseFlexible("2026-01-02", "at", endOfDay = true),
        )
    }

    @Test
    fun `normalizes offset aware input to UTC`() {
        assertEquals(
            LocalDateTime.of(2026, 1, 2, 3, 4, 5),
            DateTimes.parseFlexible("2026-01-02T05:04:05+02:00", "at"),
        )
    }

    @Test
    fun `rejects malformed values with the field name`() {
        val error = assertFailsWith<ValidationException> { DateTimes.parseFlexible("nope", "startDate") }
        assertEquals("Invalid startDate: nope", error.message)
    }

    @Test
    fun `optional parser ignores null and blank`() {
        assertNull(DateTimes.parseFlexibleOrNull(null, "at"))
        assertNull(DateTimes.parseFlexibleOrNull("  ", "at"))
        assertEquals(
            LocalDateTime.of(2026, 1, 2, 3, 4, 5),
            DateTimes.parseFlexibleOrNull("2026-01-02T03:04:05", "at"),
        )
    }

    @Test
    fun `parses plain local dates`() {
        assertEquals(LocalDate.of(2026, 1, 2), DateTimes.parseLocalDate("2026-01-02", "birthDate"))
        assertFailsWith<ValidationException> { DateTimes.parseLocalDate("02/01/2026", "birthDate") }
    }
}

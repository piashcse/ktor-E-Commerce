package com.piashcse.utils

import com.piashcse.utils.common.DateTimes
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.DateTimes.toUtcStringOrNull
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
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
}

package com.piashcse.utils

import com.piashcse.utils.common.CursorCodec
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CursorPageTest {
    @Test
    fun `cursor round-trips createdAt and id`() {
        val createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val cursor = CursorCodec.encode(createdAt, "order-123")
        val decoded = CursorCodec.decode(cursor)
        assertEquals(createdAt, decoded?.createdAt)
        assertEquals("order-123", decoded?.id)
    }

    @Test
    fun `invalid cursor is rejected`() {
        assertNull(CursorCodec.decode("not-a-cursor"))
    }
}

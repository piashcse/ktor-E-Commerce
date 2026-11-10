package com.piashcse.utils

import com.piashcse.utils.common.PaginationMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaginationMetadataTest {
    @Test
    fun `first page computes totals and next page`() {
        val metadata = PaginationMetadata(totalCount = 25, limit = 10, offset = 0)
        assertEquals(1, metadata.page)
        assertEquals(10, metadata.perPage)
        assertEquals(3, metadata.totalPages)
        assertTrue(metadata.hasNext)
        assertFalse(metadata.hasPrev)
        assertEquals(2, metadata.nextPage)
        assertNull(metadata.prevPage)
    }

    @Test
    fun `last page computes previous page`() {
        val metadata = PaginationMetadata(totalCount = 25, limit = 10, offset = 20)
        assertEquals(3, metadata.page)
        assertFalse(metadata.hasNext)
        assertTrue(metadata.hasPrev)
        assertEquals(2, metadata.prevPage)
        assertNull(metadata.nextPage)
    }

    @Test
    fun `zero limit does not divide by zero`() {
        val metadata = PaginationMetadata(totalCount = 5, limit = 0, offset = 0)
        assertEquals(0, metadata.totalPages)
        assertFalse(metadata.hasNext)
        assertFalse(metadata.hasPrev)
    }

    @Test
    fun `offset past the end has no next page`() {
        val metadata = PaginationMetadata(totalCount = 5, limit = 10, offset = 10)
        assertFalse(metadata.hasNext)
        assertTrue(metadata.hasPrev)
    }
}

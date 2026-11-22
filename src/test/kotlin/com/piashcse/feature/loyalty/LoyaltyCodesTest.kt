package com.piashcse.feature.loyalty

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LoyaltyCodesTest {
    @Test
    fun `codes use the unambiguous alphabet and requested length`() {
        val code = LoyaltyCodes.random(16)
        assertEquals(16, code.length)
        assertTrue(code.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" })
        assertNotEquals(code, LoyaltyCodes.random(16))
    }
}

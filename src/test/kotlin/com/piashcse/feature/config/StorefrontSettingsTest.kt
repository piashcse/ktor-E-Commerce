package com.piashcse.feature.config

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorefrontSettingsTest {
    @Test
    fun `free shipping is reached at the threshold`() {
        val threshold = StorefrontSettings.freeShippingThreshold
        assertTrue(StorefrontSettings.freeShippingReached(threshold))
        assertTrue(StorefrontSettings.freeShippingReached(threshold.add(BigDecimal("0.01"))))
    }

    @Test
    fun `free shipping is not reached just below the threshold`() {
        val below = StorefrontSettings.freeShippingThreshold.subtract(BigDecimal("0.01"))
        assertFalse(StorefrontSettings.freeShippingReached(below))
        assertEquals(BigDecimal("0.01"), StorefrontSettings.freeShippingRemaining(below))
    }

    @Test
    fun `remaining never goes negative`() {
        val above = StorefrontSettings.freeShippingThreshold.add(BigDecimal("100"))
        assertEquals(BigDecimal.ZERO.setScale(2), StorefrontSettings.freeShippingRemaining(above))
    }
}

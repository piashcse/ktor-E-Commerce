package com.piashcse.feature.coupon

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.Message
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CouponRulesTest {
    private val now = LocalDateTime.of(2026, 1, 1, 12, 0)

    private fun coupon(
        type: CouponDiscountType = CouponDiscountType.PERCENTAGE,
        value: String = "10",
        minOrder: String = "0",
        maxDiscount: String? = null,
        start: LocalDateTime = now.minusDays(1),
        end: LocalDateTime = now.plusDays(1),
        usageLimit: Int? = null,
        usageCount: Int = 0,
        perUserLimit: Int = 1,
        shopId: String? = null,
        categoryId: String? = null,
        active: Boolean = true,
    ) = CouponSnapshot(
        discountType = type,
        discountValue = BigDecimal(value),
        minOrderAmount = BigDecimal(minOrder),
        maxDiscountAmount = maxDiscount?.let(::BigDecimal),
        startDate = start,
        endDate = end,
        usageLimit = usageLimit,
        usageCount = usageCount,
        perUserLimit = perUserLimit,
        shopId = shopId,
        categoryId = categoryId,
        isActive = active,
    )

    private fun line(
        price: String,
        quantity: Int = 1,
        shopId: String? = "shop-1",
        categoryId: String = "cat-1",
    ) = CouponLine(shopId, categoryId, BigDecimal(price), quantity)

    @Test
    fun `normalize trims and uppercases`() {
        assertEquals("SAVE10", CouponRules.normalize("  save10 "))
    }

    @Test
    fun `scope matching accepts unscoped and rejects other shops`() {
        assertEquals(true, CouponRules.matches(coupon(), "shop-1", "cat-1"))
        assertEquals(false, CouponRules.matches(coupon(shopId = "shop-2"), "shop-1", "cat-1"))
        assertEquals(true, CouponRules.matches(coupon(shopId = "shop-1"), "shop-1", "cat-9"))
        assertEquals(false, CouponRules.matches(coupon(categoryId = "cat-2"), "shop-1", "cat-1"))
    }

    @Test
    fun `eligible subtotal only includes matching lines`() {
        val scoped = coupon(shopId = "shop-1")
        val eligible =
            CouponRules.eligibleSubtotalUsd(
                scoped,
                listOf(line("10.00", 2), line("50.00", shopId = "shop-2")),
            )
        assertEquals(BigDecimal("20.00"), eligible)
    }

    @Test
    fun `percentage discount is capped`() {
        val snapshot = coupon(value = "50", maxDiscount = "15")
        assertEquals(BigDecimal("15.00"), CouponRules.discountUsd(snapshot, BigDecimal("100.00")))
    }

    @Test
    fun `fixed discount never exceeds eligible amount`() {
        val snapshot = coupon(type = CouponDiscountType.FIXED, value = "20")
        assertEquals(BigDecimal("5.00"), CouponRules.discountUsd(snapshot, BigDecimal("5.00")))
    }

    @Test
    fun `rejection covers inactive expired usage and per-user limits`() {
        assertEquals(Message.Orders.INVALID_COUPON, CouponRules.rejection(coupon(active = false), now))
        assertEquals(Message.Orders.COUPON_EXPIRED, CouponRules.rejection(coupon(start = now.plusDays(1)), now))
        assertEquals(
            Message.Orders.COUPON_LIMIT_REACHED,
            CouponRules.rejection(coupon(usageLimit = 1, usageCount = 1), now),
        )
        assertEquals(
            Message.Orders.COUPON_LIMIT_REACHED,
            CouponRules.rejection(coupon(perUserLimit = 1), now, usedByUser = 1),
        )
        assertNull(CouponRules.rejection(coupon(), now))
    }

    @Test
    fun `eligibility error reports empty scope and minimum spend`() {
        assertEquals(
            "Coupon is not valid for the products in this order",
            CouponRules.eligibilityError(coupon(), BigDecimal.ZERO),
        )
        assertEquals(
            Message.Orders.couponMinOrderAmount("25.00"),
            CouponRules.eligibilityError(coupon(minOrder = "25"), BigDecimal("10.00")),
        )
        assertNull(CouponRules.eligibilityError(coupon(minOrder = "25"), BigDecimal("25.00")))
    }
}

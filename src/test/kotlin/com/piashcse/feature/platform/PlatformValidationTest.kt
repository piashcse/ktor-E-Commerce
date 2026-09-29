package com.piashcse.feature.platform

import com.piashcse.constants.CouponDiscountType
import com.piashcse.constants.PolicyType
import com.piashcse.model.request.CouponRequest
import com.piashcse.model.request.CreatePolicyRequest
import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.request.PolicyConsentRequest
import com.piashcse.model.request.ShippingMethodRequest
import com.piashcse.model.request.UserProfileRequest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlatformValidationTest {
    // ─── CouponRequest ────────────────────────────────────────────────

    @Test
    fun `coupon valid request constructs`() {
        val req =
            CouponRequest(
                code = "SAVE10",
                discountType = CouponDiscountType.PERCENTAGE,
                discountValue = BigDecimal("10"),
                minOrderAmount = BigDecimal("50"),
                startDate = "2024-01-01",
                endDate = "2025-01-01",
            )
        assertEquals("SAVE10", req.code)
    }

    @Test
    fun `coupon blank code fails validation`() {
        assertFailsWith<Exception> {
            CouponRequest(
                code = "",
                discountType = CouponDiscountType.FIXED,
                discountValue = BigDecimal("5"),
                startDate = "2024-01-01",
                endDate = "2025-01-01",
            )
        }
    }

    @Test
    fun `coupon zero discount fails validation`() {
        assertFailsWith<Exception> {
            CouponRequest(
                code = "SAVE10",
                discountType = CouponDiscountType.FIXED,
                discountValue = BigDecimal.ZERO,
                startDate = "2024-01-01",
                endDate = "2025-01-01",
            )
        }
    }

    @Test
    fun `coupon percentage over 100 fails`() {
        assertFailsWith<IllegalArgumentException> {
            CouponRequest(
                code = "SAVE10",
                discountType = CouponDiscountType.PERCENTAGE,
                discountValue = BigDecimal("150"),
                startDate = "2024-01-01",
                endDate = "2025-01-01",
            )
        }
    }

    @Test
    fun `coupon negative min order fails`() {
        assertFailsWith<IllegalArgumentException> {
            CouponRequest(
                code = "SAVE10",
                discountType = CouponDiscountType.FIXED,
                discountValue = BigDecimal("5"),
                minOrderAmount = BigDecimal("-1"),
                startDate = "2024-01-01",
                endDate = "2025-01-01",
            )
        }
    }

    @Test
    fun `coupon start after end fails`() {
        assertFailsWith<IllegalArgumentException> {
            CouponRequest(
                code = "SAVE10",
                discountType = CouponDiscountType.FIXED,
                discountValue = BigDecimal("5"),
                startDate = "2025-01-01",
                endDate = "2024-01-01",
            )
        }
    }

    @Test
    fun `coupon zero usage limit fails`() {
        assertFailsWith<IllegalArgumentException> {
            CouponRequest(
                code = "SAVE10",
                discountType = CouponDiscountType.FIXED,
                discountValue = BigDecimal("5"),
                startDate = "2024-01-01",
                endDate = "2025-01-01",
                usageLimit = 0,
            )
        }
    }

    // ─── InventoryRequest ─────────────────────────────────────────────

    @Test
    fun `inventory valid request constructs`() {
        val req = InventoryRequest(productId = "p-1", shopId = "s-1", stockQuantity = 10)
        assertEquals("p-1", req.productId)
    }

    @Test
    fun `inventory blank product id fails`() {
        assertFailsWith<Exception> {
            InventoryRequest(productId = "", shopId = "s-1", stockQuantity = 10)
        }
    }

    @Test
    fun `inventory negative stock fails`() {
        assertFailsWith<Exception> {
            InventoryRequest(productId = "p-1", shopId = "s-1", stockQuantity = -1)
        }
    }

    @Test
    fun `inventory negative reserved fails`() {
        assertFailsWith<Exception> {
            InventoryRequest(productId = "p-1", shopId = "s-1", stockQuantity = 10, reservedQuantity = -2)
        }
    }

    // ─── ShippingMethodRequest ────────────────────────────────────────

    @Test
    fun `shipping method valid request constructs`() {
        val req = ShippingMethodRequest(name = "Express", type = "EXPRESS", price = BigDecimal("9.99"), deliveryTime = "2-3 days")
        assertEquals("Express", req.name)
    }

    @Test
    fun `shipping method blank name fails`() {
        assertFailsWith<Exception> {
            ShippingMethodRequest(name = "", type = null, price = BigDecimal("9.99"), deliveryTime = null)
        }
    }

    @Test
    fun `shipping method zero price fails`() {
        assertFailsWith<Exception> {
            ShippingMethodRequest(name = "Express", type = null, price = BigDecimal.ZERO, deliveryTime = null)
        }
    }

    // ─── CreatePolicyRequest ──────────────────────────────────────────

    @Test
    fun `policy valid request constructs`() {
        val req =
            CreatePolicyRequest(
                title = "Privacy Policy",
                type = PolicyType.PRIVACY_POLICY,
                content = "body",
                version = "1.0",
                effectiveDate = "2024-01-01",
            )
        assertEquals(PolicyType.PRIVACY_POLICY, req.type)
    }

    @Test
    fun `policy blank title fails`() {
        assertFailsWith<Exception> {
            CreatePolicyRequest(
                title = "",
                type = PolicyType.PRIVACY_POLICY,
                content = "body",
                version = "1.0",
                effectiveDate = "2024-01-01",
            )
        }
    }

    @Test
    fun `policy blank content fails`() {
        assertFailsWith<Exception> {
            CreatePolicyRequest(
                title = "T",
                type = PolicyType.PRIVACY_POLICY,
                content = "",
                version = "1.0",
                effectiveDate = "2024-01-01",
            )
        }
    }

    // ─── PolicyConsentRequest ─────────────────────────────────────────

    @Test
    fun `consent valid request constructs`() {
        val req = PolicyConsentRequest(policyId = "pol-1")
        assertEquals("pol-1", req.policyId)
    }

    @Test
    fun `consent blank policy id fails`() {
        assertFailsWith<Exception> {
            PolicyConsentRequest(policyId = "")
        }
    }

    // ─── UserProfileRequest ───────────────────────────────────────────

    @Test
    fun `profile request accepts nullable fields`() {
        val req =
            UserProfileRequest(
                firstName = "John",
                lastName = null,
                mobile = null,
                faxNumber = null,
                streetAddress = null,
                city = null,
                identificationType = null,
                identificationNo = null,
                occupation = null,
                postCode = null,
                gender = null,
            )
        assertEquals("John", req.firstName)
        assertNull(req.lastName)
    }
}

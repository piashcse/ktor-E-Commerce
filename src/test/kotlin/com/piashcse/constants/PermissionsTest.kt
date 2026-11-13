package com.piashcse.constants

import com.piashcse.model.request.JwtTokenRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionsTest {
    @Test
    fun `role defaults grant the expected permissions`() {
        assertTrue(Permission.FEATURE_FLAG_MANAGE in UserType.SUPER_ADMIN.defaultPermissions())
        assertFalse(Permission.FEATURE_FLAG_MANAGE in UserType.ADMIN.defaultPermissions())
        assertTrue(Permission.ORDER_MANAGE in UserType.SELLER.defaultPermissions())
        assertTrue(Permission.API_KEY_MANAGE in UserType.CUSTOMER.defaultPermissions())
        assertFalse(Permission.PAYOUT_MANAGE in UserType.CUSTOMER.defaultPermissions())
    }

    @Test
    fun `jwt principal uses role defaults`() {
        val customer = JwtTokenRequest("u-1", "c@example.com", "CUSTOMER")
        assertTrue(customer.hasPermission(Permission.API_KEY_MANAGE))
        assertFalse(customer.hasPermission(Permission.PAYOUT_MANAGE))
    }

    @Test
    fun `api key principal is restricted to explicit scopes`() {
        val scoped =
            JwtTokenRequest(
                "u-1",
                "seller@example.com",
                "SELLER",
                scopes = setOf(Permission.ORDER_MANAGE.name),
                isApiKey = true,
            )
        assertTrue(scoped.hasPermission(Permission.ORDER_MANAGE))
        assertFalse(scoped.hasPermission(Permission.PAYOUT_MANAGE))
    }
}

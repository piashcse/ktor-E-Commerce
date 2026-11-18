package com.piashcse.feature.rbac

import com.piashcse.constants.Permission
import com.piashcse.model.request.JwtTokenRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RbacPrincipalTest {
    @Test
    fun `permission codes are stable and parse both forms`() {
        assertEquals("user:manage", Permission.USER_MANAGE.code)
        assertEquals("dashboard:read", Permission.DASHBOARD_VIEW.code)
        assertEquals(Permission.USER_MANAGE, Permission.fromString("user:manage"))
        assertEquals(Permission.USER_MANAGE, Permission.fromString("USER_MANAGE"))
    }

    @Test
    fun `resolved permissions replace legacy role defaults`() {
        val principal =
            JwtTokenRequest(
                userId = "u-1",
                email = "auditor@example.com",
                userType = "ADMIN",
                permissions = setOf("audit:read", "dashboard:read"),
            )
        assertTrue(principal.hasPermission(Permission.AUDIT_VIEW))
        assertTrue(principal.hasPermission(Permission.DASHBOARD_VIEW))
        assertFalse(principal.hasPermission(Permission.USER_MANAGE))
    }

    @Test
    fun `missing snapshot keeps legacy fallback`() {
        val principal = JwtTokenRequest(userId = "u-1", email = "admin@example.com", userType = "ADMIN")
        assertTrue(principal.hasPermission(Permission.USER_MANAGE))
    }

    @Test
    fun `api keys accept legacy enum names and codes`() {
        val principal =
            JwtTokenRequest(
                userId = "u-1",
                email = "seller@example.com",
                userType = "SELLER",
                scopes = setOf("ORDER_MANAGE"),
                isApiKey = true,
            )
        assertTrue(principal.hasPermission(Permission.ORDER_MANAGE))
        assertFalse(principal.hasPermission(Permission.USER_MANAGE))
    }
}

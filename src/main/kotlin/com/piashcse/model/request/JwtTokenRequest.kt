package com.piashcse.model.request

import com.piashcse.constants.Permission
import com.piashcse.constants.UserType
import com.piashcse.constants.defaultPermissions
import io.ktor.server.auth.*

data class JwtTokenRequest(
    val userId: String,
    val email: String,
    val userType: String,
    /**
     * Effective permission codes resolved from the database. `null` keeps the
     * legacy role-default fallback for callers that construct a principal
     * directly (tests, older code paths); an empty set means "no permissions".
     */
    val permissions: Set<String>? = null,
    /** Authorization snapshot version; cache keys must include it. */
    val authzVersion: Int = 0,
    /** API-key scopes; empty means no scoped access (never role defaults). */
    val scopes: Set<String> = emptySet(),
    /** Distinguishes API-key principals from JWT sessions. */
    val isApiKey: Boolean = false,
) : Principal {
    /**
     * Check if current user has access to a specific role (with hierarchy)
     */
    fun hasAccessTo(role: UserType): Boolean {
        val currentUserType =
            try {
                UserType.valueOf(this.userType.uppercase())
            } catch (e: IllegalArgumentException) {
                return false
            }
        return currentUserType.hasAccessTo(role)
    }

    /**
     * Check if current user has specific role
     */
    fun hasRole(role: UserType): Boolean {
        return try {
            UserType.valueOf(this.userType.uppercase()) == role
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Get current user type
     */
    fun getUserType(): UserType? {
        return try {
            UserType.valueOf(this.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Check if user is super admin
     */
    fun isSuperAdmin(): Boolean = getUserType()?.isSuperAdmin == true

    /**
     * Check if user is admin
     */
    fun isAdmin(): Boolean = getUserType()?.isAdminOrHigher == true

    /**
     * Check if user is seller
     */
    fun isSeller(): Boolean = getUserType()?.isSellerOrHigher == true

    /**
     * Check if user is customer
     */
    fun isCustomer(): Boolean = getUserType()?.isCustomerOrHigher == true

    /**
     * Check if the principal has a specific permission. API-key principals are
     * restricted to their explicit scopes; JWT sessions use the resolved
     * effective permission set, falling back to role defaults only when no
     * snapshot was supplied.
     */
    fun hasPermission(permission: Permission): Boolean {
        val code = permission.code
        if (isApiKey) {
            // API keys historically stored enum names; accept both forms.
            return permission.name in scopes || code in scopes
        }
        permissions?.let { return code in it || permission.name in it }
        val role = getUserType() ?: return false
        return permission in role.defaultPermissions()
    }
}

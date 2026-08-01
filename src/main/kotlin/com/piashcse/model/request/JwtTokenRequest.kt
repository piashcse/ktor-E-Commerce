package com.piashcse.model.request

import com.piashcse.constants.UserType
import io.ktor.server.auth.*

data class JwtTokenRequest(val userId: String, val email: String, val userType: String) : Principal {
    /**
     * Check if current user has access to a specific role (with hierarchy)
     */
    fun hasAccessTo(role: UserType): Boolean = getUserType()?.hasAccessTo(role) ?: false

    /**
     * Check if current user has specific role
     */
    fun hasRole(role: UserType): Boolean = getUserType() == role

    /**
     * Get current user type
     */
    fun getUserType(): UserType? = UserType.fromString(userType)

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
     * Check if user has a specific permission
     */
}

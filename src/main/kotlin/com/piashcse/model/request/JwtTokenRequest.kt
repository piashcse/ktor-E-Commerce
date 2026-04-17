package com.piashcse.model.request

import com.piashcse.constants.UserType
import io.ktor.server.auth.*

data class JwtTokenRequest(val userId: String, val email: String, val userType: String) : Principal {

    /**
     * Check if current user has access to a specific role (with hierarchy)
     */
    fun hasAccessTo(role: UserType): Boolean {
        val currentUserType = try {
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
     * Check if user has a specific permission
     */
    
}
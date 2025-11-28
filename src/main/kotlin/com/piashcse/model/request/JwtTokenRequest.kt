package com.piashcse.model.request

import com.piashcse.constants.UserType
import com.piashcse.utils.RoleBasedAuth
import com.piashcse.utils.RoleHierarchy
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
        return RoleHierarchy.hasAccess(currentUserType, role)
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
    fun isSuperAdmin(): Boolean = RoleBasedAuth.isSuperAdmin(this.userType)

    /**
     * Check if user is admin
     */
    fun isAdmin(): Boolean = RoleBasedAuth.isAdmin(this.userType)

    /**
     * Check if user is seller
     */
    fun isSeller(): Boolean = RoleBasedAuth.isSeller(this.userType)

    /**
     * Check if user is customer
     */
    fun isCustomer(): Boolean = RoleBasedAuth.isCustomer(this.userType)

    /**
     * Check if user has a specific permission
     */
    fun hasPermission(permission: com.piashcse.utils.Permission): Boolean =
        com.piashcse.utils.Permissions.hasPermission(this, permission)

    /**
     * Get user's permissions
     */
    fun getPermissions(): Set<com.piashcse.utils.Permission> {
        return com.piashcse.utils.Permissions.getPermissions(this.getUserType() ?: return emptySet())
    }
}
package com.piashcse.utils

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

/**
 * Defines role hierarchy and permissions for the e-commerce platform
 */
object RoleHierarchy {
    // Define role hierarchy where higher roles can access lower role resources
    val roleHierarchy = mapOf(
        UserType.SUPER_ADMIN to setOf(UserType.SUPER_ADMIN, UserType.ADMIN, UserType.SELLER, UserType.CUSTOMER),
        UserType.ADMIN to setOf(UserType.ADMIN, UserType.SELLER, UserType.CUSTOMER),
        UserType.SELLER to setOf(UserType.SELLER, UserType.CUSTOMER),
        UserType.CUSTOMER to setOf(UserType.CUSTOMER)
    )

    /**
     * Check if a role has access to a specific resource role
     */
    fun hasAccess(userRole: UserType, resourceRole: UserType): Boolean {
        return roleHierarchy[userRole]?.contains(resourceRole) ?: false
    }

    /**
     * Get all roles that a user role can access
     */
    fun getAccessibleRoles(userRole: UserType): Set<UserType> {
        return roleHierarchy[userRole] ?: emptySet()
    }

    /**
     * Check if a user can manage users of a specific type
     */
    fun canManageUser(currentUserType: UserType, targetUserType: UserType): Boolean {
        return when (currentUserType) {
            UserType.SUPER_ADMIN -> true  // Super admin can manage all users
            UserType.ADMIN -> targetUserType != UserType.SUPER_ADMIN && targetUserType != UserType.ADMIN
            UserType.SELLER -> targetUserType == UserType.CUSTOMER
            UserType.CUSTOMER -> targetUserType == UserType.CUSTOMER
        }
    }
}


/**
 * Utility function to require specific role access
 */
suspend fun ApplicationCall.requireRole(role: UserType) {
    val jwtPrincipal = this.principal<JwtTokenRequest>()
    if (jwtPrincipal == null || !jwtPrincipal.hasAccessTo(role)) {
        this.respond(io.ktor.http.HttpStatusCode.Forbidden, "Access denied")
        return
    }
}

/**
 * Utility function to require specific user type
 */
suspend fun ApplicationCall.requireSpecificRole(role: UserType) {
    val jwtPrincipal = this.principal<JwtTokenRequest>()
    if (jwtPrincipal == null || !jwtPrincipal.hasRole(role)) {
        this.respond(io.ktor.http.HttpStatusCode.Forbidden, "Access denied")
        return
    }
}


/**
 * Role-based authorization utilities for different user types
 */
object RoleBasedAuth {

    fun isSuperAdmin(userType: String): Boolean {
        return userType.equals("SUPER_ADMIN", ignoreCase = true)
    }

    fun isAdmin(userType: String): Boolean {
        return userType.equals("ADMIN", ignoreCase = true) || isSuperAdmin(userType)
    }

    fun isSeller(userType: String): Boolean {
        return userType.equals("SELLER", ignoreCase = true)
    }

    fun isCustomer(userType: String): Boolean {
        return userType.equals("CUSTOMER", ignoreCase = true) || isAdmin(userType) || isSeller(userType)
    }

    fun canManageUsers(currentRole: String, targetRole: String): Boolean {
        val currentUserType = UserType.fromString(currentRole) ?: return false
        val targetUserType = UserType.fromString(targetRole) ?: return false

        return RoleHierarchy.canManageUser(currentUserType, targetUserType)
    }
}
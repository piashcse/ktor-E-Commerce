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
}

/**
 * Extension function to check if current user has access to a specific role
 */
fun JwtTokenRequest.hasAccessTo(role: UserType): Boolean {
    val currentUserType = try {
        UserType.valueOf(this.userType.uppercase())
    } catch (e: IllegalArgumentException) {
        return false
    }
    return RoleHierarchy.hasAccess(currentUserType, role)
}

/**
 * Extension function to check if current user has specific role
 */
fun JwtTokenRequest.hasRole(role: UserType): Boolean {
    return try {
        UserType.valueOf(this.userType.uppercase()) == role
    } catch (e: IllegalArgumentException) {
        false
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
 * Extension function to get current user role
 */
fun JwtTokenRequest.getUserType(): UserType? {
    return try {
        UserType.valueOf(this.userType.uppercase())
    } catch (e: IllegalArgumentException) {
        null
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
        val currentUserType = try { 
            UserType.valueOf(currentRole.uppercase()) 
        } catch (e: IllegalArgumentException) { 
            return false 
        }
        
        val targetUserType = try { 
            UserType.valueOf(targetRole.uppercase()) 
        } catch (e: IllegalArgumentException) { 
            return false 
        }
        
        // Super admin can manage all users
        if (currentUserType == UserType.SUPER_ADMIN) return true
        
        // Admin can manage sellers and customers, but not other admins or super admins
        if (currentUserType == UserType.ADMIN) {
            return targetUserType == UserType.SELLER || targetUserType == UserType.CUSTOMER
        }
        
        // Other roles can only manage their own resources
        return currentRole.equals(targetRole, ignoreCase = true)
    }
}
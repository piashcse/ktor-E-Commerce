package com.piashcse.utils

import com.piashcse.constants.UserType
import com.piashcse.model.request.JwtTokenRequest

/**
 * Defines permissions for different user types in the e-commerce platform
 */
object Permissions {
    
    // Define permissions for different user types
    val userPermissions = mapOf(
        UserType.SUPER_ADMIN to setOf(
            Permission.MANAGE_ALL_USERS,
            Permission.MANAGE_ALL_PRODUCTS,
            Permission.MANAGE_ALL_ORDERS,
            Permission.MANAGE_ALL_SHOPS,
            Permission.MANAGE_SYSTEM_SETTINGS,
            Permission.VIEW_ALL_REPORTS,
            Permission.MANAGE_ALL_REVIEWS
        ),
        UserType.ADMIN to setOf(
            Permission.MANAGE_USERS,
            Permission.MANAGE_PRODUCTS,
            Permission.MANAGE_ORDERS,
            Permission.MANAGE_SHOPS,
            Permission.VIEW_REPORTS,
            Permission.MANAGE_REVIEWS
        ),
        UserType.SELLER to setOf(
            Permission.MANAGE_OWN_PRODUCTS,
            Permission.MANAGE_OWN_SHOP,
            Permission.MANAGE_OWN_ORDERS,
            Permission.VIEW_OWN_REPORTS,
            Permission.MANAGE_OWN_REVIEWS
        ),
        UserType.CUSTOMER to setOf(
            Permission.CREATE_ORDERS,
            Permission.MANAGE_OWN_PROFILE,
            Permission.MANAGE_OWN_WISHLIST,
            Permission.MANAGE_OWN_CART,
            Permission.CREATE_REVIEWS,
            Permission.VIEW_PRODUCTS
        )
    )
    
    /**
     * Check if a user type has a specific permission
     */
    fun hasPermission(userType: UserType, permission: Permission): Boolean {
        return userPermissions[userType]?.contains(permission) ?: false
    }
    
    /**
     * Check if a user has a specific permission
     */
    fun hasPermission(jwtPrincipal: JwtTokenRequest?, permission: Permission): Boolean {
        if (jwtPrincipal == null) return false
        
        val userType = try {
            UserType.valueOf(jwtPrincipal.userType.uppercase())
        } catch (e: IllegalArgumentException) {
            return false
        }
        
        return hasPermission(userType, permission)
    }
    
    /**
     * Get all permissions for a user type
     */
    fun getPermissions(userType: UserType): Set<Permission> {
        return userPermissions[userType] ?: emptySet()
    }
}

/**
 * Enum defining specific permissions in the system
 */
enum class Permission {
    // User management permissions
    MANAGE_ALL_USERS,
    MANAGE_USERS,

    // Product management permissions
    MANAGE_ALL_PRODUCTS,
    MANAGE_PRODUCTS,
    MANAGE_OWN_PRODUCTS,

    // Order management permissions
    MANAGE_ALL_ORDERS,
    MANAGE_ORDERS,
    MANAGE_OWN_ORDERS,

    // Shop management permissions
    MANAGE_ALL_SHOPS,
    MANAGE_SHOPS,
    MANAGE_OWN_SHOP,

    // System permissions
    MANAGE_SYSTEM_SETTINGS,

    // Report permissions
    VIEW_ALL_REPORTS,
    VIEW_REPORTS,
    VIEW_OWN_REPORTS,

    // Review permissions
    MANAGE_ALL_REVIEWS,
    MANAGE_REVIEWS,
    MANAGE_OWN_REVIEWS,
    CREATE_REVIEWS,

    // Customer permissions
    CREATE_ORDERS,
    MANAGE_OWN_PROFILE,
    MANAGE_OWN_WISHLIST,
    MANAGE_OWN_CART,
    VIEW_PRODUCTS;

    // Permission category helpers
    val isUserPermission get() = this in listOf(MANAGE_ALL_USERS, MANAGE_USERS)
    val isProductPermission get() = this in listOf(MANAGE_ALL_PRODUCTS, MANAGE_PRODUCTS, MANAGE_OWN_PRODUCTS, VIEW_PRODUCTS)
    val isOrderPermission get() = this in listOf(MANAGE_ALL_ORDERS, MANAGE_ORDERS, MANAGE_OWN_ORDERS)
    val isShopPermission get() = this in listOf(MANAGE_ALL_SHOPS, MANAGE_SHOPS, MANAGE_OWN_SHOP)
    val isReviewPermission get() = this in listOf(MANAGE_ALL_REVIEWS, MANAGE_REVIEWS, MANAGE_OWN_REVIEWS, CREATE_REVIEWS)

    companion object {
        // Groups of related permissions
        val adminPermissions = setOf(
            MANAGE_USERS, MANAGE_PRODUCTS, MANAGE_ORDERS, MANAGE_SHOPS,
            MANAGE_REVIEWS, VIEW_REPORTS, MANAGE_SYSTEM_SETTINGS
        )

        val sellerPermissions = setOf(
            MANAGE_OWN_PRODUCTS, MANAGE_OWN_SHOP, MANAGE_OWN_ORDERS,
            MANAGE_OWN_REVIEWS, VIEW_OWN_REPORTS
        )

        val customerPermissions = setOf(
            CREATE_ORDERS, MANAGE_OWN_PROFILE, MANAGE_OWN_WISHLIST,
            MANAGE_OWN_CART, CREATE_REVIEWS, VIEW_PRODUCTS
        )
    }
}

/**
 * Extension function to check if current user has permission
 */
fun JwtTokenRequest.hasPermission(permission: Permission): Boolean {
    return Permissions.hasPermission(this, permission)
}

/**
 * Extension function to get user's permissions
 */
fun JwtTokenRequest.getPermissions(): Set<Permission> {
    val userType = try {
        UserType.valueOf(this.userType.uppercase())
    } catch (e: IllegalArgumentException) {
        return emptySet()
    }
    
    return Permissions.getPermissions(userType)
}

/**
 * Role-based permissions utilities
 */
object RoleBasedPermissions {

    /**
     * Check if a user has permission to access a resource
     */
    fun canAccessResource(currentPrincipal: JwtTokenRequest?, resourceOwnerId: String?, requiredPermission: Permission): Boolean {
        if (currentPrincipal == null) return false

        // If no resource owner specified, check general permission
        if (resourceOwnerId == null) {
            return Permissions.hasPermission(currentPrincipal, requiredPermission)
        }

        // If it's a super admin, they can access everything
        if (currentPrincipal.isSuperAdmin()) {
            return true
        }

        // If it's an admin, they can access resources based on permission
        if (currentPrincipal.isAdmin() && requiredPermission != Permission.MANAGE_ALL_USERS) {
            return Permissions.hasPermission(currentPrincipal, requiredPermission)
        }

        // For own resources (e.g., a seller accessing their own products), check if it's their resource
        if (currentPrincipal.userId == resourceOwnerId) {
            // For own resource permissions
            return when (requiredPermission) {
                Permission.MANAGE_OWN_PRODUCTS, Permission.MANAGE_OWN_SHOP,
                Permission.MANAGE_OWN_ORDERS, Permission.VIEW_OWN_REPORTS,
                Permission.MANAGE_OWN_REVIEWS, Permission.MANAGE_OWN_PROFILE,
                Permission.MANAGE_OWN_WISHLIST, Permission.MANAGE_OWN_CART -> true
                else -> Permissions.hasPermission(currentPrincipal, requiredPermission)
            }
        }

        // Default permission check
        return Permissions.hasPermission(currentPrincipal, requiredPermission)
    }

    /**
     * Check if a user can manage a specific resource type
     */
    fun canManageResource(currentPrincipal: JwtTokenRequest?, resourceType: String): Boolean {
        if (currentPrincipal == null) return false

        return when (resourceType.lowercase()) {
            "user" -> currentPrincipal.hasPermission(Permission.MANAGE_USERS) ||
                      currentPrincipal.hasPermission(Permission.MANAGE_ALL_USERS)
            "product" -> currentPrincipal.hasPermission(Permission.MANAGE_PRODUCTS) ||
                         currentPrincipal.hasPermission(Permission.MANAGE_ALL_PRODUCTS)
            "order" -> currentPrincipal.hasPermission(Permission.MANAGE_ORDERS) ||
                       currentPrincipal.hasPermission(Permission.MANAGE_ALL_ORDERS)
            "shop" -> currentPrincipal.hasPermission(Permission.MANAGE_SHOPS) ||
                      currentPrincipal.hasPermission(Permission.MANAGE_ALL_SHOPS)
            "review" -> currentPrincipal.hasPermission(Permission.MANAGE_REVIEWS) ||
                        currentPrincipal.hasPermission(Permission.MANAGE_ALL_REVIEWS)
            "inventory" -> currentPrincipal.hasPermission(Permission.MANAGE_PRODUCTS) ||
                           currentPrincipal.hasPermission(Permission.MANAGE_ALL_PRODUCTS)
            else -> false
        }
    }

    /**
     * Check if a user can perform a specific action on a resource
     */
    fun canPerformAction(currentPrincipal: JwtTokenRequest?, userId: String?, action: Permission): Boolean {
        return canAccessResource(currentPrincipal, userId, action)
    }
}
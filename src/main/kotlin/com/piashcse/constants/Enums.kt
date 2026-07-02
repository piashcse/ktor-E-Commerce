package com.piashcse.constants

import kotlinx.serialization.Serializable

@Serializable
enum class CouponDiscountType {
    FIXED,
    PERCENTAGE,
}

@Serializable
enum class ReviewStatus {
    ACTIVE,
    HIDDEN,
    FLAGGED,
}

@Serializable
enum class RefundStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REFUNDED,
    SHIPPED,
}

@Serializable
enum class RefundMethod {
    ORIGINAL,
    BANK_TRANSFER,
}

@Serializable
enum class PaymentMethod {
    CREDIT_CARD,
    PAYPAL,
    BANK_TRANSFER,
    COD,
}

@Serializable
enum class PolicyType {
    PRIVACY_POLICY,
    TERMS_CONDITIONS,
    REFUND_POLICY,
    COOKIE_POLICY,
    DISCLAIMER,
    EULA,
    SHIPPING_POLICY,
    ;

    val isLegalPolicy get() = this in listOf(PRIVACY_POLICY, TERMS_CONDITIONS, DISCLAIMER, EULA)
    val requiresConsent get() = this != SHIPPING_POLICY
}

/**
 * Status of orders throughout their lifecycle
 */
@Serializable
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    DELIVERED,
    CANCELED,
    RECEIVED,
    ;

    companion object {
        fun canTransitionTo(
            current: OrderStatus,
            target: OrderStatus,
        ): Boolean =
            when (current) {
                PENDING -> target in listOf(CONFIRMED, CANCELED)
                CONFIRMED -> target in listOf(PAID, CANCELED)
                PAID -> target in listOf(DELIVERED, CANCELED)
                DELIVERED -> target in listOf(RECEIVED) // Completed order can be marked as received
                CANCELED -> false // Canceled orders cannot transition to other statuses
                RECEIVED -> false // Completed orders remain in received status
            }

        fun canBeCanceled(current: OrderStatus): Boolean = current in listOf(PENDING, CONFIRMED)
    }
}

/**
 * Payment status throughout transaction lifecycle
 */
@Serializable
enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED,
    ;

    val isFinal get() = this in listOf(COMPLETED, FAILED, REFUNDED)
    val isSuccessful get() = this == COMPLETED
}

/**
 * Product availability status
 */
@Serializable
enum class ProductStatus {
    ACTIVE, // Product is available for purchase
    OUT_OF_STOCK, // Product is not available
    ;

    val isActive get() = this == ACTIVE
    val isAvailable get() = this == ACTIVE
}

/**
 * Shop approval and operational status
 */
@Serializable
enum class ShopStatus {
    PENDING, // Shop application pending approval
    APPROVED, // Shop approved and active
    REJECTED, // Shop application rejected
    SUSPENDED, // Shop suspended by admin
    ;

    val isOperational get() = this in listOf(APPROVED, SUSPENDED)
    val isActive get() = this == APPROVED
}

/**
 * Inventory level status indicators
 */
@Serializable
enum class InventoryStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    ;

    val isAvailable get() = this == IN_STOCK
    val needsAttention get() = this in listOf(LOW_STOCK, OUT_OF_STOCK)

    companion object {
        fun fromStockLevel(stock: Int, minLevel: Int): InventoryStatus = when {
            stock <= 0 -> OUT_OF_STOCK
            stock <= minLevel -> LOW_STOCK
            else -> IN_STOCK
        }
    }
}

/**
 * User role hierarchy and permissions
 */
@Serializable
enum class UserType {
    SUPER_ADMIN,
    ADMIN,
    SELLER,
    CUSTOMER,
    ;

    val isSuperAdmin get() = this == SUPER_ADMIN
    val isAdminOrHigher get() = this in listOf(SUPER_ADMIN, ADMIN)
    val isSellerOrHigher get() = this in listOf(SUPER_ADMIN, ADMIN, SELLER)
    val isCustomerOrHigher get() = true // All roles can act as customers

    fun canManage(targetUserType: UserType): Boolean =
        when (this) {
            SUPER_ADMIN -> true
            ADMIN -> targetUserType != SUPER_ADMIN && targetUserType != ADMIN
            SELLER -> targetUserType == CUSTOMER
            CUSTOMER -> targetUserType == CUSTOMER
        }

    fun hasAccessTo(role: UserType): Boolean =
        when (role) {
            SUPER_ADMIN -> this == SUPER_ADMIN
            ADMIN -> this.isAdminOrHigher
            SELLER -> this.isSellerOrHigher
            CUSTOMER -> this.isCustomerOrHigher
        }

    companion object {
        fun fromString(role: String): UserType? = values().find { it.name.equals(role, ignoreCase = true) }
    }
}

package com.piashcse.constants

/**
 * Status of orders throughout their lifecycle
 */
enum class OrderStatus {
    PENDING, CONFIRMED, PAID, DELIVERED, CANCELED, RECEIVED;

    companion object {
        fun canTransitionTo(current: OrderStatus, target: OrderStatus): Boolean = when (current) {
            PENDING -> target in listOf(CONFIRMED, CANCELED)
            CONFIRMED -> target in listOf(PAID, CANCELED)
            PAID -> target in listOf(DELIVERED, CANCELED)
            DELIVERED -> target in listOf(RECEIVED) // Completed order can be marked as received
            CANCELED -> false // Canceled orders cannot transition to other statuses
            RECEIVED -> false // Completed orders remain in received status
        }
    }
}

/**
 * Payment status throughout transaction lifecycle
 */
enum class PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED;

    val isFinal get() = this in listOf(COMPLETED, FAILED, REFUNDED)
    val isSuccessful get() = this == COMPLETED
}

/**
 * Product availability status
 */
enum class ProductStatus {
    ACTIVE, // Product is available for purchase
    OUT_OF_STOCK; // Product is not available

    val isActive get() = this == ACTIVE
    val isAvailable get() = this == ACTIVE
}

/**
 * Shop approval and operational status
 */
enum class ShopStatus {
    PENDING, // Shop application pending approval
    APPROVED, // Shop approved and active
    REJECTED, // Shop application rejected
    SUSPENDED; // Shop suspended by admin

    val isOperational get() = this in listOf(APPROVED, SUSPENDED)
    val isActive get() = this == APPROVED
}

/**
 * Inventory level status indicators
 */
enum class InventoryStatus {
    IN_STOCK, // Product is in stock
    LOW_STOCK, // Product has low stock
    OUT_OF_STOCK; // Product is out of stock

    val isAvailable get() = this == IN_STOCK
    val needsAttention get() = this in listOf(LOW_STOCK, OUT_OF_STOCK)
}

/**
 * User role hierarchy and permissions
 */
enum class UserType {
    SUPER_ADMIN,
    ADMIN,
    SELLER,
    CUSTOMER;

    val isAdminOrHigher get() = this in listOf(SUPER_ADMIN, ADMIN)
    val isSellerOrHigher get() = this in listOf(SUPER_ADMIN, ADMIN, SELLER)
    val isCustomerOrHigher get() = true // All roles can act as customers

    companion object {
        fun fromString(role: String): UserType? =
            values().find { it.name.equals(role, ignoreCase = true) }
    }
}
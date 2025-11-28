package com.piashcse.constants

enum class OrderStatus {
    PENDING, CONFIRMED, PAID, DELIVERED, CANCELED, RECEIVED
}
enum class PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}
enum class ProductStatus {
    ACTIVE, // Product is available for purchase
    OUT_OF_STOCK // Product is not available
}
enum class ShopStatus {
    PENDING, // Shop application pending approval
    APPROVED, // Shop approved and active
    REJECTED, // Shop application rejected
    SUSPENDED // Shop suspended by admin
}
enum class InventoryStatus {
    IN_STOCK, // Product is in stock
    LOW_STOCK, // Product has low stock
    OUT_OF_STOCK // Product is out of stock
}
enum class UserType {
    SUPER_ADMIN,
    ADMIN,
    SELLER,
    CUSTOMER
}
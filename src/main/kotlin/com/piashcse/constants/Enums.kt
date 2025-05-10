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
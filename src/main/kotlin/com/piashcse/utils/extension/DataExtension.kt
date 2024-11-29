package com.piashcse.utils.extension


fun String.fileExtension(): String {
    return this.substring(this.lastIndexOf("."))
}

fun String.orderStatusCode(): Int {
    return when (this) {
        OrderStatus.PENDING.name.lowercase() -> 0
        OrderStatus.CONFIRMED.name.lowercase() -> 1
        OrderStatus.PAID.name.lowercase() -> 2
        OrderStatus.DELIVERED.name.lowercase() -> 3
        OrderStatus.CANCELED.name.lowercase() -> 4
        OrderStatus.RECEIVED.name.lowercase() -> 5
        else -> 0
    }
}

enum class OrderStatus {
    PENDING, CONFIRMED, PAID, DELIVERED, CANCELED, RECEIVED
}
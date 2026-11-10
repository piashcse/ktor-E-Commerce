package com.piashcse.utils.validator

import com.piashcse.constants.Message
import java.math.BigDecimal

/**
 * Shared request-validation rules that used to be copy-pasted across DTOs and
 * services. Keep business rules here so the same input is accepted/rejected
 * everywhere with the same message.
 */
fun String.requireStrongPassword() {
    if (!PasswordPolicy.isStrong(this)) {
        throw ValidationException(Message.Validation.WEAK_PASSWORD)
    }
}

fun BigDecimal.requirePositivePrice(fieldName: String = "Price"): BigDecimal {
    if (this <= BigDecimal.ZERO) {
        throw ValidationException("$fieldName must be positive")
    }
    return this
}

/**
 * Validates an optional discount price against the regular price.
 * A missing discount is valid; a discount must be positive and strictly lower.
 */
fun validateDiscountPrice(
    price: BigDecimal?,
    discountPrice: BigDecimal?,
) {
    discountPrice ?: return
    discountPrice.requirePositivePrice("Discount price")
    if (price != null && discountPrice >= price) {
        throw ValidationException("Discount price must be lower than the regular price")
    }
}

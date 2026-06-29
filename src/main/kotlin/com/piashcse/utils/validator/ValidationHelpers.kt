package com.piashcse.utils.validator

import com.piashcse.constants.Message

// ============================================================================
//  VALIDATION HELPERS - Uses Message constants for consistency
// ============================================================================

fun requireNotBlank(
    value: String,
    fieldName: String,
) {
    if (value.isBlank()) throw ValidationException(Message.Validation.blankField(fieldName))
}

fun requirePositive(
    value: Number,
    fieldName: String,
) {
    if (value.toDouble() <= 0) throw ValidationException(Message.Validation.notPositive(fieldName))
}

fun requireNonNegative(
    value: Number,
    fieldName: String,
) {
    if (value.toDouble() < 0) throw ValidationException(Message.Validation.negativeValue(fieldName))
}

fun requireValidEmail(email: String) {
    val regex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")
    if (!regex.matches(email)) throw ValidationException(Message.Validation.INVALID_EMAIL)
}

fun requireValidPassword(
    password: String,
    minLength: Int = 8,
) {
    if (password.length < minLength) throw ValidationException(Message.Validation.WEAK_PASSWORD)
    if (!password.any { it.isUpperCase() }) throw ValidationException(Message.Validation.WEAK_PASSWORD)
    if (!password.any { it.isLowerCase() }) throw ValidationException(Message.Validation.WEAK_PASSWORD)
    if (!password.any { it.isDigit() }) throw ValidationException(Message.Validation.WEAK_PASSWORD)
    if (!password.any { !it.isLetterOrDigit() }) throw ValidationException(Message.Validation.WEAK_PASSWORD)
}

package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class CancelOrderRequest(
    val reason: String
) {
    fun validation() {
        validate(this) {
            validate(CancelOrderRequest::reason).isNotNull().isNotEmpty()
        }
    }
}

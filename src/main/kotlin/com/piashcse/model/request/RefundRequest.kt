package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RefundRequestRequest(
    val orderItemId: String,
    val reason: String,
    val images: String? = null,
) {
    init {
        validate(this) {
            validate(RefundRequestRequest::orderItemId).isNotNull().isNotEmpty()
            validate(RefundRequestRequest::reason).isNotNull().isNotEmpty()
        }
    }
}

@Serializable
data class UpdateRefundStatusRequest(
    val status: String,
    val refundAmount: Double? = null,
    val refundMethod: String? = null,
) {
    init {
        validate(this) {
            validate(UpdateRefundStatusRequest::status).isNotNull().isNotEmpty()
        }
    }
}

@Serializable
data class ShipRefundRequest(
    val trackingNumber: String,
) {
    init {
        validate(this) {
            validate(ShipRefundRequest::trackingNumber).isNotNull().isNotEmpty()
        }
    }
}

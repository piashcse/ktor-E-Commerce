package com.piashcse.model.request

import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class RefundRequestRequest(
    val orderItemId: String,
    val reason: String,
    val images: String? = null,
    val quantity: Int = 1,
) {
    init {
        validate(this) {
            validate(RefundRequestRequest::orderItemId).isNotNull().isNotEmpty()
            validate(RefundRequestRequest::reason).isNotNull().isNotEmpty()
            validate(RefundRequestRequest::quantity).isNotNull().isGreaterThan(0)
        }
    }
}

@Serializable
data class UpdateRefundStatusRequest(
    val status: RefundStatus,
    @Contextual val refundAmount: BigDecimal? = null,
    @Contextual val refundShippingAmount: BigDecimal? = null,
    @Contextual val refundTaxAmount: BigDecimal? = null,
    val refundMethod: RefundMethod? = null,
) {
    init {
        validate(this) {
            validate(UpdateRefundStatusRequest::status).isNotNull()
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

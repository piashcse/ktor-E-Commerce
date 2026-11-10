package com.piashcse.model.request

import com.piashcse.constants.PaymentMethod
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

/**
 * Payment intent. Status is deliberately NOT client-controlled: the server
 * decides whether a payment is PENDING/COMPLETED based on the gateway result
 * or an explicit admin confirmation.
 */
@Serializable
data class PaymentRequest(
    val orderId: String,
    @Contextual val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val transactionId: String? = null,
    val idempotencyKey: String? = null,
    /** Where the provider should send the customer after 3DS/SCA. */
    val returnUrl: String? = null,
) {
    init {
        validate(this) {
            validate(PaymentRequest::orderId).isNotNull().isNotEmpty()
            validate(PaymentRequest::amount).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(PaymentRequest::paymentMethod).isNotNull()
        }
    }
}

package com.piashcse.model.request

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class PaymentRequest(
    val orderId: String,
    @Contextual val amount: BigDecimal,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod,
    val transactionId: String?,
) {
    init {
        validate(this) {
            validate(PaymentRequest::orderId).isNotNull().isNotEmpty()
            validate(PaymentRequest::amount).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(PaymentRequest::status).isNotNull()
            validate(PaymentRequest::paymentMethod).isNotNull()
        }
    }
}

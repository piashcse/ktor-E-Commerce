package com.piashcse.feature.payment

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.*
import com.piashcse.model.request.PaymentRequest
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal

/** Loads and validates a payable order snapshot. */
internal object PaymentOrders {
    data class PaymentOrderSnapshot(
        val orderId: EntityID<String>,
        val userId: EntityID<String>,
        val total: BigDecimal,
        val currency: String,
        val status: OrderStatus,
        val paymentStatus: PaymentStatus,
        val remaining: BigDecimal,
    )

    fun load(
        paymentRequest: PaymentRequest,
        callerUserId: String,
        forUpdate: Boolean,
    ): PaymentOrderSnapshot {
        val query =
            OrderDAO.find { OrderTable.id eq paymentRequest.orderId.entityID(OrderTable) }
        val order =
            (if (forUpdate) query.forUpdate() else query).firstOrNull()
                ?: paymentRequest.orderId.throwNotFound("Order")

        if (order.userId.value != callerUserId) throw ForbiddenException(Message.Errors.FORBIDDEN)
        if (order.status !in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)) {
            throw ValidationException("Order is not payable in status ${order.status}")
        }
        if (order.paymentStatus == PaymentStatus.COMPLETED || order.paymentStatus == PaymentStatus.REFUNDED) {
            throw ValidationException(Message.Payments.ALREADY_PAID)
        }

        val remaining = order.total.subtract(PaymentCompletion.completedTotal(order.id, order.currency))
        if (remaining <= BigDecimal.ZERO) throw ValidationException(Message.Payments.ALREADY_PAID)
        if (paymentRequest.amount > remaining) {
            throw ValidationException(
                Message.Payments.amountExceedsRemaining(
                    paymentRequest.amount.toPlainString(),
                    remaining.toPlainString(),
                ),
            )
        }
        // Partial card captures leave an order that can be neither cancelled
        // (a captured payment exists) nor refunded (refund requires delivery).
        // Until a deposit/partial-payment model exists, require settlement of
        // the full remaining amount.
        if (paymentRequest.amount.compareTo(remaining) != 0) {
            throw ValidationException(
                "Payment must settle the full remaining amount (${remaining.toPlainString()})",
            )
        }
        return PaymentOrderSnapshot(
            orderId = order.id,
            userId = order.userId,
            total = order.total,
            currency = order.currency,
            status = order.status,
            paymentStatus = order.paymentStatus,
            remaining = remaining,
        )
    }
}

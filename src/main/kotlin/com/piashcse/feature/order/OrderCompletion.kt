package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.ReservationStatus
import com.piashcse.database.entities.StockReservationDAO
import com.piashcse.database.entities.StockReservationTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.seller.SellerLedger
import com.piashcse.service.DigitalDeliveryService
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The single completion path for orders that become paid. Used by the payment
 * pipeline, zero-total checkout and offline settlement so loyalty, payout,
 * digital delivery and reservation state can never diverge.
 *
 * Must run inside a DB transaction.
 */
object OrderCompletion {
    fun completeTx(
        order: OrderDAO,
        payment: PaymentDAO,
        loyalty: LoyaltyService,
        digital: DigitalDeliveryService,
    ) {
        order.paymentStatus = PaymentStatus.COMPLETED
        if (order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED) {
            order.status = OrderStatus.PAID
        }
        val totalUsd =
            if (order.fxRate > BigDecimal.ZERO) {
                order.total.divide(order.fxRate, 2, RoundingMode.HALF_UP)
            } else {
                order.total
            }
        loyalty.earnForPaidOrderTx(order.userId.value, order.id.value, totalUsd)
        digital.grantForOrderTx(order)
        SellerLedger.createPayout(order)
        StockReservationDAO.find { StockReservationTable.orderId eq order.id }
            .forEach { it.status = ReservationStatus.FINALIZED }
        OutboxPublisher.enqueueTx(
            OutboxAggregate.PAYMENT,
            payment.id.value,
            PaymentCompletedEvent(
                paymentId = payment.id.value,
                orderId = order.id.value,
                userId = order.userId.value,
                email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                amount = payment.amount,
            ),
        )
    }

    /** Wallet/gift/coupon can settle an order without a provider charge. */
    fun completeZeroTotalTx(
        order: OrderDAO,
        loyalty: LoyaltyService,
        digital: DigitalDeliveryService,
    ) {
        if (order.paymentStatus == PaymentStatus.COMPLETED) return
        val payment =
            PaymentDAO.new {
                this.orderId = order.id
                this.userId = order.userId
                amount = BigDecimal.ZERO.setScale(2)
                currency = order.currency
                status = PaymentStatus.COMPLETED
                paymentMethod = order.paymentMethod ?: PaymentMethod.COD
                transactionId = "zero-total:${order.id.value}"
                provider = PaymentProvider.MANUAL
                providerStatus = "zero-total"
            }
        completeTx(order, payment, loyalty, digital)
    }
}

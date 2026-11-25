package com.piashcse.feature.payment

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RiskDecision
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderCompletion
import com.piashcse.service.DigitalDeliveryService
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal

/** Shared "is this order fully paid?" completion guard for payment and webhook paths. */
internal object PaymentCompletion {
    fun completedTotal(
        orderId: EntityID<String>,
        currency: String,
    ): BigDecimal =
        PaymentDAO.find {
            (PaymentTable.orderId eq orderId) and
                (PaymentTable.status eq PaymentStatus.COMPLETED) and
                (PaymentTable.currency eq currency)
        }.fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amount) }

    /**
     * Marks the order PAID once completed payments cover its total. Must run inside
     * the transaction that locked the order row.
     */
    fun completeIfFullyPaid(
        order: OrderDAO,
        payment: PaymentDAO,
        loyaltyService: LoyaltyService,
        digitalDelivery: DigitalDeliveryService,
    ) {
        // REVIEW payments must be explicitly released by an admin before the order is paid.
        if (payment.riskDecision != RiskDecision.ALLOW) return
        if (order.paymentStatus == PaymentStatus.COMPLETED) return
        // A refunded order is terminal. A late/retried success webhook or an
        // admin confirmation must never move it back to PAID.
        if (order.paymentStatus == PaymentStatus.REFUNDED) return
        // Never resurrect an auto-canceled/received order. If the provider has
        // already captured money, the late payment needs an explicit refund.
        if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.RECEIVED) return
        val paid = completedTotal(order.id, order.currency)
        if (paid < order.total) return

        OrderCompletion.completeTx(order, payment, loyaltyService, digitalDelivery)
    }
}

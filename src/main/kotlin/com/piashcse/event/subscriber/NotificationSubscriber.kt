package com.piashcse.event.subscriber

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserTable
import com.piashcse.event.AdminActionEvent
import com.piashcse.event.DomainEvent
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.event.RefundStatusChangedEvent
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.Subscriber
import com.piashcse.event.UserRegisteredEvent
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.slf4j.LoggerFactory

/** Writes in-app notifications + seller payout rows. Failures are non-fatal. */
class NotificationSubscriber : Subscriber {
    private val log = LoggerFactory.getLogger(NotificationSubscriber::class.java)

    private fun notify(
        userId: String,
        type: String,
        title: String,
        body: String,
        resourceType: String,
        resourceId: String,
    ) {
        NotificationDAO.new {
            this.userId = userId.entityID(UserTable)
            channel = "IN_APP"
            this.type = type
            this.title = title
            this.body = body
            this.resourceType = resourceType
            this.resourceId = resourceId
        }
    }

    override suspend fun onEvent(event: DomainEvent) {
        try {
            when (event) {
                is OrderPlacedEvent ->
                    query {
                        notify(
                            event.userId,
                            "ORDER_PLACED",
                            "Order ${event.orderNumber} placed",
                            "Total ${Money.str(event.total)}",
                            "ORDER",
                            event.orderId,
                        )
                        Unit
                    }
                is PaymentCompletedEvent -> {
                    query {
                        notify(
                            event.userId,
                            "PAYMENT_COMPLETED",
                            "Payment received",
                            "Amount ${Money.str(event.amount)} for order ${event.orderId}",
                            "PAYMENT",
                            event.paymentId,
                        )
                        Unit
                    }
                    // Seller payout capture — best effort.
                    runCatching {
                        query {
                            val order = OrderDAO.findById(event.orderId) ?: return@query
                            val shopId = order.shopId?.value ?: return@query
                            val seller =
                                SellerDAO.find { SellerTable.shopId eq shopId.entityID(ShopTable) }.firstOrNull()
                                    ?: return@query
                            val exists =
                                SellerPayoutDAO.find {
                                    (SellerPayoutTable.sellerId eq seller.id) and
                                        (SellerPayoutTable.orderId eq order.id)
                                }.firstOrNull() != null
                            if (exists) return@query
                            val commission = Money.commission(order.subTotal, seller.commissionRate)
                            SellerPayoutDAO.new {
                                this.sellerId = seller.id
                                this.orderId = order.id
                                subTotal = order.subTotal
                                commissionAmount = commission
                                payoutAmount = order.subTotal.subtract(commission)
                                status = "PENDING"
                            }
                            Unit
                        }
                    }.onFailure { log.warn("Payout capture failed for order ${event.orderId}: ${it.message}") }
                }
                is RefundStatusChangedEvent ->
                    query {
                        notify(
                            event.userId,
                            "REFUND_${event.toStatus}",
                            "Refund ${event.toStatus.lowercase()}",
                            "Refund ${event.refundId}: ${event.fromStatus} -> ${event.toStatus}",
                            "REFUND",
                            event.refundId,
                        )
                        Unit
                    }
                is UserRegisteredEvent, is SendEmailEvent, is AdminActionEvent -> Unit
            }
        } catch (e: Exception) {
            log.error("Notification write failed for ${event::class.simpleName}", e)
        }
    }
}

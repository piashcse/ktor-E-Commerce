package com.piashcse.event.subscriber

import com.piashcse.database.entities.NotificationDAO
import com.piashcse.database.entities.NotificationTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
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
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.eq
import org.slf4j.LoggerFactory

/** Writes in-app notifications + seller payout rows. Failures are non-fatal. */
class NotificationSubscriber : Subscriber {
    private val log = LoggerFactory.getLogger(NotificationSubscriber::class.java)

    override suspend fun onEvent(event: DomainEvent) {
        try {
            when (event) {
                is OrderPlacedEvent -> query {
                    NotificationDAO.new {
                        userId = event.userId.entityID(UserTable)
                        channel = "IN_APP"
                        type = "ORDER_PLACED"
                        title = "Order ${event.orderNumber} placed"
                        body = "Total ${event.total.toPlainString()}"
                        resourceType = "ORDER"
                        resourceId = event.orderId
                    }
                    Unit
                }
                is PaymentCompletedEvent -> {
                    query {
                        NotificationDAO.new {
                            userId = event.userId.entityID(UserTable)
                            channel = "IN_APP"
                            type = "PAYMENT_COMPLETED"
                            title = "Payment received"
                            body = "Amount ${event.amount.toPlainString()} for order ${event.orderId}"
                            resourceType = "PAYMENT"
                            resourceId = event.paymentId
                        }
                        Unit
                    }
                    // Seller payout capture — best effort.
                    runCatching {
                        query {
                            val order = OrderDAO.findById(event.orderId) ?: return@query
                            val shopId = order.shopId?.value ?: return@query
                            val seller = SellerDAO.find { SellerTable.shopId eq shopId.entityID(ShopTable) }.firstOrNull()
                                ?: return@query
                            val commission = seller.commissionRate.let { rate ->
                                order.subTotal.multiply(rate).divide(java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP)
                            }
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
                is RefundStatusChangedEvent -> query {
                    NotificationDAO.new {
                        userId = event.userId.entityID(UserTable)
                        channel = "IN_APP"
                        type = "REFUND_${event.toStatus}"
                        title = "Refund ${event.toStatus.lowercase()}"
                        body = "Refund ${event.refundId}: ${event.fromStatus} -> ${event.toStatus}"
                        resourceType = "REFUND"
                        resourceId = event.refundId
                    }
                    Unit
                }
                is UserRegisteredEvent, is SendEmailEvent, is AdminActionEvent -> Unit
            }
        } catch (e: Exception) {
            log.error("Notification write failed for ${event::class.simpleName}", e)
        }
    }
}

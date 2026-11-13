package com.piashcse.event.subscriber

import com.piashcse.database.entities.AuditLogDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.StockAlertDAO
import com.piashcse.database.entities.StockAlertTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminActionEvent
import com.piashcse.event.BackInStockEvent
import com.piashcse.event.DomainEvent
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.event.RefundStatusChangedEvent
import com.piashcse.event.RmaStatusChangedEvent
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.Subscriber
import com.piashcse.event.UserRegisteredEvent
import com.piashcse.feature.notification.NotificationDispatcher
import com.piashcse.feature.notification.NotificationMessage
import com.piashcse.feature.notification.NotificationTemplates
import com.piashcse.feature.seller.CommissionService
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.slf4j.LoggerFactory

/** Writes in-app notifications + seller payout rows. Notification writes are non-fatal;
 * payout capture failures are audit-logged and propagated (EventBus retries + dead-letters). */
class NotificationSubscriber(
    private val dispatcher: NotificationDispatcher = NotificationDispatcher(),
) : Subscriber {
    private val log = LoggerFactory.getLogger(NotificationSubscriber::class.java)

    private fun messageFor(event: DomainEvent): NotificationMessage? =
        when (event) {
            is OrderPlacedEvent -> {
                val content = NotificationTemplates.orderPlaced(event.orderNumber, Money.str(event.total))
                NotificationMessage(
                    userId = event.userId,
                    email = event.email,
                    eventType = "ORDER_PLACED",
                    title = content.title,
                    body = content.body,
                    resourceType = "ORDER",
                    resourceId = event.orderId,
                )
            }
            is OrderStatusChangedEvent -> {
                val content =
                    NotificationTemplates.orderStatusChanged(
                        event.orderNumber,
                        event.toStatus,
                        event.trackingNumber,
                    )
                NotificationMessage(
                    userId = event.userId,
                    email = event.email,
                    eventType = "ORDER_${event.toStatus}",
                    title = content.title,
                    body = content.body,
                    resourceType = "ORDER",
                    resourceId = event.orderId,
                )
            }
            is PaymentCompletedEvent -> {
                val content = NotificationTemplates.paymentCompleted(Money.str(event.amount), event.orderId)
                NotificationMessage(
                    userId = event.userId,
                    email = event.email,
                    eventType = "PAYMENT_COMPLETED",
                    title = content.title,
                    body = content.body,
                    resourceType = "PAYMENT",
                    resourceId = event.paymentId,
                )
            }
            is RefundStatusChangedEvent -> {
                val content =
                    NotificationTemplates.refundStatusChanged(event.refundId, event.fromStatus, event.toStatus)
                NotificationMessage(
                    userId = event.userId,
                    email = event.email,
                    eventType = "REFUND_${event.toStatus}",
                    title = content.title,
                    body = content.body,
                    resourceType = "REFUND",
                    resourceId = event.refundId,
                )
            }
            is RmaStatusChangedEvent -> {
                val content = NotificationTemplates.rmaStatusChanged(event.rmaId, event.fromStatus, event.toStatus)
                NotificationMessage(
                    userId = event.userId,
                    email = event.email,
                    eventType = "RMA_${event.toStatus}",
                    title = content.title,
                    body = content.body,
                    resourceType = "RMA",
                    resourceId = event.rmaId,
                )
            }
            is UserRegisteredEvent -> {
                val content = NotificationTemplates.welcome(event.userType)
                NotificationMessage(
                    userId = event.userId,
                    email = event.email,
                    eventType = "USER_REGISTERED",
                    title = content.title,
                    body = content.body,
                    resourceType = "USER",
                    resourceId = event.userId,
                )
            }
            is SendEmailEvent, is AdminActionEvent -> null
            // Back-in-stock fans out to many subscribers and is handled in onEvent.
            is BackInStockEvent -> null
        }

    override suspend fun onEvent(event: DomainEvent) {
        if (event is BackInStockEvent) {
            notifyStockAlerts(event)
            return
        }
        // Payout capture must propagate (see captureSellerPayout), so it runs outside
        // the non-fatal notification try/catch below.
        if (event is PaymentCompletedEvent) {
            try {
                dispatcher.dispatch(messageFor(event) ?: return)
            } catch (e: Exception) {
                log.error("Notification dispatch failed for ${event::class.simpleName}", e)
            }
            captureSellerPayout(event)
            return
        }
        val message = messageFor(event) ?: return
        try {
            dispatcher.dispatch(message)
        } catch (e: Exception) {
            log.error("Notification dispatch failed for ${event::class.simpleName}", e)
        }
    }

    private suspend fun notifyStockAlerts(event: BackInStockEvent) {
        val product = query { ProductDAO.findById(event.productId) } ?: return
        val alerts =
            query {
                StockAlertDAO.find {
                    (StockAlertTable.productId eq product.id) and
                        StockAlertTable.notifiedAt.isNull() and
                        if (event.variantId == null) {
                            StockAlertTable.variantId.isNull()
                        } else {
                            StockAlertTable.variantId.isNull() or
                                (StockAlertTable.variantId eq event.variantId.entityID(ProductVariantTable))
                        }
                }.toList()
            }
        val content = NotificationTemplates.backInStock(product.name)
        alerts.forEach { alert ->
            val user = query { UserDAO.findById(alert.userId.value) } ?: return@forEach
            try {
                dispatcher.dispatch(
                    NotificationMessage(
                        userId = user.id.value,
                        email = user.email,
                        eventType = "BACK_IN_STOCK",
                        title = content.title,
                        body = content.body,
                        resourceType = "PRODUCT",
                        resourceId = product.id.value,
                    ),
                )
                query { alert.notifiedAt = currentUtc() }
            } catch (e: Exception) {
                log.error("Back-in-stock notification failed for ${user.id.value}", e)
            }
        }
    }

    private suspend fun captureSellerPayout(event: PaymentCompletedEvent) {
        try {
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
                // Seller is paid on the net product amount after seller-funded
                // discounts only. Wallet points/credit and gift cards are
                // platform payment instruments; they must not reduce seller pay.
                val sellerFundedDiscount =
                    order.discountAmount
                        .subtract(order.walletDiscount)
                        .subtract(order.giftCardDiscount)
                        .max(java.math.BigDecimal.ZERO)
                val netSubTotal = order.subTotal.subtract(sellerFundedDiscount).max(java.math.BigDecimal.ZERO)
                val commission = CommissionService().commissionForOrderTx(order, seller)
                val effectiveRate =
                    if (netSubTotal > java.math.BigDecimal.ZERO) {
                        commission
                            .multiply(java.math.BigDecimal(100))
                            .divide(netSubTotal, 2, java.math.RoundingMode.HALF_UP)
                    } else {
                        java.math.BigDecimal.ZERO.setScale(2)
                    }
                SellerPayoutDAO.new {
                    this.sellerId = seller.id
                    this.orderId = order.id
                    subTotal = netSubTotal
                    commissionAmount = commission
                    commissionRate = effectiveRate
                    payoutAmount = netSubTotal.subtract(commission)
                    currency = order.currency
                    status = "PENDING"
                }
                Unit
            }
        } catch (e: Exception) {
            // The payout insert already rolled back with its transaction; leave a
            // durable audit trail, then rethrow so EventBus retries + dead-letters
            // instead of silently dropping seller earnings.
            runCatching {
                query {
                    AuditLogDAO.new {
                        actorId = event.userId.entityID(UserTable)
                        actorEmail = event.email
                        actorRole = "SYSTEM"
                        action = "PAYOUT_CAPTURE_FAILED"
                        resourceType = "ORDER"
                        resourceId = event.orderId
                        details = "Payout capture failed for payment ${event.paymentId}: ${e.message}"
                        outcome = "FAILURE"
                    }
                    Unit
                }
            }.onFailure { auditError ->
                log.warn("Payout failure audit write failed for order ${event.orderId}: ${auditError.message}")
            }
            log.error("Payout capture failed for order ${event.orderId}", e)
            throw e
        }
    }
}

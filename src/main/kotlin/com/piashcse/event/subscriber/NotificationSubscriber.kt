package com.piashcse.event.subscriber

import com.piashcse.constants.UserType
import com.piashcse.database.entities.AuditLogDAO
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.StockAlertDAO
import com.piashcse.database.entities.StockAlertTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminActionEvent
import com.piashcse.event.BackInStockEvent
import com.piashcse.event.DisputeCreatedEvent
import com.piashcse.event.DomainEvent
import com.piashcse.event.KycStatusChangedEvent
import com.piashcse.event.LowStockEvent
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.PaymentCompletedEvent
import com.piashcse.event.PayoutStatusChangedEvent
import com.piashcse.event.ProductReviewCreatedEvent
import com.piashcse.event.RefundStatusChangedEvent
import com.piashcse.event.ReviewReplyEvent
import com.piashcse.event.RmaStatusChangedEvent
import com.piashcse.event.SellerReviewCreatedEvent
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.ShopStatusChangedEvent
import com.piashcse.event.Subscriber
import com.piashcse.event.UserRegisteredEvent
import com.piashcse.feature.notification.NotificationDispatcher
import com.piashcse.feature.notification.NotificationMessage
import com.piashcse.feature.notification.NotificationTemplates
import com.piashcse.feature.notification.StorefrontLinks
import com.piashcse.feature.seller.PayoutLedger
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
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
                val content =
                    NotificationTemplates.orderPlaced(
                        event.orderNumber,
                        Money.str(event.total),
                        StorefrontLinks.orderTracking(event.orderNumber, event.email),
                    )
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
                        StorefrontLinks.orderTracking(event.orderNumber, event.email),
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
                    NotificationTemplates.refundStatusChanged(event.refundId, event.fromStatus, event.toStatus, event.reason)
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
                val content =
                    NotificationTemplates.rmaStatusChanged(
                        event.rmaId,
                        event.fromStatus,
                        event.toStatus,
                        event.note,
                    )
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
            is SellerReviewCreatedEvent,
            is ProductReviewCreatedEvent,
            is ReviewReplyEvent,
            is DisputeCreatedEvent,
            is LowStockEvent,
            is KycStatusChangedEvent,
            is ShopStatusChangedEvent,
            is PayoutStatusChangedEvent,
            -> null
            // Back-in-stock fans out to many subscribers and is handled in onEvent.
            is BackInStockEvent -> null
        }

    private suspend fun messagesFor(event: DomainEvent): List<NotificationMessage> {
        val messages = mutableListOf<NotificationMessage>()
        messageFor(event)?.let { messages += it }
        messages += sellerMessagesFor(event)
        messages += adminAlertMessagesFor(event)
        return messages
    }

    private fun NotificationTemplates.Content.toMessage(
        userId: String,
        email: String?,
        eventType: String,
        resourceType: String,
        resourceId: String?,
    ) = NotificationMessage(userId, email, eventType, title, body, resourceType, resourceId)

    // Flat event fan-out is intentionally centralized so notification copy and
    // routing stay in one place; splitting it would hide the event matrix.
    @Suppress("CyclomaticComplexMethod")
    private suspend fun sellerMessagesFor(event: DomainEvent): List<NotificationMessage> {
        val messages = mutableListOf<NotificationMessage>()
        when (event) {
            is OrderPlacedEvent -> {
                sellerForShop(event.shopId)?.let { (userId, email) ->
                    val content = NotificationTemplates.sellerNewOrder(event.orderNumber, Money.str(event.total))
                    messages += content.toMessage(userId, email, "SELLER_ORDER_PLACED", "ORDER", event.orderId)
                }
            }
            is PaymentCompletedEvent -> {
                val order = query { OrderDAO.findById(event.orderId) }
                sellerForShop(order?.shopId?.value)?.let { (userId, email) ->
                    val content = NotificationTemplates.sellerOrderPaid(order?.orderNumber.orEmpty())
                    messages += content.toMessage(userId, email, "SELLER_ORDER_PAID", "PAYMENT", event.paymentId)
                }
            }
            is OrderStatusChangedEvent -> {
                if (event.toStatus == "CANCELED") {
                    val shopId = query { OrderDAO.findById(event.orderId)?.shopId?.value }
                    sellerForShop(shopId)?.let { (userId, email) ->
                        val content = NotificationTemplates.sellerOrderCanceled(event.orderNumber)
                        messages += content.toMessage(userId, email, "SELLER_ORDER_CANCELED", "ORDER", event.orderId)
                    }
                }
            }
            is RefundStatusChangedEvent -> {
                val shopId =
                    query {
                        RefundRequestDAO.findById(event.refundId)
                            ?.let { refund -> OrderDAO.findById(refund.orderId.value)?.shopId?.value }
                    }
                sellerForShop(shopId)?.let { (userId, email) ->
                    val content = NotificationTemplates.sellerRefundUpdate(event.orderId, event.toStatus, event.reason)
                    messages += content.toMessage(userId, email, "SELLER_REFUND_${event.toStatus}", "REFUND", event.refundId)
                }
            }
            is RmaStatusChangedEvent -> {
                val shopId = query { OrderDAO.findById(event.orderId)?.shopId?.value }
                sellerForShop(shopId)?.let { (userId, email) ->
                    val content = NotificationTemplates.sellerRmaUpdate(event.orderId, event.toStatus, event.note)
                    messages += content.toMessage(userId, email, "SELLER_RMA_${event.toStatus}", "RMA", event.rmaId)
                }
            }
            is SellerReviewCreatedEvent -> {
                recipientForSellerId(event.sellerId)?.let { (userId, email) ->
                    val content = NotificationTemplates.sellerReview(event.rating, event.comment)
                    messages += content.toMessage(userId, email, "SELLER_REVIEW", "REVIEW", event.reviewId)
                }
            }
            is ProductReviewCreatedEvent -> {
                val shopId = query { ProductDAO.findById(event.productId)?.shopId?.value }
                sellerForShop(shopId)?.let { (userId, email) ->
                    val content = NotificationTemplates.sellerReview(event.rating, event.comment)
                    messages += content.toMessage(userId, email, "PRODUCT_REVIEW", "REVIEW", event.reviewId)
                }
            }
            is ReviewReplyEvent -> {
                recipientForUserId(event.customerUserId)?.let { (userId, email) ->
                    val content = NotificationTemplates.reviewReply(event.sellerReply)
                    messages += content.toMessage(userId, email, "REVIEW_REPLY", "REVIEW", event.reviewId)
                }
            }
            is LowStockEvent -> {
                sellerForShop(event.shopId)?.let { (userId, email) ->
                    messages +=
                        NotificationMessage(
                            userId,
                            email,
                            "LOW_STOCK",
                            "Low stock alert",
                            "Product ${event.productId} has ${event.stockQuantity} units left.",
                            "PRODUCT",
                            event.productId,
                        )
                }
            }
            is DisputeCreatedEvent -> {
                event.sellerId?.let { sellerId ->
                    recipientForSellerId(sellerId)?.let { (userId, email) ->
                        val content = NotificationTemplates.disputeCreated(event.disputeId, event.subject)
                        messages += content.toMessage(userId, email, "DISPUTE_CREATED", "DISPUTE", event.disputeId)
                    }
                }
            }
            is KycStatusChangedEvent -> {
                recipientForUserId(event.userId)?.let { (userId, email) ->
                    val content = NotificationTemplates.kycStatus(event.status, event.reason)
                    messages += content.toMessage(userId, email, "KYC_${event.status}", "KYC", event.sellerId)
                }
            }
            is ShopStatusChangedEvent -> {
                recipientForUserId(event.userId)?.let { (userId, email) ->
                    val content = NotificationTemplates.shopStatus(event.status, event.reason)
                    messages += content.toMessage(userId, email, "SHOP_${event.status}", "SHOP", event.shopId)
                }
            }
            is PayoutStatusChangedEvent -> {
                recipientForUserId(event.userId)?.let { (userId, email) ->
                    val content =
                        NotificationTemplates.payoutStatus(
                            event.status,
                            Money.str(event.amount),
                            event.currency,
                            event.reason,
                        )
                    messages += content.toMessage(userId, email, "PAYOUT_${event.status}", "PAYOUT", event.payoutRequestId)
                }
            }
            else -> Unit
        }
        return messages
    }

    private suspend fun adminAlertMessagesFor(event: DomainEvent): List<NotificationMessage> =
        when (event) {
            is KycStatusChangedEvent ->
                if (event.status == "PENDING") {
                    adminMessages("New KYC submission", "Seller ${event.sellerId} submitted KYC for review.", "KYC", event.sellerId)
                } else {
                    emptyList()
                }
            is ShopStatusChangedEvent ->
                if (event.status == "PENDING") {
                    adminMessages("New shop application", "Shop ${event.shopId} is awaiting approval.", "SHOP", event.shopId)
                } else {
                    emptyList()
                }
            is PayoutStatusChangedEvent ->
                if (event.status == "REQUESTED") {
                    adminMessages(
                        "New payout request",
                        "${event.currency} ${Money.str(event.amount)} requested by seller ${event.sellerId}.",
                        "PAYOUT",
                        event.payoutRequestId,
                    )
                } else {
                    emptyList()
                }
            is DisputeCreatedEvent -> adminMessages("New dispute", event.subject, "DISPUTE", event.disputeId)
            is RefundStatusChangedEvent ->
                if (event.toStatus == "PENDING") {
                    adminMessages("New refund request", "Refund ${event.refundId} is awaiting review.", "REFUND", event.refundId)
                } else {
                    emptyList()
                }
            is RmaStatusChangedEvent ->
                if (event.toStatus == "REQUESTED") {
                    adminMessages("New return request", "RMA ${event.rmaId} is awaiting review.", "RMA", event.rmaId)
                } else {
                    emptyList()
                }
            else -> emptyList()
        }

    private suspend fun sellerForShop(shopId: String?): Pair<String, String?>? {
        if (shopId == null) return null
        return query {
            SellerDAO.find { SellerTable.shopId eq shopId.entityID(ShopTable) }.firstOrNull()
        }?.let { recipientForUserId(it.userId.value) }
    }

    private suspend fun recipientForSellerId(sellerId: String): Pair<String, String?>? =
        query { SellerDAO.findById(sellerId)?.userId?.value }?.let { recipientForUserId(it) }

    private suspend fun recipientForUserId(userId: String): Pair<String, String?>? =
        query {
            UserDAO.findById(userId)?.takeIf { it.isActive }?.let { it.id.value to it.email }
        }

    private suspend fun adminMessages(
        title: String,
        body: String,
        resourceType: String,
        resourceId: String?,
    ): List<NotificationMessage> =
        query {
            UserDAO.find {
                (UserTable.userType inList listOf(UserType.ADMIN, UserType.SUPER_ADMIN)) and (UserTable.isActive eq true)
            }.map { user ->
                NotificationMessage(user.id.value, user.email, "ADMIN_QUEUE", title, body, resourceType, resourceId)
            }
        }

    override suspend fun onEvent(event: DomainEvent) {
        if (event is BackInStockEvent) {
            notifyStockAlerts(event)
            return
        }
        // Payout capture must propagate (see captureSellerPayout), so it runs outside
        // the non-fatal notification try/catch below.
        if (event is PaymentCompletedEvent) {
            val failures = mutableListOf<Throwable>()
            messagesFor(event).forEach { message ->
                runCatching { dispatcher.dispatch(message) }
                    .onFailure { failures += it }
            }
            captureSellerPayout(event)
            failures.firstOrNull()?.let { throw it }
            return
        }
        val failures = mutableListOf<Throwable>()
        messagesFor(event).forEach { message ->
            runCatching { dispatcher.dispatch(message) }
                .onFailure { failures += it }
        }
        // A failed email/SMS must reach the outbox ack policy so it is retried
        // instead of being marked delivered.
        failures.firstOrNull()?.let {
            log.error("Notification dispatch failed for ${event::class.simpleName}", it)
            throw it
        }
    }

    private suspend fun notifyStockAlerts(event: BackInStockEvent) {
        val product = query { ProductDAO.findById(event.productId) } ?: return
        if (product.deletedAt != null || product.status != com.piashcse.constants.ProductStatus.ACTIVE) return
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
        val content =
            NotificationTemplates.backInStock(
                product.name,
                StorefrontLinks.product(product.id.value),
            )
        val users =
            query {
                UserDAO.find { UserTable.id inList alerts.map { it.userId }.distinct() }
                    .associateBy { it.id.value }
            }
        alerts.forEach { alert ->
            val user = users[alert.userId.value] ?: return@forEach
            if (!user.isActive) return@forEach
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
                // Idempotent fallback for payments created before the ledger
                // was moved into the payment transaction.
                PayoutLedger.createForOrderTx(order)
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

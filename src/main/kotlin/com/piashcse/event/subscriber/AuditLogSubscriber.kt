package com.piashcse.event.subscriber

import com.piashcse.event.*
import com.piashcse.feature.audit_log.AuditLogRepository
import org.slf4j.LoggerFactory

class AuditLogSubscriber(
    private val auditLogRepository: AuditLogRepository,
) : Subscriber {
    private val log = LoggerFactory.getLogger(AuditLogSubscriber::class.java)

    private fun isExtendedEvent(event: DomainEvent): Boolean =
        when (event) {
            is SellerReviewCreatedEvent,
            is ProductReviewCreatedEvent,
            is DisputeCreatedEvent,
            is KycStatusChangedEvent,
            is ShopStatusChangedEvent,
            is PayoutStatusChangedEvent,
            -> true
            else -> false
        }

    override suspend fun onEvent(event: DomainEvent) {
        try {
            if (isExtendedEvent(event)) {
                auditExtended(event)
                return
            }
            when (event) {
                is OrderPlacedEvent ->
                    auditLogRepository.log(
                        actorId = event.userId,
                        actorEmail = event.email,
                        actorRole = "CUSTOMER",
                        action = "ORDER_PLACED",
                        resourceType = "ORDER",
                        resourceId = event.orderId,
                        details = "Order ${event.orderNumber} placed, total ${event.total.toPlainString()}",
                    )
                is OrderStatusChangedEvent ->
                    auditLogRepository.log(
                        actorId = event.userId,
                        actorEmail = event.email,
                        actorRole = "CUSTOMER",
                        action = "ORDER_${event.toStatus}",
                        resourceType = "ORDER",
                        resourceId = event.orderId,
                        details = "Order ${event.orderNumber}: ${event.fromStatus} -> ${event.toStatus}",
                    )
                is UserRegisteredEvent ->
                    auditLogRepository.log(
                        actorId = event.userId,
                        actorEmail = event.email,
                        actorRole = event.userType,
                        action = "USER_REGISTERED",
                        resourceType = "USER",
                        resourceId = event.userId,
                    )
                is PaymentCompletedEvent ->
                    auditLogRepository.log(
                        actorId = event.userId,
                        actorEmail = event.email,
                        actorRole = "CUSTOMER",
                        action = "PAYMENT_COMPLETED",
                        resourceType = "PAYMENT",
                        resourceId = event.paymentId,
                        details = "Payment for order ${event.orderId}, amount ${event.amount.toPlainString()}",
                    )
                is AdminActionEvent ->
                    // Covers LOGIN_FAILED, PAYOUT_MARKED_PAID, COUPON_CREATED/UPDATED,
                    // CONSENT_GRANTED/UPDATED/REVOKED, PROFILE_UPDATED — publishers use
                    // EventBus.publishAdminAction so no new event types are needed.
                    auditLogRepository.log(
                        actorId = event.actorId,
                        actorEmail = event.actorEmail,
                        actorRole = event.actorRole,
                        action = event.action,
                        resourceType = event.resourceType,
                        resourceId = event.resourceId,
                        details = event.details,
                        outcome = if (event.action == "LOGIN_FAILED") "FAILURE" else "SUCCESS",
                    )
                is RefundStatusChangedEvent ->
                    auditLogRepository.log(
                        actorId = event.userId,
                        actorEmail = event.email,
                        actorRole = "SELLER",
                        action = "REFUND_${event.toStatus}",
                        resourceType = "REFUND",
                        resourceId = event.refundId,
                        details = "Refund for order ${event.orderId}: ${event.fromStatus} -> ${event.toStatus}",
                    )
                is RmaStatusChangedEvent ->
                    auditLogRepository.log(
                        actorId = event.userId,
                        actorEmail = event.email,
                        actorRole = "CUSTOMER",
                        action = "RMA_${event.toStatus}",
                        resourceType = "RMA",
                        resourceId = event.rmaId,
                        details = "RMA for order ${event.orderId}: ${event.fromStatus} -> ${event.toStatus}",
                    )
                is SendEmailEvent -> Unit
                is BackInStockEvent -> Unit
                is LowStockEvent -> Unit
                is SellerReviewCreatedEvent,
                is ProductReviewCreatedEvent,
                is ReviewReplyEvent,
                is DisputeCreatedEvent,
                is KycStatusChangedEvent,
                is ShopStatusChangedEvent,
                is PayoutStatusChangedEvent,
                -> Unit
            }
        } catch (e: Exception) {
            log.error("Failed to write audit log for event ${event::class.simpleName}", e)
            // Do not ACK the outbox row when the audit record was not written.
            throw e
        }
    }

    private suspend fun auditExtended(event: DomainEvent) {
        when (event) {
            is SellerReviewCreatedEvent ->
                auditLogRepository.log(
                    actorId = event.userId,
                    actorEmail = "",
                    actorRole = "CUSTOMER",
                    action = "SELLER_REVIEW_CREATED",
                    resourceType = "SELLER_REVIEW",
                    resourceId = event.reviewId,
                    details = "Rating ${event.rating}",
                )
            is ProductReviewCreatedEvent ->
                auditLogRepository.log(
                    actorId = event.userId,
                    actorEmail = "",
                    actorRole = "CUSTOMER",
                    action = "PRODUCT_REVIEW_CREATED",
                    resourceType = "PRODUCT_REVIEW",
                    resourceId = event.reviewId,
                    details = "Product ${event.productId}, rating ${event.rating}",
                )
            is DisputeCreatedEvent ->
                auditLogRepository.log(
                    actorId = event.userId,
                    actorEmail = "",
                    actorRole = "CUSTOMER",
                    action = "DISPUTE_CREATED",
                    resourceType = "DISPUTE",
                    resourceId = event.disputeId,
                    details = event.subject,
                )
            is KycStatusChangedEvent ->
                auditLogRepository.log(
                    actorId = event.userId,
                    actorEmail = "",
                    actorRole = "SELLER",
                    action = "KYC_${event.status}",
                    resourceType = "KYC",
                    resourceId = event.sellerId,
                    details = event.reason,
                )
            is ShopStatusChangedEvent ->
                auditLogRepository.log(
                    actorId = event.userId,
                    actorEmail = "",
                    actorRole = "SELLER",
                    action = "SHOP_${event.status}",
                    resourceType = "SHOP",
                    resourceId = event.shopId,
                    details = event.reason,
                )
            is PayoutStatusChangedEvent ->
                auditLogRepository.log(
                    actorId = event.userId,
                    actorEmail = "",
                    actorRole = "SELLER",
                    action = "PAYOUT_${event.status}",
                    resourceType = "PAYOUT",
                    resourceId = event.payoutRequestId,
                    details = "${event.currency} ${event.amount.toPlainString()}" + (event.reason?.let { " - $it" } ?: ""),
                )
            else -> Unit
        }
    }
}

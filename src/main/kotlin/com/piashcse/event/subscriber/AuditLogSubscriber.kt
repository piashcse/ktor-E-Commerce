package com.piashcse.event.subscriber

import com.piashcse.event.*
import com.piashcse.feature.audit_log.AuditLogRepository
import org.slf4j.LoggerFactory

class AuditLogSubscriber(
    private val auditLogRepository: AuditLogRepository,
) : Subscriber {
    private val log = LoggerFactory.getLogger(AuditLogSubscriber::class.java)

    override suspend fun onEvent(event: DomainEvent) {
        try {
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
                    auditLogRepository.log(
                        actorId = event.actorId,
                        actorEmail = event.actorEmail,
                        actorRole = event.actorRole,
                        action = event.action,
                        resourceType = event.resourceType,
                        resourceId = event.resourceId,
                        details = event.details,
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
                is SendEmailEvent -> Unit
            }
        } catch (e: Exception) {
            log.error("Failed to write audit log for event ${event::class.simpleName}", e)
        }
    }
}

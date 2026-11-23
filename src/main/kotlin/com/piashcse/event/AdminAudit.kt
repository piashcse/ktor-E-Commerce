package com.piashcse.event

import com.piashcse.database.entities.UserDAO
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Publishes a consistent audit event for an authenticated action. The actor's email/role
 * are resolved from the user row so callers only need the acting user id.
 *
 * The event is written to the transactional outbox so a crash after the audited
 * mutation cannot lose the audit trail; the outbox poller relays it to the
 * AuditLogSubscriber.
 */
object AdminAudit {
    fun publish(
        actorUserId: String,
        action: String,
        resourceType: String,
        resourceId: String?,
        details: String? = null,
    ) {
        // Callers may be inside or outside a transaction; a nested transaction
        // reuses the outer one and keeps this helper safe in both cases.
        val actor = runCatching { transaction { UserDAO.findById(actorUserId) } }.getOrNull()
        val event =
            AdminActionEvent(
                actorId = actorUserId,
                actorEmail = actor?.email.orEmpty(),
                actorRole = actor?.userType?.name ?: "ADMIN",
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                details = details,
            )
        runCatching {
            transaction {
                OutboxPublisher.enqueueTx(OutboxAggregate.AUDIT, resourceId ?: actorUserId, event)
            }
        }
    }
}

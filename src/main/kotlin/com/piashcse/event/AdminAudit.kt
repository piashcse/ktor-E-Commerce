package com.piashcse.event

import com.piashcse.database.entities.UserDAO
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Publishes a consistent audit event for an authenticated action. The actor's email/role
 * are resolved from the user row so callers only need the acting user id.
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
        EventBus.publishAdminAction(
            Triple(actorUserId, actor?.email.orEmpty(), actor?.userType?.name ?: "ADMIN"),
            action,
            resourceType,
            resourceId,
            details,
        )
    }
}

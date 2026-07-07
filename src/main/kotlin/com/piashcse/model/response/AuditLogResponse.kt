package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class AuditLogResponse(val id: String, val actorId: String, val actorEmail: String, val actorRole: String,
                            val action: String, val resourceType: String, val resourceId: String? = null,
                            val details: String? = null, val ipAddress: String? = null, val userAgent: String? = null,
                            val outcome: String, val executedAt: String, val createdAt: String)

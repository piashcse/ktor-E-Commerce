package com.piashcse.feature.webhook

import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class BulkWebhookReplayRequest(
    val source: WebhookSource,
    val ids: List<String>,
)

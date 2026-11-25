package com.piashcse.feature.outbox

import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RequeueDeadLettersRequest(val ids: List<String>)

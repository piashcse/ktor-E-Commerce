@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.apikey

import com.piashcse.constants.Permission
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Self-service API-key management. */
fun Route.apiKeyRoutes() {
    val service: ApiKeyService by inject()
    requirePermission(Permission.API_KEY_MANAGE) {
        get {
            call.respondOk(service.list(call.currentUserId))
        }.describe {
            summary = "List API keys"
            responses { ok<List<ApiKeyResponse>>("Active and revoked keys") }
        }

        post {
            call.respondCreated(service.create(call.currentUserId, call.receive<ApiKeyCreateRequest>()))
        }.describe {
            summary = "Create API key"
            description = "The plaintext key is returned once"
            requestBody { jsonBody<ApiKeyCreateRequest>() }
            responses { created<ApiKeyCreatedResponse>("Key created") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!service.revoke(call.currentUserId, id)) throw NotFoundException("API key not found")
            call.respondDeleted("API key revoked")
        }.describe { summary = "Revoke API key" }
    }
}

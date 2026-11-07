package com.piashcse.feature.apikey

import com.piashcse.constants.Permission
import com.piashcse.plugin.requirePermission
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Self-service API-key management. */
fun Route.apiKeyRoutes() {
    val service: ApiKeyService by inject()
    requirePermission(Permission.API_KEY_MANAGE) {
        get {
            call.respondOk(service.list(call.currentUserId))
        }

        post {
            call.respondCreated(service.create(call.currentUserId, call.receive<ApiKeyCreateRequest>()))
        }

        delete("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw com.piashcse.utils.validator.ValidationException("id is required")
            if (!service.revoke(call.currentUserId, id)) throw NotFoundException("API key not found")
            call.respondOk(MessageResponse("API key revoked"))
        }
    }
}

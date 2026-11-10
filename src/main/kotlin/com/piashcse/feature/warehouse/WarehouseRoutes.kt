package com.piashcse.feature.warehouse

import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Seller warehouse management. */
fun Route.warehouseSellerRoutes() {
    val repo: WarehouseRepository by inject()
    sellerWriteRateLimit {
        get {
            call.respondOk(repo.list(call.currentUserId))
        }

        post {
            call.respondCreated(repo.create(call.currentUserId, call.receive<WarehouseRequest>()))
        }

        put("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw com.piashcse.utils.validator.ValidationException("id is required")
            call.respondOk(repo.update(call.currentUserId, id, call.receive<WarehouseRequest>()))
        }

        delete("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw com.piashcse.utils.validator.ValidationException("id is required")
            repo.deactivate(call.currentUserId, id)
            call.respondOk(MessageResponse("Warehouse deactivated"))
        }
    }
}

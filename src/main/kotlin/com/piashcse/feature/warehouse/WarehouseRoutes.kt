package com.piashcse.feature.warehouse

import com.piashcse.plugin.created
import com.piashcse.plugin.docs
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Seller warehouse management. */
fun Route.warehouseSellerRoutes() {
    val repo: WarehouseRepository by inject()
    sellerWriteRateLimit {
        get {
            call.respondOk(repo.list(call.currentUserId))
        }.docs("List warehouses") {
            responses { ok<List<WarehouseResponse>>("Seller warehouses") }
        }

        post {
            call.respondCreated(repo.create(call.currentUserId, call.receive<WarehouseRequest>()))
        }.docs("Create warehouse") {
            requestBody { jsonBody<WarehouseRequest>() }
            responses { created<WarehouseResponse>("Warehouse created") }
        }

        put("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw ValidationException("id is required")
            call.respondOk(repo.update(call.currentUserId, id, call.receive<WarehouseRequest>()))
        }.docs("Update warehouse") {
            requestBody { jsonBody<WarehouseRequest>() }
            responses { ok<WarehouseResponse>("Warehouse updated") }
        }

        delete("{id}") {
            val id =
                call.parameters["id"]?.takeIf { it.isNotBlank() }
                    ?: throw ValidationException("id is required")
            repo.deactivate(call.currentUserId, id)
            call.respondOk(MessageResponse("Warehouse deactivated"))
        }.docs("Deactivate warehouse")
    }
}

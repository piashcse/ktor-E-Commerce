@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.warehouse

import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Seller warehouse management. */
fun Route.warehouseSellerRoutes() {
    val repo: WarehouseRepository by inject()
    sellerWriteRateLimit {
        get {
            call.respondOk(repo.list(call.currentUserId))
        }.describe {
            summary = "List warehouses"
            responses { ok<List<WarehouseResponse>>("Seller warehouses") }
        }

        post {
            call.respondCreated(repo.create(call.currentUserId, call.receive<WarehouseRequest>()))
        }.describe {
            summary = "Create warehouse"
            requestBody { jsonBody<WarehouseRequest>() }
            responses { created<WarehouseResponse>("Warehouse created") }
        }

        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(repo.update(call.currentUserId, id, call.receive<WarehouseRequest>()))
        }.describe {
            summary = "Update warehouse"
            requestBody { jsonBody<WarehouseRequest>() }
            responses { ok<WarehouseResponse>("Warehouse updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            repo.deactivate(call.currentUserId, id)
            call.respondOk(MessageResponse("Warehouse deactivated"))
        }.describe { summary = "Deactivate warehouse" }
    }
}

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.promotion

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.queryBooleanOrNull
import com.piashcse.utils.extension.requireSellerShopId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Admin automatic-promotion management. */
fun Route.promotionAdminRoutes() {
    val service: PromotionService by inject()
    get {
        val (limit, offset) = call.pageRequest()
        val active = call.queryBooleanOrNull("active")
        call.respondOk(service.list(active, limit, offset))
    }.describe { summary = "Admin: list promotions" }

    adminWriteRateLimit {
        post {
            call.respondCreated(service.create(call.receive<PromotionRequest>()))
        }.describe {
            summary = "Admin: create a promotion"
            requestBody { jsonBody<PromotionRequest>() }
            responses { created<PromotionResponse>("Promotion created") }
        }

        put("{id}") {
            call.respondOk(service.update(call.requirePathParameter("id"), call.receive<PromotionRequest>()))
        }.describe {
            summary = "Admin: update a promotion"
            requestBody { jsonBody<PromotionRequest>() }
            responses { ok<PromotionResponse>("Promotion updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!service.delete(id)) throw NotFoundException("Promotion not found")
            call.respondDeleted("Promotion deactivated")
        }.describe { summary = "Admin: deactivate a promotion" }
    }
}

/** Seller automatic-promotion management scoped to the seller's shop. */
fun Route.sellerPromotionRoutes() {
    val service: PromotionService by inject()
    get {
        val (limit, offset) = call.pageRequest()
        val shopId = requireSellerShopId(call.currentUserId)
        call.respondOk(service.listForShop(shopId, call.queryBooleanOrNull("active"), limit, offset))
    }.describe { summary = "Seller: list my promotions" }

    sellerWriteRateLimit {
        post {
            val shopId = requireSellerShopId(call.currentUserId)
            call.respondCreated(service.createForShop(shopId, call.receive<PromotionRequest>()))
        }.describe {
            summary = "Seller: create a promotion"
            requestBody { jsonBody<PromotionRequest>() }
        }
        put("{id}") {
            val shopId = requireSellerShopId(call.currentUserId)
            call.respondOk(
                service.updateForShop(shopId, call.requirePathParameter("id"), call.receive<PromotionRequest>()),
            )
        }.describe {
            summary = "Seller: update a promotion"
            requestBody { jsonBody<PromotionRequest>() }
        }
        delete("{id}") {
            val shopId = requireSellerShopId(call.currentUserId)
            if (!service.deleteForShop(shopId, call.requirePathParameter("id"))) {
                throw NotFoundException("Promotion not found")
            }
            call.respondDeleted("Promotion deactivated")
        }.describe { summary = "Seller: deactivate a promotion" }
    }
}

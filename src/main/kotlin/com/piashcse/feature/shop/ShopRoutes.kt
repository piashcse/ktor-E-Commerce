package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.NotFoundException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Shop Discovery Routes.
 */
fun Route.shopRoutes(shopService: ShopService) {
    /**
     * @tag Shop
     * @description Retrieve detailed information about a specific shop
     */
    get("/{id}") {
        val shopId = call.requirePathParameter("id")
        val shop = shopService.getShopById(shopId) ?: throw NotFoundException(Message.Shops.NOT_FOUND)
        call.respond(HttpStatusCode.OK, shop)
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Shop
         * @description Retrieve public shops with filters
         */
        get("/public") {
            val (limit, offset) = call.paginateQueryParams()
            call.respond(
                HttpStatusCode.OK,
                shopService.getShops(call.request.queryParameters["status"], call.request.queryParameters["category"], limit, offset),
            )
        }

        /**
         * @tag Shop
         * @description Retrieve shops by category
         */
        get("/category/{categoryId}") {
            val categoryId = call.requirePathParameter("categoryId")
            val (limit, offset) = call.paginateQueryParams()
            call.respond(HttpStatusCode.OK, shopService.getShopsByCategory(categoryId, limit, offset))
        }

        /**
         * @tag Shop
         * @description Retrieve featured shops
         */
        get("/featured") {
            val (limit, offset) = call.paginateQueryParams()
            call.respond(HttpStatusCode.OK, shopService.getFeaturedShops(limit, offset))
        }
    }
}

/**
 * V1 Seller Shop Management Routes.
 */
fun Route.shopSellerRoutesV1(shopService: ShopService) {
    /**
     * @tag Shop
     * @description Seller: Create a new shop
     */
    post {
        val requestBody = call.receive<ShopRequest>()
        call.respond(HttpStatusCode.OK, shopService.createShop(call.currentUserId, requestBody))
    }

    /**
     * @tag Shop
     * @description Seller: Retrieve owned shops
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respond(HttpStatusCode.OK, shopService.getShopsByUser(call.currentUserId, limit, offset))
    }

    /**
     * @tag Shop
     * @description Seller: Update shop details
     */
    put("/{id}") {
        val shopId = call.requirePathParameter("id")
        val requestBody = call.receive<UpdateShopRequest>()
        call.respond(HttpStatusCode.OK, shopService.updateShop(call.currentUserId, shopId, requestBody))
    }
}

/**
 * V2 Seller Shop Management Routes.
 */
fun Route.shopSellerRoutesV2(shopService: ShopService) {
    /**
     * @tag Shop
     * @description (V2) Seller: Update shop details with enhanced params and response structure
     */
    put("/{shopId}") {
        val shopId = call.requirePathParameter("shopId")
        val source = call.request.queryParameters["source"] ?: "unknown"

        val requestBody = call.receive<UpdateShopRequest>()
        val response = shopService.updateShop(call.currentUserId, shopId, requestBody)

        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "v2_data" to response,
                "source" to source,
            ),
        )
    }
}

/**
 * Admin Shop Management Routes.
 */
fun Route.shopAdminRoutes(shopService: ShopService) {
    /**
     * @tag Shop
     * @description Admin: Retrieve shops filtered by status
     */
    get("/status") {
        val statusParam = call.requireQueryParameter("status")
        val status =
            try {
                ShopStatus.valueOf(statusParam.uppercase())
            } catch (e: IllegalArgumentException) {
                throw InvalidEnumValueException(
                    message = "Invalid shop status: $statusParam",
                    enumName = ShopStatus.values().joinToString(", ") { it.name },
                    invalidValue = statusParam,
                )
            }
        val (limit, offset) = call.paginateQueryParams()
        call.respond(HttpStatusCode.OK, shopService.getShopsByStatus(status, limit, offset))
    }

    /**
     * @tag Shop
     * @description Admin: Approve a pending shop application
     */
    put("/approve/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respond(HttpStatusCode.OK, shopService.approveShop(shopId))
    }

    /**
     * @tag Shop
     * @description Admin: Reject a shop application
     */
    put("/reject/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respond(HttpStatusCode.OK, shopService.rejectShop(shopId))
    }

    /**
     * @tag Shop
     * @description Admin: Suspend an active shop
     */
    put("/suspend/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respond(HttpStatusCode.OK, shopService.suspendShop(shopId))
    }

    /**
     * @tag Shop
     * @description Admin: Activate a suspended shop
     */
    put("/activate/{id}") {
        val shopId = call.requirePathParameter("id")
        call.respond(HttpStatusCode.OK, shopService.activateShop(shopId))
    }
}

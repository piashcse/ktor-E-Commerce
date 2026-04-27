package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.sellerAuth
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.MissingParameterException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public and seller-specific shop routes.
 */
fun Route.shopRoutes(shopController: ShopService, version: Int = 1) {
    sellerAuth {
        if (version == 1) {
            /**
             * @tag Shop
             * @description Seller: Create a new shop
             */
            post {
                val requestBody = call.receive<ShopRequest>()
                call.respond(HttpStatusCode.OK, shopController.createShop(call.currentUserId, requestBody))
            }

            /**
             * @tag Shop
             * @description Seller: Update shop details
             */
            put("/{id}") {
                val (shopId) = call.requireParameters("id")
                val requestBody = call.receive<UpdateShopRequest>()
                call.respond(HttpStatusCode.OK, shopController.updateShop(call.currentUserId, shopId, requestBody))
            }

            /**
             * @tag Shop
             * @description Seller: Retrieve owned shops
             */
            get {
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getShopsByUser(call.currentUserId, limit, offset))
            }

            /**
             * @tag Shop
             * @description Retrieve detailed information about a specific shop
             */
            get("/{id}") {
                val (shopId) = call.requireParameters("id")
                val shop = shopController.getShopById(shopId)
                    ?: throw NotFoundException(Message.Shops.NOT_FOUND)
                call.respond(HttpStatusCode.OK, shop)
            }
        }

        if (version >= 2) {
            /**
             * @tag Shop
             * @description (V2) Seller: Update shop details
             */
            put("/{id}") {
                val (shopId) = call.requireParameters("id")
                val requestBody = call.receive<UpdateShopRequest>()
                val response = shopController.updateShop(call.currentUserId, shopId, requestBody)
                call.respond(HttpStatusCode.OK, mapOf("v2_migration" to true, "data" to response))
            }
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        if (version == 1) {
            /**
             * @tag Shop
             * @description Retrieve public shops with filters
             */
            get("/public") {
                val status = call.parameters["status"]
                val category = call.parameters["category"]
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getShops(status, category, limit, offset))
            }

            /**
             * @tag Shop
             * @description Retrieve shops by category
             */
            get("/category/{categoryId}") {
                val (categoryId) = call.requireParameters("categoryId")
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getShopsByCategory(categoryId, limit, offset))
            }

            /**
             * @tag Shop
             * @description Retrieve featured shops
             */
            get("/featured") {
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getFeaturedShops(limit, offset))
            }
        }
    }
}

/**
 * Admin shop management routes.
 */
fun Route.shopAdminRoutes(shopController: ShopService) {
    adminAuth {
        /**
         * @tag Shop
         * @description Admin: Retrieve shops filtered by status
         */
        get("/status") {
            val statusParam = call.parameters["status"]
                ?: throw MissingParameterException("status")

            val status = try {
                ShopStatus.valueOf(statusParam.uppercase())
            } catch (e: IllegalArgumentException) {
                throw InvalidEnumValueException(
                    message = "Invalid shop status: $statusParam",
                    enumName = ShopStatus.values().joinToString(", ") { it.name },
                    invalidValue = statusParam
                )
            }
            val (limit, offset) = call.paginationParameters()
            call.respond(HttpStatusCode.OK, shopController.getShopsByStatus(status, limit, offset))
        }

        /**
         * @tag Shop
         * @description Admin: Approve a pending shop
         */
        put("/approve/{id}") {
            val (shopId) = call.requireParameters("id")
            call.respond(HttpStatusCode.OK, shopController.approveShop(shopId))
        }

        /**
         * @tag Shop
         * @description Admin: Reject a shop application
         */
        put("/reject/{id}") {
            val (shopId) = call.requireParameters("id")
            call.respond(HttpStatusCode.OK, shopController.rejectShop(shopId))
        }

        /**
         * @tag Shop
         * @description Admin: Suspend an active shop
         */
        put("/suspend/{id}") {
            val (shopId) = call.requireParameters("id")
            call.respond(HttpStatusCode.OK, shopController.suspendShop(shopId))
        }

        /**
         * @tag Shop
         * @description Admin: Activate a shop
         */
        put("/activate/{id}") {
            val (shopId) = call.requireParameters("id")
            call.respond(HttpStatusCode.OK, shopController.activateShop(shopId))
        }
    }
}

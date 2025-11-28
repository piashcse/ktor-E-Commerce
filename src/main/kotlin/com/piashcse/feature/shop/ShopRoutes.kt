package com.piashcse.feature.shop

import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing shops.
 *
 * @param shopController The controller handling shop-related operations.
 */
fun Route.shopRoutes(shopController: ShopService) {
    // Main route for shop management
    route("/shop") {
        
        // Routes for sellers to manage their shops
        authenticate(RoleManagement.SELLER.role) {
            
            /**
             * @tag Shop
             * @body requestBody
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<ShopRequest>()
                call.respond(ApiResponse.success(shopController.createShop(call.currentUser().userId, requestBody), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path id (required)
             * @body requestBody
             * @response 200 [Response]
             */
            put("/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                val requestBody = call.receive<UpdateShopRequest>()
                call.respond(ApiResponse.success(shopController.updateShop(call.currentUser().userId, shopId, requestBody), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @response 200 [Response]
             */
            get {
                call.respond(ApiResponse.success(shopController.getShopsByUser(call.currentUser().userId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path shopId (required)
             * @response 200 [Response]
             */
            get("/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@get
                val shop = shopController.getShopById(shopId)
                if (shop != null) {
                    call.respond(ApiResponse.success(shop, HttpStatusCode.OK))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        // Routes for customers to view shops
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role) {
            
            /**
             * @tag Shop
             * @query status
             * @query category
             * @query limit
             * @response 200 [Response]
             */
            get("/public") {
                val status = call.parameters["status"]
                val category = call.parameters["category"]
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                call.respond(ApiResponse.success(shopController.getShops(status, category, limit), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path categoryId (required)
             * @response 200 [Response]
             */
            get("/category/{categoryId}") {
                val (categoryId) = call.requiredParameters("categoryId") ?: return@get
                call.respond(ApiResponse.success(shopController.getShopsByCategory(categoryId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @response 200 [Response]
             */
            get("/featured") {
                call.respond(ApiResponse.success(shopController.getFeaturedShops(), HttpStatusCode.OK))
            }
        }

        // Routes for admins to manage all shops
        authenticate(RoleManagement.ADMIN.role) {
            
            /**
             * @tag Shop
             * @query status
             * @response 200 [Response]
             */
            get("/status") {
                val statusParam = call.parameters["status"] ?: return@get
                val status = try {
                    com.piashcse.constants.ShopStatus.valueOf(statusParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid status")
                    return@get
                }
                call.respond(ApiResponse.success(shopController.getShopsByStatus(status), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path shopId (required)
             * @response 200 [Response]
             */
            put("/approve/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.approveShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path shopId (required)
             * @response 200 [Response]
             */
            put("/reject/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.rejectShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path shopId (required)
             * @response 200 [Response]
             */
            put("/suspend/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.suspendShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @path shopId (required)
             * @response 200 [Response]
             */
            put("/activate/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.activateShop(shopId), HttpStatusCode.OK))
            }
        }
    }
}
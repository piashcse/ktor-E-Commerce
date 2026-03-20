package com.piashcse.feature.shop

import com.piashcse.constants.ShopStatus
import com.piashcse.constants.UserType
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
    route("/shop") {
        authenticate(RoleManagement.SELLER.role) {

            /**
             * @tag Shop
             * @description Create a new shop for the authenticated seller
             * @operationId createShop
             * @body ShopRequest Shop creation request with shop details
             * @response 200 Shop created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ShopRequest>()
                call.respond(ApiResponse.success(shopController.createShop(call.currentUser().userId, requestBody), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Update shop details for the authenticated seller
             * @operationId updateShop
             * @path id (required) Unique identifier of the shop to update
             * @body UpdateShopRequest Shop update request with new details
             * @response 200 Shop updated successfully
             * @security jwtToken
             */
            put("/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                val requestBody = call.receive<UpdateShopRequest>()
                call.respond(ApiResponse.success(shopController.updateShop(call.currentUser().userId, shopId, requestBody), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Retrieve all shops owned by the authenticated seller
             * @operationId getShopsByUser
             * @response 200 Seller's shops retrieved successfully
             * @security jwtToken
             */
            get {
                call.respond(ApiResponse.success(shopController.getShopsByUser(call.currentUser().userId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Retrieve detailed information about a specific shop
             * @operationId getShopById
             * @path id (required) Unique identifier of the shop
             * @response 200 Shop details retrieved successfully
             * @response 404 Shop not found
             * @security jwtToken
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

        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role) {

            /**
             * @tag Shop
             * @description Retrieve a paginated list of shops with optional filters
             * @operationId getShops
             * @query status Filter shops by status
             * @query category Filter shops by category
             * @query limit Maximum number of shops to return (default 20)
             * @response 200 Shops retrieved successfully
             * @security jwtToken
             */
            get("/public") {
                val status = call.parameters["status"]
                val category = call.parameters["category"]
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                call.respond(ApiResponse.success(shopController.getShops(status, category, limit), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Retrieve all shops belonging to a specific category
             * @operationId getShopsByCategory
             * @path categoryId (required) Unique identifier of the shop category
             * @response 200 Shops in category retrieved successfully
             * @security jwtToken
             */
            get("/category/{categoryId}") {
                val (categoryId) = call.requiredParameters("categoryId") ?: return@get
                call.respond(ApiResponse.success(shopController.getShopsByCategory(categoryId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Retrieve all shops marked as featured
             * @operationId getFeaturedShops
             * @response 200 Featured shops retrieved successfully
             * @security jwtToken
             */
            get("/featured") {
                call.respond(ApiResponse.success(shopController.getFeaturedShops(), HttpStatusCode.OK))
            }
        }

        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {

            /**
             * @tag Shop
             * @description Admin-only: Retrieve shops filtered by their status
             * @operationId getShopsByStatus
             * @query status (required) Shop status to filter by (PENDING, APPROVED, REJECTED, SUSPENDED, etc.)
             * @response 200 Shops filtered by status retrieved successfully
             * @response 400 Invalid status parameter
             * @security jwtToken
             */
            get("/status") {
                val statusParam = call.parameters["status"] ?: return@get
                val status = try {
                    ShopStatus.valueOf(statusParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid status")
                    return@get
                }
                call.respond(ApiResponse.success(shopController.getShopsByStatus(status), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Admin-only: Approve a pending shop for operation
             * @operationId approveShop
             * @path id (required) Unique identifier of the shop to approve
             * @response 200 Shop approved successfully
             * @security jwtToken
             */
            put("/approve/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.approveShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Admin-only: Reject a shop application
             * @operationId rejectShop
             * @path id (required) Unique identifier of the shop to reject
             * @response 200 Shop rejected successfully
             * @security jwtToken
             */
            put("/reject/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.rejectShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Admin-only: Suspend an active shop temporarily
             * @operationId suspendShop
             * @path id (required) Unique identifier of the shop to suspend
             * @response 200 Shop suspended successfully
             * @security jwtToken
             */
            put("/suspend/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.suspendShop(shopId), HttpStatusCode.OK))
            }

            /**
             * @tag Shop
             * @description Admin-only: Activate a suspended or inactive shop
             * @operationId activateShop
             * @path id (required) Unique identifier of the shop to activate
             * @response 200 Shop activated successfully
             * @security jwtToken
             */
            put("/activate/{id}") {
                val (shopId) = call.requiredParameters("id") ?: return@put
                call.respond(ApiResponse.success(shopController.activateShop(shopId), HttpStatusCode.OK))
            }
        }
    }
}
package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.InvalidEnumValueException
import com.piashcse.utils.MissingParameterException
import com.piashcse.utils.NotFoundException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
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
                call.respond(HttpStatusCode.OK, shopController.createShop(call.currentUserId, requestBody))
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
                val (shopId) = call.requireParameters("id")
                val requestBody = call.receive<UpdateShopRequest>()
                call.respond(HttpStatusCode.OK, shopController.updateShop(call.currentUserId, shopId, requestBody))
            }

            /**
             * @tag Shop
             * @description Retrieve all shops owned by the authenticated seller with pagination
             * @operationId getShopsByUser
             * @query limit Maximum number of shops to return (default 20)
             * @query offset Number of shops to skip (default 0)
             * @response 200 Seller's shops retrieved successfully
             * @security jwtToken
             */
            get {
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getShopsByUser(call.currentUserId, limit, offset))
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
                val (shopId) = call.requireParameters("id")
                val shop = shopController.getShopById(shopId)
                    ?: throw NotFoundException(Message.Shops.NOT_FOUND)
                call.respond(HttpStatusCode.OK, shop)
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
             * @query offset Number of shops to skip (default 0)
             * @response 200 Shops retrieved successfully
             * @security jwtToken
             */
            get("/public") {
                val status = call.parameters["status"]
                val category = call.parameters["category"]
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getShops(status, category, limit, offset))
            }

            /**
             * @tag Shop
             * @description Retrieve all shops belonging to a specific category with pagination
             * @operationId getShopsByCategory
             * @path categoryId (required) Unique identifier of the shop category
             * @query limit Maximum number of shops to return (default 20)
             * @query offset Number of shops to skip (default 0)
             * @response 200 Shops in category retrieved successfully
             * @security jwtToken
             */
            get("/category/{categoryId}") {
                val (categoryId) = call.requireParameters("categoryId")
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getShopsByCategory(categoryId, limit, offset))
            }

            /**
             * @tag Shop
             * @description Retrieve all shops marked as featured with pagination
             * @operationId getFeaturedShops
             * @query limit Maximum number of shops to return (default 20)
             * @query offset Number of shops to skip (default 0)
             * @response 200 Featured shops retrieved successfully
             * @security jwtToken
             */
            get("/featured") {
                val (limit, offset) = call.paginationParameters()
                call.respond(HttpStatusCode.OK, shopController.getFeaturedShops(limit, offset))
            }
        }

        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {

            /**
             * @tag Shop
             * @description Admin-only: Retrieve shops filtered by their status with pagination
             * @operationId getShopsByStatus
             * @query status (required) Shop status to filter by (PENDING, APPROVED, REJECTED, SUSPENDED, etc.)
             * @query limit Maximum number of shops to return (default 20)
             * @query offset Number of shops to skip (default 0)
             * @response 200 Shops filtered by status retrieved successfully
             * @response 400 Invalid status parameter
             * @security jwtToken
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
             * @description Admin-only: Approve a pending shop for operation
             * @operationId approveShop
             * @path id (required) Unique identifier of the shop to approve
             * @response 200 Shop approved successfully
             * @security jwtToken
             */
            put("/approve/{id}") {
                val (shopId) = call.requireParameters("id")
                call.respond(HttpStatusCode.OK, shopController.approveShop(shopId))
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
                val (shopId) = call.requireParameters("id")
                call.respond(HttpStatusCode.OK, shopController.rejectShop(shopId))
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
                val (shopId) = call.requireParameters("id")
                call.respond(HttpStatusCode.OK, shopController.suspendShop(shopId))
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
                val (shopId) = call.requireParameters("id")
                call.respond(HttpStatusCode.OK, shopController.activateShop(shopId))
            }
        }
    }
}

package com.piashcse.feature.shop

import com.piashcse.model.request.ShopRequest
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
 * Route for managing shops in the application.
 *
 * @param shopController The controller responsible for handling shop-related operations.
 */
fun Route.shopRoutes(shopController: ShopService) {
    route("shop") {

        // Route for creating a shop
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * POST request to create a new shop.
             *
             * @tag Shop
             * @summary auth[admin]
             * @body [ShopRequest] The body of the request containing shop creation details.
             * @response 200 [ApiResponse] A response containing the created shop details.
             */
            post("/shop") {
                val requestBody = call.receive<ShopRequest>()
                requestBody.validation()
                shopController.createShop(
                    call.currentUser().userId, requestBody.categoryId, requestBody.name
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }

            // Route for getting a list of shops
            /**
             * GET request to retrieve a list of shops.
             *
             * @tag Shop
             * @summary auth[admin]
             * @query limit The maximum number of shops to retrieve.
             * @response 200 [ApiResponse] A response containing the list of shops.
             * @response 400 Bad request if limit is missing
             */
            get("/shop") {
                val (limit) = call.requiredParameters("limit") ?: return@get
                shopController.getShops(
                    call.currentUser().userId, limit.toInt()
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }

            // Route for updating a shop
            /**
             * PUT request to update the name of an existing shop.
             *
             * @tag Shop
             * @summary auth[admin]
             * @path id The ID of the shop to update.
             * @query name The new name of the shop.
             * @response 200 [ApiResponse] A response containing the updated shop details.
             * @response 400 Bad request if required parameters are missing
             */
            put("/shop/{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                shopController.updateShop(
                    call.currentUser().userId, id, name
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }

            // Route for deleting a shop
            /**
             * DELETE request to remove an existing shop.
             *
             * @tag Shop
             * @summary auth[admin]
             * @path id The ID of the shop to delete.
             * @response 200 [ApiResponse] A response indicating the result of the deletion.
             * @response 400 Bad request if id is missing
             */
            delete("/shop/{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                shopController.deleteShop(
                    call.currentUser().userId, id
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
    }
}
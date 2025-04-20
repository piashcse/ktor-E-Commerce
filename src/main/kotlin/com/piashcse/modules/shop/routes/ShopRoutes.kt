package com.piashcse.modules.shop.routes

import com.piashcse.modules.shop.controller.ShopController
import com.piashcse.database.models.shop.ShopRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.shopRoutes(shopController: ShopController) {
    route("shop") {

        // Route for creating a shop
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * POST request to create a new shop.
             *
             * @param requestBody The body of the request containing shop creation details.
             * @response A response containing the created shop details.
             */
            post({
                tags("Shop")
                summary = "auth[admin]"
                request {
                    body<ShopRequest>()
                }
                apiResponse()
            }) {
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
             * @param limit The maximum number of shops to retrieve.
             * @response A response containing the list of shops.
             */
            get({
                tags("Shop")
                summary = "auth[admin]"
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
             * @param id The ID of the shop to update.
             * @param name The new name of the shop.
             * @response A response containing the updated shop details.
             */
            put("{id}", {
                tags("Shop")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("name") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
             * @param id The ID of the shop to delete.
             * @response A response indicating the result of the deletion.
             */
            delete("{id}", {
                tags("Shop")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
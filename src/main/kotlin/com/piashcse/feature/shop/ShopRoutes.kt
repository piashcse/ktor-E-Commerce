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
    route("/shop") {
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag Shop
             * @body [ShopRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<ShopRequest>()
                requestBody.validation()
                shopController.createShop(
                    call.currentUser().userId, requestBody.categoryId, requestBody.name
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }

            /**
             * @tag Shop
             * @query limit (required)
             * @response 200 [Response]
             * @response 400
             */
            get {
                val (limit) = call.requiredParameters("limit") ?: return@get
                shopController.getShops(
                    call.currentUser().userId, limit.toInt()
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }

            /**
             * @tag Shop
             * @path id (required)
             * @query name (required)
             * @response 200 [Response]
             * @response 400
             */
            put("{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                shopController.updateShop(
                    call.currentUser().userId, id, name
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }

            /**
             * @tag Shop
             * @path id (required)
             * @response 200 [Response]
             * @response 400
             */
            delete("{id}") {
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
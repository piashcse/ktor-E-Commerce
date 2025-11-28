package com.piashcse.feature.shop_category

import com.piashcse.model.request.ShopCategoryRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing shop categories.
 *
 * Allows admin users to create, retrieve, update, and delete shop categories.
 *
 * @param shopCategoryController The controller responsible for handling shop category operations.
 */
fun Route.shopCategoryRoutes(shopCategoryController: ShopCategoryService) {
    route("/shop-category") {
        authenticate(RoleManagement.ADMIN.role) {

            /**
             * @tag Shop Category
             * @body [ShopCategoryRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<ShopCategoryRequest>()
                call.respond(
                    ApiResponse.success(
                        shopCategoryController.createCategory(requestBody.name), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Shop Category
             * @query limit (required)
             * @response 200 [Response]
             * @response 400
             */
            get {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        shopCategoryController.getCategories(limit.toInt()),
                        HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Shop Category
             * @path id (required)
             * @response 200 [Response]
             * @response 400
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        shopCategoryController.deleteCategory(id), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Shop Category
             * @path id (required)
             * @query name (required)
             * @response 200 [Response]
             * @response 400
             */
            put("{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                call.respond(
                    ApiResponse.success(
                        shopCategoryController.updateCategory(id, name), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}
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
    authenticate(RoleManagement.ADMIN.role) {

        /**
         * POST request to create a new shop category.
         *
         * @tag Shop Category
         * @summary auth[admin]
         * @body [ShopCategoryRequest] The name of the new shop category.
         * @response 200 [ApiResponse] Success response after creation
         */
        post("/shop-category") {
            val requestBody = call.receive<ShopCategoryRequest>()
            call.respond(
                ApiResponse.success(
                    shopCategoryController.createCategory(requestBody.name), HttpStatusCode.OK
                )
            )
        }

        /**
         * GET request to retrieve a list of shop categories with a specified limit.
         *
         * @tag Shop Category
         * @summary auth[admin]
         * @query limit The maximum number of shop categories to retrieve.
         * @response 200 [ApiResponse] Success response with categories
         * @response 400 Bad request if limit is missing
         */
        get("/shop-category") {
            val (limit) = call.requiredParameters("limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    shopCategoryController.getCategories(limit.toInt()),
                    HttpStatusCode.OK
                )
            )
        }

        /**
         * DELETE request to remove a shop category by its ID.
         *
         * @tag Shop Category
         * @summary auth[admin]
         * @path id The ID of the shop category to delete.
         * @response 200 [ApiResponse] Success response after deletion
         * @response 400 Bad request if id is missing
         */
        delete("/shop-category/{id}") {
            val (id) = call.requiredParameters("id") ?: return@delete
            call.respond(
                ApiResponse.success(
                    shopCategoryController.deleteCategory(id), HttpStatusCode.OK
                )
            )
        }

        /**
         * PUT request to update the name of an existing shop category.
         *
         * @tag Shop Category
         * @summary auth[admin]
         * @path id The ID of the shop category to update.
         * @query name The new name for the shop category.
         * @response 200 [ApiResponse] Success response after update
         * @response 400 Bad request if required parameters are missing
         */
        put("/shop-category/{id}") {
            val (id, name) = call.requiredParameters("id", "name") ?: return@put
            call.respond(
                ApiResponse.success(
                    shopCategoryController.updateCategory(id, name), HttpStatusCode.OK
                )
            )
        }
    }
}
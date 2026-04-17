package com.piashcse.feature.shop_category

import com.piashcse.model.request.ShopCategoryRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
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
    adminAuth {

            /**
             * @tag Shop Category
             * @description Create a new shop category
             * @operationId createShopCategory
             * @body ShopCategoryRequest Category creation request with name
             * @response 200 Shop category created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ShopCategoryRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    shopCategoryController.createCategory(requestBody.name)
                )
            }

            /**
             * @tag Shop Category
             * @description Retrieve a paginated list of all shop categories
             * @operationId getShopCategories
             * @query limit Maximum number of categories to return
             * @query offset Number of categories to skip
             * @response 200 Shop categories retrieved successfully
             * @response 400 Invalid limit parameter
             * @security jwtToken
             */
            get {
                val (limit, offset) = call.paginationParameters()
                call.respond(
                    HttpStatusCode.OK,
                    shopCategoryController.getCategories(limit, offset)
                )
            }

            /**
             * @tag Shop Category
             * @description Permanently delete a shop category
             * @operationId deleteShopCategory
             * @path id (required) Unique identifier of the category to delete
             * @response 200 Shop category deleted successfully
             * @response 400 Invalid category ID
             * @security jwtToken
             */
            delete("{id}") {
                val id = call.requireParameters("id")
                call.respond(
                    HttpStatusCode.OK,
                    shopCategoryController.deleteCategory(id.first())
                )
            }

            /**
             * @tag Shop Category
             * @description Update an existing shop category name
             * @operationId updateShopCategory
             * @path id (required) Unique identifier of the category to update
             * @query name (required) New name for the category
             * @response 200 Shop category updated successfully
             * @response 400 Invalid category ID or name parameter
             * @security jwtToken
             */
            put("{id}") {
                val params = call.requireParameters("id", "name")
                call.respond(
                    HttpStatusCode.OK,
                    shopCategoryController.updateCategory(params[0], params[1])
                )
            }
        }
    }

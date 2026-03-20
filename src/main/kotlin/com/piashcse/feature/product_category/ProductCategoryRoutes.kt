package com.piashcse.feature.product_category

import com.piashcse.model.request.ProductCategoryRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing product categories.
 *
 * Accessible by customers, sellers, and admins for viewing product categories.
 * Admins have additional permissions to create, update, and delete categories.
 *
 * @param productCategoryController The controller handling product category-related operations.
 */
fun Route.productCategoryRoutes(productCategoryController: ProductCategoryService) {
    route("/product-category") {
        /**
         * @tag ProductCategory
         * @description Retrieve a paginated list of all product categories
         * @operationId getCategories
         * @query limit (required) Maximum number of categories to return
         * @response 200 Product categories retrieved successfully
         * @response 400 Invalid limit parameter
         */
        get {
            val (limit) = call.requiredParameters("limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    productCategoryController.getCategories(
                        limit.toInt()
                    ), HttpStatusCode.OK
                )
            )
        }
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag ProductCategory
             * @description Create a new product category
             * @operationId createCategory
             * @body ProductCategoryRequest Category creation request with name
             * @response 200 Product category created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ProductCategoryRequest>()
                call.respond(
                    ApiResponse.success(
                        productCategoryController.createCategory(
                            requestBody.name
                        ), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag ProductCategory
             * @description Update an existing product category name
             * @operationId updateCategory
             * @path id (required) Unique identifier of the category to update
             * @query name (required) New name for the category
             * @response 200 Product category updated successfully
             * @response 400 Invalid category ID or name parameter
             * @security jwtToken
             */
            put("{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                call.respond(
                    ApiResponse.success(
                        productCategoryController.updateCategory(
                            id, name
                        ), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag ProductCategory
             * @description Permanently delete a product category
             * @operationId deleteCategory
             * @path id (required) Unique identifier of the category to delete
             * @response 200 Product category deleted successfully
             * @response 400 Invalid category ID
             * @security jwtToken
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        productCategoryController.deleteCategory(
                            id
                        ), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}
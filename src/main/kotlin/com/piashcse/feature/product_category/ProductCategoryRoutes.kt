package com.piashcse.feature.product_category

import com.piashcse.model.request.ProductCategoryRequest
import com.piashcse.plugin.*
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
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
    /**
         * @tag ProductCategory
         * @description Retrieve a paginated list of all product categories
         * @operationId getCategories
         * @query limit Maximum number of categories to return (default 20)
         * @query offset Number of categories to skip (default 0)
         * @response 200 Product categories retrieved successfully
         * @response 400 Invalid limit parameter
         */
        get {
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                productCategoryController.getCategories(limit, offset)
            )
        }
        adminAuth {
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
                    HttpStatusCode.OK,
                    productCategoryController.createCategory(
                        requestBody.name
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
                val params = call.requireParameters("id", "name")
                call.respond(
                    HttpStatusCode.OK,
                    productCategoryController.updateCategory(
                        params[0], params[1]
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
                val id = call.requireParameters("id")
                call.respond(
                    HttpStatusCode.OK,
                    productCategoryController.deleteCategory(
                        id.first()
                    )
                )
            }
        }
    }

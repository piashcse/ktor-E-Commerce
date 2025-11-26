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

    /**
     * GET request to retrieve product categories.
     *
     * @tag Product Category
     * @query limit The number of categories to return.
     * @response 200 [ApiResponse] Success response with product categories
     * @response 400 Bad request if limit is missing
     */
    get("/product-category") {
        val (limit) = call.requiredParameters("limit") ?: return@get
        call.respond(
            ApiResponse.success(
                productCategoryController.getCategories(
                    limit.toInt()
                ), HttpStatusCode.OK
            )
        )
    }
    // Routes for admins to create, update, and delete product categories
    authenticate(RoleManagement.ADMIN.role) {
        /**
         * POST request to create a new product category.
         *
         * @tag Product Category
         * @summary auth[admin]
         * @body [ProductCategoryRequest] The details of the category to create, including the category name.
         * @response 200 [ApiResponse] Success response after creation
         */
        post("/product-category") {
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
         * PUT request to update an existing product category by ID.
         *
         * @tag Product Category
         * @summary auth[admin]
         * @path id The ID of the category to update.
         * @query name The new name for the category.
         * @response 200 [ApiResponse] Success response after update
         * @response 400 Bad request if required parameters are missing
         */
        put("/product-category/{id}") {
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
         * DELETE request to remove a product category by ID.
         *
         * @tag Product Category
         * @summary auth[admin]
         * @path id The ID of the category to delete.
         * @response 200 [ApiResponse] Success response after deletion
         * @response 400 Bad request if id is missing
         */
        delete("/product-category/{id}") {
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
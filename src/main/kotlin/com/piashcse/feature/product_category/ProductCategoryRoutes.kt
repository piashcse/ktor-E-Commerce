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
         * @query limit (required)
         * @response 200 [Response]
         * @response 400
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
        // Routes for admins to create, update, and delete product categories
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag ProductCategory
             * @body [ProductCategoryRequest]
             * @response 200 [Response]
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
             * @path id (required)
             * @query name
             * @response 200 [Response]
             * @response 400
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
             * @path id (required)
             * @response 200 [Response]
             * @response 400
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
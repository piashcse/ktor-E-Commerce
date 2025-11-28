package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing product subcategories.
 *
 * Available for customers, sellers, and admins with different levels of access.
 *
 * @param subCategoryController The controller handling product subcategory operations.
 */
fun Route.productSubCategoryRoutes(subCategoryController: ProductSubCategoryService) {
    route("/product-subcategory") {

        /**
         * @tag ProductSubCategory
         * @query categoryId (required)
         * @query limit (required)
         * @response 200 [Response]
         * @response 400
         */
        get {
            val (categoryId, limit) = call.requiredParameters("categoryId", "limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    subCategoryController.getProductSubCategory(categoryId, limit.toInt()), HttpStatusCode.OK
                )
            )
        }

        // Routes for admins to manage product subcategories
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag ProductSubCategory
             * @body [ProductSubCategoryRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<ProductSubCategoryRequest>()
                call.respond(
                    ApiResponse.success(
                        subCategoryController.addProductSubCategory(requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag ProductSubCategory
             * @path id (required)
             * @query name (required)
             * @response 200 [Response]
             * @response 400
             */
            put("{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                call.respond(
                    ApiResponse.success(
                        subCategoryController.updateProductSubCategory(
                            id, name
                        ), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag ProductSubCategory
             * @path id (required)
             * @response 200 [Response]
             * @response 400
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        subCategoryController.deleteProductSubCategory(
                            id
                        ), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}
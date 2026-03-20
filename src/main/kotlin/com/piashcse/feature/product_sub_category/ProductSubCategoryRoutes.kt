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
         * @description Retrieve subcategories for a specific category
         * @operationId getProductSubCategory
         * @query categoryId (required) Unique identifier of the parent category
         * @query limit (required) Maximum number of subcategories to return
         * @response 200 Product subcategories retrieved successfully
         * @response 400 Invalid category ID or limit parameter
         */
        get {
            val (categoryId, limit) = call.requiredParameters("categoryId", "limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    subCategoryController.getProductSubCategory(categoryId, limit.toInt()), HttpStatusCode.OK
                )
            )
        }
        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag ProductSubCategory
             * @description Create a new product subcategory
             * @operationId addProductSubCategory
             * @body ProductSubCategoryRequest Subcategory creation request with name and category ID
             * @response 200 Product subcategory created successfully
             * @security jwtToken
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
             * @description Update an existing product subcategory name
             * @operationId updateProductSubCategory
             * @path id (required) Unique identifier of the subcategory to update
             * @query name (required) New name for the subcategory
             * @response 200 Product subcategory updated successfully
             * @response 400 Invalid subcategory ID or name parameter
             * @security jwtToken
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
             * @description Permanently delete a product subcategory
             * @operationId deleteProductSubCategory
             * @path id (required) Unique identifier of the subcategory to delete
             * @response 200 Product subcategory deleted successfully
             * @response 400 Invalid subcategory ID
             * @security jwtToken
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
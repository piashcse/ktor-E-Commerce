package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
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

        /**
         * @tag ProductSubCategory
         * @description Retrieve subcategories for a specific category with pagination
         * @operationId getProductSubCategory
         * @query categoryId (required) Unique identifier of the parent category
         * @query limit Maximum number of subcategories to return (default 20)
         * @query offset Number of subcategories to skip (default 0)
         * @response 200 Product subcategories retrieved successfully
         * @response 400 Invalid category ID or limit parameter
         */
        get {
            val categoryId = call.parameters["categoryId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "categoryId is required")
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                subCategoryController.getProductSubCategory(categoryId, limit, offset)
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
                    HttpStatusCode.OK,
                    subCategoryController.addProductSubCategory(requestBody)
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
                val params = call.requireParameters("id", "name")
                call.respond(
                    HttpStatusCode.OK,
                    subCategoryController.updateProductSubCategory(
                        params[0], params[1]
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
                val id = call.requireParameters("id")
                call.respond(
                    HttpStatusCode.OK,
                    subCategoryController.deleteProductSubCategory(
                        id.first()
                    )
                )
            }
        }
    }

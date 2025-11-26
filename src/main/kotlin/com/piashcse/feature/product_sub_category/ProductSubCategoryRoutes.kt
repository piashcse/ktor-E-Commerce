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

    /**
     * GET request to retrieve product subcategories by category ID.
     *
     * @tag Product SubCategory
     * @query categoryId The category ID to filter subcategories.
     * @query limit The number of subcategories to retrieve.
     * @response 200 [ApiResponse] Success response with product subcategories
     * @response 400 Bad request if required parameters are missing
     */
    get("/product-subcategory") {
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
         * POST request to create a new product subcategory.
         *
         * @tag Product SubCategory
         * @summary auth[admin]
         * @body [ProductSubCategoryRequest] The details of the product subcategory to create.
         * @response 200 [ApiResponse] Success response after creation
         */
        post("/product-subcategory") {
            val requestBody = call.receive<ProductSubCategoryRequest>()
            call.respond(
                ApiResponse.success(
                    subCategoryController.addProductSubCategory(requestBody), HttpStatusCode.OK
                )
            )
        }

        /**
         * PUT request to update an existing product subcategory by ID.
         *
         * @tag Product SubCategory
         * @summary auth[admin]
         * @path id The ID of the product subcategory to update.
         * @query name The new name for the product subcategory.
         * @response 200 [ApiResponse] Success response after update
         * @response 400 Bad request if required parameters are missing
         */
        put("/product-subcategory/{id}") {
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
         * DELETE request to delete a product subcategory by ID.
         *
         * @tag Product SubCategory
         * @summary auth[admin]
         * @path id The ID of the product subcategory to delete.
         * @response 200 [ApiResponse] Success response after deletion
         * @response 400 Bad request if id is missing
         */
        delete("/product-subcategory/{id}") {
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
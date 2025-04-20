package com.piashcse.modules.productsubcategory.routes

import com.piashcse.modules.productsubcategory.controller.ProductSubCategoryController
import com.piashcse.database.models.subcategory.ProductSubCategoryRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.productSubCategoryRoutes(subCategoryController: ProductSubCategoryController) {

    /**
     * GET request to retrieve product subcategories by category ID.
     *
     * Accessible by customers, sellers, and admins.
     *
     * @param categoryId The category ID to filter subcategories.
     * @param limit The number of subcategories to retrieve.
     */
    get("product-subcategory", {
        tags("Product SubCategory")
        request {
            queryParameter<String>("categoryId") {
                required = true
            }
            queryParameter<String>("limit") {
                required = true
            }
        }
        apiResponse()
    }) {
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
         * Accessible by admins only.
         *
         * @param requestBody The details of the product subcategory to create.
         */
        post("product-subcategory", {
            tags("Product SubCategory")
            summary = "auth[admin]"
            request {
                body<ProductSubCategoryRequest>()
            }
            apiResponse()
        }) {
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
         * Accessible by admins only.
         *
         * @param id The ID of the product subcategory to update.
         * @param name The new name for the product subcategory.
         */
        put("product-subcategory/{id}", {
            tags("Product SubCategory")
            summary = "auth[admin]"
            request {
                pathParameter<String>("id") {
                    required = true
                }
                queryParameter<String>("name") {
                    required = true
                }
            }
            apiResponse()
        }) {
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
         * Accessible by admins only.
         *
         * @param id The ID of the product subcategory to delete.
         */
        delete("product-subcategory/{id}", {
            tags("Product SubCategory")
            summary = "auth[admin]"
            request {
                pathParameter<String>("id") {
                    required = true
                }
            }
            apiResponse()
        }) {
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
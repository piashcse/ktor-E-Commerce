package com.piashcse.modules.productcategory.routes

import com.piashcse.modules.productcategory.controller.ProductCategoryController
import com.piashcse.database.models.category.ProductCategoryRequest
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
 * Defines routes for managing product categories.
 *
 * Accessible by customers, sellers, and admins for viewing product categories.
 * Admins have additional permissions to create, update, and delete categories.
 *
 * @param productCategoryController The controller handling product category-related operations.
 */
fun Route.productCategoryRoutes(productCategoryController: ProductCategoryController) {

    /**
     * GET request to retrieve product categories.
     *
     * Accessible by customers, sellers, and admins.
     *
     * @param limit The number of categories to return.
     */
    get("product-category", {
        tags("Product Category")
        request {
            queryParameter<String>("limit") {
                required = true
            }
        }
        apiResponse()
    }) {
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
         * Accessible by admins only.
         *
         * @param requestBody The details of the category to create, including the category name.
         */
        post("product-category", {
            tags("Product Category")
            summary = "auth[customer]"
            request {
                body<ProductCategoryRequest>()
            }
            apiResponse()
        }) {
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
         * Accessible by admins only.
         *
         * @param id The ID of the category to update.
         * @param name The new name for the category.
         */
        put("product-category/{id}", {
            tags("Product Category")
            summary = "auth[customer]"
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
                    productCategoryController.updateCategory(
                        id, name
                    ), HttpStatusCode.OK
                )
            )
        }

        /**
         * DELETE request to remove a product category by ID.
         *
         * Accessible by admins only.
         *
         * @param id The ID of the category to delete.
         */
        delete("product-category/{id}", {
            tags("Product Category")
            summary = "auth[customer]"
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
                    productCategoryController.deleteCategory(
                        id
                    ), HttpStatusCode.OK
                )
            )
        }
    }
}
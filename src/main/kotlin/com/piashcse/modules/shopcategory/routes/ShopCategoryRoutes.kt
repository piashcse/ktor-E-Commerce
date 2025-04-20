package com.piashcse.modules.shopcategory.routes

import com.piashcse.modules.shopcategory.controller.ShopCategoryController
import com.piashcse.database.models.shop.ShopCategoryRequest
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
 * Defines routes for managing shop categories.
 *
 * Allows admin users to create, retrieve, update, and delete shop categories.
 *
 * @param shopCategoryController The controller responsible for handling shop category operations.
 */
fun Route.shopCategoryRoutes(shopCategoryController: ShopCategoryController) {
    authenticate(RoleManagement.ADMIN.role) {

        /**
         * POST request to create a new shop category.
         *
         * Accessible by **admin** only.
         *
         * @param requestBody The name of the new shop category.
         */
        post("shop-category", {
            tags("Shop Category")
            summary = "auth[admin]"
            request {
                body<ShopCategoryRequest>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<ShopCategoryRequest>()
            call.respond(
                ApiResponse.success(
                    shopCategoryController.createCategory(requestBody.name), HttpStatusCode.OK
                )
            )
        }

        /**
         * GET request to retrieve a list of shop categories with a specified limit.
         *
         * Accessible by **admin** only.
         *
         * @param limit The maximum number of shop categories to retrieve.
         */
        get("shop-category", {
            tags("Shop Category")
            summary = "auth[admin]"
            request {
                queryParameter<Int>("limit") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (limit) = call.requiredParameters("limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    shopCategoryController.getCategories(limit.toInt()),
                    HttpStatusCode.OK
                )
            )
        }

        /**
         * DELETE request to remove a shop category by its ID.
         *
         * Accessible by **admin** only.
         *
         * @param id The ID of the shop category to delete.
         */
        delete("shop-category/{id}", {
            tags("Shop Category")
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
                    shopCategoryController.deleteCategory(id), HttpStatusCode.OK
                )
            )
        }

        /**
         * PUT request to update the name of an existing shop category.
         *
         * Accessible by **admin** only.
         *
         * @param id The ID of the shop category to update.
         * @param name The new name for the shop category.
         */
        put("shop-category/{id}", {
            tags("Shop Category")
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
                    shopCategoryController.updateCategory(id, name), HttpStatusCode.OK
                )
            )
        }
    }
}
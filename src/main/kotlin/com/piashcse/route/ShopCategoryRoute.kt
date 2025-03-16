package com.piashcse.route

import com.piashcse.controller.ShopCategoryController
import com.piashcse.models.shop.AddShopCategory
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

fun Route.shopCategoryRoute(shopCategoryController: ShopCategoryController) {
    authenticate(RoleManagement.ADMIN.role) {
        post("shop-category", {
            tags("Shop Category")
            request {
                body<AddShopCategory>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<AddShopCategory>()
            call.respond(
                ApiResponse.success(
                    shopCategoryController.addShopCategory(requestBody.name), HttpStatusCode.OK
                )
            )
        }
        get("shop-category", {
            tags("Shop Category")
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
                    shopCategoryController.getShopCategories(
                        limit.toInt()
                    ), HttpStatusCode.OK
                )
            )
        }
        delete("shop-category/{id}", {
            tags("Shop Category")
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
                    shopCategoryController.deleteShopCategory(id), HttpStatusCode.OK
                )
            )
        }
        put("shop-category/{id}", {
            tags("Shop Category")
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
                    shopCategoryController.updateShopCategory(id, name), HttpStatusCode.OK
                )
            )
        }
    }
}
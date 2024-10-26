package com.piashcse.route

import com.piashcse.controller.ShopCategoryController
import com.piashcse.models.shop.AddShopCategory
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
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
                queryParameter<Long>("offset") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val requiredParams = listOf("limit", "offset")
            requiredParams.filterNot { call.parameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (limit, offset) = requiredParams.map { call.parameters[it]!! }
            call.respond(
                ApiResponse.success(
                    shopCategoryController.getShopCategories(
                        limit.toInt(), offset.toLong()
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
            val requiredParams = listOf("id")
            requiredParams.filterNot { call.parameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (id) = requiredParams.map { call.parameters[it]!! }
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
            val requiredParams = listOf("id", "name")
            requiredParams.filterNot { call.parameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (id, name) = requiredParams.map { call.parameters[it]!! }

            call.respond(
                ApiResponse.success(
                    shopCategoryController.updateShopCategory(id, name), HttpStatusCode.OK
                )
            )
        }
    }
}
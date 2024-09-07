package com.piashcse.route

import com.piashcse.controller.ShopController
import com.piashcse.models.shop.*
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shopRoute(shopController: ShopController) {
    authenticate(RoleManagement.ADMIN.role) {
        post("shop-category", {
            tags("Shop")
            request {
                body<AddShopCategory>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<AddShopCategory>()
            call.respond(
                ApiResponse.success(
                    shopController.addShopCategory(requestBody.name), HttpStatusCode.OK
                )
            )
        }
        get("shop-category", {
            tags("Shop")
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
            requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (limit, offset) = requiredParams.map { call.parameters[it]!! }
            call.respond(
                ApiResponse.success(
                    shopController.getShopCategories(
                        limit.toInt(), offset.toLong()
                    ), HttpStatusCode.OK
                )
            )
        }
        delete("shop-category/{id}", {
            tags("Shop")
            request {
                pathParameter<String>("id") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val shopCategoryId = call.parameters["id"]!!
            call.respond(
                ApiResponse.success(
                    shopController.deleteShopCategory(shopCategoryId), HttpStatusCode.OK
                )
            )
        }
        put("shop-category/{id}", {
            tags("Shop")
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
            val shopCategoryId = call.parameters["id"]!!
            val shopCategoryName = call.parameters["name"]!!

            call.respond(
                ApiResponse.success(
                    shopController.updateShopCategory(shopCategoryId, shopCategoryName), HttpStatusCode.OK
                )
            )
            shopController.updateShopCategory(
                shopCategoryId, shopCategoryName
            ).let {
                call.respond(ApiResponse.success(it, HttpStatusCode.OK))
            }
        }
    }
    authenticate(RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
        post("shop", {
            tags("Shop")
            request {
                body<AddShop>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<AddShop>()
            requestBody.validation()
            shopController.createShop(
                getCurrentUser().userId, requestBody.shopCategoryId, requestBody.shopName
            ).let {
                call.respond(ApiResponse.success(it, HttpStatusCode.OK))
            }
        }
    }

}
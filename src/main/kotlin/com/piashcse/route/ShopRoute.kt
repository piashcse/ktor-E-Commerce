package com.piashcse.route

import com.piashcse.controller.ShopController
import com.piashcse.models.shop.AddShop
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shopRoute(shopController: ShopController) {
    route("shop") {
        authenticate(RoleManagement.ADMIN.role) {
            post({
                tags("Shop")
                request {
                    body<AddShop>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddShop>()
                requestBody.validation()
                shopController.addShop(
                    call.currentUser().userId, requestBody.shopCategoryId, requestBody.shopName
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            get({
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
                val (limit, offset) = call.requiredParameters("limit", "offset") ?: return@get
                shopController.getShop(
                    call.currentUser().userId, limit.toInt(), offset.toLong()
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            put("{id}", {
                tags("Shop")
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("shopName") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id, shopName) = call.requiredParameters("id", "shopName") ?: return@put
                shopController.updateShop(
                    call.currentUser().userId, id, shopName
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            delete("{id}", {
                tags("Shop")
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (id) = call.requiredParameters("id") ?: return@delete
                shopController.deleteShop(
                    call.currentUser().userId, id
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
    }
}
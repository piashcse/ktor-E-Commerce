package com.piashcse.route

import com.piashcse.controller.ShopController
import com.piashcse.models.shop.AddShop
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.application.*
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
                    getCurrentUser().userId, requestBody.shopCategoryId, requestBody.shopName
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            get({
                tags("Shop")
                request {
                    queryParameter<Int>("limit"){
                        required = true
                    }
                    queryParameter<Long>("offset"){
                        required = true
                    }
                }
                apiResponse()
            }) {
                val limit = call.parameters["limit"]!!
                val offset = call.parameters["offset"]!!
                shopController.getShop(
                    getCurrentUser().userId, limit.toInt(), offset.toLong()
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            put("{id}", {
                tags("Shop")
                request {
                    pathParameter<String>("id"){
                        required = true
                    }
                    queryParameter<String>("shopName"){
                        required = true
                    }
                }
                apiResponse()
            }) {
                val shopId = call.parameters["id"]!!
                val shopName = call.parameters["shopName"]!!
                shopController.updateShop(
                    getCurrentUser().userId, shopId, shopName
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
            delete("{id}", {
                tags("Shop")
                request {
                    pathParameter<String>("id"){
                        required = true
                    }
                }
                apiResponse()
            }) {
                val shopId = call.parameters["id"]!!
                shopController.deleteShop(
                    getCurrentUser().userId, shopId
                ).let {
                    call.respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
    }
}
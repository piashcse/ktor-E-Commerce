package com.piashcse.route

import com.piashcse.controller.WishListController
import com.piashcse.models.AddWisList
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.wishListRoute(wishlistController: WishListController) {
    route("wishlist") {
        authenticate(RoleManagement.CUSTOMER.role) {
            post({
                tags("Wish List")
                request {
                    body<AddWisList>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddWisList>()
                call.respond(
                    ApiResponse.success(
                        wishlistController.addToWishList(call.getCurrentUser().userId, requestBody.productId), HttpStatusCode.OK
                    )
                )
            }
            get({
                tags("Wish List")
                request {
                    queryParameter<String>("limit") {
                        required = true
                    }
                    queryParameter<String>("offset") {
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
                        wishlistController.getWishList(call.getCurrentUser().userId, limit.toInt(), offset.toLong()), HttpStatusCode.OK
                    )
                )
            }
            delete({
                tags("Wish List")
                request {
                    queryParameter<String>("productId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("productId")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (productId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        wishlistController.deleteWishList(call.getCurrentUser().userId, productId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}
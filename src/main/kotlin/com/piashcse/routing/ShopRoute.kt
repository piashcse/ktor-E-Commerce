package com.piashcse.routing

import com.piashcse.controller.ShopController
import com.piashcse.models.shop.*
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.*
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.shopRoute(shopController: ShopController) {
    route("shop/") {
        authenticateWithJwt(RoleManagement.ADMIN.role) {
            route("category").post<Unit, Response, AddShopCategory, JwtTokenBody> { _, requestBody ->
                requestBody.validation()
                respond(
                    ApiResponse.success(
                        shopController.createShopCategory(requestBody.shopCategoryName), HttpStatusCode.OK
                    )
                )
            }
            route("category").get<ShopCategory, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        shopController.getShopCategories(
                            params.limit, params.offset
                        ), HttpStatusCode.OK
                    )
                )
            }
            route("category").delete<DeleteShopCategory, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        shopController.deleteShopCategory(params.shopCategoryId), HttpStatusCode.OK
                    )
                )
            }
            route("category").put<Unit, Response, UpdateShopCategory, JwtTokenBody> { _, requestBody ->
                requestBody.validation()
                shopController.updateShopCategory(
                    requestBody.shopCategoryId, requestBody.shopCategoryName
                ).let {
                    respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
        authenticateWithJwt(RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            route("add-shop").post<Unit, Response, AddShop, JwtTokenBody> { _, requestBody ->
                val jwtTokenToUserData = principal()
                requestBody.validation()
                shopController.createShop(
                    jwtTokenToUserData.userId, requestBody.shopCategoryId, requestBody.shopName
                ).let {
                    respond(ApiResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
    }
}
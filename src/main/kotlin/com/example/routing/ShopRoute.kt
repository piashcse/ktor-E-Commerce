package com.example.routing

import com.example.controller.ShopController
import com.example.models.shop.AddShopCategory
import com.example.models.shop.AddShop
import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.example.utils.nullProperties
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.shopRoute(shopController: ShopController) {
    route("shop/") {
        authenticate(AppConstants.RoleManagement.ADMIN) {
            post("add-shop-category") {
                val addShopCategory = call.receive<AddShopCategory>()
                nullProperties(addShopCategory) {
                    if (it.isNotEmpty()) {
                        throw MissingRequestParameterException(it.toString())
                    }
                }
                val db = shopController.createShopCategory(addShopCategory.shopCategoryName)
                db.let {
                    call.respond(JsonResponse.success(db, HttpStatusCode.OK))
                }
            }
        }
        authenticate(AppConstants.RoleManagement.MERCHANT) {
            post("add-shop") {
                val jwtTokenToUserData = call.principal<JwtTokenBody>()
                val addShop = call.receive<AddShop>()
                nullProperties(addShop) {
                    if (it.isNotEmpty()) {
                        throw MissingRequestParameterException(it.toString())
                    }
                }
                val db = shopController.createShop(
                    jwtTokenToUserData!!.userId, addShop.shopCategoryId, addShop.shopName
                )
                db.let {
                    call.respond(JsonResponse.success(db, HttpStatusCode.OK))
                }
            }
        }
    }
}
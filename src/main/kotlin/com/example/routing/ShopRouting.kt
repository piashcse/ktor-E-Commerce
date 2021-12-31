package com.example.routing

import com.example.controller.ShopController
import com.example.models.shop.*
import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.example.utils.extension.nullProperties
import com.example.utils.CustomResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shopRoute(shopController: ShopController) {
    route("shop/") {
        authenticate(AppConstants.RoleManagement.ADMIN) {
            post("add-shop-category") {
                val addShopCategory = call.receive<AddShopCategory>()
                addShopCategory.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.createShopCategory(addShopCategory.shopCategoryName)
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }
            post("shop-categories") {
                val shopCategories = call.receive<GetShopCategory>()
                shopCategories.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.getShopCategories(shopCategories.offset, shopCategories.limit)
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }
            delete("delete-shop-category") {
                val deleteShopCategory = call.receive<DeleteShopCategory>()
                deleteShopCategory.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.deleteShopCategory(deleteShopCategory.shopCategoryId)
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }
            put("update-shop-category") {
                val updateShopCategory = call.receive<UpdateShopCategory>()
                updateShopCategory.nullProperties{
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.updateShopCategory(
                    updateShopCategory.shopCategoryId, updateShopCategory.shopCategoryName
                )
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }
        }
        authenticate(AppConstants.RoleManagement.MERCHANT) {
            post("add-shop") {
                val jwtTokenToUserData = call.principal<JwtTokenBody>()
                val addShop = call.receive<AddShop>()
                addShop.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.createShop(
                    jwtTokenToUserData!!.userId, addShop.shopCategoryId, addShop.shopName
                )
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }
        }
    }
}
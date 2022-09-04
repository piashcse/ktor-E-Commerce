package com.example.routing

import com.example.controller.ShopController
import com.example.models.shop.*
import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.example.utils.extension.nullProperties
import com.example.utils.CustomResponse
import com.example.utils.Response
import com.example.utils.extension.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.*
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.plugins.*

fun NormalOpenAPIRoute.shopRoute(shopController: ShopController) {
    route("shop/") {
        authenticateWithJwt(AppConstants.RoleManagement.ADMIN) {
            route("category").post<Unit, Response, AddShopCategory, JwtTokenBody> { response, addShopCategory ->
                addShopCategory.validation()
                respond(
                    CustomResponse.success(
                        shopController.createShopCategory(addShopCategory.shopCategoryName), HttpStatusCode.OK
                    )
                )
            }

            route("category").get<GetShopCategory, Response, JwtTokenBody> { shopCategories ->
                shopCategories.validation()
                respond(
                    CustomResponse.success(
                        shopController.getShopCategories(
                            shopCategories.offset, shopCategories.limit
                        ), HttpStatusCode.OK
                    )
                )
            }

            route("category").delete<DeleteShopCategory, Response, JwtTokenBody> { deleteShopCategory ->
                deleteShopCategory.validation()
                respond(
                    CustomResponse.success(
                        shopController.deleteShopCategory(deleteShopCategory.shopCategoryId), HttpStatusCode.OK
                    )
                )
            }

            route("category").put<Unit, Response, UpdateShopCategory, JwtTokenBody> { response, updateShopCategory ->
                updateShopCategory.validation()
                shopController.updateShopCategory(
                    updateShopCategory.shopCategoryId, updateShopCategory.shopCategoryName
                ).let {
                    respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
        authenticateWithJwt(AppConstants.RoleManagement.MERCHANT, AppConstants.RoleManagement.ADMIN) {
            // shop
            post<Unit, Response, AddShop, JwtTokenBody> { response, addShop ->
                val jwtTokenToUserData = principal()
                addShop.validation()
                shopController.createShop(
                    jwtTokenToUserData!!.userId, addShop.shopCategoryId, addShop.shopName
                ).let {
                    respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
    }

    /* route("shop/") {
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
         //
         authenticate(AppConstants.RoleManagement.MERCHANT, AppConstants.RoleManagement.ADMIN) {
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
     }*/
}
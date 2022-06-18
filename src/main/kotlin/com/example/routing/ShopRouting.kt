package com.example.routing

import com.example.controller.ShopController
import com.example.models.shop.*
import com.example.models.user.JwtTokenBody
import com.example.models.user.RegistrationBody
import com.example.utils.AppConstants
import com.example.utils.extension.nullProperties
import com.example.utils.CustomResponse
import com.example.utils.Response
import com.example.utils.extension.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.path.normal.route
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun NormalOpenAPIRoute.shopRoute(shopController: ShopController) {
    route("shop/") {
        authenticateWithJwt(AppConstants.RoleManagement.ADMIN) {
            route("add-shop-category").post<Unit, Response, AddShopCategory, JwtTokenBody> { response, addShopCategory ->
                addShopCategory.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.createShopCategory(addShopCategory.shopCategoryName)
                db.let {
                    respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }

            route("shop-categories").post<Unit, Response, GetShopCategory, JwtTokenBody> { response, shopCategories ->
                shopCategories.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.getShopCategories(shopCategories.offset, shopCategories.limit)
                db.let {
                    respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }

            route("delete-shop-category").post<Unit, Response, DeleteShopCategory, JwtTokenBody> { response, deleteShopCategory ->
                deleteShopCategory.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.deleteShopCategory(deleteShopCategory.shopCategoryId)
                db.let {
                    respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }

            route("update-shop-category").post<Unit, Response, UpdateShopCategory, JwtTokenBody> { response, updateShopCategory ->
                updateShopCategory.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.updateShopCategory(
                    updateShopCategory.shopCategoryId, updateShopCategory.shopCategoryName
                )
                db.let {
                    respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }
        }
        authenticateWithJwt(AppConstants.RoleManagement.MERCHANT, AppConstants.RoleManagement.ADMIN) {
            route("add-shop").post<Unit, Response, AddShop, JwtTokenBody> { response, addShop ->
                val jwtTokenToUserData = principal()
                //val addShop = call.receive<AddShop>()
                addShop.nullProperties {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = shopController.createShop(
                    jwtTokenToUserData!!.userId, addShop.shopCategoryId, addShop.shopName
                )
                db.let {
                    respond(CustomResponse.success(db, HttpStatusCode.OK))
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
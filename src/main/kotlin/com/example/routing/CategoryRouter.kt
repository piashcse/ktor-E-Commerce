package com.example.routing

import com.example.controller.CategoryController
import com.example.models.category.AddCategoryBody
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

fun Route.categoryRouter(categoryController:CategoryController) {
    route("category/"){
        authenticate(AppConstants.RoleManagement.ADMIN) {
            post("add-category") {
                val jwtTokenToUserData = call.principal<JwtTokenBody>()
                val addCategory = call.receive<AddCategoryBody>()
                nullProperties(addCategory) {
                    if (it.isNotEmpty()) {
                        throw MissingRequestParameterException(it.toString())
                    }
                }
                val db = categoryController.createProductCategory(addCategory.categoryName)
                db.let {
                    call.respond(JsonResponse.success(db, HttpStatusCode.OK))
                }
            }
        }
    }
}
package com.example.routing

import com.example.controller.CategoryController
import com.example.models.category.AddCategoryBody
import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.example.utils.extension.nullProperties
import com.example.utils.JsonResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
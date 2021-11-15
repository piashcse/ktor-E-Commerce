package com.example.routing

import com.example.controller.CategoryController
import com.example.models.AddCategoryBody
import com.example.utils.AppConstants
import com.example.utils.UserTypeException
import com.example.utils.nullProperties
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.http.auth.AuthenticationException

fun Route.categoryRouter(categoryController:CategoryController) {
    route("category/"){
        authenticate() {
            post("add-category") {
                val addCategory = call.receive<AddCategoryBody>()
                nullProperties(addCategory) {
                    if (it.isNotEmpty()) {
                        throw MissingRequestParameterException(it.toString())
                    }
                }
                if (!AppConstants.ALL_USERS_TYPE.contains(addCategory.userType)) {
                    throw UserTypeException()
                }
                val db = categoryController.insertCategory(addCategory.userType, addCategory.categoryName)
                db?.let {
                    call.respond(JsonResponse.success(it, HttpStatusCode.OK))
                }?:run {
                    throw AuthenticationException()
                }
            }
        }
    }
}
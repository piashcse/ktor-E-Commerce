package com.example.routing

import com.example.controller.CategoryController
import com.example.models.PagingData
import com.example.models.category.AddCategory
import com.example.models.category.DeleteCategory
import com.example.models.category.UpdateCategory
import com.example.models.user.body.JwtTokenBody
import com.example.plugins.RoleManagement
import com.example.utils.ApiResponse
import com.example.utils.Response
import com.example.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.delete
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.*
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.categoryRoute(categoryController: CategoryController) {
    route("category") {
        authenticateWithJwt(RoleManagement.SELLER.role) {
            post<Unit, Response, AddCategory, JwtTokenBody>(
                exampleRequest = AddCategory(
                    categoryName = "Mens Cloth"
                )
            ) { _, addCategoryBody ->
                addCategoryBody.validation()
                respond(ApiResponse.success(categoryController.createCategory(addCategoryBody), HttpStatusCode.OK))
            }
            get<PagingData, Response, JwtTokenBody> { pagingData ->
                pagingData.validation()
                respond(ApiResponse.success(categoryController.getCategory(pagingData), HttpStatusCode.OK))
            }
            put<UpdateCategory, Response, Unit, JwtTokenBody> { categoryParams, _ ->
                categoryParams.validation()
                respond(ApiResponse.success(categoryController.updateCategory(categoryParams), HttpStatusCode.OK))
            }
            delete<DeleteCategory, Response, JwtTokenBody> { categoryParams ->
                categoryParams.validation()
                respond(
                    ApiResponse.success(
                        categoryController.deleteCategory(categoryParams), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}